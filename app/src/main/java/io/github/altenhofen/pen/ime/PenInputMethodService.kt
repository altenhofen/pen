package io.github.altenhofen.pen.ime

import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import io.github.altenhofen.pen.recognition.GlyphRecognizer
import io.github.altenhofen.pen.recognition.PrototypeStore
import io.github.altenhofen.pen.recognition.PrototypeUpdate
import io.github.altenhofen.pen.recognition.RecognitionMatch

class PenInputMethodService : InputMethodService() {
    private val recognizer = GlyphRecognizer.seeded()
    private var canvas: DrawingCanvasView? = null
    private var store: PrototypeStore? = null
    private var pending: RecognitionMatch? = null
    private var pendingAt: Long = 0L

    override fun onCreateInputView(): View {
        val view = DrawingCanvasView(this)
        val height = (resources.displayMetrics.heightPixels * 0.45f).toInt()
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            height,
        )
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        view.setOnGlyphSettledListener { strokes ->
            val match = recognizer.recognize(strokes) ?: return@setOnGlyphSettledListener
            acceptPending()
            currentInputConnection?.commitText(match.character.toString(), 1)
            pending = match
            pendingAt = SystemClock.elapsedRealtime()
        }
        canvas = view
        return view
    }

    override fun onCreate() {
        super.onCreate()
        val opened = PrototypeStore.open(this)
        store = opened
        opened.loadOrSeed().forEach { (label, values) ->
            recognizer.replacePrototype(label, values)
        }
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
            apply(last.character, PrototypeUpdate.repel(recognizer.prototypeValues(last.character), last.sample))
            pending = null
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        currentInputConnection?.finishComposingText()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        canvas?.cancelPendingGlyph()
        canvas = null
        super.onFinishInputView(finishingInput)
    }

    private fun acceptPending() {
        val last = pending ?: return
        apply(
            last.character,
            PrototypeUpdate.attract(
                recognizer.prototypeValues(last.character),
                last.sample,
                PrototypeUpdate.ACCEPT_REWARD,
            ),
        )
        pending = null
    }

    private fun apply(label: Char, values: FloatArray) {
        recognizer.replacePrototype(label, values)
        store?.save(label, values)
    }

    private companion object {
        const val REJECT_WINDOW_MS = 3_000L
    }
}
