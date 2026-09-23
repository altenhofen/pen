package io.github.altenhofen.pen.recognition

data class Suggestion(val text: String, val score: Float)

private const val CALIBRATED_WEIGHT = 1.5f
private const val SEED_WEIGHT = 0.6f
private const val INK_MODEL_WEIGHT = 1f

/**
 * Weighted reciprocal-rank fusion. Ranks, not raw scores, are fused because template DTW
 * distances and ink model ranks live on unrelated scales. A template match only speaks with
 * full weight when its nearest cluster came from the user's own calibration.
 */
fun blendSuggestions(templateRanked: List<RankedMatch>, inkModel: List<String>, limit: Int = 5): List<Suggestion> {
    val scores = LinkedHashMap<String, Float>()
    fun add(text: String, score: Float) {
        scores[text] = (scores[text] ?: 0f) + score
    }
    templateRanked.forEachIndexed { rank, match ->
        val weight = if (match.clusterId.isCalibrated) CALIBRATED_WEIGHT else SEED_WEIGHT
        add(match.character.toString(), weight / (rank + 1))
    }
    inkModel.map { it.trim() }.filter { it.isNotEmpty() }.distinct().forEachIndexed { rank, text ->
        add(text, INK_MODEL_WEIGHT / (rank + 1))
    }
    return scores.entries
        .sortedByDescending { it.value }
        .take(limit)
        .map { Suggestion(it.key, it.value) }
}

val ClusterId.isCalibrated: Boolean get() = !value.startsWith("seed:")
