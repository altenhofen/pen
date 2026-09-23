package io.github.altenhofen.pen.profile

import io.github.altenhofen.pen.recognition.ClusterId
import io.github.altenhofen.pen.recognition.FeatureVector
import io.github.altenhofen.pen.recognition.PrototypeCluster
import io.github.altenhofen.pen.settings.MotorSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

/** The encrypted envelope around the format v4 body. */
class ProfileVaultTest {
    @Test
    fun theRightPassphraseReturnsTheProfile() {
        val decoded = ProfileVault.decrypt(encode(profile(), PASSPHRASE), PASSPHRASE.copyOf())
        assertEquals(600L, decoded.settings.settleMillis)
        assertEquals("train:8:session:0", decoded.prototypes.single { it.label == '8' && it.id.value.startsWith("train") }.id.value)
    }

    @Test
    fun headerAnnouncesTheFormat() {
        val encoded = encode(profile(), PASSPHRASE)
        assertEquals(byteArrayOf(0x50, 0x45, 0x4E, 0x56).toList(), encoded.copyOf(4).toList())
        assertEquals(ProfileVault.FORMAT_VERSION, encoded[4].toInt())
        assertEquals(1, encoded[5].toInt())
        assertEquals(true, ProfileVault.isVaultArchive(encoded))
        assertEquals(false, ProfileVault.isVaultArchive(byteArrayOf(0x50, 0x4B, 0x03, 0x04)))
    }

    @Test
    fun wrongPassphraseFails() {
        val failure = failureOf { ProfileVault.decrypt(encode(profile(), PASSPHRASE), "correct horse".toCharArray()) }
        assertEquals(ProfileFailure.Passphrase, failure.failure)
        assertEquals("wrong passphrase, or the file was changed after it was written", failure.message)
    }

    @Test
    fun flippedCiphertextByteFails() {
        val encoded = encode(profile(), PASSPHRASE)
        val at = ProfileVault.HEADER_BYTES + 3
        encoded[at] = (encoded[at].toInt() xor 1).toByte()
        assertEquals(ProfileFailure.Passphrase, failureOf { ProfileVault.decrypt(encoded, PASSPHRASE.copyOf()) }.failure)
    }

    @Test
    fun aSaltByteEditedInTheHeaderFails() {
        val encoded = encode(profile(), PASSPHRASE)
        encoded[20] = (encoded[20].toInt() xor 1).toByte()
        assertEquals(ProfileFailure.Passphrase, failureOf { ProfileVault.decrypt(encoded, PASSPHRASE.copyOf()) }.failure)
    }

    @Test
    fun anImpossibleIterationCountIsRefusedBeforeDeriving() {
        val encoded = encode(profile(), PASSPHRASE)
        for (index in 6..9) encoded[index] = if (index == 6) 0x7F else 0xFF.toByte()
        assertEquals(
            "iteration count 2147483647 is out of range",
            failureOf { ProfileVault.decrypt(encoded, PASSPHRASE.copyOf()) }.message,
        )
    }

    @Test
    fun truncatedFilesFail() {
        val encoded = encode(profile(), PASSPHRASE)
        assertEquals(
            ProfileFailure.Passphrase,
            failureOf { ProfileVault.decrypt(encoded.copyOf(encoded.size - 8), PASSPHRASE.copyOf()) }.failure,
        )
        assertEquals(
            "archive is truncated",
            failureOf { ProfileVault.decrypt(encoded.copyOf(20), PASSPHRASE.copyOf()) }.message,
        )
        assertEquals(
            "not a pen profile archive",
            failureOf { ProfileVault.decrypt(ByteArray(200), PASSPHRASE.copyOf()) }.message,
        )
    }

    @Test
    fun emptyPassphraseIsRejected() {
        assertEquals("passphrase must not be empty", failureOf { encode(profile(), CharArray(0)) }.message)
    }

    @Test
    fun theEncryptedFileIsFarSmallerThanTheVersionTwoArchive() {
        val realistic = RealisticProfile.build()
        val versionTwo = RealisticProfile.encodeVersionTwo(realistic).size
        val versionThree = encode(realistic, PASSPHRASE).size
        println(
            "${RealisticProfile.WORD_COUNT} words x ${RealisticProfile.SAMPLES_PER_WORD} samples, " +
                "26 letters x ${RealisticProfile.CLUSTERS_PER_LETTER} clusters: " +
                "v2 zip $versionTwo bytes, v3 encrypted $versionThree bytes",
        )
        assertTrue("v2 zip $versionTwo, v3 file $versionThree", versionThree * 3 < versionTwo)
    }

    private fun encode(profile: PenProfile, passphrase: CharArray): ByteArray =
        ByteArrayOutputStream().also { ProfileVault.encrypt(it, profile, passphrase.copyOf()) }.toByteArray()

    private fun failureOf(action: () -> Unit): ProfileTransferException = try {
        action()
        throw AssertionError("expected a ProfileTransferException")
    } catch (error: ProfileTransferException) {
        error
    }

    private fun profile() = PenProfile.create(
        MotorSettings.Default,
        listOf(
            PrototypeCluster(
                ClusterId("train:8:session:0"),
                '8',
                FeatureVector.from(FloatArray(96) { index -> if (index % 3 == 2) 0f else index * 0.004f }),
            ),
        ),
    )

    private companion object {
        val PASSPHRASE = "a strong enough passphrase".toCharArray()
    }
}
