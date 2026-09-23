package io.github.altenhofen.pen.calibration

import io.github.altenhofen.pen.ime.Stroke
import io.github.altenhofen.pen.recognition.AdaptiveRecognizer
import io.github.altenhofen.pen.recognition.ClusterRow
import io.github.altenhofen.pen.recognition.PrototypeDao
import io.github.altenhofen.pen.recognition.PrototypeStore
import io.github.altenhofen.pen.recognition.seedStrokes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationSessionTest {
    @Test
    fun nextNeedsAtLeastOneSample() {
        val session = CalibrationSession.begin(listOf('A', 'B'))
        assertFalse(session.canAdvance)
        assertEquals(CalibrationEvent.Ignored, session.advanceToNextLabel())
        assertEquals(CalibrationEvent.Recorded, session.recordInk(strokesFor('A')))
        assertEquals(1, session.sampleCount)
        assertEquals('A', session.currentLabel)
    }

    @Test
    fun variableSamplesStayOnLabelUntilNext() {
        val session = CalibrationSession.begin(listOf('A', 'B'))
        repeat(4) {
            session.recordInk(strokesFor('A', jitter = it * 0.05f))
        }
        assertEquals(4, session.sampleCount)
        assertEquals('A', session.currentLabel)
        assertEquals(CalibrationEvent.Recorded, session.advanceToNextLabel())
        assertEquals('B', session.currentLabel)
        assertEquals(0, session.sampleCount)
    }

    @Test
    fun lastNextIsReadyToCommit() {
        val session = CalibrationSession.begin(listOf('A', 'B'))
        session.recordInk(strokesFor('A'))
        session.advanceToNextLabel()
        session.recordInk(strokesFor('B'))
        assertEquals(CalibrationEvent.ReadyToCommit, session.advanceToNextLabel())
        assertEquals(null, session.currentLabel)
    }

    @Test
    fun payloadKeepsOneClusterPerExperiment() {
        val session = CalibrationSession.begin(listOf('A'))
        session.recordInk(strokesFor('A', jitter = 0.05f))
        session.recordInk(strokesFor('A', jitter = 0.12f))
        session.advanceToNextLabel()
        assertEquals(
            listOf("train:A:0", "train:A:1"),
            session.payload().clusters.map { it.id.value },
        )
    }

    @Test
    fun secondCommitReplacesPriorExperiments() {
        val first = CalibrationSession.begin(listOf('A'))
        repeat(3) { first.recordInk(strokesFor('A', jitter = (it + 1) * 0.05f)) }
        first.advanceToNextLabel()

        val dao = InMemoryPrototypeDao()
        val recognizer = AdaptiveRecognizer(PrototypeStore(dao))
        recognizer.commitTraining(first.payload())
        assertEquals(listOf("train:A:0", "train:A:1", "train:A:2"), dao.trainingIds())

        val second = CalibrationSession.begin(listOf('A'))
        second.recordInk(strokesFor('A', jitter = 0.2f))
        second.advanceToNextLabel()
        recognizer.commitTraining(second.payload())
        assertEquals(listOf("train:A:0"), dao.trainingIds())
        assertEquals(37, dao.all().size)
    }

    @Test
    fun partitionSplitsSideBySideInk() {
        val left = offset(strokesFor('0'), dx = 10f)
        val right = offset(strokesFor('0'), dx = 400f)
        val parts = partitionGlyphs(left + right)
        assertEquals(2, parts.size)
    }
}

class SelectedGlyphsTest {
    @Test
    fun emptySetIsNull() {
        assertEquals(null, SelectedGlyphs.of(emptySet()))
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

private fun strokesFor(label: Char, jitter: Float = 0f) =
    seedStrokes(if (label.isUpperCase()) label.lowercaseChar() else label, jitter)

private fun offset(strokes: List<Stroke>, dx: Float): List<Stroke> =
    strokes.map { source ->
        Stroke().also { copy ->
            source.points().forEach { copy.append(it.x + dx, it.y) }
        }
    }

private class InMemoryPrototypeDao : PrototypeDao() {
    private val rows = LinkedHashMap<String, ClusterRow>()

    override fun all(): List<ClusterRow> = rows.values.toList()

    fun trainingIds(): List<String> = rows.keys.filter { it.startsWith("train:") }.sorted()

    override fun find(clusterId: String): ClusterRow? = rows[clusterId]

    override fun upsert(rows: List<ClusterRow>) {
        rows.forEach { this.rows[it.clusterId] = it }
    }

    override fun deleteAll() {
        rows.clear()
    }

    override fun deleteMatching(exact: String, like: String) {
        val prefix = like.removeSuffix("%")
        rows.keys.filter { it == exact || it.startsWith(prefix) }.forEach { rows.remove(it) }
    }
}
