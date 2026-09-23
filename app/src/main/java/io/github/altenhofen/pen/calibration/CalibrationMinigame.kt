package io.github.altenhofen.pen.calibration

import io.github.altenhofen.pen.ime.Stroke

internal sealed interface GlyphCalibrationPhase {
    data class PickGlyphs(val selected: Set<Char>) : GlyphCalibrationPhase
    data class WriteGlyphs(val session: CalibrationSession) : GlyphCalibrationPhase
    data object Complete : GlyphCalibrationPhase
}

internal class CalibrationMinigame {
    private var selected = emptySet<Char>()
    private var session: CalibrationSession? = null
    private var complete = false
    private var trainedCount = 0

    fun phase(): GlyphCalibrationPhase = when {
        complete -> GlyphCalibrationPhase.Complete
        session != null -> GlyphCalibrationPhase.WriteGlyphs(session!!)
        else -> GlyphCalibrationPhase.PickGlyphs(selected)
    }

    fun trainedCount(): Int = trainedCount

    fun toggle(glyph: Char) {
        if (session != null || complete) return
        if (glyph !in CalibrateGlyphs.ALL) return
        selected = if (glyph in selected) selected - glyph else selected + glyph
    }

    fun selectAll() {
        if (session != null || complete) return
        selected = CalibrateGlyphs.ALL.toSet()
    }

    fun selectNone() {
        if (session != null || complete) return
        selected = emptySet()
    }

    fun start() {
        if (session != null || complete) return
        val glyphs = SelectedGlyphs.of(selected) ?: return
        session = CalibrationSession.begin(glyphs.labels)
    }

    fun recordInk(strokes: List<Stroke>): CalibrationEvent {
        val active = session ?: return CalibrationEvent.Ignored
        if (complete) return CalibrationEvent.Ignored
        return active.recordInk(strokes)
    }

    fun next(): CalibrationEvent {
        val active = session ?: return CalibrationEvent.Ignored
        if (complete) return CalibrationEvent.Ignored
        val event = active.advanceToNextLabel()
        if (event == CalibrationEvent.ReadyToCommit) {
            trainedCount = active.labels.size
            complete = true
        }
        return event
    }

    fun payload(): CalibrationPayload = requireNotNull(session).payload()
}
