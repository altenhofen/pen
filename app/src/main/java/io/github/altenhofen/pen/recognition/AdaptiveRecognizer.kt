package io.github.altenhofen.pen.recognition

import android.content.Context
import io.github.altenhofen.pen.calibration.CalibrationPayload
import io.github.altenhofen.pen.calibration.GestureCalibrationPayload
import io.github.altenhofen.pen.ime.Stroke

internal class AdaptiveRecognizer(
    private val store: PrototypeStore,
    private val gestureStore: GestureStore,
    metric: DistanceMetric = DistanceMetric.DTW,
) {
    private val recognizer = GlyphRecognizer(metric)
    private val gestureRecognizer = GestureRecognizer(metric)

    init {
        reload()
    }

    fun reload() {
        recognizer.replaceAll(store.loadOrSeed())
        gestureRecognizer.replaceAll(gestureStore.load())
    }

    fun recognize(strokes: List<Stroke>, ambiguityThreshold: Float): RecognitionResult? {
        val sample = featuresFromStrokes(strokes) ?: return null
        return recognizer.rank(sample, ambiguityThreshold)
    }

    fun recognizeGesture(strokes: List<Stroke>, ambiguityThreshold: Float): GestureRecognitionResult? {
        val sample = featuresFromStrokes(strokes) ?: return null
        return gestureRecognizer.rank(sample, ambiguityThreshold)
    }

    fun feedback(result: RecognitionResult, feedback: Feedback) {
        val updated = store.adapt(result.winner.clusterId, result.sample, feedback)
        if (updated == null) reload() else recognizer.replace(updated)
    }

    fun commitTraining(payload: CalibrationPayload) {
        store.commitTraining(payload)
        reload()
    }

    fun commitGestureTraining(payload: GestureCalibrationPayload) {
        if (payload.clusters.size >= GestureAction.MIN_TRAINING_SAMPLES) {
            gestureStore.removeTraining(payload.clusters.map { it.action }.distinct())
        }
        gestureStore.commitTraining(payload)
        reload()
    }

    fun clearGestureTraining(actions: Collection<GestureAction>) {
        gestureStore.removeTraining(actions)
        reload()
    }

    fun clearAllGestureTraining() {
        gestureStore.clearAllTraining()
        reload()
    }

    companion object {
        fun open(context: Context): AdaptiveRecognizer =
            AdaptiveRecognizer(PrototypeStore.open(context), GestureStore.open(context))
    }
}
