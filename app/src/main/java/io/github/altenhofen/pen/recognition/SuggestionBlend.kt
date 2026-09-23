package io.github.altenhofen.pen.recognition

data class Suggestion(val text: String, val score: Float)

/**
 * One opinion about the ink. A source proposes candidates it is allowed to add and scores any
 * candidate. Ranks and thresholds, not raw scores, cross source boundaries because template DTW,
 * word DTW, and ink model ranks live on unrelated scales.
 */
internal interface SuggestionSource {
    fun proposals(): List<String>
    fun score(candidate: String): Float
}

internal class InkModelSource(candidates: List<String>) : SuggestionSource {
    val ranked = candidates.map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    override fun proposals() = ranked

    override fun score(candidate: String): Float {
        val rank = ranked.indexOf(candidate)
        return if (rank < 0) 0f else WEIGHT / (rank + 1)
    }

    private companion object {
        const val WEIGHT = 1f
    }
}

/**
 * The single-glyph templates. They cannot judge word ink, so they only speak when the ink model
 * is silent or itself reads a single character, and only propose when the ink model is silent.
 * Labels are lowercase, so single-character candidates match case-insensitively and keep the
 * ink model's casing.
 */
internal class GlyphTemplateSource(
    private val result: RecognitionResult?,
    inkModel: InkModelSource,
) : SuggestionSource {
    private val silentModel = inkModel.ranked.isEmpty()
    private val active = result != null && (silentModel || inkModel.ranked.first().length == 1)
    private val rankOf: Map<Char, Int> =
        result?.ranked?.withIndex()?.associate { (rank, match) -> match.character.lowercaseChar() to rank }.orEmpty()

    override fun proposals(): List<String> =
        if (active && silentModel) result!!.ranked.map { it.character.toString() } else emptyList()

    override fun score(candidate: String): Float {
        if (!active || candidate.length != 1) return 0f
        val result = result!!
        val rank = rankOf[candidate.single().lowercaseChar()] ?: return 0f
        val weight = if (result.winner.clusterId.isCalibrated) CALIBRATED_WEIGHT else SEED_WEIGHT
        if (rank > 0) return weight / (rank + 1)
        val confidence = (result.ambiguity.gap / result.ambiguity.threshold).coerceIn(0f, MAX_CONFIDENCE)
        return weight * (0.5f + 0.5f * confidence)
    }

    private companion object {
        const val CALIBRATED_WEIGHT = 1.5f
        const val SEED_WEIGHT = 0.6f
        const val MAX_CONFIDENCE = 2f
    }
}

/**
 * The user's own confirmed writing. A recall inside [WORD_MATCH_DISTANCE] may add a word the ink
 * model never proposed, and a close one outweighs the ink model's top pick. Confirmed words also
 * get a small prior so they win ties among the ink model's own candidates.
 */
internal class WordMemorySource(
    private val recalls: List<WordRecall>,
    private val confirmations: Map<String, Int>,
) : SuggestionSource {
    private val close = recalls.filter { it.distance < WORD_MATCH_DISTANCE }

    override fun proposals() = close.map { it.word }

    override fun score(candidate: String): Float {
        val shape = close.firstOrNull { it.word == candidate }
            ?.let { MATCH_WEIGHT * (1f - it.distance / WORD_MATCH_DISTANCE) } ?: 0f
        val prior = (confirmations[candidate] ?: 0).coerceAtMost(WordMemory.PER_WORD_CAP)
            .toFloat() / WordMemory.PER_WORD_CAP * PRIOR_WEIGHT
        return shape + prior
    }

    companion object {
        const val WORD_MATCH_DISTANCE = 0.025f
        const val MATCH_WEIGHT = 2f
        const val PRIOR_WEIGHT = 0.6f
    }
}

internal fun blendSuggestions(sources: List<SuggestionSource>, limit: Int = 5): List<Suggestion> =
    sources.flatMap { it.proposals() }
        .distinct()
        .map { candidate -> Suggestion(candidate, sources.sumOf { it.score(candidate).toDouble() }.toFloat()) }
        .sortedByDescending { it.score }
        .take(limit)

val ClusterId.isCalibrated: Boolean get() = !value.startsWith("seed:")
