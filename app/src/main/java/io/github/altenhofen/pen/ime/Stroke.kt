package io.github.altenhofen.pen.ime

data class InkSample(val x: Float, val y: Float)

/** One stylus contact, from down through the matching up. */
class Stroke {
    private val samples = ArrayList<InkSample>()

    fun append(x: Float, y: Float) {
        samples.add(InkSample(x, y))
    }

    fun points(): List<InkSample> = samples
}
