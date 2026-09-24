package io.github.altenhofen.pen.ime

import io.github.altenhofen.pen.recognition.GestureAction
import io.github.altenhofen.pen.recognition.GestureMatchPolicy
import io.github.altenhofen.pen.recognition.GestureRecognitionResult

/** Wait briefly for the next stroke of multi-stroke gestures such as //. */
internal object GestureStitchPolicy {
    const val STITCH_MS = 1_200L

    fun shouldDeferNextStroke(
        gesture: GestureRecognitionResult?,
        stitchedStrokeCount: Int,
        threshold: Float,
    ): Boolean {
        if (stitchedStrokeCount != 1) return false
        if (gesture == null) return false
        if (gesture.winner.distance > GestureAction.MAX_FIRE_DISTANCE) return false
        if (GestureMatchPolicy.shouldFire(gesture, threshold)) return false
        return gesture.ambiguity.gap < threshold
    }
}
