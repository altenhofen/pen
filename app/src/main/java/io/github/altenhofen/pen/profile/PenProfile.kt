package io.github.altenhofen.pen.profile

import io.github.altenhofen.pen.recognition.PrototypeCluster
import io.github.altenhofen.pen.settings.MotorSettings

internal class PenProfile private constructor(
    val settings: MotorSettings,
    val prototypes: List<PrototypeCluster>,
) {
    companion object {
        const val MAX_PROTOTYPES = 4096

        fun create(settings: MotorSettings, prototypes: List<PrototypeCluster>): PenProfile {
            require(prototypes.isNotEmpty()) { "prototypes must be nonempty" }
            require(prototypes.size <= MAX_PROTOTYPES) { "prototypes exceed $MAX_PROTOTYPES" }
            val ids = prototypes.map { it.id.value }
            require(ids.all { it.isNotBlank() }) { "cluster id must be non-blank" }
            require(ids.size == ids.toSet().size) { "cluster ids must be unique" }
            return PenProfile(settings, prototypes.sortedBy { it.id.value })
        }
    }
}

internal class ProfileTransferException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)
