package io.github.altenhofen.pen.ime

import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import io.github.altenhofen.pen.recognition.AdaptiveRecognizer
import io.github.altenhofen.pen.recognition.GlyphTemplateSource
import io.github.altenhofen.pen.recognition.InkModel
import io.github.altenhofen.pen.recognition.InkModelSource
import io.github.altenhofen.pen.recognition.Suggestion
import io.github.altenhofen.pen.recognition.WordMemory
import io.github.altenhofen.pen.recognition.WordMemorySource
import io.github.altenhofen.pen.recognition.WordMemoryStore
import io.github.altenhofen.pen.recognition.WordSample
import io.github.altenhofen.pen.recognition.blendSuggestions
import io.github.altenhofen.pen.recognition.wordFeatures
import java.util.UUID
import io.github.altenhofen.pen.settings.MotorSettings
import io.github.altenhofen.pen.settings.MotorSettingsStore

class PenInputMethodService : InputMethodService() {
    private lateinit var settings: MotorSettingsStore
    private lateinit var recognizer: AdaptiveRecognizer
    private lateinit var inkModel: InkModel
    private lateinit var wordStore: WordMemoryStore
    private var words = WordMemory()
    private var activeSettings: MotorSettings = MotorSettings.Default
    private var keyboard: InkKeyboardView? = null
    private val pending = PendingCommit()
    private var pendingAt: Long = 0L
    private var committed: String? = null
    private var glyphGeneration = 0

    override fun onCreate() {
        super.onCreate()
        settings = MotorSettingsStore.open(this)
        recognizer = AdaptiveRecognizer.open(this)
        wordStore = WordMemoryStore.open(this)
        inkModel = InkModel(INK_LANGUAGE) { state -> keyboard?.showModelState(state) }
    }

    override fun onDestroy() {
        inkModel.close()
        super.onDestroy()
    }

    override fun onCreateInputView(): View {
        val canvas = DrawingCanvasView(
            this,
            acceptsTool = { StylusGate.acceptsIme(it, activeSettings.allowFingerInput) },
        )
        canvas.isFocusable = true
        canvas.isFocusableInTouchMode = true
        canvas.configure(activeSettings.capture())
        canvas.setOnGlyphSettledListener(::onGlyph)
        val view = InkKeyboardView(
            this,
            canvas,
            canvasHeight = (resources.displayMetrics.heightPixels * 0.36f).toInt(),
            onSuggestion = ::replaceWith,
            onKey = ::onKey,
        )
        view.showModelState(inkModel.state)
        keyboard = view
        return view
    }

    private fun onGlyph(strokes: List<Stroke>) {
        val template = recognizer.recognize(strokes, activeSettings.ambiguityThreshold)
        val shape = wordFeatures(strokes)
        val recalls = shape?.let(words::recall).orEmpty()
        val generation = ++glyphGeneration
        val preContext = currentInputConnection?.getTextBeforeCursor(PRE_CONTEXT, 0)?.toString().orEmpty()
        val canvas = keyboard?.canvas
        inkModel.recognize(strokes, preContext, canvas?.width?.toFloat() ?: 0f, canvas?.height?.toFloat() ?: 0f) { candidates ->
            if (generation != glyphGeneration) return@recognize
            val ink = InkModelSource(candidates)
            val sources = listOf(ink, GlyphTemplateSource(template, ink), WordMemorySource(recalls, words.confirmations()))
            val suggestions = blendSuggestions(sources)
            val best = suggestions.firstOrNull() ?: return@recognize
            resolve(pending.committed(best.text, shape, template))
            currentInputConnection?.commitText(best.text, 1)
            committed = best.text
            pendingAt = SystemClock.elapsedRealtime()
            keyboard?.showSuggestions(suggestions, best.text)
        }
    }

    private fun replaceWith(suggestion: Suggestion) {
        val previous = committed ?: return
        val connection = currentInputConnection ?: return
        if (connection.getTextBeforeCursor(previous.length, 0)?.toString() != previous) return
        resolve(pending.picked(suggestion.text))
        connection.deleteSurroundingText(previous.length, 0)
        connection.commitText(suggestion.text, 1)
        committed = suggestion.text
        keyboard?.showSuggestions(listOf(suggestion), suggestion.text)
    }

    private fun resolve(resolution: Resolution?) {
        resolution ?: return
        val glyph = resolution.glyph
        val feedback = resolution.glyphFeedback
        if (glyph != null && feedback != null) recognizer.feedback(glyph, feedback)
        if (resolution is Resolution.Kept && resolution.ink != null) {
            val sample = WordSample(UUID.randomUUID().toString(), resolution.text, resolution.ink, System.currentTimeMillis())
            val (next, evicted) = words.remember(sample)
            wordStore.apply(sample, evicted)
            words = next
        }
    }

    private fun onKey(key: InkKey) {
        val connection = currentInputConnection ?: return
        resolve(if (key == InkKey.Backspace) pending.corrected() else pending.kept())
        committed = null
        keyboard?.showSuggestions(emptyList(), null)
        when (key) {
            InkKey.Space -> connection.commitText(" ", 1)
            InkKey.Backspace -> sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
            InkKey.Enter -> {
                val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
                val noEnterAction = ((currentInputEditorInfo?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
                if (action != null && action > EditorInfo.IME_ACTION_NONE && !noEnterAction) {
                    connection.performEditorAction(action)
                } else {
                    sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
                }
            }
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
        val withinWindow = SystemClock.elapsedRealtime() - pendingAt <= REJECT_WINDOW_MS
        if (withinWindow && newSelStart < oldSelStart) resolve(pending.corrected())
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        currentInputConnection?.finishComposingText()
        activeSettings = settings.readBlocking()
        keyboard?.canvas?.configure(activeSettings.capture())
        keyboard?.showSuggestions(emptyList(), null)
        committed = null
        pending.discard()
        recognizer.reload()
        words = wordStore.load()
        inkModel.ensureReady()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        keyboard?.canvas?.cancelPendingGlyph()
        super.onFinishInputView(finishingInput)
    }

    private companion object {
        const val REJECT_WINDOW_MS = 3_000L
        const val PRE_CONTEXT = 20
        const val INK_LANGUAGE = "en-US"
    }
}
