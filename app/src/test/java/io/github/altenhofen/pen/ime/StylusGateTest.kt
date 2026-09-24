package io.github.altenhofen.pen.ime

import android.view.MotionEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StylusGateTest {
    @Test
    fun imeRejectsFingerWhenFingerInkDisabled() {
        assertFalse(StylusGate.acceptsIme(MotionEvent.TOOL_TYPE_FINGER, allowFingerInput = false))
        assertTrue(StylusGate.acceptsIme(MotionEvent.TOOL_TYPE_STYLUS, allowFingerInput = false))
    }

    @Test
    fun imeAcceptsFingerWhenFingerInkEnabled() {
        assertTrue(StylusGate.acceptsIme(MotionEvent.TOOL_TYPE_FINGER, allowFingerInput = true))
    }

    @Test
    fun narrowContactCountsAsPassivePen() {
        assertTrue(StylusGate.narrowContact(major = 18f, minor = 12f, density = 3f))
    }

    @Test
    fun wideContactIsNotPassivePen() {
        assertFalse(StylusGate.narrowContact(major = 48f, minor = 40f, density = 3f))
    }

    @Test
    fun zeroEllipseUsesSmallNormalizedSize() {
        assertTrue(StylusGate.smallNormalizedTouchSize(0.05f))
        assertFalse(StylusGate.smallNormalizedTouchSize(0.3f))
    }

    @Test
    fun missingContactEllipseAcceptsFingerToolType() {
        assertTrue(StylusGate.passivePenWithoutContactEllipse(MotionEvent.TOOL_TYPE_FINGER))
        assertFalse(StylusGate.passivePenWithoutContactEllipse(MotionEvent.TOOL_TYPE_STYLUS))
    }
}
