package io.github.altenhofen.pen.profile

import android.content.ContentResolver
import android.net.Uri
import io.github.altenhofen.pen.recognition.PrototypeStore
import io.github.altenhofen.pen.recognition.WordMemoryStore
import io.github.altenhofen.pen.settings.MotorSettingsStore

internal class ProfileTransfer(
    private val settings: MotorSettingsStore,
    private val prototypes: PrototypeStore,
    private val words: WordMemoryStore,
    private val resolver: ContentResolver,
) {
    suspend fun exportTo(uri: Uri) {
        val profile = PenProfile.create(settings.readBlocking(), prototypes.loadOrSeed(), words.load().samples)
        val stream = resolver.openOutputStream(uri)
            ?: throw ProfileTransferException("cannot open export stream")
        stream.use { ProfileArchiveCodec.encode(it, profile) }
    }

    suspend fun importFrom(uri: Uri): PenProfile {
        val stream = resolver.openInputStream(uri)
            ?: throw ProfileTransferException("cannot open import stream")
        val profile = stream.use { ProfileArchiveCodec.decode(it) }
        prototypes.replaceAll(profile.prototypes)
        words.replaceAll(profile.words)
        settings.save(profile.settings)
        return profile
    }
}
