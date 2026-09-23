package io.github.altenhofen.pen.calibration

import io.github.altenhofen.pen.ime.Stroke
import io.github.altenhofen.pen.recognition.ClusterId
import io.github.altenhofen.pen.recognition.FeatureVector
import io.github.altenhofen.pen.recognition.PrototypeCluster
import io.github.altenhofen.pen.recognition.bandedDtw
import io.github.altenhofen.pen.recognition.featuresFromStrokes
import java.util.UUID

sealed interface CalibrationEvent {
    data object NeedMoreSamples : CalibrationEvent
    data object ReadyToCommit : CalibrationEvent
    data object Ignored : CalibrationEvent
}

internal data class CalibrationPayload(
    val sessionId: String,
    val clusters: List<PrototypeCluster>,
)

internal class CalibrationSession private constructor(
    val sessionId: String,
    val labels: List<Char>,
    val samplesPerLabel: Int,
    private val oneClusterPerSample: Boolean,
) {
    private val samples: Map<Char, MutableList<FeatureVector>> = labels.associateWith { ArrayList(samplesPerLabel) }

    val currentLabel: Char?
        get() = labels.firstOrNull { samples.getValue(it).size < samplesPerLabel }

    val progress: Pair<Int, Int>
        get() = (currentLabel?.let { samples.getValue(it).size } ?: samplesPerLabel) to samplesPerLabel

    fun recordSettled(strokes: List<Stroke>): CalibrationEvent {
        val label = currentLabel ?: return CalibrationEvent.Ignored
        val sample = featuresFromStrokes(strokes) ?: return CalibrationEvent.Ignored
        samples.getValue(label).add(sample)
        return if (currentLabel == null) CalibrationEvent.ReadyToCommit else CalibrationEvent.NeedMoreSamples
    }

    fun payload(): CalibrationPayload {
        check(currentLabel == null) { "payload before every label has $samplesPerLabel samples" }
        val clusters = if (oneClusterPerSample) {
            labels.flatMap { label ->
                samples.getValue(label).mapIndexed { index, vector ->
                    PrototypeCluster(ClusterId.userTraining(sessionId, label, index), label, vector)
                }
            }
        } else {
            labels.map { label ->
                PrototypeCluster(ClusterId.calibration(sessionId, label), label, medoid(samples.getValue(label)))
            }
        }
        return CalibrationPayload(sessionId, clusters)
    }

    companion object {
        fun begin(
            labels: List<Char>,
            samplesPerLabel: Int = 5,
            oneClusterPerSample: Boolean = false,
        ): CalibrationSession {
            require(labels.isNotEmpty() && labels.distinct().size == labels.size)
            require(samplesPerLabel >= 1)
            return CalibrationSession(
                UUID.randomUUID().toString(),
                labels,
                samplesPerLabel,
                oneClusterPerSample,
            )
        }
    }
}

/** Digits and lowercase letters seeded in phase 2; same set users fine-tune in phase 5. */
val FineTuneAlphabet: List<Char> = ('0'..'9').toList() + ('a'..'z').toList()

private fun medoid(vectors: List<FeatureVector>): FeatureVector =
    vectors.minBy { candidate -> vectors.sumOf { bandedDtw(candidate, it).toDouble() } }
