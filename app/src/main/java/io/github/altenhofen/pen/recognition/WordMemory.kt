package io.github.altenhofen.pen.recognition

import io.github.altenhofen.pen.ime.Stroke

internal const val WORD_SAMPLE_COUNT = 64
private const val WORD_DTW_WINDOW = 16

/** One confirmed writing of [word]: the user kept it, or picked it from the strip. */
internal data class WordSample(
    val id: String,
    val word: String,
    val vector: FeatureVector,
    val confirmedAt: Long,
    val pinned: Boolean = false,
) {
    init {
        require(word.isNotBlank()) { "word must be non-blank" }
        require(vector.sampleCount == WORD_SAMPLE_COUNT) { "word vector must have $WORD_SAMPLE_COUNT samples" }
    }
}

internal data class WordRecall(val word: String, val distance: Float)

internal fun wordFeatures(strokes: List<Stroke>): FeatureVector? =
    wordVector(strokes.map { stroke -> stroke.points().map { Point2(it.x, it.y) } })

internal fun wordVector(polylines: List<List<Point2>>): FeatureVector? =
    preprocessPolylines(polylines, WORD_SAMPLE_COUNT, Encoding.Positions)

internal class WordMemory(val samples: List<WordSample> = emptyList()) {
    /** Returns the new memory and the samples that fell out of it. */
    fun remember(sample: WordSample): Pair<WordMemory, List<WordSample>> {
        val evicted = ArrayList<WordSample>()
        val sameWord = samples.filter { it.word == sample.word }
        if (!sample.pinned) {
            val unpinned = sameWord.filter { !it.pinned }
            if (unpinned.size >= PER_WORD_CAP) {
                evicted += unpinned.sortedBy { it.confirmedAt }.take(unpinned.size - PER_WORD_CAP + 1)
            }
        }
        val evictedIds = evicted.map { it.id }.toSet()
        var kept = samples.filter { it.id !in evictedIds } + sample
        if (kept.size > TOTAL_CAP) {
            val overflow = kept.filter { !it.pinned }.sortedBy { it.confirmedAt }
            val dropCount = kept.size - TOTAL_CAP
            if (overflow.size < dropCount) throw IllegalStateException("only pinned samples remain")
            val overflowIds = overflow.take(dropCount).map { it.id }.toSet()
            evicted += kept.filter { it.id in overflowIds }
            kept = kept.filter { it.id !in overflowIds }
        }
        return WordMemory(kept) to evicted
    }

    fun forget(id: String): Pair<WordMemory, WordSample?> {
        val removed = samples.singleOrNull { it.id == id } ?: return this to null
        return WordMemory(samples.filter { it.id != id }) to removed
    }

    fun forgetWord(word: String): Pair<WordMemory, List<WordSample>> {
        val removed = samples.filter { it.word == word }
        return WordMemory(samples.filter { it.word != word }) to removed
    }

    /** Each word scored by the mean DTW distance of its nearest samples, closest first. */
    fun recall(vector: FeatureVector): List<WordRecall> = samples
        .groupBy { it.word }
        .map { (word, perWord) ->
            val nearest = perWord.map { bandedDtw(vector, it.vector, WORD_DTW_WINDOW) }.sorted().take(NEAREST_PER_WORD)
            WordRecall(word, nearest.average().toFloat())
        }
        .sortedBy { it.distance }

    fun confirmations(): Map<String, Int> = samples.filter { !it.pinned }.groupingBy { it.word }.eachCount()

    fun pinnedTrainingCount(word: String): Int = samples.count { it.word == word && it.pinned }

    companion object {
        const val PER_WORD_CAP = 5
        const val TOTAL_CAP = 1000
        const val NEAREST_PER_WORD = 3
        const val PINNED_TRAINING_CAP = 5
    }
}
