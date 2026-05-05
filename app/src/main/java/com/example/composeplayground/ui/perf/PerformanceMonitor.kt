package com.example.composeplayground.ui.perf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.example.composeplayground.BuildConfig

class PerformanceMonitor {
    val fps = mutableIntStateOf(0)
    val memoryUsageMb = mutableLongStateOf(0L)
    val recompositionCounts = mutableStateMapOf<String, Int>()
    val pagingLoadTimeMs = mutableLongStateOf(0L)

    fun incrementRecompositionCount(name: String) {
        val current = recompositionCounts[name] ?: 0
        recompositionCounts[name] = current + 1
    }
}

val LocalPerformanceMonitor = staticCompositionLocalOf<PerformanceMonitor> {
    error("PerformanceMonitor not provided")
}

@Composable
fun Modifier.trackRecomposition(name: String): Modifier {
    val monitor = LocalPerformanceMonitor.current
    SideEffect {
        if (BuildConfig.DEBUG) {
            monitor.incrementRecompositionCount(name)
        }
    }
    return this
}
