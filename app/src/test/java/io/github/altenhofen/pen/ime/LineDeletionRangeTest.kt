package io.github.altenhofen.pen.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LineDeletionRangeTest {
    @Test
    fun endOfFirstLineDeletesLineAndTrailingNewline() {
        assertEquals(5 to 1, lineDeletionRange("line1", "\nline2"))
    }

    @Test
    fun lastLineDeletesThroughEndWithoutExtraNewline() {
        assertEquals(5 to 0, lineDeletionRange("line1", ""))
    }

    @Test
    fun middleOfLineDeletesWholeLine() {
        assertEquals(3 to 3, lineDeletionRange("hel", "lo\nnext"))
    }

    @Test
    fun emptyLineWithFollowingNewlineDeletesNewline() {
        assertEquals(0 to 1, lineDeletionRange("line1\n", "\nline3"))
    }

    @Test
    fun emptyFieldReturnsNull() {
        assertNull(lineDeletionRange("", ""))
    }
}
