package io.github.altenhofen.pen.ime

import android.view.MotionEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StylusGateTest {
    @Test
    fun fingerDoubleTapAllowedWhenInkDisabled() {
        assertFalse(StylusGate.acceptsIme(MotionEvent.TOOL_TYPE_FINGER, allowFingerInput = false))
        assertTrue(
            StylusGate.acceptsImePointer(
                MotionEvent.TOOL_TYPE_FINGER,
                allowFingerInput = false,
                doubleTapForSpace = true,
            ),
        )
    }

    @Test
    fun fingerPointerBlockedWhenDoubleTapOff() {
        assertFalse(
            StylusGate.acceptsImePointer(
                MotionEvent.TOOL_TYPE_FINGER,
                allowFingerInput = false,
                doubleTapForSpace = false,
            ),
        )
    }
}
