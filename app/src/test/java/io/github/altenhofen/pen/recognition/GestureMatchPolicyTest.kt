package io.github.altenhofen.pen.recognition

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureMatchPolicyTest {
    @Test
    fun firesWhenGestureIsUnambiguousAndWithinMaxDistance() {
        val gesture = gestureResult(GestureAction.DeleteLastWord, distance = 0.1f, gap = 0.3f)
        assertTrue(GestureMatchPolicy.shouldFire(gesture, 0.15f))
    }

    @Test
    fun doesNotFireWhenAmbiguousAmongGestures() {
        val gesture = gestureResult(GestureAction.DeleteLastWord, distance = 0.1f, gap = 0.05f)
        assertFalse(GestureMatchPolicy.shouldFire(gesture, 0.15f))
    }

    @Test
    fun firesWhenGestureBeatsSecondActionEvenIfNotLetterClose() {
        val gesture = gestureResult(GestureAction.DeleteLastWord, distance = 0.3f, gap = 0.2f)
        assertTrue(GestureMatchPolicy.shouldFire(gesture, 0.15f))
    }

    @Test
    fun doesNotFireWhenGestureDistanceExceedsMax() {
        val gesture = gestureResult(GestureAction.DeleteLastWord, distance = 0.6f, gap = 0.4f)
        assertFalse(GestureMatchPolicy.shouldFire(gesture, GestureAction.MATCH_AMBIGUITY_THRESHOLD))
    }

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
}
