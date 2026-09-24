package io.github.altenhofen.pen.ime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasTapTest {
    @Test
    fun tinyStrokeIsTap() {
        val stroke = Stroke().apply {
            append(100f, 100f)
            append(101f, 100f)
        }
        assertTrue(strokeIsTap(stroke, 24f))
    }

    @Test
    fun longStrokeIsNotTap() {
        val stroke = Stroke().apply {
            append(10f, 10f)
            append(200f, 10f)
        }
        assertFalse(strokeIsTap(stroke, 24f))
    }

    @Test
    fun doubleTapFiresOnceWithinWindow() {
        var fired = 0
        val detector = DoubleTapDetector(24f, 300, { fired++ })
        assertTrue(!detector.onTap(50f, 50f, 1_000L))
        assertTrue(detector.onTap(52f, 51f, 1_100L))
        assertTrue(fired == 1)
        assertTrue(!detector.onTap(52f, 51f, 1_150L))
        assertTrue(fired == 1)
    }

    @Test
    fun jitteryFingerStrokeCanStillBeTap() {
        val stroke = Stroke()
        var x = 100f
        var y = 100f
        repeat(20) {
            stroke.append(x, y)
            x += if (it % 2 == 0) 1f else -1f
            y += 0.5f
        }
        assertTrue(strokeIsTap(stroke, 24f))
    }

    @Test
    fun slowSecondTapDoesNotFire() {
        var fired = 0
        val detector = DoubleTapDetector(24f, 300, { fired++ })
        assertTrue(!detector.onTap(50f, 50f, 1_000L))
        assertTrue(!detector.onTap(52f, 51f, 1_500L))
        assertTrue(fired == 0)
    }
}
