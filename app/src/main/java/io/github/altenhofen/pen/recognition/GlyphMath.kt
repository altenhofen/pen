package io.github.altenhofen.pen.recognition

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

internal const val SAMPLE_COUNT = 32
internal const val FEATURE_WIDTH = 3

internal data class Point2(val x: Float, val y: Float)

internal class FeatureVector private constructor(private val values: FloatArray) {
    operator fun get(pointIndex: Int, componentIndex: Int): Float =
        values[pointIndex * FEATURE_WIDTH + componentIndex]

    fun copyValues(): FloatArray = values.copyOf()

    companion object {
        fun from(values: FloatArray): FeatureVector {
            require(values.size == SAMPLE_COUNT * FEATURE_WIDTH)
            require(values.all { it.isFinite() })
            return FeatureVector(values.copyOf())
        }
    }
}

internal fun preprocessPolylines(strokes: List<List<Point2>>): FeatureVector? {
    val cleaned = strokes.map { dropAdjacentDuplicates(it) }.filter { it.isNotEmpty() }
    if (cleaned.isEmpty()) return null
    if (cleaned.any { stroke -> stroke.any { !it.x.isFinite() || !it.y.isFinite() } }) return null
    val normalized = normalizeAspect(cleaned)
    val counts = allocateSampleCounts(normalized) ?: return null
    val sampled = normalized.mapIndexed { index, stroke ->
        resampleEquidistant(stroke, counts[index])
    }
    return vectorize(sampled)
}

private fun dropAdjacentDuplicates(points: List<Point2>): List<Point2> {
    if (points.isEmpty()) return points
    val out = ArrayList<Point2>(points.size)
    out.add(points[0])
    for (i in 1 until points.size) {
        val previous = out.last()
        val point = points[i]
        if (point.x != previous.x || point.y != previous.y) out.add(point)
    }
    return out
}

private fun normalizeAspect(strokes: List<List<Point2>>): List<List<Point2>> {
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    for (stroke in strokes) {
        for (point in stroke) {
            minX = min(minX, point.x)
            minY = min(minY, point.y)
            maxX = max(maxX, point.x)
            maxY = max(maxY, point.y)
        }
    }
    val width = maxX - minX
    val height = maxY - minY
    val longest = max(width, height)
    val scale = if (longest == 0f) 1f else 1f / longest
    val offsetX = (1f - width * scale) / 2f
    val offsetY = (1f - height * scale) / 2f
    return strokes.map { stroke ->
        stroke.map { point ->
            Point2((point.x - minX) * scale + offsetX, (point.y - minY) * scale + offsetY)
        }
    }
}

private fun allocateSampleCounts(strokes: List<List<Point2>>): IntArray? {
    val lengths = FloatArray(strokes.size) { arcLength(strokes[it]) }
    val counts = IntArray(strokes.size) { index -> if (lengths[index] == 0f) 1 else 2 }
    val minimum = counts.sum()
    if (minimum > SAMPLE_COUNT) return null
    var remaining = SAMPLE_COUNT - minimum
    val ink = lengths.sum()
    if (ink == 0f) return null
    if (remaining == 0) return counts
    val raw = FloatArray(strokes.size) { index ->
        if (lengths[index] == 0f) 0f else remaining * lengths[index] / ink
    }
    val extras = IntArray(strokes.size) { raw[it].toInt() }
    var leftover = remaining - extras.sum()
    val byFraction = strokes.indices.sortedByDescending { raw[it] - extras[it] }
    var cursor = 0
    while (leftover > 0) {
        extras[byFraction[cursor % byFraction.size]]++
        leftover--
        cursor++
    }
    for (index in strokes.indices) counts[index] += extras[index]
    return counts
}

internal fun arcLength(points: List<Point2>): Float {
    var length = 0f
    for (index in 1 until points.size) {
        length += hypot(points[index].x - points[index - 1].x, points[index].y - points[index - 1].y)
    }
    return length
}

internal fun resampleEquidistant(points: List<Point2>, count: Int): List<Point2> {
    require(count >= 1)
    if (points.isEmpty()) return emptyList()
    if (count == 1 || arcLength(points) == 0f) return List(count) { points[0] }
    val cumulative = FloatArray(points.size)
    for (index in 1 until points.size) {
        cumulative[index] = cumulative[index - 1] + hypot(
            points[index].x - points[index - 1].x,
            points[index].y - points[index - 1].y,
        )
    }
    val total = cumulative.last()
    return List(count) { step ->
        val target = total * step / (count - 1).toFloat()
        var index = 1
        while (index < points.lastIndex && cumulative[index] < target) index++
        val span = cumulative[index] - cumulative[index - 1]
        val t = if (span == 0f) 0f else (target - cumulative[index - 1]) / span
        Point2(
            points[index - 1].x + (points[index].x - points[index - 1].x) * t,
            points[index - 1].y + (points[index].y - points[index - 1].y) * t,
        )
    }
}

private fun vectorize(strokes: List<List<Point2>>): FeatureVector {
    val values = FloatArray(SAMPLE_COUNT * FEATURE_WIDTH)
    var slot = 0
    for (stroke in strokes) {
        for ((index, point) in stroke.withIndex()) {
            val previous = if (index == 0) null else stroke[index - 1]
            values[slot * FEATURE_WIDTH] = if (previous == null) 0f else point.x - previous.x
            values[slot * FEATURE_WIDTH + 1] = if (previous == null) 0f else point.y - previous.y
            values[slot * FEATURE_WIDTH + 2] = if (index == 0) 1f else 0f
            slot++
        }
    }
    check(slot == SAMPLE_COUNT)
    return FeatureVector.from(values)
}

internal fun meanEuclidean(left: FeatureVector, right: FeatureVector): Float {
    var sum = 0f
    for (step in 0 until SAMPLE_COUNT) {
        var local = 0f
        for (component in 0 until FEATURE_WIDTH) {
            val delta = left[step, component] - right[step, component]
            local += delta * delta
        }
        sum += kotlin.math.sqrt(local)
    }
    return sum / SAMPLE_COUNT
}

internal fun bandedDtw(left: FeatureVector, right: FeatureVector, window: Int = 8): Float {
    val infinity = Float.POSITIVE_INFINITY
    var previous = FloatArray(SAMPLE_COUNT) { infinity }
    var current = FloatArray(SAMPLE_COUNT) { infinity }
    var previousLength = IntArray(SAMPLE_COUNT)
    var currentLength = IntArray(SAMPLE_COUNT)
    for (i in 0 until SAMPLE_COUNT) {
        val start = max(0, i - window)
        val end = min(SAMPLE_COUNT - 1, i + window)
        for (j in start..end) {
            val cost = localCost(left, i, right, j)
            var best = infinity
            var bestLength = 0
            if (i > 0 && j >= max(0, (i - 1) - window) && j <= min(SAMPLE_COUNT - 1, (i - 1) + window)) {
                val candidate = previous[j]
                if (candidate < best) {
                    best = candidate
                    bestLength = previousLength[j]
                }
            }
            if (j > 0 && (j - 1) >= start) {
                val candidate = current[j - 1]
                if (candidate < best) {
                    best = candidate
                    bestLength = currentLength[j - 1]
                }
            }
            if (i > 0 && j > 0) {
                val diagonalColumn = j - 1
                val diagonalRowStart = max(0, (i - 1) - window)
                val diagonalRowEnd = min(SAMPLE_COUNT - 1, (i - 1) + window)
                if (diagonalColumn in diagonalRowStart..diagonalRowEnd) {
                    val candidate = previous[diagonalColumn]
                    if (candidate < best) {
                        best = candidate
                        bestLength = previousLength[diagonalColumn]
                    }
                }
            }
            if (i == 0 && j == 0) {
                best = 0f
                bestLength = 0
            }
            if (best == infinity) continue
            current[j] = best + cost
            currentLength[j] = bestLength + 1
        }
        val swapCost = previous
        previous = current
        current = swapCost
        current.fill(infinity)
        val swapLength = previousLength
        previousLength = currentLength
        currentLength = swapLength
    }
    val total = previous[SAMPLE_COUNT - 1]
    val length = previousLength[SAMPLE_COUNT - 1]
    if (!total.isFinite() || length == 0) return infinity
    return total / length
}

private fun localCost(left: FeatureVector, leftIndex: Int, right: FeatureVector, rightIndex: Int): Float {
    var sum = 0f
    for (component in 0 until FEATURE_WIDTH) {
        val delta = left[leftIndex, component] - right[rightIndex, component]
        sum += delta * delta
    }
    return kotlin.math.sqrt(sum)
}
