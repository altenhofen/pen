package io.github.altenhofen.pen.calibration

import io.github.altenhofen.pen.recognition.AdaptiveRecognizer
import io.github.altenhofen.pen.recognition.ClusterRow
import io.github.altenhofen.pen.recognition.PrototypeDao
import io.github.altenhofen.pen.recognition.PrototypeStore
import io.github.altenhofen.pen.recognition.seedStrokes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationSessionTest {
    @Test
    fun threeSamplesHoldLabelUntilAdvance() {
        val session = CalibrationSession.begin(listOf('A', 'B'))
        repeat(3) {
            assertEquals('A', session.currentLabel)
            assertEquals(
                CalibrationEvent.NeedMoreSamples,
                session.recordSettled(strokesFor('A', jitter = it * 0.05f)),
            )
        }
        assertEquals('A', session.currentLabel)
        assertTrue(session.awaitingAdvance)
        assertEquals(CalibrationEvent.Ignored, session.recordSettled(strokesFor('A')))
        assertEquals('A', session.currentLabel)
        assertEquals(CalibrationEvent.NeedMoreSamples, session.advanceToNextLabel())
        assertEquals('B', session.currentLabel)
    }

    @Test
    fun lastAdvanceIsReadyToCommit() {
        val session = CalibrationSession.begin(listOf('A', 'B'))
        fill(session, 'A')
        session.advanceToNextLabel()
        fill(session, 'B')
        assertEquals(CalibrationEvent.ReadyToCommit, session.advanceToNextLabel())
        assertEquals(null, session.currentLabel)
    }

    @Test
    fun payloadIsOneMedoidClusterPerLabel() {
        val session = CalibrationSession.begin(listOf('A', 'B'))
        fill(session, 'A')
        session.advanceToNextLabel()
        fill(session, 'B')
        session.advanceToNextLabel()
        assertEquals(
            listOf("train:A", "train:B"),
            session.payload().clusters.map { it.id.value },
        )
        assertEquals(listOf('A', 'B'), session.payload().clusters.map { it.label })
    }

    @Test
    fun secondCommitUpsertsSameTrainingIds() {
        val first = CalibrationSession.begin(listOf('A'))
        fill(first, 'A')
        first.advanceToNextLabel()

        val dao = InMemoryPrototypeDao()
        val recognizer = AdaptiveRecognizer(PrototypeStore(dao))
        recognizer.commitTraining(first.payload())

        assertEquals(listOf("train:A"), dao.trainingIds())
        assertEquals(37, dao.all().size)

        val second = CalibrationSession.begin(listOf('A'))
        fill(second, 'A', jitterBase = 0.2f)
        second.advanceToNextLabel()
        recognizer.commitTraining(second.payload())

        assertEquals(listOf("train:A"), dao.trainingIds())
        assertEquals(37, dao.all().size)
    }
}

class SelectedGlyphsTest {
    @Test
    fun emptySetIsNull() {
        assertEquals(null, SelectedGlyphs.of(emptySet()))
    }

    @Test
    fun singleGlyphKeepsThatLabel() {
        assertEquals(listOf('8'), SelectedGlyphs.of(setOf('8'))?.labels)
    }

    @Test
    fun mixedSetFollowsAllOrder() {
        assertEquals(listOf('0', 'A', 'z'), SelectedGlyphs.of(setOf('z', '0', 'A'))?.labels)
    }
}

class CalibrationMinigameTest {
    @Test
    fun startWithEmptySelectionStaysPicking() {
        val game = CalibrationMinigame()
        game.start()
        assertTrue(game.phase() is GlyphCalibrationPhase.PickGlyphs)
    }
}

private fun fill(session: CalibrationSession, label: Char, jitterBase: Float = 0.05f) {
    repeat(session.samplesPerLabel) {
        session.recordSettled(strokesFor(label, jitter = (it + 1) * jitterBase))
    }
}

private fun strokesFor(label: Char, jitter: Float = 0f) =
    seedStrokes(if (label.isUpperCase()) label.lowercaseChar() else label, jitter)

private class InMemoryPrototypeDao : PrototypeDao() {
    private val rows = LinkedHashMap<String, ClusterRow>()

    override fun all(): List<ClusterRow> = rows.values.toList()

    fun trainingIds(): List<String> = rows.keys.filter { it.startsWith("train:") }

    override fun find(clusterId: String): ClusterRow? = rows[clusterId]

    override fun upsert(rows: List<ClusterRow>) {
        rows.forEach { this.rows[it.clusterId] = it }
    }

    override fun deleteAll() {
        rows.clear()
    }
}
