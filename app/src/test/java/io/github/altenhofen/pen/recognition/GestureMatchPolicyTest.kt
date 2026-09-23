package io.github.altenhofen.pen.recognition

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureMatchPolicyTest {
    @Test
    fun firesWhenGestureIsCloserAndNotAmbiguous() {
        val gesture = gestureResult(GestureAction.DeleteLastWord, distance = 0.1f, gap = 0.3f)
        val glyph = RecognitionResult(
            RankedMatch('a', ClusterId("g"), 0.5f),
            listOf(RankedMatch('a', ClusterId("g"), 0.5f), trained('o')),
            Ambiguity(0.3f, 0.15f, false),
            seedVector('a'),
        )
        assertTrue(GestureMatchPolicy.shouldFire(gesture, glyph, 0.15f))
    }

    @Test
    fun doesNotFireWhenAmbiguousAmongGestures() {
        val gesture = gestureResult(GestureAction.DeleteLastWord, distance = 0.1f, gap = 0.05f)
        val glyph = RecognitionResult(
            trained('a'),
            listOf(trained('a'), trained('o')),
            Ambiguity(0.3f, 0.15f, false),
            seedVector('a'),
        )
        assertFalse(GestureMatchPolicy.shouldFire(gesture, glyph, 0.15f))
    }

    @Test
    fun doesNotFireWhenGlyphIsCloser() {
        val gesture = gestureResult(GestureAction.DeleteLastWord, distance = 0.5f, gap = 0.4f)
        val glyph = RecognitionResult(
            RankedMatch('a', ClusterId("g"), 0.1f),
            listOf(RankedMatch('a', ClusterId("g"), 0.1f), trained('o')),
            Ambiguity(0.3f, 0.15f, false),
            seedVector('a'),
        )
        assertFalse(GestureMatchPolicy.shouldFire(gesture, glyph, 0.15f))
    }

    private fun gestureResult(action: GestureAction, distance: Float, gap: Float): GestureRecognitionResult {
        val winner = RankedGestureMatch(action, ClusterId("g1"), distance, GestureAction.MIN_TRAINING_SAMPLES)
        val runner = RankedGestureMatch(GestureAction.Undo, ClusterId("g2"), distance + gap, GestureAction.MIN_TRAINING_SAMPLES)
        return GestureRecognitionResult(
            winner,
            listOf(winner, runner),
            Ambiguity(gap, 0.15f, gap < 0.15f),
            seedVector('a'),
        )
    }
}
