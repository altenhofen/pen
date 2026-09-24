package io.github.altenhofen.pen.ime

import android.os.SystemClock
import android.view.ViewConfiguration
import kotlin.math.hypot

fun interface OnDoubleTapListener {
    fun onDoubleTap()
}

internal fun strokeIsTap(stroke: Stroke, slopPx: Float): Boolean {
    val points = stroke.points()
    if (points.isEmpty()) return false
    if (points.size > 48) return false
    val relaxed = slopPx * 2f
    val origin = points.first()
    for (p in points) {
        if (hypot(p.x - origin.x, p.y - origin.y) > relaxed) return false
    }
    return true
}

internal class DoubleTapDetector(
    private val slopPx: Float,
    private val timeoutMs: Int,
    private val onDoubleTap: () -> Unit,
) {
    private var lastTapAt = 0L
    private var lastX = 0f
    private var lastY = 0f

    fun onTap(x: Float, y: Float, nowMs: Long = SystemClock.uptimeMillis()): Boolean {
        val withinTime = lastTapAt > 0L && nowMs - lastTapAt <= timeoutMs
        val withinDistance = hypot(x - lastX, y - lastY) <= slopPx
        if (withinTime && withinDistance) {
            lastTapAt = 0L
            onDoubleTap()
            return true
        }
        lastTapAt = nowMs
        lastX = x
        lastY = y
        return false
    }

    fun cancel() {
        lastTapAt = 0L
    }

    companion object {
        fun fromView(view: android.view.View, onDoubleTap: () -> Unit): DoubleTapDetector {
            val config = ViewConfiguration.get(view.context)
            return DoubleTapDetector(
                config.scaledDoubleTapSlop.toFloat(),
                ViewConfiguration.getDoubleTapTimeout(),
                onDoubleTap,
            )
        }
    }
}
