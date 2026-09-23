package io.github.altenhofen.pen.recognition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

private fun wave(humps: Int, wobble: Float = 0f, lift: Float = 0f): FeatureVector {
    val points = (0..200).map { step ->
        val t = step / 200f
        val y = sin(t * humps * 2 * PI).toFloat() * (0.3f + lift) + wobble * sin(t * 37f)
        Point2(t * humps, y)
    }
    return requireNotNull(wordVector(listOf(points)))
}

private fun sample(word: String, at: Long, vector: FeatureVector = wave(7)) = WordSample("$word@$at", word, vector, at)

class WordMemoryTest {
    @Test
    fun perWordCapEvictsTheOldestSample() {
        var memory = WordMemory()
        for (at in 1L..WordMemory.PER_WORD_CAP) memory = memory.remember(sample("augusto", at)).first
        memory = memory.remember(sample("pen", 10L)).first

        val (next, evicted) = memory.remember(sample("augusto", 99L))

        assertEquals(listOf("augusto@1"), evicted.map { it.id })
        assertEquals(
            listOf("augusto@2", "augusto@3", "augusto@4", "augusto@5", "pen@10", "augusto@99"),
            next.samples.map { it.id },
        )
    }

    @Test
    fun totalCapEvictsTheOldestAcrossWords() {
        var memory = WordMemory()
        for (at in 1L..WordMemory.TOTAL_CAP) memory = memory.remember(sample("w$at", at)).first

        val (next, evicted) = memory.remember(sample("new", 5000L))

        assertEquals(listOf("w1@1"), evicted.map { it.id })
        assertEquals(WordMemory.TOTAL_CAP, next.samples.size)
    }

    @Test
    fun recallFindsTheSameWordWrittenAgainWithinTheMatchDistance() {
        val memory = WordMemory(listOf(sample("augusto", 1L, wave(7)), sample("pen", 2L, wave(3))))

        val recalls = memory.recall(wave(7, wobble = 0.03f, lift = 0.05f))

        assertEquals(listOf("augusto", "pen"), recalls.map { it.word })
        assertTrue(recalls[0].distance < WordMemorySource.WORD_MATCH_DISTANCE)
        assertTrue(recalls[1].distance > WordMemorySource.WORD_MATCH_DISTANCE)
    }

    @Test
    fun confirmationsCountSamplesPerWord() {
        val memory = WordMemory(listOf(sample("a", 1L), sample("a", 2L), sample("b", 3L)))
        assertEquals(mapOf("a" to 2, "b" to 1), memory.confirmations())
    }
}
