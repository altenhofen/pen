package io.github.altenhofen.pen.recognition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LetterFoldTest {
    @Test
    fun foldsAccentedLettersToBase() {
        assertEquals('e', baseLetter('é'))
        assertEquals('e', baseLetter('Ê'))
        assertEquals('a', baseLetter('ã'))
    }

    @Test
    fun detectsDiacritics() {
        assertTrue(hasDiacritic('é'))
        assertTrue(!hasDiacritic('e'))
    }
}
