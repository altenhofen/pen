package io.github.altenhofen.pen.ime

import io.github.altenhofen.pen.recognition.Ambiguity
import io.github.altenhofen.pen.recognition.ClusterId
import io.github.altenhofen.pen.recognition.GestureAction
import io.github.altenhofen.pen.recognition.GestureRecognitionResult
import io.github.altenhofen.pen.recognition.RankedGestureMatch
import io.github.altenhofen.pen.recognition.seedVector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureStitchPolicyTest {
    @Test
    fun defersWhenFirstStrokeIsCloseButAmbiguousAmongGestures() {
        val gesture = gestureResult(GestureAction.DeleteLine, distance = 0.25f, gap = 0.05f)
        assertTrue(
            GestureStitchPolicy.shouldDeferNextStroke(
                gesture,
                stitchedStrokeCount = 1,
                GestureAction.MATCH_AMBIGUITY_THRESHOLD,
            ),
        )
    }

    @Test
    fun doesNotDeferWhenSecondStrokeAlreadyPresent() {
        val gesture = gestureResult(GestureAction.DeleteLine, distance = 0.25f, gap = 0.05f)
        assertFalse(
            GestureStitchPolicy.shouldDeferNextStroke(
                gesture,
                stitchedStrokeCount = 2,
                GestureAction.MATCH_AMBIGUITY_THRESHOLD,
            ),
        )
    }

    private fun gestureResult(action: GestureAction, distance: Float, gap: Float): GestureRecognitionResult {
        val winner = RankedGestureMatch(action, ClusterId("g1"), distance, GestureAction.MIN_TRAINING_SAMPLES)
        val runner = RankedGestureMatch(GestureAction.Undo, ClusterId("g2"), distance + gap, GestureAction.MIN_TRAINING_SAMPLES)
        return GestureRecognitionResult(
            winner,
            listOf(winner, runner),
            Ambiguity(gap, GestureAction.MATCH_AMBIGUITY_THRESHOLD),
            seedVector('a'),
        )
    }
}
