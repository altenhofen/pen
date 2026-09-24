package io.github.altenhofen.pen.ime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.hypot
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
    private val finished = ArrayList<Stroke>()
    private var active: Stroke? = null
    private val inkPath = Path()
    private val activePath = Path()
    private var settledListener: OnGlyphSettledListener? = null
    private val settleHandler = Handler(Looper.getMainLooper())
    private val settleRunnable = Runnable { dispatchSettledGlyph() }
    private var settleMillis = MotorSettings.Default.settleMillis
    private var prompt: String? = null
    private val tapSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private var doubleTapListener: OnDoubleTapListener? = null
    private var doubleTapEnabled = false
    private var doubleTapDetector: DoubleTapDetector? = null
    private var downX = 0f
    private var downY = 0f
    private var strokeIsTapCandidate = true
    private var activeToolType = MotionEvent.TOOL_TYPE_UNKNOWN

    fun setOnGlyphSettledListener(listener: OnGlyphSettledListener?) {
        settledListener = listener
    }

    fun setOnDoubleTapListener(listener: OnDoubleTapListener?) {
        doubleTapListener = listener
        rebuildDoubleTapDetector()
    }

    fun setDoubleTapForSpaceEnabled(enabled: Boolean) {
        doubleTapEnabled = enabled
        rebuildDoubleTapDetector()
    }

    private var acceptsPointerTool: (Int) -> Boolean = acceptsTool
    private var mayInkPointer: (Int) -> Boolean = acceptsTool

    fun configureImePointers(acceptsPointer: (Int) -> Boolean, mayInk: (Int) -> Boolean) {
        acceptsPointerTool = acceptsPointer
        mayInkPointer = mayInk
    }

    private fun rebuildDoubleTapDetector() {
        doubleTapDetector = if (doubleTapEnabled && doubleTapListener != null) {
            DoubleTapDetector.fromView(this, doubleTapListener!!::onDoubleTap)
        } else {
            null
        }
    }

    fun setPrompt(text: String?) {
        prompt = text
        invalidate()
    }

    fun cancelPendingGlyph() {
        settleHandler.removeCallbacks(settleRunnable)
        clearInk()
    }

    fun takeInk(): List<Stroke> {
        settleHandler.removeCallbacks(settleRunnable)
        val strokes = ArrayList<Stroke>(finished.size + 1)
        strokes.addAll(finished)
        active?.let { strokes.add(it) }
        clearInk()
        return strokes
    }

    private fun clearInk() {
        active = null
        finished.clear()
        inkPath.reset()
        activePath.reset()
        invalidate()
    }

    override fun onDetachedFromWindow() {
        settleHandler.removeCallbacks(settleRunnable)
        super.onDetachedFromWindow()
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
        val toolType = event.getToolType(0)
        if (!acceptsPointerTool(toolType)) {
            return false
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                settleHandler.removeCallbacks(settleRunnable)
                activeToolType = toolType
                downX = event.x
                downY = event.y
                strokeIsTapCandidate = true
                val stroke = Stroke()
                stroke.append(event.x, event.y)
                active = stroke
                activePath.reset()
                activePath.moveTo(event.x, event.y)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                val stroke = active ?: return true
                if (strokeIsTapCandidate && hypot(event.x - downX, event.y - downY) > tapSlop) {
                    strokeIsTapCandidate = false
                    if (!mayInkPointer(activeToolType)) {
                        doubleTapDetector?.cancel()
                        active = null
                        activePath.reset()
                        invalidate()
                        return true
                    }
                }
                val history = event.historySize
                for (i in 0 until history) {
                    stroke.append(event.getHistoricalX(i), event.getHistoricalY(i))
                    activePath.lineTo(event.getHistoricalX(i), event.getHistoricalY(i))
                }
                stroke.append(event.x, event.y)
                activePath.lineTo(event.x, event.y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                val stroke = active ?: return true
                stroke.append(event.x, event.y)
                val detector = doubleTapDetector
                if (detector != null && strokeIsTapCandidate && strokeIsTap(stroke, tapSlop)) {
                    active = null
                    activePath.reset()
                    detector.onTap(event.x, event.y)
                    invalidate()
                    return true
                }
                if (!mayInkPointer(activeToolType)) {
                    doubleTapDetector?.cancel()
                    active = null
                    activePath.reset()
                    invalidate()
                    return true
                }
                finished.add(stroke)
                appendStroke(inkPath, stroke)
                active = null
                activePath.reset()
                if (autoSettle) scheduleSettle()
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                active = null
                activePath.reset()
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
        canvas.drawPath(inkPath, ink)
        canvas.drawPath(activePath, ink)
    }

    private fun scheduleSettle() {
        settleHandler.removeCallbacks(settleRunnable)
        settleHandler.postDelayed(settleRunnable, settleMillis)
    }

    private fun dispatchSettledGlyph() {
        if (finished.isEmpty()) return
        val snapshot = finished.toList()
        finished.clear()
        if (!keepInkAfterSettle) inkPath.reset()
        invalidate()
        settledListener?.onGlyphSettled(snapshot)
    }

    private fun appendStroke(path: Path, stroke: Stroke) {
        val points = stroke.points()
        if (points.isEmpty()) return
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }
    }
}
