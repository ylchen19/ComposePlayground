package com.example.composeplayground.ui.screen.camera.model

import androidx.camera.core.ImageCapture

enum class FlashMode(val imageCaptureMode: Int) {
    Off(ImageCapture.FLASH_MODE_OFF),
    Auto(ImageCapture.FLASH_MODE_AUTO),
    On(ImageCapture.FLASH_MODE_ON),
}
