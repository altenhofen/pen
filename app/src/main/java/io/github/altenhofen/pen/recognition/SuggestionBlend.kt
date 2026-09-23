package io.github.altenhofen.pen.recognition

data class Suggestion(val text: String, val score: Float)

internal interface SuggestionSource {
    fun proposals(): List<String>
    fun score(candidate: String): Float
}

internal class InkModelSource(
    candidates: List<String>,
    recognizeSpaces: Boolean,
) : SuggestionSource {
    val ranked = normalizeInkCandidates(candidates, recognizeSpaces)

    override fun proposals() = ranked

    override fun score(candidate: String): Float {
        val rank = ranked.indexOf(candidate)
        return if (rank < 0) 0f else WEIGHT / (rank + 1)
    }

    private companion object {
        const val WEIGHT = 1f
    }
}

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
        const val WORD_MATCH_DISTANCE = 0.04f
        const val MATCH_WEIGHT = 2f
        const val PRIOR_WEIGHT = 0.6f
    }
}

/**
 * Custom dictionary entries matched by edit distance to ink-model candidates. They only surface
 * when ML Kit's own top pick is weak or already close in spelling.
 */
internal class CustomDictionarySource(
    private val dictionary: List<String>,
    private val inkModel: InkModelSource,
) : SuggestionSource {
    private val topInk = inkModel.ranked.firstOrNull()
    private val weakTop = topInk == null || inkModel.score(topInk) <= WEAK_TOP_SCORE

    override fun proposals(): List<String> = dictionary.filter { entry ->
        val alreadyListed = inkModel.ranked.any { it.equals(entry, ignoreCase = true) }
        if (alreadyListed) return@filter false
        inkModel.ranked.any { spellingDistance(it, entry) <= MAX_SPELLING_DISTANCE }
    }

    override fun score(candidate: String): Float {
        val nearestInk = inkModel.ranked.minOfOrNull { spellingDistance(it, candidate) } ?: return 0f
        if (nearestInk > MAX_SPELLING_DISTANCE) return 0f
        val spelling = SPELLING_WEIGHT * (1f - nearestInk.toFloat() / MAX_SPELLING_DISTANCE)
        val prior = PRIOR_WEIGHT
        val inkTop = topInk?.let { inkModel.score(it) } ?: 0f
        val cap = if (inkTop > WEAK_TOP_SCORE && nearestInk > 0) spelling * 0.5f else spelling
        return cap + prior
    }

    companion object {
        const val MAX_SPELLING_DISTANCE = 2
        const val SPELLING_WEIGHT = 1.4f
        const val PRIOR_WEIGHT = 0.6f
        const val WEAK_TOP_SCORE = 0.55f
    }
}

internal fun blendSuggestions(sources: List<SuggestionSource>, limit: Int = 5): List<Suggestion> =
    sources.flatMap { it.proposals() }
        .distinct()
        .map { candidate -> Suggestion(candidate, sources.sumOf { it.score(candidate).toDouble() }.toFloat()) }
        .sortedByDescending { it.score }
        .take(limit)

val ClusterId.isCalibrated: Boolean get() = !value.startsWith("seed:")
