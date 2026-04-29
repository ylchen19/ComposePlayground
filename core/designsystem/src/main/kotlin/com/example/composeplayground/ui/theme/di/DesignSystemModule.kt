package com.example.composeplayground.ui.theme.di

import com.example.composeplayground.ui.theme.DataStoreThemeRepository
import com.example.composeplayground.ui.theme.ThemeRepository
import com.example.composeplayground.ui.theme.ThemeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Design system 模組的 Koin 設定，提供 Theme 相關依賴。
 *
 * - [ThemeRepository] 以介面型別註冊，實作為 DataStore 版本，可在測試時替換
 * - [ThemeViewModel] 提供 Settings 螢幕觀察並切換主題偏好
 */
val designSystemModule = module {

    single<ThemeRepository> { DataStoreThemeRepository(get()) }

    viewModel { ThemeViewModel(get()) }
}
