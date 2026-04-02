package com.example.composeplayground.di

import com.example.composeplayground.ui.theme.DataStoreThemeRepository
import com.example.composeplayground.ui.theme.ThemeRepository
import com.example.composeplayground.ui.theme.ThemeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class GreetingProvider {
    fun greeting(name: String): String = "Hello $name!"
}

val appModule = module {
    single { GreetingProvider() }
    single<ThemeRepository> { DataStoreThemeRepository(get()) }
    viewModel { ThemeViewModel(get()) }
}

