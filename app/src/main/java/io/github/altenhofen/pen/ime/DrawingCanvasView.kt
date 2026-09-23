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
    private val autoSettle: Boolean = true,
    private val keepInkAfterSettle: Boolean = false,
) : View(context) {
    private val kept = ArrayList<Stroke>()
    private val finished = ArrayList<Stroke>()
    private var active: Stroke? = null
    private var settledListener: OnGlyphSettledListener? = null
    private val settleHandler = Handler(Looper.getMainLooper())
    private val settleRunnable = Runnable { dispatchSettledGlyph() }
    private var settleMillis = MotorSettings.Default.settleMillis
    private var prompt: String? = null

    fun setOnGlyphSettledListener(listener: OnGlyphSettledListener?) {
        settledListener = listener
    }

    fun setPrompt(text: String?) {
        prompt = text
        invalidate()
    }

    fun cancelPendingGlyph() {
        settleHandler.removeCallbacks(settleRunnable)
        active = null
        finished.clear()
        kept.clear()
        invalidate()
    }

    fun takeInk(): List<Stroke> {
        settleHandler.removeCallbacks(settleRunnable)
        val strokes = ArrayList<Stroke>(finished.size + 1)
        strokes.addAll(finished)
        active?.let { strokes.add(it) }
        active = null
        finished.clear()
        kept.clear()
        invalidate()
        return strokes
    }

    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = 0xFF1A1A1A.toInt()
    }

    private val promptPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x241A1A1A
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    init {
        configure(MotorSettings.Default.capture())
        isClickable = true
        isFocusable = true
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
                if (autoSettle) scheduleSettle()
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                active = null
                if (autoSettle && finished.isNotEmpty()) scheduleSettle()
                invalidate()
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(0xFFF7F4EF.toInt())
        val label = prompt
        if (!label.isNullOrEmpty() && width > 0 && height > 0) {
            promptPaint.textSize = minOf(width, height) * 0.55f
            val metrics = promptPaint.fontMetrics
            val y = height / 2f - (metrics.ascent + metrics.descent) / 2f
            canvas.drawText(label, width / 2f, y, promptPaint)
        }
        for (stroke in kept) {
            drawStroke(canvas, stroke)
        }
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
        if (keepInkAfterSettle) kept.addAll(snapshot)
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
