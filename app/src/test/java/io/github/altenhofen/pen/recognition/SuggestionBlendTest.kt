package io.github.altenhofen.pen.recognition

import org.junit.Assert.assertEquals
import org.junit.Test

class SuggestionBlendTest {
    private fun seed(c: Char) = RankedMatch(c, ClusterId.seed(c), 0f)
    private fun trained(c: Char, index: Int = 0) = RankedMatch(c, ClusterId.training(c, "s1", index), 0f)

    @Test
    fun inkModelWordWinsOverHeavilyTrainedLetter() {
        val template = listOf(trained('w'), trained('W'), seed('a'))
        val out = blendSuggestions(template, listOf("Augusto", "augusto", "Augustus"))
        assertEquals(listOf("Augusto", "augusto", "Augustus"), out.map { it.text })
    }

    @Test
    fun calibratedTemplateReranksSingleCharacterInkModel() {
        val out = blendSuggestions(listOf(trained('0'), seed('o')), listOf("o", "0"))
        assertEquals(listOf("0", "o"), out.map { it.text })
    }

    @Test
    fun templateNeverInjectsCandidatesTheInkModelDidNotPropose() {
        val out = blendSuggestions(listOf(trained('w'), seed('o')), listOf("o", "0", "O"))
        assertEquals(listOf("o", "0", "O"), out.map { it.text })
    }

    @Test
    fun offlineFallsBackToTemplateOrder() {
        val out = blendSuggestions(listOf(seed('a'), trained('d'), seed('q')), emptyList(), limit = 2)
        assertEquals(listOf("a", "d"), out.map { it.text })
    }
}
