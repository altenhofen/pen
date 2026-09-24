package io.github.altenhofen.pen.ime

import android.view.MotionEvent
import kotlin.math.max

object StylusGate {
    private const val PASSIVE_PEN_MAX_CONTACT_DP = 10f

    fun accepts(toolType: Int): Boolean = toolType == MotionEvent.TOOL_TYPE_STYLUS

    fun acceptsIme(toolType: Int, allowFingerInput: Boolean): Boolean =
        if (allowFingerInput) acceptsTraining(toolType) else accepts(toolType)

    fun acceptsImeInk(event: MotionEvent, density: Float, allowFingerInput: Boolean): Boolean {
        if (acceptsIme(event.getToolType(0), allowFingerInput)) return true
        if (!allowFingerInput && looksLikePassivePen(event, density)) return true
        return false
    }

    /** Ink canvas may receive the pointer for double-tap space even when finger ink is off. */
    fun acceptsImePointer(
        event: MotionEvent,
        density: Float,
        allowFingerInput: Boolean,
        doubleTapForSpace: Boolean,
    ): Boolean {
        if (acceptsImeInk(event, density, allowFingerInput)) return true
        if (!doubleTapForSpace) return false
        val toolType = event.getToolType(0)
        return toolType == MotionEvent.TOOL_TYPE_FINGER || toolType == MotionEvent.TOOL_TYPE_MOUSE
    }

    fun acceptsTraining(toolType: Int): Boolean =
        accepts(toolType) ||
            toolType == MotionEvent.TOOL_TYPE_FINGER ||
            toolType == MotionEvent.TOOL_TYPE_MOUSE

    internal fun looksLikePassivePen(event: MotionEvent, density: Float): Boolean {
        val toolType = event.getToolType(0)
        if (toolType != MotionEvent.TOOL_TYPE_FINGER && toolType != MotionEvent.TOOL_TYPE_UNKNOWN) return false
        val major = event.getToolMajor(0).takeIf { it > 0f }
            ?: event.getAxisValue(MotionEvent.AXIS_TOUCH_MAJOR)
        val minor = event.getToolMinor(0).takeIf { it > 0f }
            ?: event.getAxisValue(MotionEvent.AXIS_TOUCH_MINOR)
        if (major <= 0f && minor <= 0f) {
            return smallNormalizedTouchSize(event.getSize(0))
        }
        return narrowContact(major, minor, density)
    }

    internal fun smallNormalizedTouchSize(size: Float): Boolean = size > 0f && size <= 0.15f

    internal fun narrowContact(major: Float, minor: Float, density: Float): Boolean {
        if (major <= 0f && minor <= 0f) return false
        val contactPx = max(major, minor)
        return contactPx / density <= PASSIVE_PEN_MAX_CONTACT_DP
    }
}
