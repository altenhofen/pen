package io.github.altenhofen.pen.profile

import io.github.altenhofen.pen.recognition.ClusterId
import io.github.altenhofen.pen.recognition.FeatureVector
import io.github.altenhofen.pen.recognition.PrototypeCluster
import io.github.altenhofen.pen.recognition.WORD_SAMPLE_COUNT
import io.github.altenhofen.pen.recognition.WordSample
import io.github.altenhofen.pen.settings.MotorSettings
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.zip.ZipException
import java.util.zip.ZipInputStream

/**
 * Reads the unencrypted zip-of-JSON archives that versions 1 and 2 wrote. Nothing writes this
 * format any more, so profiles already on a user's disk keep importing and nothing new lands
 * outside the encrypted [ProfileVault].
 */
internal object LegacyProfileArchive {
    const val ENTRY_NAME = "profile.json"
    private val READABLE_VERSIONS = 1..2

    private val json = Json {
        ignoreUnknownKeys = true
        allowSpecialFloatingPointValues = true
    }

    fun decode(archive: ByteArray): PenProfile {
        val bytes = readEntry(archive) ?: throw ProfileTransferException("missing $ENTRY_NAME")
        return try {
            val wire = json.decodeFromString(ProfileWire.serializer(), bytes.toString(Charsets.UTF_8))
            if (wire.formatVersion !in READABLE_VERSIONS) {
                throw ProfileTransferException("unsupported formatVersion ${wire.formatVersion}")
            }
            PenProfile.create(
                MotorSettings.parse(
                    wire.motor.settleMillis,
                    wire.motor.strokeWidthDp,
                    wire.motor.ambiguityThreshold,
                    wire.motor.allowFingerInput,
                    wire.motor.handwritingLanguage,
                ),
                wire.prototypes.map { it.toCluster() },
                wire.words.map { it.toSample() },
            )
        } catch (error: ProfileTransferException) {
            throw error
        } catch (error: IllegalArgumentException) {
            throw ProfileTransferException(error.message ?: "invalid archive", error)
        } catch (error: SerializationException) {
            throw ProfileTransferException("invalid archive", error)
        }
    }

    private fun readEntry(archive: ByteArray): ByteArray? = try {
        ZipInputStream(ByteArrayInputStream(archive)).use { zip ->
            var payload: ByteArray? = null
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name == ENTRY_NAME) payload = zip.readBytes()
                zip.closeEntry()
            }
            payload
        }
    } catch (error: ZipException) {
        throw ProfileTransferException("invalid archive", error)
    } catch (error: IOException) {
        throw ProfileTransferException("invalid archive", error)
    }

    private fun WordWire.toSample() =
        WordSample(id, word, FeatureVector.from(vector.toFloatArray(), WORD_SAMPLE_COUNT), confirmedAt)

    private fun PrototypeWire.toCluster(): PrototypeCluster {
        if (label.length != 1) throw ProfileTransferException("label must be a single character")
        return PrototypeCluster(ClusterId(id), label.single(), FeatureVector.from(vector.toFloatArray()))
    }
}

@Serializable
private data class ProfileWire(
    val formatVersion: Int,
    val motor: MotorWire,
    val prototypes: List<PrototypeWire>,
    val words: List<WordWire> = emptyList(),
)

@Serializable
private data class MotorWire(
    val settleMillis: Long,
    val strokeWidthDp: Float,
    val ambiguityThreshold: Float,
    val allowFingerInput: Boolean = false,
    val handwritingLanguage: String? = null,
)

@Serializable
private data class PrototypeWire(
    val id: String,
    val label: String,
    val vector: List<Float>,
)

@Serializable
private data class WordWire(
    val id: String,
    val word: String,
    val vector: List<Float>,
    val confirmedAt: Long,
)
