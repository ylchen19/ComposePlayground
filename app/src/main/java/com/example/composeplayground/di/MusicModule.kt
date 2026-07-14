package com.example.composeplayground.di

import com.example.composeplayground.data.repository.MusicRepository
import com.example.composeplayground.data.repository.MusicRepositoryImpl
import com.example.composeplayground.ui.screen.music.MusicDetailViewModel
import com.example.composeplayground.ui.screen.music.MusicSearchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * 音樂搜尋（iTunes Search API）功能的 Koin 模組。
 *
 * Repository 注入 [appNetworkModule] 以 `named("musicApi")`（搜尋/查詢）與
 * `named("musicChartsApi")`（排行榜 RSS Feed）註冊的兩個 ApiService 實例。
 */
val musicModule = module {
    single<MusicRepository> {
        MusicRepositoryImpl(get(named("musicApi")), get(named("musicChartsApi")))
    }

    viewModel { MusicSearchViewModel(get()) }
    viewModel { params -> MusicDetailViewModel(track = params.get()) }
}
