package com.example.composeplayground.ui.perf

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

@Composable
fun PerformanceDashboard(
    monitor: PerformanceMonitor,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(16f) }
    var offsetY by remember { mutableStateOf(100f) }

    LaunchedEffect(Unit) {
        var frames = 0
        var windowStart = 0L
        while (isActive) {
            withFrameNanos { frameTimeNanos ->
                if (windowStart == 0L) {
                    windowStart = frameTimeNanos
                }
                frames++
                val elapsed = frameTimeNanos - windowStart
                if (elapsed >= 1_000_000_000L) {
                    monitor.fps.intValue = frames
                    frames = 0
                    windowStart = frameTimeNanos
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val runtime = Runtime.getRuntime()
        while (isActive) {
            val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
            monitor.memoryUsageMb.longValue = usedMemory
            delay(1000)
        }
    }

    Column(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .width(180.dp)
            .background(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .padding(12.dp)
    ) {
        Text(
            text = "Performance Metrics",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.Gray)

        MetricRow(label = "FPS", value = "${monitor.fps.intValue}")
        MetricRow(label = "Memory", value = "${monitor.memoryUsageMb.longValue} MB")
        MetricRow(label = "Paging Load", value = "${monitor.pagingLoadTimeMs.longValue} ms")

        if (monitor.recompositionCounts.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.Gray)
            Text(
                text = "Recompositions",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp
            )
            monitor.recompositionCounts.forEach { (name, count) ->
                MetricRow(label = name, value = "$count")
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Text(
        text = "$label: $value",
        color = if (label == "FPS" && (value.toIntOrNull() ?: 60) < 50) Color.Red else Color.Green,
        style = MaterialTheme.typography.bodySmall,
        fontSize = 12.sp,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
