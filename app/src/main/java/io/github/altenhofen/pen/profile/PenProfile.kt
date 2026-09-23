package io.github.altenhofen.pen.profile

import io.github.altenhofen.pen.recognition.PrototypeCluster
import io.github.altenhofen.pen.recognition.WordMemory
import io.github.altenhofen.pen.recognition.WordSample
import io.github.altenhofen.pen.settings.MotorSettings

internal class PenProfile private constructor(
    val settings: MotorSettings,
    val prototypes: List<PrototypeCluster>,
    val words: List<WordSample>,
) {
    companion object {
        const val MAX_PROTOTYPES = 4096

        fun create(settings: MotorSettings, prototypes: List<PrototypeCluster>, words: List<WordSample> = emptyList()): PenProfile {
            require(prototypes.isNotEmpty()) { "prototypes must be nonempty" }
            require(prototypes.size <= MAX_PROTOTYPES) { "prototypes exceed $MAX_PROTOTYPES" }
            val ids = prototypes.map { it.id.value }
            require(ids.all { it.isNotBlank() }) { "cluster id must be non-blank" }
            require(ids.size == ids.toSet().size) { "cluster ids must be unique" }
            require(words.size <= WordMemory.TOTAL_CAP) { "word samples exceed ${WordMemory.TOTAL_CAP}" }
            require(words.map { it.id }.toSet().size == words.size) { "word sample ids must be unique" }
            return PenProfile(settings, prototypes.sortedBy { it.id.value }, words.sortedBy { it.confirmedAt })
        }
    }
}

internal class ProfileTransferException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)
