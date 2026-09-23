package io.github.altenhofen.pen.profile

import io.github.altenhofen.pen.recognition.ClusterId
import io.github.altenhofen.pen.recognition.FEATURE_WIDTH
import io.github.altenhofen.pen.recognition.FeatureVector
import io.github.altenhofen.pen.recognition.PrototypeCluster
import io.github.altenhofen.pen.recognition.SAMPLE_COUNT
import io.github.altenhofen.pen.recognition.WORD_SAMPLE_COUNT
import io.github.altenhofen.pen.recognition.WordMemory
import io.github.altenhofen.pen.recognition.WordSample
import io.github.altenhofen.pen.recognition.seedClusters
import io.github.altenhofen.pen.settings.MotorSettings
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The plaintext body of a format v3 archive, before Deflate and before encryption.
 *
 * ```
 * varint sampleCount            glyph samples per prototype vector
 * varint wordSampleCount        samples per word vector
 * varint settleMillis
 * f32    strokeWidthDp
 * f32    ambiguityThreshold
 * u8     allowFingerInput
 * text   handwritingLanguage tag, empty when following the app locale
 * varint clusterCount
 *   text  id
 *   varint label code point
 *   f32[FEATURE_WIDTH] per-channel scale
 *   i8[sampleCount * FEATURE_WIDTH] quantized values
 * varint wordCount
 *   text  id
 *   text  word
 *   varint zigzag(confirmedAt - previous confirmedAt)
 *   f32[FEATURE_WIDTH] per-channel scale
 *   i8[wordSampleCount * FEATURE_WIDTH] quantized values
 * ```
 *
 * The sample counts ride along so a future change to either constant rejects old archives instead
 * of reading them as garbage. Scales are per channel because x, y, and the pen-down flag live on
 * different magnitudes: one shared scale would spend most of the int8 range on the flag and
 * coarsen the deltas that recognition actually compares.
 */
internal object ProfileBinaryCodec {
    private const val MAX_TEXT_BYTES = 256
    private const val MAX_SAMPLE_COUNT = 4096
    private const val QUANTIZATION_STEPS = 127

    fun encode(profile: PenProfile): ByteArray {
        val out = ByteArrayOutputStream()
        val writer = BinaryWriter(out)
        writer.varint(SAMPLE_COUNT.toLong())
        writer.varint(WORD_SAMPLE_COUNT.toLong())
        writer.varint(profile.settings.settleMillis)
        writer.float(profile.settings.strokeWidthDp)
        writer.float(profile.settings.ambiguityThreshold)
        writer.byte(if (profile.settings.allowFingerInput) 1 else 0)
        writer.text(profile.settings.handwriting.stored() ?: "")

        val clusters = profile.prototypes.filterNot(::isPristineSeed)
        writer.varint(clusters.size.toLong())
        clusters.forEach { cluster ->
            writer.text(cluster.id.value)
            writer.varint(cluster.label.code.toLong())
            writer.quantized(cluster.vector)
        }

        writer.varint(profile.words.size.toLong())
        var previousConfirmedAt = 0L
        profile.words.forEach { sample ->
            writer.text(sample.id)
            writer.text(sample.word)
            writer.varint(zigzag(sample.confirmedAt - previousConfirmedAt))
            previousConfirmedAt = sample.confirmedAt
            writer.quantized(sample.vector)
        }
        return out.toByteArray()
    }

    fun decode(body: ByteArray): PenProfile {
        val reader = BinaryReader(body)
        val sampleCount = reader.count("sample count", MAX_SAMPLE_COUNT)
        val wordSampleCount = reader.count("word sample count", MAX_SAMPLE_COUNT)
        if (sampleCount != SAMPLE_COUNT || wordSampleCount != WORD_SAMPLE_COUNT) {
            throw ProfileTransferException("archive was written for a different feature vector size")
        }
        val settings = MotorSettings.parse(
            reader.varint(),
            reader.float(),
            reader.float(),
            reader.byte() != 0,
            handwritingTag(reader.text()),
        )

        val clusterCount = reader.count("cluster count", PenProfile.MAX_PROTOTYPES)
        val clusters = List(clusterCount) {
            val id = ClusterId(reader.text())
            val label = reader.codePoint()
            PrototypeCluster(id, label, reader.quantized(SAMPLE_COUNT))
        }

        val wordCount = reader.count("word count", WordMemory.TOTAL_CAP)
        var previousConfirmedAt = 0L
        val words = List(wordCount) {
            val id = reader.text()
            val word = reader.text()
            val confirmedAt = previousConfirmedAt + unzigzag(reader.varint())
            previousConfirmedAt = confirmedAt
            WordSample(id, word, reader.quantized(WORD_SAMPLE_COUNT), confirmedAt)
        }
        return PenProfile.create(settings, withRegeneratedSeeds(clusters), words)
    }

    private fun handwritingTag(stored: String): String? = stored.ifEmpty { null }

    private fun isPristineSeed(cluster: PrototypeCluster): Boolean =
        pristineSeeds[cluster.id]?.contentEquals(cluster.vector.copyValues()) == true

    private fun withRegeneratedSeeds(clusters: List<PrototypeCluster>): List<PrototypeCluster> {
        val present = clusters.map { it.id }.toSet()
        return clusters + seedClusters().filterNot { it.id in present }
    }

    private val pristineSeeds: Map<ClusterId, FloatArray> by lazy {
        seedClusters().associate { it.id to it.vector.copyValues() }
    }

    private fun BinaryWriter.quantized(vector: FeatureVector) {
        val values = vector.copyValues()
        val scales = FloatArray(FEATURE_WIDTH) { channel ->
            var peak = 0f
            var index = channel
            while (index < values.size) {
                peak = maxOf(peak, abs(values[index]))
                index += FEATURE_WIDTH
            }
            peak
        }
        scales.forEach(::float)
        values.forEachIndexed { index, value ->
            val scale = scales[index % FEATURE_WIDTH]
            byte(if (scale == 0f) 0 else (value / scale * QUANTIZATION_STEPS).roundToInt())
        }
    }

    private fun BinaryReader.quantized(sampleCount: Int): FeatureVector {
        val scales = FloatArray(FEATURE_WIDTH) { float() }
        if (scales.any { !it.isFinite() || it < 0f }) throw ProfileTransferException("invalid vector scale")
        val values = FloatArray(sampleCount * FEATURE_WIDTH) { index ->
            signedByte() * scales[index % FEATURE_WIDTH] / QUANTIZATION_STEPS
        }
        return FeatureVector.from(values, sampleCount)
    }

    private fun BinaryReader.count(name: String, limit: Int): Int {
        val value = varint()
        if (value < 0 || value > limit) throw ProfileTransferException("$name $value exceeds $limit")
        return value.toInt()
    }

    private fun BinaryReader.codePoint(): Char {
        val value = varint()
        if (value !in 0..Char.MAX_VALUE.code.toLong()) throw ProfileTransferException("label $value is not a character")
        return value.toInt().toChar()
    }

    private fun BinaryReader.text(): String {
        val length = count("text length", MAX_TEXT_BYTES)
        return String(take(length), Charsets.UTF_8)
    }

    private class BinaryWriter(private val out: ByteArrayOutputStream) {
        fun byte(value: Int) = out.write(value)

        fun varint(value: Long) {
            var remaining = value
            while (true) {
                val chunk = (remaining and 0x7F).toInt()
                remaining = remaining ushr 7
                if (remaining == 0L) return out.write(chunk)
                out.write(chunk or 0x80)
            }
        }

        fun float(value: Float) {
            val bits = value.toRawBits()
            for (shift in 0..24 step 8) out.write((bits ushr shift) and 0xFF)
        }

        fun text(value: String) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            require(bytes.size <= MAX_TEXT_BYTES) { "text exceeds $MAX_TEXT_BYTES bytes" }
            varint(bytes.size.toLong())
            out.write(bytes)
        }
    }

    private class BinaryReader(private val bytes: ByteArray) {
        private var cursor = 0

        fun byte(): Int = take(1)[0].toInt() and 0xFF

        fun signedByte(): Int = take(1)[0].toInt()

        fun varint(): Long {
            var result = 0L
            var shift = 0
            while (shift <= 63) {
                val chunk = byte()
                result = result or ((chunk and 0x7F).toLong() shl shift)
                if (chunk and 0x80 == 0) return result
                shift += 7
            }
            throw ProfileTransferException("malformed varint")
        }

        fun float(): Float {
            var bits = 0
            for (shift in 0..24 step 8) bits = bits or (byte() shl shift)
            return Float.fromBits(bits)
        }

        fun take(length: Int): ByteArray {
            if (length < 0 || cursor + length > bytes.size) throw ProfileTransferException("truncated profile body")
            val slice = bytes.copyOfRange(cursor, cursor + length)
            cursor += length
            return slice
        }
    }

    private fun zigzag(value: Long): Long = (value shl 1) xor (value shr 63)

    private fun unzigzag(value: Long): Long = (value ushr 1) xor -(value and 1L)
}
