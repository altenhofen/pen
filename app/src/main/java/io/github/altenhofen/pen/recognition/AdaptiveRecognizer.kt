package io.github.altenhofen.pen.recognition

import android.content.Context
import io.github.altenhofen.pen.calibration.CalibrationPayload
import io.github.altenhofen.pen.ime.Stroke

internal class AdaptiveRecognizer(
    private val store: PrototypeStore,
    metric: DistanceMetric = DistanceMetric.DTW,
) {
    private val recognizer = GlyphRecognizer(metric)

    fun reload() {
        recognizer.replaceAll(store.loadOrSeed())
    }

    fun recognize(strokes: List<Stroke>, ambiguityThreshold: Float): RecognitionResult? {
        val sample = featuresFromStrokes(strokes) ?: return null
        return recognizer.rank(sample, ambiguityThreshold)
    }

    fun feedback(result: RecognitionResult, feedback: Feedback) {
        recognizer.replace(store.adapt(result.winner.clusterId, result.sample, feedback))
    }

    fun commitCalibration(payload: CalibrationPayload) {
        store.commitCalibration(payload)
        reload()
    }

    fun commitUserTraining(payload: CalibrationPayload) {
        store.commitUserTraining(payload)
        reload()
    }

    companion object {
        fun open(context: Context): AdaptiveRecognizer = AdaptiveRecognizer(PrototypeStore.open(context))
    }
}
