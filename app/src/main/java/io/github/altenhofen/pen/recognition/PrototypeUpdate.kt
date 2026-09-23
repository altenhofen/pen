package io.github.altenhofen.pen.recognition

object PrototypeUpdate {
    const val ALPHA: Float = 0.05f
    const val BETA: Float = 0.02f
    const val ACCEPT_REWARD: Float = 1f
    const val REJECT_REWARD: Float = -1f

    fun attract(prototype: FloatArray, sample: FloatArray, reward: Float): FloatArray {
        require(prototype.size == sample.size)
        val scale = ALPHA * reward
        return FloatArray(prototype.size) { index ->
            prototype[index] + scale * (sample[index] - prototype[index])
        }
    }

    fun repel(prototype: FloatArray, sample: FloatArray): FloatArray {
        require(prototype.size == sample.size)
        return FloatArray(prototype.size) { index ->
            prototype[index] - BETA * (sample[index] - prototype[index])
        }
    }
}
