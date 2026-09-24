package io.github.altenhofen.pen.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun imeTogglesDefaultOff() {
        assertFalse(MotorSettings.Default.spaceAfterFullWord)
        assertFalse(MotorSettings.Default.recognizeSpacesInHandwriting)
    }

    @Test
    fun missingOrCorruptDiskValuesFallBackPerField() {
        val parsed = MotorSettings.parse(900L, Float.NaN, null, null, null)
        assertEquals(900L, parsed.settleMillis)
        assertEquals(6f, parsed.strokeWidthDp)
        assertEquals(CaptureStyle(900L, 6f), parsed.capture())
    }

    @Test
    fun legacyAmbiguityFieldIsIgnored() {
        val parsed = MotorSettings.parseLegacy(900L, 5f, 0.99f, true)
        assertEquals(900L, parsed.settleMillis)
        assertEquals(5f, parsed.strokeWidthDp)
        assertEquals(true, parsed.allowFingerInput)
    }

    @Test
    fun handwritingLanguageDefaultsToFollowApp() {
        assertEquals(HandwritingLanguage.FollowApp, MotorSettings.Default.handwriting)
        assertEquals(HandwritingLanguage.FollowApp, MotorSettings.parseLegacy(900L, 5f, 0.2f).handwriting)
    }

    @Test
    fun handwritingLanguageSurvivesAStoreRoundTrip() {
        val chosen = MotorSettings.Default.withHandwriting(HandwritingLanguage.Explicit(InkLanguage.Portuguese))
        val reread = MotorSettings.parse(
            chosen.settleMillis,
            chosen.strokeWidthDp,
            chosen.allowFingerInput,
            chosen.spaceAfterFullWord,
            chosen.recognizeSpacesInHandwriting,
            chosen.doubleTapForSpace,
            chosen.handwriting.stored(),
        )
        assertEquals("pt-BR", chosen.handwriting.stored())
        assertEquals(chosen, reread)
    }
}
