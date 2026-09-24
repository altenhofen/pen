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
    if (points.size > 12) return false
    var minX = points[0].x
    var maxX = points[0].x
    var minY = points[0].y
    var maxY = points[0].y
    for (i in 1 until points.size) {
        val p = points[i]
        minX = minOf(minX, p.x)
        maxX = maxOf(maxX, p.x)
        minY = minOf(minY, p.y)
        maxY = maxOf(maxY, p.y)
    }
    return maxX - minX <= slopPx && maxY - minY <= slopPx
}

internal class DoubleTapDetector(
    private val slopPx: Float,
    private val timeoutMs: Int,
    private val onDoubleTap: () -> Unit,
) {
    private var lastTapAt = 0L
    private var lastX = 0f
    private var lastY = 0f

    fun onTap(x: Float, y: Float, nowMs: Long = SystemClock.uptimeMillis()) {
        val withinTime = lastTapAt > 0L && nowMs - lastTapAt <= timeoutMs
        val withinDistance = hypot(x - lastX, y - lastY) <= slopPx
        if (withinTime && withinDistance) {
            lastTapAt = 0L
            onDoubleTap()
        } else {
            lastTapAt = nowMs
            lastX = x
            lastY = y
        }
    }

    fun cancel() {
        lastTapAt = 0L
    }

    companion object {
        fun fromView(view: android.view.View, onDoubleTap: () -> Unit): DoubleTapDetector {
            val config = ViewConfiguration.get(view.context)
            return DoubleTapDetector(
                config.scaledTouchSlop.toFloat(),
                ViewConfiguration.getDoubleTapTimeout(),
                onDoubleTap,
            )
        }
    }
}
