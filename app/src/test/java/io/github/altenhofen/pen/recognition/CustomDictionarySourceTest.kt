package io.github.altenhofen.pen.recognition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomDictionarySourceTest {
    @Test
    fun nearMissAddsDictionaryWordWhenInkModelIsWeak() {
        val ink = InkModelSource(listOf("Monsgeck"), recognizeSpaces = false)
        val source = CustomDictionarySource(listOf("Monsgeek"), ink)
        assertEquals(listOf("Monsgeek"), source.proposals())
    }

    @Test
    fun dictionaryDoesNotOutrankStrongInkModelMatch() {
        val ink = InkModelSource(listOf("hello"), recognizeSpaces = false)
        val source = CustomDictionarySource(listOf("help"), ink)
        assertTrue(source.proposals().contains("help"))
        assertTrue(ink.score("hello") > source.score("help"))
    }
}
