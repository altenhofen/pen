package io.github.altenhofen.pen.ime

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class LearningPolicyTest {
    private val text = InputType.TYPE_CLASS_TEXT

    @Test
    fun plainTextFieldLearns() {
        assertEquals(LearningPolicy.Learn, LearningPolicy.of(text or InputType.TYPE_TEXT_VARIATION_NORMAL, EditorInfo.IME_ACTION_SEARCH))
    }

    @Test
    fun passwordFieldsLearnNothing() {
        val passwords = listOf(
            text or InputType.TYPE_TEXT_VARIATION_PASSWORD,
            text or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            text or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD,
        )
        assertEquals(List(4) { LearningPolicy.Private }, passwords.map { LearningPolicy.of(it, 0) })
    }

    @Test
    fun noPersonalizedLearningFlagLearnsNothing() {
        assertEquals(LearningPolicy.Private, LearningPolicy.of(text, EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING))
    }

    @Test
    fun numberFieldThatIsNotAPasswordLearns() {
        assertEquals(LearningPolicy.Learn, LearningPolicy.of(InputType.TYPE_CLASS_NUMBER, 0))
    }
}
