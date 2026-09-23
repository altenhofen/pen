package io.github.altenhofen.pen.recognition

import io.github.altenhofen.pen.ime.Stroke
import org.junit.Assert.assertEquals
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
        val recognizer = GlyphRecognizer.seeded(DistanceMetric.EUCLIDEAN)
        for (label in seedLabels) {
            val strokes = seedPolylines(label).map { polyline ->
                val stroke = Stroke()
                for (point in polyline) stroke.append(point.x, point.y)
                stroke
            }
            val match = recognizer.recognize(strokes)
            assertEquals(label, match?.character)
            assertTrue((match?.distance ?: 1f) < 1e-4f)
        }
    }

    @Test
    fun dtwIsZeroOnIdenticalSeeds() {
        val recognizer = GlyphRecognizer.seeded(DistanceMetric.DTW)
        val strokes = seedPolylines('8').map { polyline ->
            val stroke = Stroke()
            for (point in polyline) stroke.append(point.x, point.y)
            stroke
        }
        val match = recognizer.recognize(strokes)
        assertEquals('8', match?.character)
        assertTrue((match?.distance ?: 1f) < 1e-4f)
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
        val eight = preprocessPolylines(seedPolylines('8'))
        val zero = preprocessPolylines(seedPolylines('0'))
        requireNotNull(eight)
        requireNotNull(zero)
        assertTrue(meanEuclidean(eight, zero) > 0.02f)
    }
}
