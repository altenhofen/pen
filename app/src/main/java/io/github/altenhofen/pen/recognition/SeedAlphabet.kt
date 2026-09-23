package io.github.altenhofen.pen.recognition

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal fun seedPolylines(character: Char): List<List<Point2>> = when (character) {
    '0' -> listOf(oval(4f, 8f))
    '1' -> listOf(line(5f, 1f, 5f, 9f))
    '2' -> listOf(chain(1f, 3f, 3f, 1f, 7f, 1f, 8f, 3f, 2f, 8f, 8f, 9f))
    '3' -> listOf(chain(2f, 2f, 8f, 2f, 4f, 5f, 8f, 8f, 2f, 8f))
    '4' -> listOf(line(6f, 1f, 1f, 6f), line(1f, 6f, 8f, 6f), line(6f, 1f, 6f, 9f))
    '5' -> listOf(chain(8f, 1f, 2f, 1f, 2f, 4f, 7f, 5f, 8f, 8f, 2f, 9f))
    '6' -> listOf(chain(7f, 2f, 3f, 1f, 2f, 5f, 2f, 8f, 6f, 8f, 7f, 6f, 3f, 5f))
    '7' -> listOf(chain(1f, 1f, 8f, 1f, 3f, 9f))
    '8' -> listOf(oval(3.2f, 2.4f, 5f, 3f), oval(3.6f, 2.8f, 5f, 7.2f))
    '9' -> listOf(chain(6f, 8f, 7f, 5f, 6f, 2f, 3f, 2f, 2f, 4f, 6f, 5f))
    'a' -> listOf(oval(3f, 3f, 5f, 6.5f), line(8f, 4f, 8f, 9f))
    'b' -> listOf(line(2f, 1f, 2f, 9f), oval(3f, 2.4f, 5.2f, 7f))
    'c' -> listOf(chain(8f, 3f, 5f, 1.5f, 2f, 4f, 2f, 7f, 5f, 9f, 8f, 7.5f))
    'd' -> listOf(line(8f, 1f, 8f, 9f), oval(3f, 2.4f, 4.8f, 7f))
    'e' -> listOf(chain(2f, 6f, 8f, 6f, 7f, 3f, 3f, 2f, 2f, 6f, 3f, 9f, 7f, 8.5f))
    'f' -> listOf(chain(7f, 1f, 4f, 1f, 3f, 3f, 3f, 9f), line(1f, 4f, 6f, 4f))
    'g' -> listOf(oval(3f, 2.6f, 5f, 4.2f), chain(8f, 4f, 8f, 9f, 4f, 10f, 2f, 8f))
    'h' -> listOf(line(2f, 1f, 2f, 9f), chain(2f, 5f, 4f, 4f, 7f, 5f, 7f, 9f))
    'i' -> listOf(line(5f, 4f, 5f, 9f), listOf(Point2(5f, 1.5f)))
    'j' -> listOf(chain(6f, 4f, 6f, 8f, 4f, 10f, 2f, 8f), listOf(Point2(6f, 1.5f)))
    'k' -> listOf(line(2f, 1f, 2f, 9f), line(7f, 4f, 2f, 6.5f), line(2f, 6.5f, 7f, 9f))
    'l' -> listOf(chain(4f, 1f, 4f, 8f, 6f, 9f))
    'm' -> listOf(line(1f, 9f, 1f, 4f), chain(1f, 5f, 3f, 4f, 4.5f, 6f, 4.5f, 9f), chain(4.5f, 6f, 6.5f, 4f, 8.5f, 6f, 8.5f, 9f))
    'n' -> listOf(line(2f, 9f, 2f, 4f), chain(2f, 5f, 5f, 4f, 8f, 6f, 8f, 9f))
    'o' -> listOf(oval(3.5f, 3.2f, 5f, 6.5f))
    'p' -> listOf(line(2f, 4f, 2f, 11f), oval(3f, 2.4f, 5.2f, 6f))
    'q' -> listOf(line(8f, 4f, 8f, 11f), oval(3f, 2.4f, 4.8f, 6f))
    'r' -> listOf(line(2f, 9f, 2f, 4f), chain(2f, 5.5f, 4f, 4f, 7f, 4.5f))
    's' -> listOf(chain(7.5f, 3f, 4f, 2f, 2.5f, 4f, 7f, 6.5f, 6f, 8.5f, 3f, 9f))
    't' -> listOf(line(4f, 2f, 4f, 8.5f), line(1.5f, 4f, 7f, 4f), line(4f, 8.5f, 6f, 9.5f))
    'u' -> listOf(chain(2f, 4f, 2f, 8f, 5f, 9f, 8f, 8f, 8f, 4f))
    'v' -> listOf(chain(1f, 4f, 5f, 9f, 9f, 4f))
    'w' -> listOf(chain(1f, 4f, 3f, 9f, 5f, 6f, 7f, 9f, 9f, 4f))
    'x' -> listOf(line(2f, 4f, 8f, 9f), line(8f, 4f, 2f, 9f))
    'y' -> listOf(chain(2f, 4f, 5f, 7f, 8f, 4f), chain(5f, 7f, 4f, 11f))
    'z' -> listOf(chain(2f, 4f, 8f, 4f, 2f, 9f, 8f, 9f))
    else -> error("no seed for $character")
}

internal val seedLabels: List<Char> = ('0'..'9').toList() + ('a'..'z').toList()

private fun line(x0: Float, y0: Float, x1: Float, y1: Float) = listOf(Point2(x0, y0), Point2(x1, y1))

private fun chain(vararg xy: Float): List<Point2> {
    require(xy.size % 2 == 0)
    return List(xy.size / 2) { index -> Point2(xy[index * 2], xy[index * 2 + 1]) }
}

private fun oval(radiusX: Float, radiusY: Float, centerX: Float = 5f, centerY: Float = 5f, steps: Int = 16): List<Point2> {
    val points = ArrayList<Point2>(steps + 1)
    for (step in 0..steps) {
        val angle = (PI * 2.0 * step / steps).toFloat()
        points.add(Point2(centerX + radiusX * cos(angle), centerY + radiusY * sin(angle)))
    }
    return points
}
