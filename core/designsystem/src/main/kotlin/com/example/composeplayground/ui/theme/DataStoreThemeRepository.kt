package com.example.composeplayground.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

class DataStoreThemeRepository(private val context: Context) : ThemeRepository {

    private companion object {
        val DARK_MODE_KEY = stringPreferencesKey("dark_mode_option")
        val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
        val PERFORMANCE_METRICS_KEY = booleanPreferencesKey("performance_metrics")
    }

    override val themeConfigFlow: Flow<ThemeConfig> = context.dataStore.data.map { prefs ->
        ThemeConfig(
            darkModeOption = prefs[DARK_MODE_KEY]
                ?.let { runCatching { DarkModeOption.valueOf(it) }.getOrNull() }
                ?: DarkModeOption.SYSTEM,
            dynamicColor = prefs[DYNAMIC_COLOR_KEY] ?: true,
            showPerformanceMetrics = prefs[PERFORMANCE_METRICS_KEY] ?: false,
        )
    }

    override suspend fun setDarkModeOption(option: DarkModeOption) {
        context.dataStore.edit { prefs ->
            prefs[DARK_MODE_KEY] = option.name
        }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DYNAMIC_COLOR_KEY] = enabled
        }
    }

    override suspend fun setPerformanceMetricsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PERFORMANCE_METRICS_KEY] = enabled
        }
    }
}
