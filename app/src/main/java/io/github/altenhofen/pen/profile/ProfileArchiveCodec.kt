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
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal object ProfileArchiveCodec {
    const val ENTRY_NAME = "profile.json"
    const val FORMAT_VERSION = 2
    private val READABLE_VERSIONS = 1..FORMAT_VERSION

    private val json = Json {
        ignoreUnknownKeys = true
        allowSpecialFloatingPointValues = true
    }

    fun encode(output: OutputStream, profile: PenProfile) {
        val payload = json.encodeToString(ProfileWire.serializer(), profile.toWire())
            .toByteArray(Charsets.UTF_8)
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(ENTRY_NAME))
            zip.write(payload)
            zip.closeEntry()
        }
    }

    fun decode(input: InputStream): PenProfile {
        val zip = ZipInputStream(input)
        var jsonBytes: ByteArray? = null
        while (true) {
            val entry = zip.nextEntry ?: break
            if (entry.name == ENTRY_NAME) {
                jsonBytes = zip.readBytes()
            }
            zip.closeEntry()
        }
        val bytes = jsonBytes ?: throw ProfileTransferException("missing $ENTRY_NAME")
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
        } catch (error: ZipException) {
            throw ProfileTransferException("invalid archive", error)
        } catch (error: IOException) {
            throw ProfileTransferException("invalid archive", error)
        }
    }

    private fun PenProfile.toWire() = ProfileWire(
        formatVersion = FORMAT_VERSION,
        motor = MotorWire(
            settings.settleMillis,
            settings.strokeWidthDp,
            settings.ambiguityThreshold,
            settings.allowFingerInput,
        ),
        prototypes = prototypes.map { cluster ->
            PrototypeWire(
                id = cluster.id.value,
                label = cluster.label.toString(),
                vector = cluster.vector.copyValues().toList(),
            )
        },
        words = words.map { WordWire(it.id, it.word, it.vector.copyValues().toList(), it.confirmedAt) },
    )

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
