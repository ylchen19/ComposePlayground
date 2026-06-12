package com.example.composeplayground.ui.screen.camera.model

import android.hardware.camera2.CaptureRequest

enum class WhiteBalance(val label: String, val awbMode: Int) {
    Auto("Auto", CaptureRequest.CONTROL_AWB_MODE_AUTO),
    Daylight("日光", CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT),
    Cloudy("陰天", CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT),
    Tungsten("燈泡", CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT),
    Fluorescent("日光燈", CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT),
}
