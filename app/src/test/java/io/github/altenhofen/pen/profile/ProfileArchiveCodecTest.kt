package io.github.altenhofen.pen.profile

import io.github.altenhofen.pen.recognition.ClusterId
import io.github.altenhofen.pen.recognition.FeatureVector
import io.github.altenhofen.pen.recognition.PrototypeCluster
import io.github.altenhofen.pen.settings.MotorSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ProfileArchiveCodecTest {
    @Test
    fun roundTripPersistsMotorAndSeedCluster() {
        val vector = FloatArray(96) { 0.1f }
        val profile = PenProfile.create(
            MotorSettings.Default
                .withSettleMillis(900L)
                .withStrokeWidthDp(5.5f)
                .withAmbiguityThreshold(0.2f)
                .withAllowFingerInput(true),
            listOf(PrototypeCluster(ClusterId("seed:8"), '8', FeatureVector.from(vector))),
        )
        val encoded = ByteArrayOutputStream().also { ProfileArchiveCodec.encode(it, profile) }.toByteArray()
        val decoded = ProfileArchiveCodec.decode(ByteArrayInputStream(encoded))
        assertEquals(900L, decoded.settings.settleMillis)
        assertEquals(5.5f, decoded.settings.strokeWidthDp)
        assertEquals(0.2f, decoded.settings.ambiguityThreshold)
        assertEquals(true, decoded.settings.allowFingerInput)
        assertEquals(1, decoded.prototypes.size)
        assertEquals("seed:8", decoded.prototypes.single().id.value)
        assertEquals('8', decoded.prototypes.single().label)
        assertTrue(decoded.prototypes.single().vector.copyValues().contentEquals(vector))
    }

    @Test(expected = ProfileTransferException::class)
    fun unknownFormatVersionThrows() {
        ProfileArchiveCodec.decode(ByteArrayInputStream(zipOf(profileJson(formatVersion = 2))))
    }

    @Test(expected = ProfileTransferException::class)
    fun truncatedBytesThrow() {
        ProfileArchiveCodec.decode(ByteArrayInputStream(byteArrayOf(0x50, 0x4B)))
    }

    @Test(expected = ProfileTransferException::class)
    fun emptyBytesThrow() {
        ProfileArchiveCodec.decode(ByteArrayInputStream(ByteArray(0)))
    }

    @Test
    fun extraZipEntryIsIgnored() {
        val vector = FloatArray(96) { 0.1f }
        val json = profileJson()
        val bytes = ByteArrayOutputStream().use { out ->
            ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry("sidecar.txt"))
                zip.write("ignore me".toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("profile.json"))
                zip.write(json)
                zip.closeEntry()
            }
            out.toByteArray()
        }
        val decoded = ProfileArchiveCodec.decode(ByteArrayInputStream(bytes))
        assertEquals("seed:8", decoded.prototypes.single().id.value)
        assertTrue(decoded.prototypes.single().vector.copyValues().contentEquals(vector))
    }

    @Test
    fun legacyArchiveWithoutFingerFlagDefaultsOff() {
        val decoded = ProfileArchiveCodec.decode(ByteArrayInputStream(zipOf(profileJson())))
        assertEquals(false, decoded.settings.allowFingerInput)
    }

    @Test
    fun motorNanWidthBecomesDefaultSix() {
        val json = profileJson(strokeWidth = "NaN")
        val decoded = ProfileArchiveCodec.decode(ByteArrayInputStream(zipOf(json)))
        assertEquals(6f, decoded.settings.strokeWidthDp)
        assertEquals(900L, decoded.settings.settleMillis)
        assertEquals(0.2f, decoded.settings.ambiguityThreshold)
    }

    @Test(expected = ProfileTransferException::class)
    fun duplicateIdsFail() {
        val vector = FloatArray(96) { 0.1f }.joinToString(",")
        val json = """
            {"formatVersion":1,"motor":{"settleMillis":900,"strokeWidthDp":5.5,"ambiguityThreshold":0.2},
             "prototypes":[
               {"id":"seed:8","label":"8","vector":[$vector]},
               {"id":"seed:8","label":"8","vector":[$vector]}
             ]}
        """.trimIndent().toByteArray(Charsets.UTF_8)
        ProfileArchiveCodec.decode(ByteArrayInputStream(zipOf(json)))
    }

    @Test(expected = ProfileTransferException::class)
    fun vectorOfLengthTwoFails() {
        val json = """
            {"formatVersion":1,"motor":{"settleMillis":900,"strokeWidthDp":5.5,"ambiguityThreshold":0.2},
             "prototypes":[{"id":"seed:8","label":"8","vector":[0.1,0.2]}]}
        """.trimIndent().toByteArray(Charsets.UTF_8)
        ProfileArchiveCodec.decode(ByteArrayInputStream(zipOf(json)))
    }

    private fun profileJson(
        formatVersion: Int = 1,
        strokeWidth: String = "5.5",
    ): ByteArray {
        val vector = FloatArray(96) { 0.1f }.joinToString(",")
        return """
            {"formatVersion":$formatVersion,
             "motor":{"settleMillis":900,"strokeWidthDp":$strokeWidth,"ambiguityThreshold":0.2},
             "prototypes":[{"id":"seed:8","label":"8","vector":[$vector]}]}
        """.trimIndent().toByteArray(Charsets.UTF_8)
    }

    private fun zipOf(json: ByteArray): ByteArray = ByteArrayOutputStream().use { out ->
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("profile.json"))
            zip.write(json)
            zip.closeEntry()
        }
        out.toByteArray()
    }
}
