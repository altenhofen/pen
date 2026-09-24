package io.github.altenhofen.pen.ime

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import io.github.altenhofen.pen.recognition.GestureAction

internal class GestureUndoStack {
    private var snapshot: UndoSnapshot? = null

    data class UndoSnapshot(
        val text: String,
        val selectionStart: Int,
        val selectionEnd: Int,
    )

    fun record(connection: InputConnection, window: Int = 4096) {
        val before = connection.getTextBeforeCursor(window, 0)?.toString().orEmpty()
        val after = connection.getTextAfterCursor(window, 0)?.toString().orEmpty()
        val selected = connection.getSelectedText(0)?.toString().orEmpty()
        val start = before.length
        snapshot = UndoSnapshot(before + selected + after, start, start + selected.length)
    }

    fun undo(connection: InputConnection): Boolean {
        val entry = snapshot ?: return false
        snapshot = null
        connection.beginBatchEdit()
        connection.deleteSurroundingText(Int.MAX_VALUE, Int.MAX_VALUE)
        connection.commitText(entry.text, 1)
        connection.setSelection(entry.selectionStart, entry.selectionEnd)
        connection.endBatchEdit()
        return true
    }

    fun clear() {
        snapshot = null
    }
}

internal class GestureExecutor(
    private val context: Context,
    private val undo: GestureUndoStack,
) {
    fun perform(action: GestureAction, connection: InputConnection): Boolean = when (action) {
        GestureAction.Undo -> undo.undo(connection)
        GestureAction.LowercaseLastWord -> transformLastWord(connection) { it.lowercase() }
        GestureAction.UppercaseLastWord -> transformLastWord(connection) { it.uppercase() }
        GestureAction.CapitalizeLastWord -> transformLastWord(connection) { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        GestureAction.CycleCaseLastWord -> cycleCaseLastWord(connection)
        GestureAction.DeleteLastWord -> deleteLastWord(connection)
        GestureAction.DeleteLine -> deleteLine(connection)
        GestureAction.DeleteAll -> deleteAll(connection)
        GestureAction.CursorLineStart -> moveToLineStart(connection)
        GestureAction.CursorLineEnd -> moveToLineEnd(connection)
        GestureAction.CursorFieldStart -> setCursor(connection, 0)
        GestureAction.CursorFieldEnd -> setCursor(connection, Int.MAX_VALUE)
        GestureAction.SelectLastWord -> selectLastWord(connection)
        GestureAction.SelectAll -> selectAll(connection)
        GestureAction.Copy -> clipboard(connection, cut = false)
        GestureAction.Cut -> clipboard(connection, cut = true)
        GestureAction.Paste -> paste(connection)
        GestureAction.InsertNewline -> insert(connection, "\n")
        GestureAction.InsertTab -> insert(connection, "\t")
        GestureAction.SwapLastTwoChars -> swapLastTwo(connection)
        GestureAction.HideKeyboard -> {
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                ?.hideSoftInputFromWindow(null, 0)
            true
        }
        GestureAction.SwitchKeyboard -> {
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                ?.showInputMethodPicker()
            true
        }
    }

    private fun transformLastWord(connection: InputConnection, transform: (String) -> String): Boolean {
        val word = lastWord(connection) ?: return false
        undo.record(connection)
        connection.deleteSurroundingText(word.length, 0)
        connection.commitText(transform(word), 1)
        return true
    }

    private fun cycleCaseLastWord(connection: InputConnection): Boolean {
        val word = lastWord(connection) ?: return false
        val next = when {
            word == word.lowercase() -> word.uppercase()
            word == word.uppercase() -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            else -> word.lowercase()
        }
        undo.record(connection)
        connection.deleteSurroundingText(word.length, 0)
        connection.commitText(next, 1)
        return true
    }

    private fun deleteLastWord(connection: InputConnection): Boolean {
        val word = lastWord(connection) ?: return false
        undo.record(connection)
        connection.deleteSurroundingText(word.length, 0)
        return true
    }

    private fun deleteLine(connection: InputConnection): Boolean {
        val before = connection.getTextBeforeCursor(4096, 0)?.toString().orEmpty()
        val after = connection.getTextAfterCursor(4096, 0)?.toString().orEmpty()
        val (deleteBefore, deleteAfter) = lineDeletionRange(before, after) ?: return false
        undo.record(connection)
        connection.deleteSurroundingText(deleteBefore, deleteAfter)
        return true
    }

    private fun deleteAll(connection: InputConnection): Boolean {
        undo.record(connection)
        connection.deleteSurroundingText(Int.MAX_VALUE, Int.MAX_VALUE)
        return true
    }

    private fun moveToLineStart(connection: InputConnection): Boolean {
        val before = connection.getTextBeforeCursor(4096, 0)?.toString().orEmpty()
        val drop = before.length - before.lastIndexOf('\n').let { if (it < 0) 0 else it + 1 }
        if (drop == 0) return false
        connection.deleteSurroundingText(drop, 0)
        return true
    }

    private fun moveToLineEnd(connection: InputConnection): Boolean {
        val after = connection.getTextAfterCursor(4096, 0)?.toString().orEmpty()
        val take = after.indexOf('\n').let { if (it < 0) after.length else it }
        if (take == 0) return false
        connection.deleteSurroundingText(0, take)
        return true
    }

    private fun setCursor(connection: InputConnection, position: Int): Boolean {
        val before = connection.getTextBeforeCursor(4096, 0)?.toString().orEmpty()
        val after = connection.getTextAfterCursor(4096, 0)?.toString().orEmpty()
        val total = before.length + after.length
        val target = if (position == Int.MAX_VALUE) total else position.coerceIn(0, total)
        val left = target - before.length
        if (left < 0) connection.deleteSurroundingText(-left, 0)
        else if (left > 0) connection.deleteSurroundingText(0, left)
        return true
    }

    private fun selectLastWord(connection: InputConnection): Boolean {
        val before = connection.getTextBeforeCursor(4096, 0)?.toString().orEmpty()
        val word = trailingWord(before) ?: return false
        connection.setSelection(before.length - word.length, before.length)
        return true
    }

    private fun selectAll(connection: InputConnection): Boolean {
        val before = connection.getTextBeforeCursor(4096, 0)?.toString().orEmpty()
        val after = connection.getTextAfterCursor(4096, 0)?.toString().orEmpty()
        connection.setSelection(0, before.length + after.length)
        return true
    }

    private fun clipboard(connection: InputConnection, cut: Boolean): Boolean {
        val selected = connection.getSelectedText(0)?.toString()
        val text = selected?.takeIf { it.isNotEmpty() } ?: return false
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("pen", text))
        if (cut) connection.commitText("", 1)
        return true
    }

    private fun paste(connection: InputConnection): Boolean {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString() ?: return false
        connection.commitText(text, 1)
        return true
    }

    private fun insert(connection: InputConnection, text: String): Boolean {
        connection.commitText(text, 1)
        return true
    }

    private fun swapLastTwo(connection: InputConnection): Boolean {
        val before = connection.getTextBeforeCursor(2, 0)?.toString().orEmpty()
        if (before.length < 2) return false
        undo.record(connection)
        connection.deleteSurroundingText(2, 0)
        connection.commitText(before[1].toString() + before[0], 1)
        return true
    }

    private fun lastWord(connection: InputConnection): String? =
        trailingWord(connection.getTextBeforeCursor(256, 0)?.toString().orEmpty())

    private fun trailingWord(before: String): String? {
        val trimmed = before.trimEnd()
        if (trimmed.isEmpty()) return null
        val match = Regex("\\S+").findAll(trimmed).lastOrNull() ?: return null
        return match.value
    }
}

/**
 * Text to remove around the cursor for [GestureAction.DeleteLine].
 * Includes a trailing newline when the current line is not the last one in the field.
 */
internal fun lineDeletionRange(before: String, after: String): Pair<Int, Int>? {
    val lineStartOffset = before.lastIndexOf('\n').let { if (it < 0) 0 else it + 1 }
    val deleteBefore = before.length - lineStartOffset
    val newlineIndex = after.indexOf('\n')
    val deleteAfter = if (newlineIndex >= 0) newlineIndex + 1 else after.length
    if (deleteBefore == 0 && deleteAfter == 0) return null
    return deleteBefore to deleteAfter
}
