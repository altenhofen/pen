package io.github.altenhofen.pen.recognition

import org.junit.Assert.assertEquals
import org.junit.Test

internal fun seed(c: Char) = RankedMatch(c, ClusterId.seed(c), 0f)
internal fun trained(c: Char, index: Int = 0) = RankedMatch(c, ClusterId.training(c, "s1", index), 0f)

internal fun glyphResult(vararg ranked: RankedMatch, gap: Float = 0f, threshold: Float = 0.15f) =
    RecognitionResult(ranked.first(), ranked.toList(), Ambiguity(gap, threshold), seedVector('0'))

class SuggestionBlendTest {
    private fun blend(
        inkModel: List<String>,
        glyph: RecognitionResult? = null,
        recalls: List<WordRecall> = emptyList(),
        confirmations: Map<String, Int> = emptyMap(),
        limit: Int = 5,
    ): List<String> {
        val ink = InkModelSource(inkModel, recognizeSpaces = false)
        return blendSuggestions(
            listOf(ink, GlyphTemplateSource(glyph, ink), WordMemorySource(recalls, confirmations)),
            limit,
        ).map { it.text }
    }

    @Test
    fun inkModelWordWinsOverHeavilyTrainedLetter() {
        val glyph = glyphResult(trained('w'), trained('a'), gap = 0.5f)
        assertEquals(listOf("Augusto", "augusto", "Augustus"), blend(listOf("Augusto", "augusto", "Augustus"), glyph))
    }

    @Test
    fun confidentCalibratedTemplateReranksSingleCharacterInkModel() {
        val glyph = glyphResult(trained('0'), seed('o'), gap = 0.3f)
        assertEquals(listOf("0", "o"), blend(listOf("o", "0"), glyph))
    }

    @Test
    fun borderlineTemplateDoesNotOverrideInkModel() {
        val glyph = glyphResult(trained('0'), seed('o'), gap = 0f)
        assertEquals(listOf("o", "0"), blend(listOf("o", "0"), glyph))
    }

    @Test
    fun templateReranksCaseInsensitivelyAndKeepsInkModelCasing() {
        val glyph = glyphResult(trained('a'), seed('d'), gap = 0.3f)
        assertEquals(listOf("A", "d"), blend(listOf("d", "A"), glyph))
    }

    @Test
    fun templatePrefersAccentedInkOverPlainBaseLetter() {
        val glyph = glyphResult(trained('e'), seed('l'), gap = 0.3f)
        assertEquals(listOf("é", "e"), blend(listOf("e", "é"), glyph))
    }

    @Test
    fun templateNeverInjectsCandidatesTheInkModelDidNotPropose() {
        val glyph = glyphResult(trained('w'), seed('o'), gap = 0.3f)
        assertEquals(listOf("o", "0", "x"), blend(listOf("o", "0", "x"), glyph))
    }

    @Test
    fun offlineFallsBackToTemplateOrder() {
        val glyph = glyphResult(seed('a'), trained('d'), seed('q'))
        assertEquals(listOf("a", "d"), blend(emptyList(), glyph, limit = 2))
    }

    @Test
    fun strongWordMemoryRecallBeatsInkModelMisread() {
        val recalls = listOf(WordRecall("augusto", 0.01f))
        assertEquals(
            listOf("augusto", "angusta", "augusta"),
            blend(listOf("angusta", "augusta"), recalls = recalls, confirmations = mapOf("augusto" to 1)),
        )
    }

    @Test
    fun weakWordMemoryRecallDoesNotDisplaceInkModel() {
        val weak = WordMemorySource.WORD_MATCH_DISTANCE * 0.8f
        val recalls = listOf(WordRecall("augusto", weak))
        assertEquals(
            listOf("angusta", "augusto", "augusta"),
            blend(listOf("angusta", "augusta"), recalls = recalls, confirmations = mapOf("augusto" to 1)),
        )
    }

    @Test
    fun wordMemoryBeyondThresholdIsNotProposed() {
        val recalls = listOf(WordRecall("augusto", WordMemorySource.WORD_MATCH_DISTANCE * 1.5f))
        assertEquals(listOf("angusta", "augusta"), blend(listOf("angusta", "augusta"), recalls = recalls))
    }

    @Test
    fun confirmedWordPriorBreaksCloseInkModelRanks() {
        assertEquals(
            listOf("augusta", "angusta"),
            blend(listOf("angusta", "augusta"), confirmations = mapOf("augusta" to 5)),
        )
    }
}
