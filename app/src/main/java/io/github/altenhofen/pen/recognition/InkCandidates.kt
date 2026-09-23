package io.github.altenhofen.pen.recognition

/** Normalizes ML Kit candidates for the current handwriting spacing setting. */
internal fun normalizeInkCandidates(candidates: List<String>, recognizeSpaces: Boolean): List<String> {
    val trimmed = candidates.map { it.trim() }.filter { it.isNotEmpty() }
    if (recognizeSpaces) return trimmed.distinct()
    return trimmed.map { it.replace(" ", "") }.filter { it.isNotEmpty() }.distinct()
}

internal fun isFullWord(text: String): Boolean = text.length > 1

internal fun isPunctuationOnly(text: String): Boolean =
    text.isNotEmpty() && text.all { !it.isLetterOrDigit() }

internal fun spellingDistance(left: String, right: String): Int {
    val a = left.lowercase()
    val b = right.lowercase()
    if (a == b) return 0
    val costs = IntArray(b.length + 1) { it }
    for (i in a.indices) {
        var previous = costs[0]
        costs[0] = i + 1
        for (j in b.indices) {
            val temp = costs[j + 1]
            costs[j + 1] = when {
                a[i] == b[j] -> previous
                else -> 1 + minOf(previous, costs[j], costs[j + 1])
            }
            previous = temp
        }
    }
    return costs[b.length]
}
