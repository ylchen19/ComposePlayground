package com.example.composeplayground.di

import com.example.composeplayground.ui.screen.camera.CameraViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val cameraModule = module {
    viewModel { CameraViewModel() }
}
