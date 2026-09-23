package io.github.altenhofen.pen.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class MotorSettingsTest {
    @Test
    fun settleWindowClampsToRange() {
        assertEquals(300L, MotorSettings.Default.withSettleMillis(200L).settleMillis)
        assertEquals(1_200L, MotorSettings.Default.withSettleMillis(2_000L).settleMillis)
    }

    @Test
    fun strokeWidthClampsToRange() {
        assertEquals(2f, MotorSettings.Default.withStrokeWidthDp(1f).strokeWidthDp)
        assertEquals(16f, MotorSettings.Default.withStrokeWidthDp(20f).strokeWidthDp)
    }

    @Test
    fun ambiguityStaysPositiveAndAtMostOne() {
        assertEquals(0.01f, MotorSettings.Default.withAmbiguityThreshold(0f).ambiguityThreshold)
        assertEquals(1f, MotorSettings.Default.withAmbiguityThreshold(3f).ambiguityThreshold)
    }

    @Test
    fun missingOrCorruptDiskValuesFallBackPerField() {
        val parsed = MotorSettings.parse(900L, Float.NaN, null)
        assertEquals(900L, parsed.settleMillis)
        assertEquals(6f, parsed.strokeWidthDp)
        assertEquals(0.15f, parsed.ambiguityThreshold)
        assertEquals(CaptureStyle(900L, 6f), parsed.capture())
    }
}
