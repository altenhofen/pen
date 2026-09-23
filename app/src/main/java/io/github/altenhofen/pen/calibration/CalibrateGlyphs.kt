package io.github.altenhofen.pen.calibration

internal object CalibrateGlyphs {
    val DIGITS: List<Char> = ('0'..'9').toList()
    val UPPER: List<Char> = ('A'..'Z').toList()
    val LOWER: List<Char> = ('a'..'z').toList()
    val ALL: List<Char> = DIGITS + UPPER + LOWER
}

internal class SelectedGlyphs private constructor(val labels: List<Char>) {
    companion object {
        fun of(selected: Set<Char>): SelectedGlyphs? {
            val labels = CalibrateGlyphs.ALL.filter { it in selected }
            return if (labels.isEmpty()) null else SelectedGlyphs(labels)
        }
    }
}
