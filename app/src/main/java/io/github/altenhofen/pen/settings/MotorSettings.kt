package io.github.altenhofen.pen.settings

class MotorSettings private constructor(
    val settleMillis: Long,
    val strokeWidthDp: Float,
    val ambiguityThreshold: Float,
) {
    fun withSettleMillis(value: Long): MotorSettings = of(value, strokeWidthDp, ambiguityThreshold)
    fun withStrokeWidthDp(value: Float): MotorSettings = of(settleMillis, value, ambiguityThreshold)
    fun withAmbiguityThreshold(value: Float): MotorSettings = of(settleMillis, strokeWidthDp, value)
    fun capture(): CaptureStyle = CaptureStyle(settleMillis, strokeWidthDp)

    override fun equals(other: Any?): Boolean =
        other is MotorSettings &&
            settleMillis == other.settleMillis &&
            strokeWidthDp == other.strokeWidthDp &&
            ambiguityThreshold == other.ambiguityThreshold

    override fun hashCode(): Int =
        (settleMillis.hashCode() * 31 + strokeWidthDp.hashCode()) * 31 + ambiguityThreshold.hashCode()

    companion object {
        const val MIN_SETTLE_MILLIS = 300L
        const val MAX_SETTLE_MILLIS = 1_200L
        const val MIN_STROKE_WIDTH_DP = 2f
        const val MAX_STROKE_WIDTH_DP = 16f
        const val MIN_AMBIGUITY = 0.01f
        const val MAX_AMBIGUITY = 1f

        val Default: MotorSettings = MotorSettings(600L, 6f, 0.15f)

        internal fun parse(settle: Long?, width: Float?, delta: Float?): MotorSettings = of(
            settle ?: Default.settleMillis,
            width ?: Default.strokeWidthDp,
            delta ?: Default.ambiguityThreshold,
        )

        private fun of(settle: Long, width: Float, delta: Float) = MotorSettings(
            settle.coerceIn(MIN_SETTLE_MILLIS, MAX_SETTLE_MILLIS),
            (width.takeIf { it.isFinite() } ?: Default.strokeWidthDp)
                .coerceIn(MIN_STROKE_WIDTH_DP, MAX_STROKE_WIDTH_DP),
            (delta.takeIf { it.isFinite() } ?: Default.ambiguityThreshold)
                .coerceIn(MIN_AMBIGUITY, MAX_AMBIGUITY),
        )
    }
}

data class CaptureStyle(val settleMillis: Long, val strokeWidthDp: Float)
