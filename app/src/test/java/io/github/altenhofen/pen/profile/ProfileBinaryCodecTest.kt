package io.github.altenhofen.pen.profile

import io.github.altenhofen.pen.recognition.ClusterId
import io.github.altenhofen.pen.recognition.FeatureVector
import io.github.altenhofen.pen.recognition.GlyphRecognizer
import io.github.altenhofen.pen.recognition.PrototypeCluster
import io.github.altenhofen.pen.recognition.SAMPLE_COUNT
import io.github.altenhofen.pen.recognition.WORD_SAMPLE_COUNT
import io.github.altenhofen.pen.recognition.WordMemory
import io.github.altenhofen.pen.recognition.WordMemorySource
import io.github.altenhofen.pen.recognition.WordRecall
import io.github.altenhofen.pen.recognition.WordSample
import io.github.altenhofen.pen.recognition.seedClusters
import io.github.altenhofen.pen.settings.HandwritingLanguage
import io.github.altenhofen.pen.settings.InkLanguage
import io.github.altenhofen.pen.settings.MotorSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

/** The format v3 body: what it keeps, what it leaves out, and what quantization costs. */
class ProfileBinaryCodecTest {
    @Test
    fun roundTripKeepsEverySetting() {
        val tuned = MotorSettings.Default
            .withSettleMillis(900L)
            .withStrokeWidthDp(5.5f)
            .withAmbiguityThreshold(0.2f)
            .withAllowFingerInput(true)
        val decoded = roundTrip(profileOf(settings = tuned))
        assertEquals(900L, decoded.settings.settleMillis)
        assertEquals(5.5f, decoded.settings.strokeWidthDp)
        assertEquals(0.2f, decoded.settings.ambiguityThreshold)
        assertEquals(true, decoded.settings.allowFingerInput)
    }

    @Test
    fun roundTripKeepsHandwritingLanguage() {
        val tuned = MotorSettings.Default.withHandwriting(HandwritingLanguage.Explicit(InkLanguage.Portuguese))
        val decoded = roundTrip(profileOf(settings = tuned))
        assertEquals(HandwritingLanguage.Explicit(InkLanguage.Portuguese), decoded.settings.handwriting)
    }

    @Test
    fun roundTripKeepsClusterIdentityAndWordMemory() {
        val profile = profileOf(
            clusters = listOf(cluster("train:q:session:2", 'q', 0.25f)),
            words = listOf(
                wordSample("w-1", "augusto", 1_700_000_000_123L),
                wordSample("w-2", "café", 1_700_000_009_999L),
            ),
        )
        val decoded = roundTrip(profile)
        val restored = decoded.prototypes.single { it.id.value == "train:q:session:2" }
        assertEquals('q', restored.label)
        assertEquals(listOf("w-1", "w-2"), decoded.words.map { it.id })
        assertEquals(listOf("augusto", "café"), decoded.words.map { it.word })
        assertEquals(listOf(1_700_000_000_123L, 1_700_000_009_999L), decoded.words.map { it.confirmedAt })
        assertEquals(WORD_SAMPLE_COUNT, decoded.words.first().vector.sampleCount)
    }

    @Test
    fun untouchedSeedsCostNothingAndComeBack() {
        val seeds = seedClusters()
        val body = ProfileBinaryCodec.encode(PenProfile.create(MotorSettings.Default, seeds))
        assertEquals(36, seeds.size)
        assertTrue("36 untouched seeds took ${body.size} bytes", body.size < 20)
        val decoded = ProfileBinaryCodec.decode(body)
        assertEquals(36, decoded.prototypes.size)
        assertEquals(seeds.map { it.id.value }.sorted(), decoded.prototypes.map { it.id.value }.sorted())
    }

    @Test
    fun adaptedSeedSurvivesInsteadOfBeingRegenerated() {
        val seeds = seedClusters()
        val seed = seeds.first { it.label == 'o' }
        val adapted = PrototypeCluster(seed.id, 'o', RealisticProfile.jitter(seed.vector, Random(7), SAMPLE_COUNT))
        val decoded = roundTrip(PenProfile.create(MotorSettings.Default, seeds.map { if (it.id == seed.id) adapted else it }))
        val restored = decoded.prototypes.single { it.id == seed.id }
        assertTrue(
            "an adapted seed must not come back as the pristine seed",
            maxComponentGap(restored.vector, seed.vector) > 0.001f,
        )
        assertTrue(maxComponentGap(restored.vector, adapted.vector) < 0.001f)
    }

    @Test
    fun quantizationKeepsGlyphRanking() {
        val profile = RealisticProfile.build()
        val trained = roundTrip(profile).prototypes.filter { it.id.value.startsWith("train:") }
        assertEquals(profile.prototypes.size, trained.size)
        val probe = RealisticProfile.jitter(profile.prototypes.first { it.label == 'e' }.vector, Random(99), SAMPLE_COUNT)

        val before = GlyphRecognizer().apply { replaceAll(profile.prototypes) }.rank(probe, 0.15f).ranked
        val after = GlyphRecognizer().apply { replaceAll(trained) }.rank(probe, 0.15f).ranked

        assertEquals('e', before.first().character)
        assertEquals(before.map { it.character }, after.map { it.character })
        val worst = before.zip(after).maxOf { (left, right) -> abs(left.distance - right.distance) }
        println("largest glyph distance shift: $worst over ${before.size} labels")
        assertTrue("largest glyph distance shift was $worst", worst < 0.002f)
    }

    @Test
    fun quantizationKeepsWordRecall() {
        val profile = RealisticProfile.build()
        val decoded = roundTrip(profile)
        val probe = RealisticProfile.jitter(profile.words.first().vector, Random(101), WORD_SAMPLE_COUNT)

        val before = WordMemory(profile.words).recall(probe)
        val after = WordMemory(decoded.words).recall(probe)

        assertEquals("word0", before.first().word)
        assertEquals(matchable(before), matchable(after))
        val byWord = after.associate { it.word to it.distance }
        val worst = before.maxOf { abs(it.distance - byWord.getValue(it.word)) }
        println("largest recall distance shift: $worst over ${before.size} words")
        assertTrue("largest recall distance shift was $worst", worst < 0.002f)
    }

    @Test
    fun aTruncatedBodyFails() {
        val body = ProfileBinaryCodec.encode(RealisticProfile.build())
        assertEquals(
            "truncated profile body",
            failureOf { ProfileBinaryCodec.decode(body.copyOf(body.size / 2)) }.message,
        )
    }

    @Test
    fun aBodyWrittenForAnotherVectorSizeIsRejected() {
        val body = ProfileBinaryCodec.encode(profileOf())
        body[0] = 16
        assertEquals(
            "archive was written for a different feature vector size",
            failureOf { ProfileBinaryCodec.decode(body) }.message,
        )
    }

    @Test
    fun theBodyIsSmallerThanTheVersionTwoArchive() {
        val profile = RealisticProfile.build()
        val plainJson = RealisticProfile.versionTwoJson(profile).size
        val versionTwoZip = RealisticProfile.encodeVersionTwo(profile).size
        val body = ProfileBinaryCodec.encode(profile).size
        println("v2 JSON $plainJson, v2 zip $versionTwoZip, v3 body $body")
        assertTrue("v2 zip $versionTwoZip, v3 body $body", body * 2 < versionTwoZip)
    }

    private fun matchable(recalls: List<WordRecall>) =
        recalls.filter { it.distance < WordMemorySource.WORD_MATCH_DISTANCE }.map { it.word }

    private fun roundTrip(profile: PenProfile): PenProfile =
        ProfileBinaryCodec.decode(ProfileBinaryCodec.encode(profile))

    private fun maxComponentGap(left: FeatureVector, right: FeatureVector): Float =
        left.copyValues().zip(right.copyValues().toList()).maxOf { (a, b) -> abs(a - b) }

    private fun failureOf(action: () -> Unit): ProfileTransferException = try {
        action()
        throw AssertionError("expected a ProfileTransferException")
    } catch (error: ProfileTransferException) {
        error
    }

    private fun profileOf(
        settings: MotorSettings = MotorSettings.Default,
        clusters: List<PrototypeCluster> = listOf(cluster("train:8:session:0", '8', 0.1f)),
        words: List<WordSample> = emptyList(),
    ) = PenProfile.create(settings, clusters, words)

    private fun cluster(id: String, label: Char, magnitude: Float) = PrototypeCluster(
        ClusterId(id),
        label,
        FeatureVector.from(FloatArray(96) { index -> if (index % 3 == 2) 0f else magnitude * (index % 7 - 3) / 3f }),
    )

    private fun wordSample(id: String, word: String, confirmedAt: Long) = WordSample(
        id,
        word,
        FeatureVector.from(FloatArray(WORD_SAMPLE_COUNT * 3) { it * 0.002f }, WORD_SAMPLE_COUNT),
        confirmedAt,
    )
}
