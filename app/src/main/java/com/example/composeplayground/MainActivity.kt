package com.example.composeplayground

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.composeplayground.navigation.AppNavHost
import com.example.composeplayground.ui.perf.LocalPerformanceMonitor
import com.example.composeplayground.ui.perf.PerformanceDashboard
import com.example.composeplayground.ui.perf.PerformanceMonitor
import com.example.composeplayground.ui.theme.ComposePlaygroundTheme
import com.example.composeplayground.ui.theme.DarkModeOption
import com.example.composeplayground.ui.theme.ThemeViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val themeViewModel: ThemeViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeConfig by themeViewModel.uiState.collectAsStateWithLifecycle()
            val darkTheme = when (themeConfig.darkModeOption) {
                DarkModeOption.SYSTEM -> isSystemInDarkTheme()
                DarkModeOption.LIGHT -> false
                DarkModeOption.DARK -> true
            }

            val performanceMonitor = remember { PerformanceMonitor() }

            ComposePlaygroundTheme(
                darkTheme = darkTheme,
                dynamicColor = themeConfig.dynamicColor,
            ) {
                CompositionLocalProvider(LocalPerformanceMonitor provides performanceMonitor) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AppNavHost(modifier = Modifier.fillMaxSize())

                        if (BuildConfig.DEBUG && themeConfig.showPerformanceMetrics) {
                            PerformanceDashboard(monitor = performanceMonitor)
                        }
                    }
                }
            }
        }
    }
}