package io.github.altenhofen.pen.profile

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.zip.DataFormatException
import java.util.zip.Deflater
import java.util.zip.Inflater
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * The on-disk format v3 archive: a cleartext header, then AES-256-GCM over the Deflate-compressed
 * [ProfileBinaryCodec] body.
 *
 * ```
 * 0..3    magic 'P' 'E' 'N' 'V'
 * 4       format version (3)
 * 5       key derivation id (1 = PBKDF2-HMAC-SHA256 + AES-256-GCM)
 * 6..9    PBKDF2 iterations, unsigned big endian
 * 10..25  salt, 16 random bytes
 * 26..37  nonce, 12 random bytes
 * 38..    ciphertext with the 16 byte GCM tag appended
 * ```
 *
 * The whole header is the GCM additional authenticated data, so editing the version, the
 * iteration count, the salt, or the nonce fails the tag instead of steering the reader.
 */
internal object ProfileVault {
    const val FORMAT_VERSION = 4
    const val LEGACY_FORMAT_VERSION = 3
    const val HEADER_BYTES = 38
    const val MAGIC_BYTES = 4

    /**
     * OWASP's 2023 floor for PBKDF2-HMAC-SHA256. Costs 1473 ms median on emulator-5556, which has
     * no SHA-256 acceleration. Run PassphraseCostTest on a target device before changing it.
     */
    const val ITERATIONS = 210_000

    private const val KDF_PBKDF2_HMAC_SHA256 = 1
    private const val MIN_ITERATIONS = 10_000
    private const val MAX_ITERATIONS = 4_000_000
    private const val SALT_BYTES = 16
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128
    private const val KEY_BITS = 256
    private val MAGIC = byteArrayOf('P'.code.toByte(), 'E'.code.toByte(), 'N'.code.toByte(), 'V'.code.toByte())

    fun isVaultArchive(prefix: ByteArray): Boolean =
        prefix.size >= MAGIC.size && MAGIC.indices.all { prefix[it] == MAGIC[it] }

    fun encrypt(output: OutputStream, profile: PenProfile, passphrase: CharArray) {
        if (passphrase.isEmpty()) throw ProfileTransferException("passphrase must not be empty")
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val header = header(ITERATIONS, salt, nonce)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            deriveKey(passphrase, salt, ITERATIONS.toLong()),
            GCMParameterSpec(TAG_BITS, nonce),
        )
        cipher.updateAAD(header)
        val body = deflate(ProfileBinaryCodec.encode(profile))
        try {
            output.write(header)
            output.write(cipher.doFinal(body))
        } finally {
            body.fill(0)
        }
    }

    fun decrypt(archive: ByteArray, passphrase: CharArray): PenProfile {
        if (archive.size <= HEADER_BYTES) throw ProfileTransferException("archive is truncated")
        if (!isVaultArchive(archive)) throw ProfileTransferException("not a pen profile archive")
        val header = archive.copyOf(HEADER_BYTES)
        val version = header[4].toInt()
        if (version !in setOf(FORMAT_VERSION, LEGACY_FORMAT_VERSION)) {
            throw ProfileTransferException("unsupported format version $version")
        }
        if (header[5].toInt() != KDF_PBKDF2_HMAC_SHA256) {
            throw ProfileTransferException("unsupported key derivation ${header[5].toInt()}")
        }
        // Range-checked before deriving: the tag would also reject a doctored count, but only
        // after the reader had already spent that many iterations on it.
        val iterations = readIterations(header)
        if (iterations !in MIN_ITERATIONS..MAX_ITERATIONS) {
            throw ProfileTransferException("iteration count $iterations is out of range")
        }
        val salt = header.copyOfRange(6 + Int.SIZE_BYTES, 6 + Int.SIZE_BYTES + SALT_BYTES)
        val nonce = header.copyOfRange(HEADER_BYTES - NONCE_BYTES, HEADER_BYTES)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(passphrase, salt, iterations), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(header)
        val body = try {
            cipher.doFinal(archive, HEADER_BYTES, archive.size - HEADER_BYTES)
        } catch (error: GeneralSecurityException) {
            throw ProfileTransferException(WRONG_PASSPHRASE, error, ProfileFailure.Passphrase)
        }
        return try {
            val inflated = inflate(body)
            try {
                if (version == LEGACY_FORMAT_VERSION) ProfileBinaryCodec.decodeLegacyBody(inflated)
                else ProfileBinaryCodec.decode(inflated)
            } finally {
                inflated.fill(0)
            }
        } finally {
            body.fill(0)
        }
    }

    private fun header(iterations: Int, salt: ByteArray, nonce: ByteArray): ByteArray {
        val header = ByteArray(HEADER_BYTES)
        MAGIC.copyInto(header)
        header[4] = FORMAT_VERSION.toByte()
        header[5] = KDF_PBKDF2_HMAC_SHA256.toByte()
        for (index in 0 until Int.SIZE_BYTES) {
            header[6 + index] = (iterations ushr (8 * (Int.SIZE_BYTES - 1 - index))).toByte()
        }
        salt.copyInto(header, 6 + Int.SIZE_BYTES)
        nonce.copyInto(header, HEADER_BYTES - NONCE_BYTES)
        return header
    }

    private fun readIterations(header: ByteArray): Long {
        var value = 0L
        for (index in 0 until Int.SIZE_BYTES) value = (value shl 8) or (header[6 + index].toLong() and 0xFF)
        return value
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Long): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, iterations.toInt(), KEY_BITS)
        val material = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } catch (error: GeneralSecurityException) {
            throw ProfileTransferException("this device cannot derive the profile key", error)
        } finally {
            spec.clearPassword()
        }
        return try {
            SecretKeySpec(material, "AES")
        } finally {
            material.fill(0)
        }
    }

    private fun deflate(body: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        try {
            deflater.setInput(body)
            deflater.finish()
            val out = ByteArrayOutputStream(body.size / 2)
            val chunk = ByteArray(BUFFER_BYTES)
            while (!deflater.finished()) out.write(chunk, 0, deflater.deflate(chunk))
            return out.toByteArray()
        } finally {
            deflater.end()
        }
    }

    private fun inflate(compressed: ByteArray): ByteArray {
        val inflater = Inflater()
        try {
            inflater.setInput(compressed)
            val out = ByteArrayOutputStream(compressed.size * 2)
            val chunk = ByteArray(BUFFER_BYTES)
            while (!inflater.finished()) {
                val produced = try {
                    inflater.inflate(chunk)
                } catch (error: DataFormatException) {
                    throw ProfileTransferException("profile body is corrupt", error)
                }
                if (produced == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                    throw ProfileTransferException("profile body is corrupt")
                }
                out.write(chunk, 0, produced)
                if (out.size() > MAX_BODY_BYTES) throw ProfileTransferException("profile body is too large")
            }
            return out.toByteArray()
        } finally {
            inflater.end()
        }
    }

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val BUFFER_BYTES = 16 * 1024
    private const val MAX_BODY_BYTES = 64 * 1024 * 1024
    private const val WRONG_PASSPHRASE = "wrong passphrase, or the file was changed after it was written"
}
