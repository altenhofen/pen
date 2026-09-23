package io.github.altenhofen.pen.profile

import io.github.altenhofen.pen.recognition.GestureCluster
import io.github.altenhofen.pen.recognition.PrototypeCluster
import io.github.altenhofen.pen.recognition.WordMemory
import io.github.altenhofen.pen.recognition.WordSample
import io.github.altenhofen.pen.settings.MotorSettings

internal class PenProfile private constructor(
    val settings: MotorSettings,
    val prototypes: List<PrototypeCluster>,
    val words: List<WordSample>,
    val gestures: List<GestureCluster>,
) {
    companion object {
        const val MAX_PROTOTYPES = 4096
        const val MAX_GESTURE_CLUSTERS = 2048

        fun create(
            settings: MotorSettings,
            prototypes: List<PrototypeCluster>,
            words: List<WordSample> = emptyList(),
            gestures: List<GestureCluster> = emptyList(),
        ): PenProfile {
            require(prototypes.isNotEmpty()) { "prototypes must be nonempty" }
            require(prototypes.size <= MAX_PROTOTYPES) { "prototypes exceed $MAX_PROTOTYPES" }
            val ids = prototypes.map { it.id.value }
            require(ids.all { it.isNotBlank() }) { "cluster id must be non-blank" }
            require(ids.size == ids.toSet().size) { "cluster ids must be unique" }
            require(words.size <= WordMemory.TOTAL_CAP) { "word samples exceed ${WordMemory.TOTAL_CAP}" }
            require(words.map { it.id }.toSet().size == words.size) { "word sample ids must be unique" }
            require(gestures.size <= MAX_GESTURE_CLUSTERS) { "gesture samples exceed $MAX_GESTURE_CLUSTERS" }
            require(gestures.map { it.id.value }.toSet().size == gestures.size) { "gesture cluster ids must be unique" }
            return PenProfile(
                settings,
                prototypes.sortedBy { it.id.value },
                words.sortedBy { it.confirmedAt },
                gestures.sortedBy { it.id.value },
            )
        }
    }
}

/** Why a transfer stopped, so the launcher can say "wrong passphrase" instead of "import failed". */
internal enum class ProfileFailure { Passphrase, Malformed }

internal class ProfileTransferException(
    message: String,
    cause: Throwable? = null,
    val failure: ProfileFailure = ProfileFailure.Malformed,
) : IllegalArgumentException(message, cause)
