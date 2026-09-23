package io.github.altenhofen.pen.calibration

import io.github.altenhofen.pen.ime.Stroke
import io.github.altenhofen.pen.recognition.ClusterId
import io.github.altenhofen.pen.recognition.FeatureVector
import io.github.altenhofen.pen.recognition.PrototypeCluster
import io.github.altenhofen.pen.recognition.featuresFromStrokes
import java.util.UUID

internal sealed interface CalibrationEvent {
    data object Recorded : CalibrationEvent
    data class Advanced(val payload: CalibrationPayload) : CalibrationEvent
    data class ReadyToCommit(val payload: CalibrationPayload) : CalibrationEvent
    data object Ignored : CalibrationEvent
}

internal data class CalibrationPayload(
    val sessionId: String,
    val clusters: List<PrototypeCluster>,
)

internal class CalibrationSession private constructor(
    val sessionId: String,
    val labels: List<Char>,
) {
    private val samples: Map<Char, MutableList<FeatureVector>> = labels.associateWith { ArrayList() }
    private var activeIndex = 0
    private var finished = false

    val currentLabel: Char?
        get() = if (finished) null else labels[activeIndex]

    val sampleCount: Int
        get() = currentLabel?.let { samples.getValue(it).size } ?: 0

    val canAdvance: Boolean
        get() = currentLabel != null && samples.getValue(currentLabel!!).isNotEmpty()

    fun recordInk(strokes: List<Stroke>): CalibrationEvent {
        val label = currentLabel ?: return CalibrationEvent.Ignored
        var recorded = false
        for (glyph in partitionGlyphs(strokes)) {
            val sample = featuresFromStrokes(glyph) ?: continue
            samples.getValue(label).add(sample)
            recorded = true
        }
        return if (recorded) CalibrationEvent.Recorded else CalibrationEvent.Ignored
    }

    fun advanceToNextLabel(): CalibrationEvent {
        val label = currentLabel ?: return CalibrationEvent.Ignored
        if (!canAdvance) return CalibrationEvent.Ignored
        val payload = CalibrationPayload(
            sessionId,
            samples.getValue(label).mapIndexed { index, vector ->
                PrototypeCluster(ClusterId.training(label, sessionId, index), label, vector)
            },
        )
        if (activeIndex == labels.lastIndex) {
            finished = true
            return CalibrationEvent.ReadyToCommit(payload)
        }
        activeIndex++
        return CalibrationEvent.Advanced(payload)
    }

    companion object {
        fun begin(labels: List<Char>): CalibrationSession {
            require(labels.isNotEmpty() && labels.distinct().size == labels.size)
            return CalibrationSession(UUID.randomUUID().toString(), labels)
        }
    }
}

internal fun partitionGlyphs(strokes: List<Stroke>, gapPx: Float = 48f): List<List<Stroke>> {
    val boxes = strokes.mapNotNull { stroke ->
        val points = stroke.points()
        if (points.isEmpty()) null
        else StrokeBox(
            stroke,
            points.minOf { it.x },
            points.maxOf { it.x },
        )
    }.sortedBy { it.minX }
    if (boxes.isEmpty()) return emptyList()
    val groups = mutableListOf(mutableListOf(boxes.first()))
    for (i in 1 until boxes.size) {
        val current = boxes[i]
        val group = groups.last()
        val prevMaxX = group.maxOf { it.maxX }
        if (current.minX > prevMaxX + gapPx) {
            groups.add(mutableListOf(current))
        } else {
            group.add(current)
        }
    }
    return groups.map { group -> group.map { it.stroke } }
}

private data class StrokeBox(val stroke: Stroke, val minX: Float, val maxX: Float)
