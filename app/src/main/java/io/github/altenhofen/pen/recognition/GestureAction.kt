package io.github.altenhofen.pen.recognition

import androidx.annotation.StringRes
import io.github.altenhofen.pen.R

/** Text-field actions the user can bind to a trained ink shape. */
enum class GestureAction(val id: String, @param:StringRes val labelRes: Int) {
    LowercaseLastWord("lowercase_last_word", R.string.gesture_lowercase_last_word),
    UppercaseLastWord("uppercase_last_word", R.string.gesture_uppercase_last_word),
    CapitalizeLastWord("capitalize_last_word", R.string.gesture_capitalize_last_word),
    CycleCaseLastWord("cycle_case_last_word", R.string.gesture_cycle_case_last_word),
    DeleteLastWord("delete_last_word", R.string.gesture_delete_last_word),
    DeleteLine("delete_line", R.string.gesture_delete_line),
    DeleteAll("delete_all", R.string.gesture_delete_all),
    Undo("undo", R.string.gesture_undo),
    CursorLineStart("cursor_line_start", R.string.gesture_cursor_line_start),
    CursorLineEnd("cursor_line_end", R.string.gesture_cursor_line_end),
    CursorFieldStart("cursor_field_start", R.string.gesture_cursor_field_start),
    CursorFieldEnd("cursor_field_end", R.string.gesture_cursor_field_end),
    SelectLastWord("select_last_word", R.string.gesture_select_last_word),
    SelectAll("select_all", R.string.gesture_select_all),
    Copy("copy", R.string.gesture_copy),
    Cut("cut", R.string.gesture_cut),
    Paste("paste", R.string.gesture_paste),
    InsertNewline("insert_newline", R.string.gesture_insert_newline),
    InsertTab("insert_tab", R.string.gesture_insert_tab),
    SwapLastTwoChars("swap_last_two_chars", R.string.gesture_swap_last_two_chars),
    HideKeyboard("hide_keyboard", R.string.gesture_hide_keyboard),
    SwitchKeyboard("switch_keyboard", R.string.gesture_switch_keyboard),
    ;

    companion object {
        const val MIN_TRAINING_SAMPLES = 5

        val catalog: List<GestureAction> = entries

        fun fromId(id: String): GestureAction? = entries.firstOrNull { it.id == id }
    }
}

internal data class GestureCluster(
    val id: ClusterId,
    val action: GestureAction,
    val vector: FeatureVector,
)

internal data class RankedGestureMatch(
    val action: GestureAction,
    val clusterId: ClusterId,
    val distance: Float,
    val sampleCount: Int,
)

internal class GestureRecognitionResult(
    val winner: RankedGestureMatch,
    val ranked: List<RankedGestureMatch>,
    val ambiguity: Ambiguity,
    val sample: FeatureVector,
)
