package io.github.altenhofen.pen.recognition

enum class DistanceMetric {
    EUCLIDEAN,
    DTW,
}

internal class GlyphRecognizer(private val metric: DistanceMetric = DistanceMetric.DTW) {
    @Volatile
    private var clusters: List<PrototypeCluster> = emptyList()

    fun replaceAll(clusters: List<PrototypeCluster>) {
        require(clusters.isNotEmpty())
        this.clusters = clusters.toList()
    }

    fun replace(cluster: PrototypeCluster) {
        clusters = clusters.map { if (it.id == cluster.id) cluster else it }
    }

    fun rank(sample: FeatureVector, threshold: Float): RecognitionResult {
        check(clusters.isNotEmpty()) { "rank before replaceAll" }
        val ranked = clusters
            .map { RankedMatch(it.label, it.id, distance(sample, it.vector)) }
            .groupBy { it.character }
            .values
            .map { perLabel -> perLabel.minBy { it.distance } }
            .sortedBy { it.distance }
        val winner = ranked.first()
        val gap = ranked.getOrNull(1)?.let { it.distance - winner.distance } ?: Float.POSITIVE_INFINITY
        return RecognitionResult(winner, ranked, Ambiguity(gap, threshold, gap < threshold), sample)
    }

    private fun distance(left: FeatureVector, right: FeatureVector): Float = when (metric) {
        DistanceMetric.EUCLIDEAN -> meanEuclidean(left, right)
        DistanceMetric.DTW -> bandedDtw(left, right)
    }
}
