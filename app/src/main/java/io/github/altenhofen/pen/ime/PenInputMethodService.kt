package io.github.altenhofen.pen.ime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import io.github.altenhofen.pen.recognition.PrototypeDatabase
import io.github.altenhofen.pen.recognition.AdaptiveRecognizer
import io.github.altenhofen.pen.recognition.CustomDictionarySource
import io.github.altenhofen.pen.recognition.Feedback
import io.github.altenhofen.pen.recognition.GestureMatchPolicy
import io.github.altenhofen.pen.recognition.GlyphTemplateSource
import io.github.altenhofen.pen.recognition.InkModel
import io.github.altenhofen.pen.recognition.InkModelSource
import io.github.altenhofen.pen.recognition.RecognitionResult
import io.github.altenhofen.pen.recognition.Suggestion
import io.github.altenhofen.pen.recognition.WordMemory
import io.github.altenhofen.pen.recognition.WordMemorySource
import io.github.altenhofen.pen.recognition.WordMemoryStore
import io.github.altenhofen.pen.recognition.WordSample
import io.github.altenhofen.pen.recognition.blendSuggestions
import io.github.altenhofen.pen.recognition.isFullWord
import io.github.altenhofen.pen.recognition.isPunctuationOnly
import io.github.altenhofen.pen.recognition.supportsInkLanguage
import io.github.altenhofen.pen.recognition.wordFeatures
import io.github.altenhofen.pen.recognition.CustomWordStore
import io.github.altenhofen.pen.settings.AppLocales
import io.github.altenhofen.pen.settings.MotorSettings
import io.github.altenhofen.pen.settings.MotorSettingsStore
import io.github.altenhofen.pen.settings.resolveInkLanguage
import java.util.UUID

class PenInputMethodService : InputMethodService() {
    private lateinit var settings: MotorSettingsStore
    private lateinit var recognizer: AdaptiveRecognizer
    private lateinit var inkModel: InkModel
    private lateinit var wordStore: WordMemoryStore
    private lateinit var customWordStore: CustomWordStore
    private var words = WordMemory()
    private var customWords: List<String> = emptyList()
    private var activeSettings: MotorSettings = MotorSettings.Default
    private var keyboard: InkKeyboardView? = null
    private val pending = PendingCommit()
    private val gestureUndo = GestureUndoStack()
    private lateinit var gestureExecutor: GestureExecutor
    private var learning = LearningPolicy.Private
    private var pendingAt: Long = 0L
    private var committed: String? = null
    private var trailingAutoSpace = false
    private var glyphGeneration = 0
    private var lastReinforced: ReinforcedSample? = null

    private val packageReplacedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            PrototypeDatabase.invalidate()
            if (::recognizer.isInitialized) {
                recognizer = AdaptiveRecognizer.open(this@PenInputMethodService)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        settings = MotorSettingsStore.open(this)
        recognizer = AdaptiveRecognizer.open(this)
        gestureExecutor = GestureExecutor(this, gestureUndo)
        wordStore = WordMemoryStore.open(this)
        customWordStore = CustomWordStore.open(this)
        activeSettings = settings.readBlocking()
        useResolvedInkLanguage()
        val filter = IntentFilter(Intent.ACTION_MY_PACKAGE_REPLACED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(packageReplacedReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(packageReplacedReceiver, filter)
        }
    }

    override fun onDestroy() {
        unregisterReceiver(packageReplacedReceiver)
        inkModel.close()
        super.onDestroy()
    }

    private fun useResolvedInkLanguage() {
        val tag = resolveInkLanguage(
            activeSettings.handwriting,
            AppLocales.effectiveTag(this),
            AppLocales.systemTag(),
            ::supportsInkLanguage,
        )
        if (::inkModel.isInitialized) {
            if (inkModel.languageTag == tag) return
            inkModel.close()
        }
        inkModel = InkModel(tag) { state -> keyboard?.showModelState(state) }
    }

    override fun onCreateInputView(): View {
        val canvas = DrawingCanvasView(this)
        canvas.isFocusable = true
        canvas.isFocusableInTouchMode = true
        applyImeCanvasPolicy(canvas)
        canvas.setOnGlyphSettledListener(::onGlyph)
        canvas.setOnDoubleTapListener { onKey(InkKey.Space) }
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

    private fun applyImeCanvasPolicy(canvas: DrawingCanvasView) {
        val settings = activeSettings
        canvas.configure(settings.capture())
        val density = resources.displayMetrics.density
        canvas.configureImePointers(
            acceptsPointer = {
                StylusGate.acceptsImePointer(it, density, settings.allowFingerInput, settings.doubleTapForSpace)
            },
            mayInk = { StylusGate.acceptsImeInk(it, density, settings.allowFingerInput) },
        )
        canvas.setDoubleTapForSpaceEnabled(settings.doubleTapForSpace)
    }

    private fun onGlyph(strokes: List<Stroke>) {
        recognizer.reload()
        val threshold = MotorSettings.FIXED_AMBIGUITY_THRESHOLD
        val gesture = recognizer.recognizeGesture(strokes, threshold)
        if (GestureMatchPolicy.shouldFire(gesture, threshold)) {
            val connection = currentInputConnection
            val action = gesture!!.winner.action
            if (connection != null && gestureExecutor.perform(action, connection)) {
                resolve(pending.corrected())
                committed = null
                pendingAt = 0L
                keyboard?.showSuggestions(emptyList(), null)
            }
            return
        }
        val template = recognizer.recognize(strokes, threshold)
        val shape = wordFeatures(strokes)
        val recalls = shape?.let(words::recall).orEmpty()
        val generation = ++glyphGeneration
        val preContext = currentInputConnection?.getTextBeforeCursor(PRE_CONTEXT, 0)?.toString().orEmpty()
        val canvas = keyboard?.canvas
        inkModel.recognize(strokes, preContext, canvas?.width?.toFloat() ?: 0f, canvas?.height?.toFloat() ?: 0f) { candidates ->
            if (generation != glyphGeneration) return@recognize
            val ink = InkModelSource(candidates, activeSettings.recognizeSpacesInHandwriting)
            val sources = listOf(
                ink,
                GlyphTemplateSource(template, ink),
                WordMemorySource(recalls, words.confirmations()),
                CustomDictionarySource(customWords, ink),
            )
            val suggestions = blendSuggestions(sources)
            val best = suggestions.firstOrNull() ?: return@recognize
            commitRecognized(best, suggestions, shape, template)
        }
    }

    private fun commitRecognized(
        best: Suggestion,
        suggestions: List<Suggestion>,
        shape: io.github.altenhofen.pen.recognition.FeatureVector?,
        template: RecognitionResult?,
    ) {
        val connection = currentInputConnection ?: return
        if (trailingAutoSpace && isPunctuationOnly(best.text)) {
            clearTrailingAutoSpace(connection)
            resolve(pending.committed(best.text, shape, template))
            connection.commitText(best.text + " ", 1)
            committed = best.text
            pendingAt = SystemClock.elapsedRealtime()
            trailingAutoSpace = true
            keyboard?.showSuggestions(suggestions, best.text)
            return
        }
        resolve(pending.committed(best.text, shape, template))
        connection.commitText(best.text, 1)
        committed = best.text
        pendingAt = SystemClock.elapsedRealtime()
        keyboard?.showSuggestions(suggestions, best.text)
        maybeAppendAutoSpace(connection, best.text)
    }

    private fun replaceWith(suggestion: Suggestion) {
        val previous = committed ?: return
        val connection = currentInputConnection ?: return
        if (connection.getTextBeforeCursor(previous.length, 0)?.toString() != previous) return
        clearTrailingAutoSpace(connection)
        resolve(pending.picked(suggestion.text))
        connection.deleteSurroundingText(previous.length, 0)
        connection.commitText(suggestion.text, 1)
        committed = suggestion.text
        keyboard?.showSuggestions(listOf(suggestion), suggestion.text)
        maybeAppendAutoSpace(connection, suggestion.text)
    }


    private fun maybeAppendAutoSpace(connection: android.view.inputmethod.InputConnection, text: String) {
        if (!activeSettings.spaceAfterFullWord || !isFullWord(text) || isPunctuationOnly(text)) {
            trailingAutoSpace = false
            return
        }
        connection.commitText(" ", 1)
        trailingAutoSpace = true
    }

    private fun clearTrailingAutoSpace(connection: android.view.inputmethod.InputConnection) {
        if (!trailingAutoSpace) return
        if (connection.getTextBeforeCursor(1, 0)?.toString() == " ") {
            connection.deleteSurroundingText(1, 0)
        }
        trailingAutoSpace = false
    }

    private fun resolve(resolution: Resolution?) {
        if (resolution == null || learning == LearningPolicy.Private) return
        val glyph = resolution.glyph
        val feedback = resolution.glyphFeedback
        if (glyph != null && feedback != null) recognizer.feedback(glyph, feedback)
        if (resolution is Resolution.Kept && resolution.ink != null) {
            val sample = WordSample(UUID.randomUUID().toString(), resolution.text, resolution.ink, System.currentTimeMillis())
            val (next, evicted) = words.remember(sample)
            wordStore.apply(sample, evicted)
            words = next
            lastReinforced = ReinforcedSample(sample.id, sample.word, SystemClock.elapsedRealtime(), glyph, feedback)
        }
        if (resolution is Resolution.Corrected) {
            undoLastReinforcement()
        }
    }

    private fun undoLastReinforcement() {
        val learned = lastReinforced ?: return
        if (SystemClock.elapsedRealtime() - learned.atMs > REINFORCE_UNDO_WINDOW_MS) return
        val (next, removed) = words.forget(learned.sampleId)
        if (removed != null) wordStore.remove(removed.id)
        words = next
        if (learned.glyph != null && learned.feedback == Feedback.Accepted) {
            recognizer.feedback(learned.glyph, Feedback.Rejected)
        }
        lastReinforced = null
    }

    private fun maybeUndoOnDelete(connection: android.view.inputmethod.InputConnection) {
        val learned = lastReinforced ?: return
        if (SystemClock.elapsedRealtime() - learned.atMs > REINFORCE_UNDO_WINDOW_MS) return
        val tail = connection.getTextBeforeCursor(learned.word.length + 2, 0)?.toString().orEmpty()
        if (!tail.endsWith(learned.word)) undoLastReinforcement()
    }

    private fun onKey(key: InkKey) {
        val connection = currentInputConnection ?: return
        resolve(if (key == InkKey.Backspace) pending.corrected() else pending.kept())
        committed = null
        keyboard?.showSuggestions(emptyList(), null)
        when (key) {
            InkKey.Space -> {
                trailingAutoSpace = false
                connection.commitText(" ", 1)
            }
            InkKey.Backspace -> {
                maybeUndoOnDelete(connection)
                clearTrailingAutoSpace(connection)
                sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
            }
            InkKey.Enter -> {
                trailingAutoSpace = false
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
        if (withinWindow && newSelStart < oldSelStart) {
            resolve(pending.corrected())
            currentInputConnection?.let(::maybeUndoOnDelete)
        }
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        if (::recognizer.isInitialized) recognizer.reload()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        currentInputConnection?.finishComposingText()
        activeSettings = settings.readBlocking()
        useResolvedInkLanguage()
        keyboard?.canvas?.let { applyImeCanvasPolicy(it) }
        keyboard?.showSuggestions(emptyList(), null)
        committed = null
        trailingAutoSpace = false
        pending.discard()
        lastReinforced = null
        learning = LearningPolicy.of(info?.inputType ?: 0, info?.imeOptions ?: 0)
        recognizer.reload()
        words = wordStore.load()
        customWords = customWordStore.load()
        inkModel.ensureReady()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        keyboard?.canvas?.cancelPendingGlyph()
        super.onFinishInputView(finishingInput)
    }

    private data class ReinforcedSample(
        val sampleId: String,
        val word: String,
        val atMs: Long,
        val glyph: RecognitionResult?,
        val feedback: Feedback?,
    )

    private companion object {
        const val REJECT_WINDOW_MS = 3_000L
        const val REINFORCE_UNDO_WINDOW_MS = 120_000L
        const val PRE_CONTEXT = 20
    }
}
