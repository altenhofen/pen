package io.github.altenhofen.pen.ime

import android.text.InputType
import android.view.inputmethod.EditorInfo

/** Whether ink written into the current field may teach the recognizers anything. */
internal enum class LearningPolicy {
    Learn,
    Private,
    ;

    companion object {
        fun of(inputType: Int, imeOptions: Int): LearningPolicy {
            if ((imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0) return Private
            val variation = inputType and InputType.TYPE_MASK_VARIATION
            val secret = when (inputType and InputType.TYPE_MASK_CLASS) {
                InputType.TYPE_CLASS_TEXT -> variation in TEXT_PASSWORDS
                InputType.TYPE_CLASS_NUMBER -> variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
                else -> false
            }
            return if (secret) Private else Learn
        }

        private val TEXT_PASSWORDS = setOf(
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
        )
    }
}
