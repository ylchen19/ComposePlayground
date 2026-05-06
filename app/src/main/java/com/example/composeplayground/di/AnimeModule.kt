package com.example.composeplayground.di

import com.example.composeplayground.data.repository.AnimeRepository
import com.example.composeplayground.data.repository.AnimeRepositoryImpl
import com.example.composeplayground.ui.screen.anime.AnimeDetailViewModel
import com.example.composeplayground.ui.screen.anime.AnimeListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Anime（Jikan API）功能的 Koin 模組。
 *
 * Repository 注入 [appNetworkModule] 以 `named("animeApi")` 註冊的 ApiService 實例，
 * 對應 Jikan REST API base URL（https://api.jikan.moe/v4/）。
 */
val animeModule = module {
    single<AnimeRepository> { AnimeRepositoryImpl(get(named("animeApi"))) }

    viewModel { AnimeListViewModel(get()) }
    viewModel { params -> AnimeDetailViewModel(animeId = params.get(), repository = get()) }
}
