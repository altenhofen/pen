package io.github.altenhofen.pen.recognition

import io.github.altenhofen.pen.ime.Stroke

enum class DistanceMetric {
    EUCLIDEAN,
    DTW,
}

data class RecognitionMatch(
    val character: Char,
    val distance: Float,
    val sample: FloatArray,
)

class GlyphRecognizer private constructor(
    private val metric: DistanceMetric,
    private val prototypes: MutableList<Pair<Char, FeatureVector>>,
) {
    fun prototypeValues(label: Char): FloatArray =
        prototypes.first { it.first == label }.second.copyValues()

    fun replacePrototype(label: Char, values: FloatArray) {
        val index = prototypes.indexOfFirst { it.first == label }
        require(index >= 0)
        prototypes[index] = label to FeatureVector.from(values)
    }
    fun recognize(strokes: List<Stroke>): RecognitionMatch? {
        val polylines = strokes.map { stroke -> stroke.points().map { Point2(it.x, it.y) } }
        val features = preprocessPolylines(polylines) ?: return null
        return best(features)
    }

    private fun best(features: FeatureVector): RecognitionMatch {
        var winner = prototypes.first()
        var bestDistance = distance(features, winner.second)
        for (index in 1 until prototypes.size) {
            val candidate = prototypes[index]
            val candidateDistance = distance(features, candidate.second)
            if (candidateDistance < bestDistance) {
                winner = candidate
                bestDistance = candidateDistance
            }
        }
        return RecognitionMatch(winner.first, bestDistance, features.copyValues())
    }

    private fun distance(left: FeatureVector, right: FeatureVector): Float = when (metric) {
        DistanceMetric.EUCLIDEAN -> meanEuclidean(left, right)
        DistanceMetric.DTW -> bandedDtw(left, right)
    }

    companion object {
        fun seeded(metric: DistanceMetric = DistanceMetric.DTW): GlyphRecognizer {
            val prototypes = seedLabels.map { label ->
                val features = preprocessPolylines(seedPolylines(label))
                    ?: error("seed $label failed to preprocess")
                label to features
            }
            return GlyphRecognizer(metric, prototypes.toMutableList())
        }
    }
}
