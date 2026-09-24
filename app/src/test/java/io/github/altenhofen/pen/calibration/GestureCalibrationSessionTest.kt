package io.github.altenhofen.pen.calibration

import io.github.altenhofen.pen.recognition.GestureAction
import io.github.altenhofen.pen.recognition.seedStrokes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureCalibrationSessionTest {
    @Test
    fun multiStrokeSettleCountsAsOneSample() {
        val session = GestureCalibrationSession.begin(listOf(GestureAction.DeleteLine))
        val twoStrokes = seedStrokes('z') + seedStrokes('z')
        assertEquals(GestureCalibrationEvent.Recorded, session.recordInk(twoStrokes))
        assertEquals(1, session.sampleCount)
    }

    @Test
    fun needsFiveSamplesBeforeAdvancing() {
        val session = GestureCalibrationSession.begin(listOf(GestureAction.Undo))
        repeat(4) {
            assertEquals(GestureCalibrationEvent.Recorded, session.recordInk(seedStrokes('z')))
            assertFalse(session.canAdvance)
        }
        assertEquals(GestureCalibrationEvent.Recorded, session.recordInk(seedStrokes('z')))
        assertTrue(session.canAdvance)
        val event = session.advanceToNextAction()
        assertTrue(event is GestureCalibrationEvent.ReadyToCommit)
        assertEquals(5, (event as GestureCalibrationEvent.ReadyToCommit).payload.clusters.size)
    }
}
