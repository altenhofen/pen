package io.github.altenhofen.pen.ime

import android.view.MotionEvent

object StylusGate {
    fun accepts(toolType: Int): Boolean = toolType == MotionEvent.TOOL_TYPE_STYLUS

    fun acceptsTraining(toolType: Int): Boolean =
        accepts(toolType) ||
            toolType == MotionEvent.TOOL_TYPE_FINGER ||
            toolType == MotionEvent.TOOL_TYPE_MOUSE
}
