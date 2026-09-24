package io.github.altenhofen.pen.calibration

import io.github.altenhofen.pen.R

internal data class GlyphFamily(
    val titleRes: Int,
    val labels: List<Char>,
)

internal object CalibrateGlyphs {
    val DIGITS: List<Char> = ('0'..'9').toList()
    val UPPER: List<Char> = ('A'..'Z').toList()
    val LOWER: List<Char> = ('a'..'z').toList()
    val MATH: List<Char> = listOf(
        '+', '\u2212', '\u00D7', '\u00F7', '=', '\u2260', '<', '>', '\u2264', '\u2265',
        '\u00B1', '\u221E', '\u221A', '\u03C0', '\u2211', '\u222B',
        '(', ')', '[', ']', '{', '}', '^', '/', '%',
    )
    val families: List<GlyphFamily> = listOf(
        GlyphFamily(R.string.calibrate_section_digits, DIGITS),
        GlyphFamily(R.string.calibrate_section_uppercase, UPPER),
        GlyphFamily(R.string.calibrate_section_lowercase, LOWER),
        GlyphFamily(R.string.calibrate_section_math, MATH),
    )
    val ALL: List<Char> = families.flatMap { it.labels }
}

internal class SelectedGlyphs private constructor(val labels: List<Char>) {
    companion object {
        fun of(selected: Set<Char>): SelectedGlyphs? {
            val labels = CalibrateGlyphs.ALL.filter { it in selected }
            return if (labels.isEmpty()) null else SelectedGlyphs(labels)
        }
    }
}
