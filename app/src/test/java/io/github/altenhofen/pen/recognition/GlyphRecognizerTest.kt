package io.github.altenhofen.pen.recognition

import io.github.altenhofen.pen.ime.Stroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class GlyphRecognizerTest {
    @Test
    fun straightLineIsEquidistantAfterResample() {
        val line = listOf(Point2(0f, 0f), Point2(31f, 0f))
        val samples = resampleEquidistant(line, SAMPLE_COUNT)
        assertEquals(SAMPLE_COUNT, samples.size)
        for (index in 1 until samples.size) {
            val step = samples[index].x - samples[index - 1].x
            assertTrue(abs(step - 1f) < 1e-3)
            assertEquals(0f, samples[index].y)
        }
    }

    @Test
    fun tallStrokeKeepsAspect() {
        val features = preprocessPolylines(listOf(listOf(Point2(0f, 0f), Point2(0f, 10f), Point2(2f, 10f))))
        requireNotNull(features)
        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var x = 0f
        var y = 0f
        for (step in 0 until SAMPLE_COUNT) {
            x += features[step, 0]
            y += features[step, 1]
            minX = minOf(minX, x)
            maxX = maxOf(maxX, x)
            minY = minOf(minY, y)
            maxY = maxOf(maxY, y)
        }
        val width = maxX - minX
        val height = maxY - minY
        assertTrue(height > width * 3f)
    }

    @Test
    fun eachSeedMatchesItself() {
        val recognizer = seededRecognizer(DistanceMetric.EUCLIDEAN)
        for (label in seedLabels) {
            val result = recognizer.rank(requireNotNull(featuresFromStrokes(seedStrokes(label))), 0.15f)
            assertEquals(label, result.winner.character)
            assertEquals(ClusterId("seed:$label"), result.winner.clusterId)
            assertTrue(result.winner.distance < 1e-4f)
        }
    }

    @Test
    fun dtwIsZeroOnIdenticalSeeds() {
        val recognizer = seededRecognizer(DistanceMetric.DTW)
        val result = recognizer.rank(requireNotNull(featuresFromStrokes(seedStrokes('8'))), 0.15f)
        assertEquals('8', result.winner.character)
        assertEquals(ClusterId("seed:8"), result.winner.clusterId)
        assertTrue(result.winner.distance < 1e-4f)
    }

    @Test
    fun duplicateLabelClustersCollapseBeforeRankingAndAmbiguity() {
        val nine = seedVector('9')
        val nearNine = FeatureVector.from(nine.copyValues().map { it + 0.01f }.toFloatArray())
        val eight = seedVector('8')
        val recognizer = GlyphRecognizer(DistanceMetric.DTW)
        recognizer.replaceAll(
            listOf(
                PrototypeCluster(ClusterId("cal:s:9"), '9', nearNine),
                PrototypeCluster(ClusterId("seed:8"), '8', eight),
                PrototypeCluster(ClusterId("seed:9"), '9', nine),
            ),
        )
        val nearNineDistance = bandedDtw(nine, nearNine)
        val eightDistance = bandedDtw(nine, eight)
        assertTrue(nearNineDistance < eightDistance)
        val between = (nearNineDistance + eightDistance) / 2f

        val result = recognizer.rank(nine, between)

        assertEquals(ClusterId("seed:9"), result.winner.clusterId)
        assertEquals(listOf('9', '8'), result.ranked.map { it.character })
        assertEquals(listOf(ClusterId("seed:9"), ClusterId("seed:8")), result.ranked.map { it.clusterId })
        assertEquals(eightDistance - result.winner.distance, result.ambiguity.gap, 1e-6f)
        assertTrue(result.ambiguity.gap >= between)
        val tight = recognizer.rank(nine, eightDistance + 0.01f)
        assertTrue(tight.ambiguity.gap < tight.ambiguity.threshold)
    }

    @Test
    fun heavilyTrainedLabelDoesNotWinByClusterCount() {
        val sample = seedVector('8')
        fun shifted(by: Float) = FeatureVector.from(sample.copyValues().map { it + by }.toFloatArray())
        val far = seedVector('0')
        val manyW = listOf(PrototypeCluster(ClusterId("train:w:s:0"), 'w', shifted(0.002f))) +
            (1 until 20).map { PrototypeCluster(ClusterId("train:w:s:$it"), 'w', far) }
        val oneB = PrototypeCluster(ClusterId("train:b:s:0"), 'b', shifted(0.004f))
        val recognizer = GlyphRecognizer(DistanceMetric.EUCLIDEAN)
        recognizer.replaceAll(manyW + oneB)

        val result = recognizer.rank(sample, 0.01f)

        assertEquals(listOf('b', 'w'), result.ranked.map { it.character })
        assertEquals(ClusterId("train:w:s:0"), result.ranked[1].clusterId)
    }

    @Test
    fun attractMovesPrototypeTowardSample() {
        val prototype = floatArrayOf(0f, 0f, 0f)
        val sample = floatArrayOf(1f, 0f, 0f)
        val updated = PrototypeUpdate.attract(prototype, sample, PrototypeUpdate.ACCEPT_REWARD)
        assertEquals(0.05, updated[0].toDouble(), 1e-5)
        assertEquals(0.0, updated[1].toDouble(), 1e-5)
    }

    @Test
    fun repelMovesPrototypeAwayFromSample() {
        val prototype = floatArrayOf(0f, 0f, 0f)
        val sample = floatArrayOf(1f, 0f, 0f)
        val updated = PrototypeUpdate.repel(prototype, sample)
        assertEquals(-0.02, updated[0].toDouble(), 1e-5)
    }

    @Test
    fun eightIsNotZero() {
        assertTrue(meanEuclidean(seedVector('8'), seedVector('0')) > 0.02f)
    }
}

internal fun seededRecognizer(metric: DistanceMetric): GlyphRecognizer =
    GlyphRecognizer(metric).apply { replaceAll(seedClusters()) }

internal fun seedVector(label: Char): FeatureVector = requireNotNull(preprocessPolylines(seedPolylines(label)))

internal fun seedStrokes(label: Char, jitter: Float = 0f): List<Stroke> = seedPolylines(label).map { polyline ->
    val stroke = Stroke()
    for ((index, point) in polyline.withIndex()) {
        val offset = if (index % 2 == 0) jitter else -jitter
        stroke.append(point.x + offset, point.y - offset)
    }
    stroke
}
