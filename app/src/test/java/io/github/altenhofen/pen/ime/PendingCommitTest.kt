package io.github.altenhofen.pen.ime

import io.github.altenhofen.pen.recognition.Feedback
import io.github.altenhofen.pen.recognition.glyphResult
import io.github.altenhofen.pen.recognition.seedVector
import io.github.altenhofen.pen.recognition.trained
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PendingCommitTest {
    private val ink = seedVector('8')

    @Test
    fun tappingAStripAlternativeKeepsTheInkUnderTheTappedText() {
        val pending = PendingCommit()
        pending.committed("angusta", ink, null)

        val resolution = pending.picked("augusto") as Resolution.Kept

        assertEquals("augusto", resolution.text)
        assertEquals(ink, resolution.ink)
        assertNull(pending.kept())
    }

    @Test
    fun theNextCommitConfirmsThePreviousOne() {
        val pending = PendingCommit()
        pending.committed("pen", ink, null)

        val resolution = pending.committed("ink", ink, null) as Resolution.Kept

        assertEquals("pen", resolution.text)
    }

    @Test
    fun correctionRemembersNothingAndRejectsTheGlyphWinner() {
        val pending = PendingCommit()
        pending.committed("a", ink, glyphResult(trained('a')))

        val resolution = pending.corrected()

        assertEquals(Resolution.Corrected::class, resolution!!::class)
        assertEquals(Feedback.Rejected, resolution.glyphFeedback)
    }

    @Test
    fun glyphFeedbackOnlyWhenTheTemplateProducedTheCommit() {
        val fromModel = PendingCommit().apply { committed("Augusto", ink, glyphResult(trained('w'))) }
        assertNull(fromModel.kept()!!.glyphFeedback)

        val fromTemplate = PendingCommit().apply { committed("a", ink, glyphResult(trained('a'))) }
        assertEquals(Feedback.Accepted, fromTemplate.kept()!!.glyphFeedback)

        val repicked = PendingCommit().apply { committed("a", ink, glyphResult(trained('a'))) }
        assertEquals(Feedback.Rejected, repicked.picked("d")!!.glyphFeedback)
    }
}
