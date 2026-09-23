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

    @Test
    fun handwritingLanguageDefaultsToFollowApp() {
        assertEquals(HandwritingLanguage.FollowApp, MotorSettings.Default.handwriting)
        assertEquals(HandwritingLanguage.FollowApp, MotorSettings.parse(900L, 5f, 0.2f).handwriting)
    }

    @Test
    fun handwritingLanguageSurvivesAStoreRoundTrip() {
        val chosen = MotorSettings.Default.withHandwriting(HandwritingLanguage.Explicit(InkLanguage.Portuguese))
        val reread = MotorSettings.parse(
            chosen.settleMillis,
            chosen.strokeWidthDp,
            chosen.ambiguityThreshold,
            chosen.allowFingerInput,
            chosen.handwriting.stored(),
        )
        assertEquals("pt-BR", chosen.handwriting.stored())
        assertEquals(HandwritingLanguage.Explicit(InkLanguage.Portuguese), reread.handwriting)
        assertEquals(chosen, reread)
    }

    @Test
    fun handwritingLanguageDoesNotDisturbTheOtherFields() {
        val chosen = MotorSettings.Default
            .withSettleMillis(900L)
            .withHandwriting(HandwritingLanguage.Explicit(InkLanguage.German))
        assertEquals(900L, chosen.settleMillis)
        assertEquals(6f, chosen.strokeWidthDp)
        assertEquals(0.15f, chosen.ambiguityThreshold)
        assertEquals(false, chosen.allowFingerInput)
    }
}
