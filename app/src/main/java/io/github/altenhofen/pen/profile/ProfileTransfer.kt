package io.github.altenhofen.pen.profile

import android.content.ContentResolver
import android.net.Uri
import io.github.altenhofen.pen.recognition.CustomWordStore
import io.github.altenhofen.pen.recognition.PrototypeStore
import io.github.altenhofen.pen.recognition.WordMemoryStore
import io.github.altenhofen.pen.settings.MotorSettingsStore
import java.io.IOException

internal class ProfileTransfer(
    private val settings: MotorSettingsStore,
    private val prototypes: PrototypeStore,
    private val words: WordMemoryStore,
    private val customWords: CustomWordStore,
    private val resolver: ContentResolver,
) {
    suspend fun exportTo(uri: Uri, passphrase: CharArray) {
        val profile = PenProfile.create(
            settings.readBlocking(),
            prototypes.loadOrSeed(),
            words.load().samples,
            customWords.load(),
        )
        val stream = resolver.openOutputStream(uri) ?: throw ProfileTransferException("cannot open export stream")
        stream.use { ProfileVault.encrypt(it, profile, passphrase) }
    }

    fun needsPassphrase(uri: Uri): Boolean {
        val prefix = ByteArray(ProfileVault.MAGIC_BYTES)
        try {
            val stream = resolver.openInputStream(uri) ?: throw ProfileTransferException("cannot open import stream")
            stream.use { input ->
                var filled = 0
                while (filled < prefix.size) {
                    val read = input.read(prefix, filled, prefix.size - filled)
                    if (read < 0) break
                    filled += read
                }
            }
        } catch (error: IOException) {
            throw ProfileTransferException("cannot read the profile file", error)
        }
        return ProfileVault.isVaultArchive(prefix)
    }

    suspend fun importFrom(uri: Uri, passphrase: CharArray?): PenProfile {
        val archive = read(uri)
        val profile = if (ProfileVault.isVaultArchive(archive)) {
            ProfileVault.decrypt(archive, passphrase ?: throw ProfileTransferException("passphrase required"))
        } else {
            LegacyProfileArchive.decode(archive)
        }
        prototypes.replaceAll(profile.prototypes)
        words.replaceAll(profile.words)
        customWords.replaceAll(profile.customWords)
        settings.save(profile.settings)
        return profile
    }

    private fun read(uri: Uri): ByteArray = try {
        val stream = resolver.openInputStream(uri) ?: throw ProfileTransferException("cannot open import stream")
        stream.use { it.readBytes() }
    } catch (error: IOException) {
        throw ProfileTransferException("cannot read the profile file", error)
    }
}
