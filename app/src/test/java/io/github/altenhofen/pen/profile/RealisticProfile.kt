package io.github.altenhofen.pen.profile

import io.github.altenhofen.pen.recognition.ClusterId
import io.github.altenhofen.pen.recognition.FEATURE_WIDTH
import io.github.altenhofen.pen.recognition.FeatureVector
import io.github.altenhofen.pen.recognition.PrototypeCluster
import io.github.altenhofen.pen.recognition.SAMPLE_COUNT
import io.github.altenhofen.pen.recognition.WORD_SAMPLE_COUNT
import io.github.altenhofen.pen.recognition.WordSample
import io.github.altenhofen.pen.recognition.seedClusters
import io.github.altenhofen.pen.settings.MotorSettings
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.random.Random

/**
 * A profile the size of a well-used install: every letter trained five times, and two hundred
 * words written five times each. Both format measurements and the ranking check run on this.
 */
internal object RealisticProfile {
    const val CLUSTERS_PER_LETTER = 5
    const val WORD_COUNT = 200
    const val SAMPLES_PER_WORD = 5

    fun build(): PenProfile {
        val random = Random(20260923)
        val letters = seedClusters().filter { it.label in 'a'..'z' }
        val prototypes = letters.flatMap { seed ->
            List(CLUSTERS_PER_LETTER) { index ->
                PrototypeCluster(
                    ClusterId.training(seed.label, "realistic", index),
                    seed.label,
                    jitter(seed.vector, random, SAMPLE_COUNT),
                )
            }
        }
        val words = List(WORD_COUNT) { wordIndex ->
            val word = "word$wordIndex"
            val shape = wordShape(random)
            List(SAMPLES_PER_WORD) { sampleIndex ->
                WordSample(
                    "%08x-%04x".format(wordIndex, sampleIndex),
                    word,
                    jitter(shape, random, WORD_SAMPLE_COUNT),
                    1_700_000_000_000L + wordIndex * 1_000L + sampleIndex,
                )
            }
        }.flatten()
        return PenProfile.create(MotorSettings.Default, prototypes, words)
    }

    fun jitter(vector: FeatureVector, random: Random, sampleCount: Int): FeatureVector {
        val values = vector.copyValues()
        for (index in values.indices) {
            if (index % FEATURE_WIDTH == 2) continue
            values[index] += (random.nextFloat() - 0.5f) * 0.02f
        }
        return FeatureVector.from(values, sampleCount)
    }

    private fun wordShape(random: Random): FeatureVector {
        val values = FloatArray(WORD_SAMPLE_COUNT * FEATURE_WIDTH)
        for (step in 0 until WORD_SAMPLE_COUNT) {
            values[step * FEATURE_WIDTH] = step.toFloat() / WORD_SAMPLE_COUNT
            values[step * FEATURE_WIDTH + 1] = 0.5f + (random.nextFloat() - 0.5f) * 0.6f
            values[step * FEATURE_WIDTH + 2] = if (step % 17 == 0) 1f else 0f
        }
        return FeatureVector.from(values, WORD_SAMPLE_COUNT)
    }

    fun encodeVersionTwo(profile: PenProfile): ByteArray = zipOf(versionTwoJson(profile))

    /** The format v2 writer that shipped before this change, kept here as the size baseline. */
    fun versionTwoJson(profile: PenProfile): ByteArray {
        val json = buildString {
            append("""{"formatVersion":2,"motor":{""")
            append(""""settleMillis":${profile.settings.settleMillis},""")
            append(""""strokeWidthDp":${profile.settings.strokeWidthDp},""")
            append(""""ambiguityThreshold":0.15,""")
            append(""""allowFingerInput":${profile.settings.allowFingerInput}},""")
            append(""""prototypes":[""")
            profile.prototypes.forEachIndexed { index, cluster ->
                if (index > 0) append(',')
                append("""{"id":"${cluster.id.value}","label":"${cluster.label}",""")
                append(""""vector":[${cluster.vector.copyValues().joinToString(",")}]}""")
            }
            append("""],"words":[""")
            profile.words.forEachIndexed { index, sample ->
                if (index > 0) append(',')
                append("""{"id":"${sample.id}","word":"${sample.word}",""")
                append(""""vector":[${sample.vector.copyValues().joinToString(",")}],""")
                append(""""confirmedAt":${sample.confirmedAt}}""")
            }
            append("]}")
        }
        return json.toByteArray(Charsets.UTF_8)
    }

    fun zipOf(payload: ByteArray): ByteArray = ByteArrayOutputStream().use { out ->
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("profile.json"))
            zip.write(payload)
            zip.closeEntry()
        }
        out.toByteArray()
    }
}
