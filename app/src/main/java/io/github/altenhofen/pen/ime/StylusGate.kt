package io.github.altenhofen.pen.ime

import android.view.MotionEvent

/**
 * Palm and finger contacts never enter the stroke list.
 * Only an active stylus tip does.
 */
object StylusGate {
    fun accepts(toolType: Int): Boolean = toolType == MotionEvent.TOOL_TYPE_STYLUS
}
