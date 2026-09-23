package io.github.altenhofen.pen.ime

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.altenhofen.pen.R
import io.github.altenhofen.pen.recognition.InkModelState
import io.github.altenhofen.pen.recognition.Suggestion

enum class InkKey { Space, Backspace, Enter }

class InkKeyboardView(
    context: Context,
    val canvas: DrawingCanvasView,
    canvasHeight: Int,
    private val onSuggestion: (Suggestion) -> Unit,
    private val onKey: (InkKey) -> Unit,
) : LinearLayout(context) {
    private val strip = LinearLayout(context).apply { orientation = HORIZONTAL }
    private val status = label(12f).apply {
        setTextColor(MUTED)
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(12), 0, dp(12), 0)
    }

    init {
        orientation = VERTICAL
        setBackgroundColor(SURFACE)
        val bar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(strip, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
            addView(status, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
        }
        addView(bar, LayoutParams(LayoutParams.MATCH_PARENT, dp(48)))
        addView(canvas, LayoutParams(LayoutParams.MATCH_PARENT, canvasHeight).apply {
            setMargins(dp(8), 0, dp(8), 0)
        })
        val keys = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
            addView(key("⌫", InkKey.Backspace, repeat = true), keyParams(1f))
            addView(key(context.getString(R.string.ime_key_space), InkKey.Space), keyParams(3f))
            addView(key("↵", InkKey.Enter), keyParams(1f))
        }
        addView(keys, LayoutParams(LayoutParams.MATCH_PARENT, dp(56)))
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            view.setPadding(0, 0, 0, insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom)
            insets
        }
    }

    fun showSuggestions(suggestions: List<Suggestion>, committed: String?) {
        strip.removeAllViews()
        suggestions.forEach { suggestion ->
            val chip = label(20f).apply {
                text = suggestion.text
                gravity = Gravity.CENTER
                typeface = if (suggestion.text == committed) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                setTextColor(if (suggestion.text == committed) INK else MUTED)
                contentDescription = suggestion.text
                setOnClickListener { onSuggestion(suggestion) }
            }
            strip.addView(chip, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
                setMargins(dp(2), dp(6), dp(2), dp(6))
            })
        }
    }

    fun showModelState(state: InkModelState) {
        status.text = when (state) {
            InkModelState.Checking -> ""
            InkModelState.Ready -> ""
            InkModelState.Downloading -> context.getString(R.string.ime_model_downloading)
            is InkModelState.Unavailable -> context.getString(R.string.ime_model_offline)
        }
    }

    private fun key(text: String, key: InkKey, repeat: Boolean = false): View = label(18f).apply {
        this.text = text
        gravity = Gravity.CENTER
        setTextColor(INK)
        contentDescription = key.name
        background = GradientDrawable().apply {
            cornerRadius = dp(8).toFloat()
            setColor(KEY)
        }
        setOnClickListener { onKey(key) }
        if (repeat) setOnLongClickListener {
            val repeater = object : Runnable {
                override fun run() {
                    if (!isPressed) return
                    onKey(key)
                    postDelayed(this, REPEAT_MS)
                }
            }
            post(repeater)
            true
        }
    }

    private fun keyParams(weight: Float) = LayoutParams(0, LayoutParams.MATCH_PARENT, weight).apply {
        setMargins(dp(4), 0, dp(4), 0)
    }

    private fun label(sizeSp: Float) = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        isSingleLine = true
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val REPEAT_MS = 60L
        const val SURFACE = 0xFFECE8E1.toInt()
        const val KEY = 0xFFF7F4EF.toInt()
        const val INK = 0xFF1A1A1A.toInt()
        const val MUTED = 0xFF6B665E.toInt()
    }
}
