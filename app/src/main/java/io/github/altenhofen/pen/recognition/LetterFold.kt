package io.github.altenhofen.pen.recognition

import java.text.Normalizer

/** Lowercase base letter with combining marks removed (é → e). */
internal fun baseLetter(char: Char): Char {
    val folded = Normalizer.normalize(char.toString(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
    return folded.firstOrNull()?.lowercaseChar() ?: char.lowercaseChar()
}

internal fun hasDiacritic(char: Char): Boolean = baseLetter(char) != char.lowercaseChar()
