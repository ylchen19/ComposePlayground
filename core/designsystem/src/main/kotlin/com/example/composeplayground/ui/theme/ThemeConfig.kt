package com.example.composeplayground.ui.theme

enum class DarkModeOption {
    SYSTEM,
    LIGHT,
    DARK,
}

data class ThemeConfig(
    val darkModeOption: DarkModeOption = DarkModeOption.SYSTEM,
    val dynamicColor: Boolean = true,
    val showPerformanceMetrics: Boolean = false,
)
