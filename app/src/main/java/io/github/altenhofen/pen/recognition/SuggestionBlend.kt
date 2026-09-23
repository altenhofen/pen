package io.github.altenhofen.pen.recognition

data class Suggestion(val text: String, val score: Float)

private const val CALIBRATED_WEIGHT = 1.5f
private const val SEED_WEIGHT = 0.6f
private const val INK_MODEL_WEIGHT = 1f

/**
 * The ink model owns the candidate set whenever it answers. The single-glyph template can only
 * rerank those candidates, and only when the model itself reads a single character, because it
 * cannot judge word ink. Ranks, not raw scores, are fused because template DTW distances and ink
 * model ranks live on unrelated scales.
 */
fun blendSuggestions(templateRanked: List<RankedMatch>, inkModel: List<String>, limit: Int = 5): List<Suggestion> {
    val candidates = inkModel.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    if (candidates.isEmpty()) {
        return templateRanked.take(limit).mapIndexed { rank, match ->
            Suggestion(match.character.toString(), match.weight / (rank + 1))
        }
    }
    val inkScores = candidates.mapIndexed { rank, text -> Suggestion(text, INK_MODEL_WEIGHT / (rank + 1)) }
    if (candidates.first().length != 1) return inkScores.take(limit)
    val templateScores = templateRanked
        .mapIndexed { rank, match -> match.character.toString() to match.weight / (rank + 1) }
        .toMap()
    return inkScores
        .map { it.copy(score = it.score + (templateScores[it.text] ?: 0f)) }
        .sortedByDescending { it.score }
        .take(limit)
}

private val RankedMatch.weight: Float get() = if (clusterId.isCalibrated) CALIBRATED_WEIGHT else SEED_WEIGHT

val ClusterId.isCalibrated: Boolean get() = !value.startsWith("seed:")
