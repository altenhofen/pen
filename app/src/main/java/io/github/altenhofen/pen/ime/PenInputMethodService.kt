package io.github.altenhofen.pen.ime

import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import io.github.altenhofen.pen.recognition.AdaptiveRecognizer
import io.github.altenhofen.pen.recognition.Feedback
import io.github.altenhofen.pen.recognition.RecognitionResult
import io.github.altenhofen.pen.settings.MotorSettings
import io.github.altenhofen.pen.settings.MotorSettingsStore

class PenInputMethodService : InputMethodService() {
    private lateinit var settings: MotorSettingsStore
    private lateinit var recognizer: AdaptiveRecognizer
    private var activeSettings: MotorSettings = MotorSettings.Default
    private var canvas: DrawingCanvasView? = null
    private var pending: RecognitionResult? = null
    private var pendingAt: Long = 0L

    override fun onCreate() {
        super.onCreate()
        settings = MotorSettingsStore.open(this)
        recognizer = AdaptiveRecognizer.open(this)
    }

    override fun onCreateInputView(): View {
        val view = DrawingCanvasView(this)
        val height = (resources.displayMetrics.heightPixels * 0.45f).toInt()
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            height,
        )
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        view.configure(activeSettings.capture())
        view.setOnGlyphSettledListener { strokes ->
            val result = recognizer.recognize(strokes, activeSettings.ambiguityThreshold)
                ?: return@setOnGlyphSettledListener
            acceptPending()
            currentInputConnection?.commitText(result.winner.character.toString(), 1)
            pending = result
            pendingAt = SystemClock.elapsedRealtime()
        }
        canvas = view
        return view
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int,
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        val last = pending ?: return
        val withinWindow = SystemClock.elapsedRealtime() - pendingAt <= REJECT_WINDOW_MS
        if (withinWindow && newSelStart < oldSelStart) {
            recognizer.feedback(last, Feedback.Rejected)
            pending = null
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        currentInputConnection?.finishComposingText()
        activeSettings = settings.readBlocking()
        canvas?.configure(activeSettings.capture())
        recognizer.reload()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        canvas?.cancelPendingGlyph()
        super.onFinishInputView(finishingInput)
    }

    private fun acceptPending() {
        val last = pending ?: return
        recognizer.feedback(last, Feedback.Accepted)
        pending = null
    }

    private companion object {
        const val REJECT_WINDOW_MS = 3_000L
    }
}
