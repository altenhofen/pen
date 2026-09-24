package io.github.altenhofen.pen.recognition

import io.github.altenhofen.pen.settings.SpaceAfterSuggestion

/** Normalizes ML Kit candidates for the current handwriting spacing setting. */
internal fun normalizeInkCandidates(candidates: List<String>, recognizeSpaces: Boolean): List<String> {
    val trimmed = candidates.map { it.trim() }.filter { it.isNotEmpty() }
    if (recognizeSpaces) return trimmed.distinct()
    return trimmed.map { it.replace(" ", "") }.filter { it.isNotEmpty() }.distinct()
}

internal fun isFullWord(text: String): Boolean = text.length > 1

internal fun isPunctuationOnly(text: String): Boolean =
    text.isNotEmpty() && text.all { !it.isLetterOrDigit() }

internal fun appendSpaceAfterSuggestion(
    mode: SpaceAfterSuggestion,
    picked: String,
    replaced: String,
): Boolean {
    if (isPunctuationOnly(picked)) return false
    return when (mode) {
        SpaceAfterSuggestion.Off -> false
        SpaceAfterSuggestion.On -> true
        SpaceAfterSuggestion.Smart -> suggestionPickCompletesWord(picked, replaced)
    }
}

internal fun commitTextForSuggestionPick(
    text: String,
    mode: SpaceAfterSuggestion,
    replaced: String,
): String {
    val (payload, _) = formatSuggestionCommit(text, mode, replaced)
    return payload
}

internal fun formatSuggestionCommit(
    text: String,
    mode: SpaceAfterSuggestion,
    replaced: String,
): Pair<String, Boolean> {
    val space = appendSpaceAfterSuggestion(mode, text, replaced)
    return if (space) "$text " to true else text to false
}

private fun suggestionPickCompletesWord(picked: String, replaced: String): Boolean {
    if (isSingleAlphanumeric(picked)) return false
    if (pickedExtendsReplaced(picked, replaced)) return true
    if (pickedShortensReplaced(picked, replaced)) return false
    return picked.length > 1
}

private fun isSingleAlphanumeric(picked: String): Boolean =
    picked.length == 1 && picked[0].isLetterOrDigit()

private fun pickedExtendsReplaced(picked: String, replaced: String): Boolean {
    val pickedLower = picked.lowercase()
    val replacedLower = replaced.lowercase()
    return replacedLower.length < pickedLower.length && pickedLower.startsWith(replacedLower)
}

private fun pickedShortensReplaced(picked: String, replaced: String): Boolean {
    val pickedLower = picked.lowercase()
    val replacedLower = replaced.lowercase()
    return pickedLower.length < replacedLower.length && replacedLower.startsWith(pickedLower)
}

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
