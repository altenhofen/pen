package io.github.altenhofen.pen.recognition

import io.github.altenhofen.pen.ime.Stroke

@JvmInline
value class ClusterId(val value: String) {
    companion object {
        fun seed(label: Char) = ClusterId("seed:$label")
        fun legacy(label: Char) = ClusterId("legacy:$label")
        fun training(label: Char, sessionId: String, index: Int) = ClusterId("train:$label:$sessionId:$index")
    }
}

internal data class PrototypeCluster(
    val id: ClusterId,
    val label: Char,
    val vector: FeatureVector,
)

enum class Feedback { Accepted, Rejected }

data class RankedMatch(
    val character: Char,
    val clusterId: ClusterId,
    val distance: Float,
)

data class Ambiguity(
    val gap: Float,
    val threshold: Float,
    val isAmbiguous: Boolean,
)

internal class RecognitionResult(
    val winner: RankedMatch,
    val ranked: List<RankedMatch>,
    val ambiguity: Ambiguity,
    val sample: FeatureVector,
)

internal fun featuresFromStrokes(strokes: List<Stroke>): FeatureVector? =
    preprocessPolylines(strokes.map { stroke -> stroke.points().map { Point2(it.x, it.y) } })

internal fun seedClusters(): List<PrototypeCluster> = seedLabels.map { label ->
    val vector = preprocessPolylines(seedPolylines(label)) ?: error("seed $label failed to preprocess")
    PrototypeCluster(ClusterId.seed(label), label, vector)
}
