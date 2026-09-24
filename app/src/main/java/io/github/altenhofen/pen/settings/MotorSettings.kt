package io.github.altenhofen.pen.settings

class MotorSettings private constructor(
    val settleMillis: Long,
    val strokeWidthDp: Float,
    val allowFingerInput: Boolean,
    val spaceAfterFullWord: Boolean,
    val recognizeSpacesInHandwriting: Boolean,
    val doubleTapForSpace: Boolean,
    val handwriting: HandwritingLanguage,
) {
    fun withSettleMillis(value: Long): MotorSettings =
        of(value, strokeWidthDp, allowFingerInput, spaceAfterFullWord, recognizeSpacesInHandwriting, doubleTapForSpace, handwriting)

    fun withStrokeWidthDp(value: Float): MotorSettings =
        of(settleMillis, value, allowFingerInput, spaceAfterFullWord, recognizeSpacesInHandwriting, doubleTapForSpace, handwriting)

    fun withAllowFingerInput(value: Boolean): MotorSettings =
        of(settleMillis, strokeWidthDp, value, spaceAfterFullWord, recognizeSpacesInHandwriting, doubleTapForSpace, handwriting)

    fun withSpaceAfterFullWord(value: Boolean): MotorSettings =
        of(settleMillis, strokeWidthDp, allowFingerInput, value, recognizeSpacesInHandwriting, doubleTapForSpace, handwriting)

    fun withRecognizeSpacesInHandwriting(value: Boolean): MotorSettings =
        of(settleMillis, strokeWidthDp, allowFingerInput, spaceAfterFullWord, value, doubleTapForSpace, handwriting)

    fun withDoubleTapForSpace(value: Boolean): MotorSettings =
        of(settleMillis, strokeWidthDp, allowFingerInput, spaceAfterFullWord, recognizeSpacesInHandwriting, value, handwriting)

    fun withHandwriting(value: HandwritingLanguage): MotorSettings =
        of(settleMillis, strokeWidthDp, allowFingerInput, spaceAfterFullWord, recognizeSpacesInHandwriting, doubleTapForSpace, value)

    fun capture(): CaptureStyle = CaptureStyle(settleMillis, strokeWidthDp)

    override fun equals(other: Any?): Boolean =
        other is MotorSettings &&
            settleMillis == other.settleMillis &&
            strokeWidthDp == other.strokeWidthDp &&
            allowFingerInput == other.allowFingerInput &&
            spaceAfterFullWord == other.spaceAfterFullWord &&
            recognizeSpacesInHandwriting == other.recognizeSpacesInHandwriting &&
            doubleTapForSpace == other.doubleTapForSpace &&
            handwriting == other.handwriting

    override fun hashCode(): Int =
        ((((((settleMillis.hashCode() * 31 + strokeWidthDp.hashCode()) * 31 + allowFingerInput.hashCode()) * 31 +
            spaceAfterFullWord.hashCode()) * 31 + recognizeSpacesInHandwriting.hashCode()) * 31 + doubleTapForSpace.hashCode()) * 31 +
            handwriting.hashCode())

    companion object {
        const val MIN_SETTLE_MILLIS = 300L
        const val MAX_SETTLE_MILLIS = 1_200L
        const val MIN_STROKE_WIDTH_DP = 2f
        const val MAX_STROKE_WIDTH_DP = 16f
        const val FIXED_AMBIGUITY_THRESHOLD = 0.15f

        val Default: MotorSettings = MotorSettings(600L, 6f, false, false, false, true, HandwritingLanguage.FollowApp)

        internal fun parse(
            settle: Long?,
            width: Float?,
            allowFinger: Boolean? = null,
            spaceAfterFullWord: Boolean? = null,
            recognizeSpaces: Boolean? = null,
            doubleTapForSpace: Boolean? = null,
            handwriting: String? = null,
        ): MotorSettings = of(
            settle ?: Default.settleMillis,
            width ?: Default.strokeWidthDp,
            allowFinger ?: Default.allowFingerInput,
            spaceAfterFullWord ?: Default.spaceAfterFullWord,
            recognizeSpaces ?: Default.recognizeSpacesInHandwriting,
            doubleTapForSpace ?: Default.doubleTapForSpace,
            HandwritingLanguage.parse(handwriting),
        )

        /** Legacy archives still carry a threshold float; it is ignored. */
        internal fun parseLegacy(
            settle: Long?,
            width: Float?,
            @Suppress("UNUSED_PARAMETER") legacyAmbiguity: Float?,
            allowFinger: Boolean? = null,
            handwriting: String? = null,
        ): MotorSettings = parse(settle, width, allowFinger, null, null, null, handwriting)

        private fun of(
            settle: Long,
            width: Float,
            allowFinger: Boolean,
            spaceAfterFullWord: Boolean,
            recognizeSpaces: Boolean,
            doubleTapForSpace: Boolean,
            handwriting: HandwritingLanguage,
        ) = MotorSettings(
            settle.coerceIn(MIN_SETTLE_MILLIS, MAX_SETTLE_MILLIS),
            (width.takeIf { it.isFinite() } ?: Default.strokeWidthDp)
                .coerceIn(MIN_STROKE_WIDTH_DP, MAX_STROKE_WIDTH_DP),
            allowFinger,
            spaceAfterFullWord,
            recognizeSpaces,
            doubleTapForSpace,
            handwriting,
        )
    }
}

data class CaptureStyle(val settleMillis: Long, val strokeWidthDp: Float)
