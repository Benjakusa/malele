package com.benjakusa.malele.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore by preferencesDataStore(name = "malele_theme")

@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val darkModeKey = booleanPreferencesKey("dark_mode")

    val isDarkMode: Flow<Boolean> = context.themeDataStore.data
        .map { prefs -> prefs[darkModeKey] ?: false }

    suspend fun setDarkMode(dark: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[darkModeKey] = dark
        }
    }
}
