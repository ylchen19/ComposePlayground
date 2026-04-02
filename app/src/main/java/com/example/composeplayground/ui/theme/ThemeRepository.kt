package com.example.composeplayground.ui.theme

import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    val themeConfigFlow: Flow<ThemeConfig>
    suspend fun setDarkModeOption(option: DarkModeOption)
    suspend fun setDynamicColor(enabled: Boolean)
}
