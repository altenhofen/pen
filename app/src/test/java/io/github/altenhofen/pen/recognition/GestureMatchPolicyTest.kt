package io.github.altenhofen.pen.recognition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureMatchPolicyTest {
    @Test
    fun firesWhenGestureBeatsGlyphAndStaysWithinMaxDistance() {
        val gesture = gestureResult(GestureAction.DeleteLastWord, distance = 0.1f, gap = 0.3f)
        val glyph = glyphResult(distance = 0.5f)
        assertTrue(GestureMatchPolicy.shouldFire(gesture, glyph, 0.15f))
    }

    @Test
    fun doesNotFireWhenAmbiguousAmongGestures() {
        val gesture = gestureResult(GestureAction.DeleteLastWord, distance = 0.1f, gap = 0.05f)
        val glyph = glyphResult(distance = 0.8f)
        assertFalse(GestureMatchPolicy.shouldFire(gesture, glyph, 0.15f))
    }

    @Test
    fun doesNotFireWhenGlyphIsCloser() {
        val gesture = gestureResult(GestureAction.DeleteLastWord, distance = 0.25f, gap = 0.4f)
        val glyph = glyphResult(distance = 0.1f)
        assertFalse(GestureMatchPolicy.shouldFire(gesture, glyph, 0.15f))
    }

    @Test
    fun doesNotFireWhenGestureDistanceExceedsMax() {
        val gesture = gestureResult(GestureAction.DeleteLastWord, distance = 0.6f, gap = 0.4f)
        assertFalse(GestureMatchPolicy.shouldFire(gesture, glyphResult(distance = 0.9f), 0.15f))
    }

    @Test
    fun letterInkDoesNotFireAnUnrelatedTrainedGesture() {
        val gestures = GestureRecognizer()
        val trained = seedVector('x')
        gestures.replaceAll(
            (0 until GestureAction.MIN_TRAINING_SAMPLES).map { index ->
                GestureCluster(ClusterId("g$index"), GestureAction.DeleteLastWord, trained)
            },
        )
        val glyphs = GlyphRecognizer()
        glyphs.replaceAll(seedClusters())
        val sample = seedVector('a')
        val gesture = gestures.rank(sample, 0.15f)
        val glyph = glyphs.rank(sample, 0.15f)
        assertEquals('a', glyph.winner.character)
        assertFalse(GestureMatchPolicy.shouldFire(gesture, glyph, 0.15f))
    }

    @Test
    fun trainedDigitShapeFiresInsteadOfCommittingTheSeedDigit() {
        val gestures = GestureRecognizer()
        val trained = seedVector('5')
        gestures.replaceAll(
            (0 until GestureAction.MIN_TRAINING_SAMPLES).map { index ->
                GestureCluster(ClusterId("g$index"), GestureAction.DeleteLastWord, trained)
            },
        )
        val glyphs = GlyphRecognizer()
        glyphs.replaceAll(seedClusters())
        val sample = seedVector('5')
        val gesture = gestures.rank(sample, 0.15f)
        val glyph = glyphs.rank(sample, 0.15f)
        assertEquals('5', glyph.winner.character)
        assertTrue(GestureMatchPolicy.shouldFire(gesture, glyph, 0.15f))
    }

    @Test
    fun jitteredTrainedDigitStillFiresAgainstSeedDigit() {
        val gestures = GestureRecognizer()
        val trained = seedVector('5')
        gestures.replaceAll(
            (0 until GestureAction.MIN_TRAINING_SAMPLES).map { index ->
                GestureCluster(ClusterId("g$index"), GestureAction.DeleteLastWord, trained)
            },
        )
        val glyphs = GlyphRecognizer()
        glyphs.replaceAll(seedClusters())
        val sample = requireNotNull(featuresFromStrokes(seedStrokes('5', jitter = 0.4f)))
        val gesture = gestures.rank(sample, 0.15f)
        val glyph = glyphs.rank(sample, 0.15f)
        assertTrue(GestureMatchPolicy.shouldFire(gesture, glyph, 0.15f))
    }

    @Test
    fun matchingTrainedShapeFiresWhenItBeatsLetterTemplates() {
        val gestures = GestureRecognizer()
        val trained = seedVector('x')
        gestures.replaceAll(
            (0 until GestureAction.MIN_TRAINING_SAMPLES).map { index ->
                GestureCluster(ClusterId("g$index"), GestureAction.DeleteLastWord, trained)
            },
        )
        val glyphs = GlyphRecognizer()
        glyphs.replaceAll(seedClusters().filter { it.label != 'x' })
        val sample = seedVector('x')
        val gesture = gestures.rank(sample, 0.15f)
        val glyph = glyphs.rank(sample, 0.15f)
        assertTrue(GestureMatchPolicy.shouldFire(gesture, glyph, 0.15f))
    }

    @Test
    fun triangleInkIsNotShapeCompatibleWithFive() {
        assertFalse(shapeCompatible(shapeSignature(triangleVector()), shapeSignature(seedVector('5'))))
    }

    @Test
    fun trainedTriangleDoesNotFireOnFive() {
        val gestures = GestureRecognizer()
        val trained = triangleVector()
        gestures.replaceAll(
            (0 until GestureAction.MIN_TRAINING_SAMPLES).map { index ->
                GestureCluster(ClusterId("g$index"), GestureAction.DeleteLastWord, trained)
            },
        )
        val glyphs = GlyphRecognizer()
        glyphs.replaceAll(seedClusters())
        val sample = seedVector('5')
        val gesture = gestures.rank(sample, 0.15f)
        val glyph = glyphs.rank(sample, 0.15f)
        assertEquals('5', glyph.winner.character)
        assertFalse(GestureMatchPolicy.shouldFire(gesture, glyph, 0.15f))
    }

    @Test
    fun trainedTriangleFiresOnTriangleInk() {
        val gestures = GestureRecognizer()
        val trained = triangleVector()
        gestures.replaceAll(
            (0 until GestureAction.MIN_TRAINING_SAMPLES).map { index ->
                GestureCluster(ClusterId("g$index"), GestureAction.DeleteLastWord, trained)
            },
        )
        val glyphs = GlyphRecognizer()
        glyphs.replaceAll(seedClusters())
        val sample = triangleVector()
        val gesture = gestures.rank(sample, 0.15f)
        val glyph = glyphs.rank(sample, 0.15f)
        assertTrue(GestureMatchPolicy.shouldFire(gesture, glyph, 0.15f))
    }

    private fun triangleVector(): FeatureVector = requireNotNull(
        preprocessPolylines(
            listOf(
                listOf(Point2(1f, 8f), Point2(5f, 1f), Point2(9f, 8f), Point2(1f, 8f)),
            ),
        ),
    )

    private fun gestureResult(action: GestureAction, distance: Float, gap: Float): GestureRecognitionResult {
        val winner = RankedGestureMatch(action, ClusterId("g1"), distance, GestureAction.MIN_TRAINING_SAMPLES)
        val runner = RankedGestureMatch(GestureAction.Undo, ClusterId("g2"), distance + gap, GestureAction.MIN_TRAINING_SAMPLES)
        return GestureRecognitionResult(
            winner,
            listOf(winner, runner),
            Ambiguity(gap, 0.15f),
            seedVector('a'),
        )
    }

    private fun glyphResult(distance: Float): RecognitionResult {
        val winner = RankedMatch('a', ClusterId("g"), distance)
        return RecognitionResult(
            winner,
            listOf(winner, trained('o')),
            Ambiguity(0.3f, 0.15f),
            seedVector('a'),
        )
    }
}
