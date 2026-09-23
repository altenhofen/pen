package io.github.altenhofen.pen.ime

import android.view.MotionEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StylusGateTest {
    @Test
    fun stylusIsAccepted() {
        assertTrue(StylusGate.accepts(MotionEvent.TOOL_TYPE_STYLUS))
    }

    @Test
    fun fingerAndPalmAreRejected() {
        assertFalse(StylusGate.accepts(MotionEvent.TOOL_TYPE_FINGER))
        assertFalse(StylusGate.accepts(MotionEvent.TOOL_TYPE_UNKNOWN))
        assertFalse(StylusGate.accepts(MotionEvent.TOOL_TYPE_MOUSE))
    }

    @Test
    fun trainingAcceptsStylusAndFinger() {
        assertTrue(StylusGate.acceptsTraining(MotionEvent.TOOL_TYPE_STYLUS))
        assertTrue(StylusGate.acceptsTraining(MotionEvent.TOOL_TYPE_FINGER))
        assertFalse(StylusGate.acceptsTraining(MotionEvent.TOOL_TYPE_UNKNOWN))
    }
}
