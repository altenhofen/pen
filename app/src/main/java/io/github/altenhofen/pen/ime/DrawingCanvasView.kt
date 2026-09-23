package io.github.altenhofen.pen.ime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import io.github.altenhofen.pen.settings.CaptureStyle
import io.github.altenhofen.pen.settings.MotorSettings

fun interface OnGlyphSettledListener {
    fun onGlyphSettled(strokes: List<Stroke>)
}

class DrawingCanvasView(
    context: Context,
    private val acceptsTool: (toolType: Int) -> Boolean = StylusGate::accepts,
) : View(context) {
    private val finished = ArrayList<Stroke>()
    private var active: Stroke? = null
    private var settledListener: OnGlyphSettledListener? = null
    private val settleHandler = Handler(Looper.getMainLooper())
    private val settleRunnable = Runnable { dispatchSettledGlyph() }
    private var settleMillis = MotorSettings.Default.settleMillis

    fun setOnGlyphSettledListener(listener: OnGlyphSettledListener?) {
        settledListener = listener
    }

    fun cancelPendingGlyph() {
        settleHandler.removeCallbacks(settleRunnable)
        active = null
        finished.clear()
        invalidate()
    }

    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = 0xFF1A1A1A.toInt()
    }

    init {
        configure(MotorSettings.Default.capture())
    }

    fun configure(style: CaptureStyle) {
        settleMillis = style.settleMillis
        ink.strokeWidth = style.strokeWidthDp * resources.displayMetrics.density
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!acceptsTool(event.getToolType(0))) {
            return false
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                settleHandler.removeCallbacks(settleRunnable)
                val stroke = Stroke()
                stroke.append(event.x, event.y)
                active = stroke
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                val stroke = active ?: return true
                val history = event.historySize
                for (i in 0 until history) {
                    stroke.append(event.getHistoricalX(i), event.getHistoricalY(i))
                }
                stroke.append(event.x, event.y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                val stroke = active ?: return true
                stroke.append(event.x, event.y)
                finished.add(stroke)
                active = null
                scheduleSettle()
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                active = null
                if (finished.isNotEmpty()) scheduleSettle()
                invalidate()
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(0xFFF7F4EF.toInt())
        for (stroke in finished) {
            drawStroke(canvas, stroke)
        }
        active?.let { drawStroke(canvas, it) }
    }

    private fun scheduleSettle() {
        settleHandler.removeCallbacks(settleRunnable)
        settleHandler.postDelayed(settleRunnable, settleMillis)
    }

    private fun dispatchSettledGlyph() {
        if (finished.isEmpty()) return
        val snapshot = finished.toList()
        finished.clear()
        invalidate()
        settledListener?.onGlyphSettled(snapshot)
    }

    private fun drawStroke(canvas: Canvas, stroke: Stroke) {
        val points = stroke.points()
        if (points.isEmpty()) return
        val path = Path()
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }
        canvas.drawPath(path, ink)
    }
}
