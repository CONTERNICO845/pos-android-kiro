package com.example.puntodeventa.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.puntodeventa.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Top-level DataStore delegate for theme preferences (Req 11.4).
 * Creates a single DataStore instance per process scoped to the Context.
 */
val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_prefs")

/**
 * Repository for persisting and exposing the user's selected AppTheme using Preferences DataStore.
 *
 * - Emits the stored [AppTheme] reactively via [themeFlow] (Req 2.4).
 * - Falls back to [AppTheme.DEFAULT] on read errors or corrupted values (Req 2.5, 2.6).
 * - Persists theme changes via [saveTheme] (Req 2.1).
 */
class ThemePreferencesRepository(private val dataStore: DataStore<Preferences>) {

    private val themeKey = stringPreferencesKey("selected_theme")

    /**
     * Flow that emits the current [AppTheme] stored in DataStore.
     * - On first collection emits the saved value or DEFAULT_GREEN if none exists (Req 2.2, 2.3).
     * - On IOException (corrupted file), emits emptyPreferences which resolves to DEFAULT_GREEN (Req 2.5).
     */
    val themeFlow: Flow<AppTheme> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading theme preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val name = preferences[themeKey]
            if (name != null) AppTheme.fromName(name) else AppTheme.DEFAULT
        }

    /**
     * Saves the selected [theme] to DataStore (Req 2.1).
     * If the write fails, the exception is caught and logged so that [themeFlow]
     * remains uninterrupted with the last valid value (Req 2.6).
     */
    suspend fun saveTheme(theme: AppTheme) {
        try {
            dataStore.edit { preferences ->
                preferences[themeKey] = theme.name
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error writing theme preference", e)
        }
    }

    companion object {
        private const val TAG = "ThemePrefsRepo"
    }
}
