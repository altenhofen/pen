package io.github.altenhofen.pen.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.IOException

interface MotorSettingsStore {
    val values: Flow<MotorSettings>
    fun readBlocking(): MotorSettings
    suspend fun save(settings: MotorSettings)
    suspend fun update(transform: (MotorSettings) -> MotorSettings)

    companion object {
        fun open(context: Context): MotorSettingsStore =
            PreferencesMotorSettingsStore(context.applicationContext.motorDataStore)
    }
}

private val Context.motorDataStore: DataStore<Preferences> by preferencesDataStore(name = "motor_settings")

private val SETTLE_MILLIS = longPreferencesKey("settle_millis")
private val STROKE_WIDTH_DP = floatPreferencesKey("stroke_width_dp")
private val AMBIGUITY_THRESHOLD = floatPreferencesKey("ambiguity_threshold")

private class PreferencesMotorSettingsStore(private val store: DataStore<Preferences>) : MotorSettingsStore {
    override val values: Flow<MotorSettings> = store.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs ->
            MotorSettings.parse(prefs[SETTLE_MILLIS], prefs[STROKE_WIDTH_DP], prefs[AMBIGUITY_THRESHOLD])
        }

    override fun readBlocking(): MotorSettings = runBlocking { values.first() }

    override suspend fun save(settings: MotorSettings) {
        write(settings)
    }

    override suspend fun update(transform: (MotorSettings) -> MotorSettings) {
        store.edit { prefs ->
            val next = transform(
                MotorSettings.parse(prefs[SETTLE_MILLIS], prefs[STROKE_WIDTH_DP], prefs[AMBIGUITY_THRESHOLD]),
            )
            writeInto(prefs, next)
        }
    }

    private suspend fun write(settings: MotorSettings) {
        store.edit { prefs -> writeInto(prefs, settings) }
    }

    private fun writeInto(prefs: MutablePreferences, settings: MotorSettings) {
        prefs[SETTLE_MILLIS] = settings.settleMillis
        prefs[STROKE_WIDTH_DP] = settings.strokeWidthDp
        prefs[AMBIGUITY_THRESHOLD] = settings.ambiguityThreshold
    }
}
