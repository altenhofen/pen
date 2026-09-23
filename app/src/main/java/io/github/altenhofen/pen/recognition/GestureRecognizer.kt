package io.github.altenhofen.pen.recognition

internal class GestureRecognizer(private val metric: DistanceMetric = DistanceMetric.DTW) {
    @Volatile
    private var clusters: List<GestureCluster> = emptyList()

    fun replaceAll(clusters: List<GestureCluster>) {
        this.clusters = clusters.toList()
    }

    fun rank(sample: FeatureVector, threshold: Float): GestureRecognitionResult? {
        if (clusters.isEmpty()) return null
        val counts = clusters.groupingBy { it.action }.eachCount()
        val eligible = clusters.filter { counts.getValue(it.action) >= GestureAction.MIN_TRAINING_SAMPLES }
        if (eligible.isEmpty()) return null
        val ranked = eligible
            .map { RankedGestureMatch(it.action, it.id, distance(sample, it.vector), counts.getValue(it.action)) }
            .groupBy { it.action }
            .values
            .map { perAction ->
                val nearest = perAction.sortedBy { it.distance }.take(GlyphRecognizer.NEAREST_PER_LABEL)
                nearest.first().copy(distance = nearest.map { it.distance }.average().toFloat())
            }
            .sortedBy { it.distance }
        val winner = ranked.first()
        val gap = ranked.getOrNull(1)?.let { it.distance - winner.distance } ?: Float.POSITIVE_INFINITY
        return GestureRecognitionResult(winner, ranked, Ambiguity(gap, threshold, gap < threshold), sample)
    }

    private fun distance(left: FeatureVector, right: FeatureVector): Float = when (metric) {
        DistanceMetric.EUCLIDEAN -> meanEuclidean(left, right)
        DistanceMetric.DTW -> bandedDtw(left, right)
    }
}

internal object GestureMatchPolicy {
    fun shouldFire(
        gesture: GestureRecognitionResult?,
        glyph: RecognitionResult?,
        threshold: Float,
    ): Boolean {
        if (gesture == null) return false
        if (gesture.ambiguity.isAmbiguous) return false
        val glyphDistance = glyph?.winner?.distance ?: Float.POSITIVE_INFINITY
        return gesture.winner.distance < glyphDistance
    }
}
