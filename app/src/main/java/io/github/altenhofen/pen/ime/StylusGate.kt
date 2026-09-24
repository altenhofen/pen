package io.github.altenhofen.pen.ime

import android.view.MotionEvent

object StylusGate {
    fun accepts(toolType: Int): Boolean = toolType == MotionEvent.TOOL_TYPE_STYLUS

    fun acceptsIme(toolType: Int, allowFingerInput: Boolean): Boolean =
        if (allowFingerInput) acceptsTraining(toolType) else accepts(toolType)

    /** Ink canvas may receive the pointer for double-tap space even when finger ink is off. */
    fun acceptsImePointer(toolType: Int, allowFingerInput: Boolean, doubleTapForSpace: Boolean): Boolean =
        acceptsIme(toolType, allowFingerInput) ||
            (doubleTapForSpace && (toolType == MotionEvent.TOOL_TYPE_FINGER || toolType == MotionEvent.TOOL_TYPE_MOUSE))

    fun acceptsTraining(toolType: Int): Boolean =
        accepts(toolType) ||
            toolType == MotionEvent.TOOL_TYPE_FINGER ||
            toolType == MotionEvent.TOOL_TYPE_MOUSE
}
