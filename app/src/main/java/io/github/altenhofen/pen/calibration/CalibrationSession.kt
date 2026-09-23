package io.github.altenhofen.pen.calibration

import io.github.altenhofen.pen.ime.Stroke
import io.github.altenhofen.pen.recognition.ClusterId
import io.github.altenhofen.pen.recognition.FeatureVector
import io.github.altenhofen.pen.recognition.PrototypeCluster
import io.github.altenhofen.pen.recognition.bandedDtw
import io.github.altenhofen.pen.recognition.featuresFromStrokes
import java.util.UUID

internal sealed interface CalibrationEvent {
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
) {
    private val samples: Map<Char, MutableList<FeatureVector>> = labels.associateWith { ArrayList(samplesPerLabel) }
    private var activeIndex = 0
    private var finished = false

    val currentLabel: Char?
        get() = if (finished) null else labels[activeIndex]

    val awaitingAdvance: Boolean
        get() {
            val label = currentLabel ?: return false
            return samples.getValue(label).size >= samplesPerLabel
        }

    val isComplete: Boolean
        get() = finished

    val progress: Pair<Int, Int>
        get() {
            val label = currentLabel ?: return samplesPerLabel to samplesPerLabel
            return samples.getValue(label).size to samplesPerLabel
        }

    fun recordSettled(strokes: List<Stroke>): CalibrationEvent {
        val label = currentLabel ?: return CalibrationEvent.Ignored
        if (awaitingAdvance) return CalibrationEvent.Ignored
        val sample = featuresFromStrokes(strokes) ?: return CalibrationEvent.Ignored
        samples.getValue(label).add(sample)
        return CalibrationEvent.NeedMoreSamples
    }

    fun advanceToNextLabel(): CalibrationEvent {
        if (!awaitingAdvance) return CalibrationEvent.Ignored
        if (activeIndex == labels.lastIndex) {
            finished = true
            return CalibrationEvent.ReadyToCommit
        }
        activeIndex++
        return CalibrationEvent.NeedMoreSamples
    }

    fun payload(): CalibrationPayload {
        check(finished) { "payload before every label has $samplesPerLabel samples and the last Next" }
        return CalibrationPayload(
            sessionId,
            labels.map { label ->
                PrototypeCluster(ClusterId.training(label), label, medoid(samples.getValue(label)))
            },
        )
    }

    companion object {
        fun begin(
            labels: List<Char>,
            samplesPerLabel: Int = 3,
        ): CalibrationSession {
            require(labels.isNotEmpty() && labels.distinct().size == labels.size)
            require(samplesPerLabel >= 1)
            return CalibrationSession(UUID.randomUUID().toString(), labels, samplesPerLabel)
        }
    }
}

private fun medoid(vectors: List<FeatureVector>): FeatureVector =
    vectors.minBy { candidate -> vectors.sumOf { bandedDtw(candidate, it).toDouble() } }
