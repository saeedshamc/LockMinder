package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lockminder_settings")

class SettingsManager(private val context: Context) {

    companion object {
        val THEME_KEY = stringPreferencesKey("theme_mode") // "SYSTEM", "LIGHT", "DARK"
        val COOLING_OFF_KEY = longPreferencesKey("cooling_off_duration") // duration in ms
        val STRICT_MODE_KEY = booleanPreferencesKey("strict_monotonic_mode")
    }

    val themeFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[THEME_KEY] ?: "SYSTEM"
        }

    val coolingOffFlow: Flow<Long> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[COOLING_OFF_KEY] ?: (60 * 1000L * 5) // default to 5 minutes for easy testing, or 24 hours
        }

    val strictModeFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[STRICT_MODE_KEY] ?: false
        }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }

    suspend fun setCoolingOffDuration(durationMs: Long) {
        context.dataStore.edit { preferences ->
            preferences[COOLING_OFF_KEY] = durationMs
        }
    }

    suspend fun setStrictMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[STRICT_MODE_KEY] = enabled
        }
    }
}
