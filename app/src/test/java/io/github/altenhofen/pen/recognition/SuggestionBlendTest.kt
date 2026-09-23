package io.github.altenhofen.pen.recognition

import org.junit.Assert.assertEquals
import org.junit.Test

class SuggestionBlendTest {
    private fun seed(c: Char) = RankedMatch(c, ClusterId.seed(c), 0f)
    private fun trained(c: Char) = RankedMatch(c, ClusterId.training(c, "s1", 0), 0f)

    @Test
    fun calibratedTemplateOverridesInkModel() {
        val out = blendSuggestions(listOf(trained('0'), seed('o')), listOf("o", "0", "O"))
        assertEquals(listOf("0", "o", "O"), out.map { it.text })
    }

    @Test
    fun inkModelOverridesSeedTemplate() {
        val out = blendSuggestions(listOf(seed('0'), seed('o')), listOf("o", "0", "O"))
        assertEquals(listOf("o", "0", "O"), out.map { it.text })
    }

    @Test
    fun offlineFallsBackToTemplateOrder() {
        val out = blendSuggestions(listOf(seed('a'), trained('d'), seed('q')), emptyList(), limit = 2)
        assertEquals(listOf("d", "a"), out.map { it.text })
    }
}
