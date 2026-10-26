package com.example.location_reminder.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

enum class AppTheme {
    LIGHT,
    DARK
}

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("app_theme_mode")
    }

    val themeFlow: Flow<AppTheme> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeName = preferences[PreferencesKeys.THEME_MODE] ?: AppTheme.DARK.name
            try {
                AppTheme.valueOf(themeName)
            } catch (e: IllegalArgumentException) {
                AppTheme.DARK
            }
        }

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = theme.name
        }
    }
}
