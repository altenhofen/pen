package io.github.altenhofen.pen.recognition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordMemoryUndoTest {
    @Test
    fun pinnedSamplesAreNotEvictedByNewSamples() {
        val pinned = sample("p1", "Monsgeek", pinned = true)
        var memory = WordMemory(listOf(pinned))
        repeat(WordMemory.PER_WORD_CAP) { index ->
            val (next, evicted) = memory.remember(sample("u$index", "Monsgeek"))
            memory = next
            assertTrue(evicted.none { it.pinned })
        }
        assertEquals(WordMemory.PER_WORD_CAP + 1, memory.samples.size)
        assertTrue(memory.samples.any { it.id == "p1" })
    }

    @Test
    fun forgetRemovesSample() {
        val kept = sample("k1", "augusto")
        val memory = WordMemory(listOf(kept))
        val (next, removed) = memory.forget("k1")
        assertEquals(kept, removed)
        assertEquals(0, next.samples.size)
    }

    private fun sample(id: String, word: String, pinned: Boolean = false) =
        WordSample(id, word, FeatureVector.from(FloatArray(WORD_SAMPLE_COUNT * 3) { 0.01f * it }, WORD_SAMPLE_COUNT), 1L, pinned)
}
