package io.github.altenhofen.pen.recognition

import io.github.altenhofen.pen.settings.SpaceAfterSuggestion
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

    @Test
    fun spaceAfterSuggestionOffNeverAppends() {
        assertEquals("hello", commitTextForSuggestionPick("hello", SpaceAfterSuggestion.Off, "hel"))
        assertEquals("a", commitTextForSuggestionPick("a", SpaceAfterSuggestion.Off, ""))
        assertEquals(",", commitTextForSuggestionPick(",", SpaceAfterSuggestion.Off, "word"))
    }

    @Test
    fun spaceAfterSuggestionOnSpacesNonPunctuation() {
        assertEquals("hello ", commitTextForSuggestionPick("hello", SpaceAfterSuggestion.On, "hel"))
        assertEquals("a ", commitTextForSuggestionPick("a", SpaceAfterSuggestion.On, ""))
        assertEquals("9 ", commitTextForSuggestionPick("9", SpaceAfterSuggestion.On, ""))
        assertEquals(",", commitTextForSuggestionPick(",", SpaceAfterSuggestion.On, "word"))
        assertEquals("...", commitTextForSuggestionPick("...", SpaceAfterSuggestion.On, "hi"))
    }

    @Test
    fun spaceAfterSuggestionSmartCompletingAndPartOfWord() {
        assertEquals("hello ", commitTextForSuggestionPick("hello", SpaceAfterSuggestion.Smart, "hel"))
        assertEquals("Hello ", commitTextForSuggestionPick("Hello", SpaceAfterSuggestion.Smart, "hel"))
        assertEquals("a", commitTextForSuggestionPick("a", SpaceAfterSuggestion.Smart, ""))
        assertEquals("9", commitTextForSuggestionPick("9", SpaceAfterSuggestion.Smart, ""))
        assertEquals("hel", commitTextForSuggestionPick("hel", SpaceAfterSuggestion.Smart, "hello"))
        assertEquals("world ", commitTextForSuggestionPick("world", SpaceAfterSuggestion.Smart, "hello"))
        assertEquals("hello ", commitTextForSuggestionPick("hello", SpaceAfterSuggestion.Smart, "hello"))
        assertEquals(",", commitTextForSuggestionPick(",", SpaceAfterSuggestion.Smart, "hi"))
        assertEquals("!", commitTextForSuggestionPick("!", SpaceAfterSuggestion.Smart, "word"))
    }
}
