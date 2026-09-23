package io.github.altenhofen.pen.recognition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InkCandidatesTest {
    @Test
    fun spacesAreStrippedWhenRecognitionIsOff() {
        assertEquals(listOf("LIT"), normalizeInkCandidates(listOf("L IT", "LIT"), recognizeSpaces = false))
    }

    @Test
    fun spacesAreKeptWhenRecognitionIsOn() {
        assertEquals(listOf("bom dia"), normalizeInkCandidates(listOf("bom dia"), recognizeSpaces = true))
    }

    @Test
    fun customDictionarySpellingDistance() {
        assertEquals(1, spellingDistance("Monsgeck", "Monsgeek"))
        assertTrue(spellingDistance("monsgeek", "Monsgeek") <= 2)
    }
}
