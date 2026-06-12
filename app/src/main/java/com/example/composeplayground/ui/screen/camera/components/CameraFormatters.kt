package com.example.composeplayground.ui.screen.camera.components

import com.example.composeplayground.ui.screen.camera.model.WhiteBalance
import kotlin.math.roundToInt

internal val WhiteBalance.shortLabel: String
    get() = when (this) {
        WhiteBalance.Auto -> "Auto"
        WhiteBalance.Daylight -> "Day"
        WhiteBalance.Cloudy -> "Cloud"
        WhiteBalance.Tungsten -> "Tung"
        WhiteBalance.Fluorescent -> "Fluo"
    }

internal fun Int.toEvLabel(): String = when {
    this > 0 -> "+$this"
    else -> "$this"
}

internal fun Long.toShutterLabel(): String {
    val denominator = (1_000_000_000.0 / this).roundToInt()
    return "1/$denominator"
}

internal fun Float.safeRangeTo(end: Float): ClosedFloatingPointRange<Float> {
    return if (this == end) this..(end + 1f) else this..end
}
