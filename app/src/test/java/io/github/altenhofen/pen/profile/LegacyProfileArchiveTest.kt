package io.github.altenhofen.pen.profile

import io.github.altenhofen.pen.profile.RealisticProfile.zipOf
import io.github.altenhofen.pen.recognition.WORD_SAMPLE_COUNT
import io.github.altenhofen.pen.settings.HandwritingLanguage
import io.github.altenhofen.pen.settings.InkLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Profiles a user exported before format v3 still import, unencrypted, read only. */
class LegacyProfileArchiveTest {
    @Test
    fun versionOneImportsWithEmptyWordMemory() {
        val decoded = LegacyProfileArchive.decode(zipOf(profileJson(formatVersion = 1)))
        assertEquals("seed:8", decoded.prototypes.single().id.value)
        assertEquals('8', decoded.prototypes.single().label)
        assertEquals(900L, decoded.settings.settleMillis)
        assertEquals(0, decoded.words.size)
    }

    @Test
    fun versionTwoImportsWordMemory() {
        val shape = FloatArray(WORD_SAMPLE_COUNT * 3) { it * 0.001f }
        val words = """[{"id":"w1","word":"augusto","vector":[${shape.joinToString(",")}],"confirmedAt":42}]"""
        val decoded = LegacyProfileArchive.decode(zipOf(profileJson(formatVersion = 2, words = words)))
        val word = decoded.words.single()
        assertEquals(listOf<Any>("w1", "augusto", 42L), listOf(word.id, word.word, word.confirmedAt))
        assertTrue(word.vector.copyValues().contentEquals(shape))
    }

    @Test
    fun versionTwoRoundTripsTheRealisticProfile() {
        val profile = RealisticProfile.build()
        val decoded = LegacyProfileArchive.decode(RealisticProfile.encodeVersionTwo(profile))
        assertEquals(profile.prototypes.size, decoded.prototypes.size)
        assertEquals(profile.words.size, decoded.words.size)
        assertEquals(profile.prototypes.first().id.value, decoded.prototypes.first().id.value)
    }

    @Test
    fun legacyArchiveWithoutFingerFlagDefaultsOff() {
        assertEquals(false, LegacyProfileArchive.decode(zipOf(profileJson())).settings.allowFingerInput)
    }

    @Test
    fun legacyArchiveWithoutHandwritingLanguageFollowsTheApp() {
        assertEquals(HandwritingLanguage.FollowApp, LegacyProfileArchive.decode(zipOf(profileJson())).settings.handwriting)
    }

    @Test
    fun legacyArchiveWithHandwritingLanguageRestoresIt() {
        val json = String(profileJson()).replaceFirst(
            "\"ambiguityThreshold\":0.2",
            "\"ambiguityThreshold\":0.2,\"handwritingLanguage\":\"pt-BR\"",
        )
        assertEquals(
            HandwritingLanguage.Explicit(InkLanguage.Portuguese),
            LegacyProfileArchive.decode(zipOf(json.toByteArray(Charsets.UTF_8))).settings.handwriting,
        )
    }

    @Test
    fun legacyArchiveWithUnknownHandwritingTagFollowsTheApp() {
        val json = String(profileJson()).replaceFirst(
            "\"ambiguityThreshold\":0.2",
            "\"ambiguityThreshold\":0.2,\"handwritingLanguage\":\"kl-GL\"",
        )
        assertEquals(HandwritingLanguage.FollowApp, LegacyProfileArchive.decode(zipOf(json.toByteArray(Charsets.UTF_8))).settings.handwriting)
    }

    @Test
    fun motorNanWidthBecomesDefaultSix() {
        val decoded = LegacyProfileArchive.decode(zipOf(profileJson(strokeWidth = "NaN")))
        assertEquals(6f, decoded.settings.strokeWidthDp)
        assertEquals(900L, decoded.settings.settleMillis)
    }

    @Test
    fun extraZipEntryIsIgnored() {
        val bytes = ByteArrayOutputStream().use { out ->
            ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry("sidecar.txt"))
                zip.write("ignore me".toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                zip.putNextEntry(ZipEntry(LegacyProfileArchive.ENTRY_NAME))
                zip.write(profileJson())
                zip.closeEntry()
            }
            out.toByteArray()
        }
        assertEquals("seed:8", LegacyProfileArchive.decode(bytes).prototypes.single().id.value)
    }

    @Test
    fun versionThreeImportsGestureTraining() {
        val vector = FloatArray(96) { 0.1f }.joinToString(",")
        val gestures = """[{"id":"g1","actionId":"undo","vector":[$vector]}]"""
        val decoded = LegacyProfileArchive.decode(zipOf(profileJson(formatVersion = 3, gestures = gestures)))
        assertEquals(1, decoded.gestures.size)
        assertEquals(io.github.altenhofen.pen.recognition.GestureAction.Undo, decoded.gestures.single().action)
    }

    @Test
    fun emptyAndTruncatedBytesFail() {
        assertEquals("missing profile.json", failureOf { LegacyProfileArchive.decode(ByteArray(0)) }.message)
        assertEquals(
            "missing profile.json",
            failureOf { LegacyProfileArchive.decode(byteArrayOf(0x50, 0x4B)) }.message,
        )
    }

    @Test
    fun duplicateClusterIdsFail() {
        val vector = FloatArray(96) { 0.1f }.joinToString(",")
        val json = """
            {"formatVersion":1,"motor":{"settleMillis":900,"strokeWidthDp":5.5,"ambiguityThreshold":0.2},
             "prototypes":[
               {"id":"seed:8","label":"8","vector":[$vector]},
               {"id":"seed:8","label":"8","vector":[$vector]}
             ]}
        """.trimIndent().toByteArray(Charsets.UTF_8)
        assertEquals("cluster ids must be unique", failureOf { LegacyProfileArchive.decode(zipOf(json)) }.message)
    }

    @Test
    fun vectorOfTheWrongLengthFails() {
        val json = """
            {"formatVersion":1,"motor":{"settleMillis":900,"strokeWidthDp":5.5,"ambiguityThreshold":0.2},
             "prototypes":[{"id":"seed:8","label":"8","vector":[0.1,0.2]}]}
        """.trimIndent().toByteArray(Charsets.UTF_8)
        assertEquals(
            "vector must have 96 values, got 2",
            failureOf { LegacyProfileArchive.decode(zipOf(json)) }.message,
        )
    }

    private fun failureOf(action: () -> Unit): ProfileTransferException = try {
        action()
        throw AssertionError("expected a ProfileTransferException")
    } catch (error: ProfileTransferException) {
        error
    }

    private fun profileJson(
        formatVersion: Int = 1,
        strokeWidth: String = "5.5",
        words: String = "[]",
        gestures: String = "[]",
    ): ByteArray {
        val vector = FloatArray(96) { 0.1f }.joinToString(",")
        return """
            {"formatVersion":$formatVersion,
             "motor":{"settleMillis":900,"strokeWidthDp":$strokeWidth,"ambiguityThreshold":0.2},
             "words":$words,
             "gestures":$gestures,
             "prototypes":[{"id":"seed:8","label":"8","vector":[$vector]}]}
        """.trimIndent().toByteArray(Charsets.UTF_8)
    }
}
