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
        assertTrue(session.advanceToNextLabel() is CalibrationEvent.Advanced)
        assertEquals('B', session.currentLabel)
        assertEquals(0, session.sampleCount)
    }

    @Test
    fun lastNextIsReadyToCommit() {
        val session = CalibrationSession.begin(listOf('A', 'B'))
        session.recordInk(strokesFor('A'))
        assertTrue(session.advanceToNextLabel() is CalibrationEvent.Advanced)
        session.recordInk(strokesFor('B'))
        assertTrue(session.advanceToNextLabel() is CalibrationEvent.ReadyToCommit)
        assertEquals(null, session.currentLabel)
    }

    @Test
    fun eachNextCarriesOnlyThatLabelsSamples() {
        val session = CalibrationSession.begin(listOf('A', 'B'))
        session.recordInk(strokesFor('A', jitter = 0.05f))
        session.recordInk(strokesFor('A', jitter = 0.12f))
        val event = session.advanceToNextLabel() as CalibrationEvent.Advanced
        assertEquals(
            listOf("train:A:${session.sessionId}:0", "train:A:${session.sessionId}:1"),
            event.payload.clusters.map { it.id.value },
        )
    }

    @Test
    fun heldPenWithoutMovementIsIgnored() {
        val session = CalibrationSession.begin(listOf('A'))
        val held = Stroke().apply { repeat(40) { append(500f, 1000f) } }
        assertEquals(CalibrationEvent.Ignored, session.recordInk(listOf(held)))
        assertEquals(0, session.sampleCount)
    }

    @Test
    fun secondSessionAddsToPriorExperiments() {
        val dao = InMemoryPrototypeDao()
        val recognizer = AdaptiveRecognizer(PrototypeStore(dao))

        val first = CalibrationSession.begin(listOf('A'))
        repeat(3) { first.recordInk(strokesFor('A', jitter = (it + 1) * 0.05f)) }
        recognizer.commitTraining((first.advanceToNextLabel() as CalibrationEvent.ReadyToCommit).payload)

        val second = CalibrationSession.begin(listOf('A'))
        second.recordInk(strokesFor('A', jitter = 0.2f))
        recognizer.commitTraining((second.advanceToNextLabel() as CalibrationEvent.ReadyToCommit).payload)

        assertEquals(4, dao.trainingIds().size)
        assertEquals(40, dao.all().size)
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
}
