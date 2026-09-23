package io.github.altenhofen.pen.ime

import io.github.altenhofen.pen.recognition.FeatureVector
import io.github.altenhofen.pen.recognition.Feedback
import io.github.altenhofen.pen.recognition.RecognitionResult

/** What the last committed ink taught us, once the user's next action settles it. */
internal sealed interface Resolution {
    val glyph: RecognitionResult?
    val glyphFeedback: Feedback?

    /** The user kept [text] for [ink], by carrying on or by picking it from the strip. */
    data class Kept(val text: String, val ink: FeatureVector?, override val glyph: RecognitionResult?) : Resolution {
        override val glyphFeedback: Feedback?
            get() = glyph?.let { if (it.winner.character.toString() == text) Feedback.Accepted else Feedback.Rejected }
    }

    /** The user deleted the commit. Nothing is remembered. */
    data class Corrected(override val glyph: RecognitionResult?) : Resolution {
        override val glyphFeedback: Feedback? get() = glyph?.let { Feedback.Rejected }
    }
}

internal class PendingCommit {
    private class Entry(val text: String, val ink: FeatureVector?, val glyph: RecognitionResult?)

    private var entry: Entry? = null

    /** [glyph] only counts when it produced [text], so the template learns from its own guesses. */
    fun committed(text: String, ink: FeatureVector?, glyph: RecognitionResult?): Resolution? {
        val previous = kept()
        entry = Entry(text, ink, glyph?.takeIf { it.winner.character.toString() == text })
        return previous
    }

    fun kept(): Resolution? = take()?.let { Resolution.Kept(it.text, it.ink, it.glyph) }

    fun picked(text: String): Resolution? = take()?.let { Resolution.Kept(text, it.ink, it.glyph) }

    fun corrected(): Resolution? = take()?.let { Resolution.Corrected(it.glyph) }

    fun discard() {
        entry = null
    }

    private fun take(): Entry? = entry.also { entry = null }
}
