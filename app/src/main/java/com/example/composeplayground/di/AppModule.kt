package com.example.composeplayground.di

import com.example.composeplayground.ui.theme.DataStoreThemeRepository
import com.example.composeplayground.ui.theme.ThemeRepository
import com.example.composeplayground.ui.theme.ThemeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * 應用程式層的 Koin 模組，負責註冊與 UI 主題相關的依賴。
 *
 * 在 [com.example.composeplayground.ComposePlaygroundApp] 初始化 Koin 時載入。
 * 需要 Android Context（由 Koin 的 `androidContext()` 提供），
 * 因此 [DataStoreThemeRepository] 以 `get()` 取得 Context。
 */
val appModule = module {

    // ThemeRepository 以介面型別註冊，實作為 DataStore 版本，可在測試時替換
    single<ThemeRepository> { DataStoreThemeRepository(get()) }

    viewModel { ThemeViewModel(get()) }
}
