package io.github.altenhofen.pen.calibration

import io.github.altenhofen.pen.ime.Stroke
import io.github.altenhofen.pen.recognition.ClusterId
import io.github.altenhofen.pen.recognition.FeatureVector
import io.github.altenhofen.pen.recognition.GestureAction
import io.github.altenhofen.pen.recognition.GestureCluster
import io.github.altenhofen.pen.recognition.featuresFromStrokes

internal data class GestureCalibrationPayload(
    val sessionId: String,
    val clusters: List<GestureCluster>,
)

internal sealed interface GestureCalibrationEvent {
    data object Recorded : GestureCalibrationEvent
    data class Advanced(val payload: GestureCalibrationPayload) : GestureCalibrationEvent
    data class ReadyToCommit(val payload: GestureCalibrationPayload) : GestureCalibrationEvent
    data object Ignored : GestureCalibrationEvent
}

internal sealed interface GestureCalibrationPhase {
    data class PickActions(
        val selected: Set<GestureAction>,
        val sampleCounts: Map<GestureAction, Int>,
    ) : GestureCalibrationPhase

    data class WriteGestures(val session: GestureCalibrationSession) : GestureCalibrationPhase
    data object Complete : GestureCalibrationPhase
}

internal class GestureCalibrationSession private constructor(
    val sessionId: String,
    val actions: List<GestureAction>,
) {
    private val samples: Map<GestureAction, MutableList<FeatureVector>> = actions.associateWith { ArrayList() }
    private var activeIndex = 0
    private var finished = false

    val currentAction: GestureAction?
        get() = if (finished) null else actions[activeIndex]

    val sampleCount: Int
        get() = currentAction?.let { samples.getValue(it).size } ?: 0

    val canAdvance: Boolean
        get() = currentAction != null && samples.getValue(currentAction!!).size >= GestureAction.MIN_TRAINING_SAMPLES

    fun recordInk(strokes: List<Stroke>): GestureCalibrationEvent {
        val action = currentAction ?: return GestureCalibrationEvent.Ignored
        var recorded = false
        for (glyph in partitionGlyphs(strokes)) {
            val sample = featuresFromStrokes(glyph) ?: continue
            samples.getValue(action).add(sample)
            recorded = true
        }
        return if (recorded) GestureCalibrationEvent.Recorded else GestureCalibrationEvent.Ignored
    }

    fun payloadForCurrentAction(): GestureCalibrationPayload? {
        val action = currentAction ?: return null
        val vectors = samples.getValue(action)
        if (vectors.isEmpty()) return null
        return GestureCalibrationPayload(
            sessionId,
            vectors.mapIndexed { index, vector ->
                GestureCluster(ClusterId.trainingGesture(action.id, sessionId, index), action, vector)
            },
        )
    }

    fun advanceToNextAction(): GestureCalibrationEvent {
        val action = currentAction ?: return GestureCalibrationEvent.Ignored
        if (!canAdvance) return GestureCalibrationEvent.Ignored
        val payload = GestureCalibrationPayload(
            sessionId,
            samples.getValue(action).mapIndexed { index, vector ->
                GestureCluster(ClusterId.trainingGesture(action.id, sessionId, index), action, vector)
            },
        )
        if (activeIndex == actions.lastIndex) {
            finished = true
            return GestureCalibrationEvent.ReadyToCommit(payload)
        }
        activeIndex++
        return GestureCalibrationEvent.Advanced(payload)
    }

    companion object {
        fun begin(actions: List<GestureAction>): GestureCalibrationSession {
            require(actions.isNotEmpty() && actions.distinct().size == actions.size)
            return GestureCalibrationSession(java.util.UUID.randomUUID().toString(), actions)
        }
    }
}

internal class GestureCalibrationMinigame {
    private var selected = emptySet<GestureAction>()
    private var session: GestureCalibrationSession? = null
    private var complete = false
    private var trainedCount = 0
    var sampleCounts: Map<GestureAction, Int> = emptyMap()
        private set

    fun phase(): GestureCalibrationPhase = when {
        complete -> GestureCalibrationPhase.Complete
        session != null -> GestureCalibrationPhase.WriteGestures(session!!)
        else -> GestureCalibrationPhase.PickActions(selected, sampleCounts)
    }

    fun trainedCount(): Int = trainedCount

    fun refreshCounts(counts: Map<GestureAction, Int>) {
        sampleCounts = counts
    }

    fun toggle(action: GestureAction) {
        if (session != null || complete) return
        selected = if (action in selected) selected - action else selected + action
    }

    fun selectAll() {
        if (session != null || complete) return
        selected = GestureAction.catalog.toSet()
    }

    fun selectNone() {
        if (session != null || complete) return
        selected = emptySet()
    }

    fun start() {
        if (session != null || complete) return
        val actions = GestureAction.catalog.filter { it in selected }
        if (actions.isEmpty()) return
        session = GestureCalibrationSession.begin(actions)
    }

    fun recordInk(strokes: List<Stroke>): GestureCalibrationEvent {
        val active = session ?: return GestureCalibrationEvent.Ignored
        if (complete) return GestureCalibrationEvent.Ignored
        return active.recordInk(strokes)
    }

    fun payloadForCurrentAction(): GestureCalibrationPayload? = session?.payloadForCurrentAction()

    fun next(): GestureCalibrationEvent {
        val active = session ?: return GestureCalibrationEvent.Ignored
        if (complete) return GestureCalibrationEvent.Ignored
        val event = active.advanceToNextAction()
        if (event is GestureCalibrationEvent.ReadyToCommit) {
            trainedCount = active.actions.size
            complete = true
        }
        return event
    }
}
