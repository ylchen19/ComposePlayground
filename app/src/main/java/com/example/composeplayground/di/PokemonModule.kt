package com.example.composeplayground.di

import com.example.composeplayground.data.repository.PokemonRepository
import com.example.composeplayground.data.repository.PokemonRepositoryImpl
import com.example.composeplayground.ui.screen.pokemon.PokemonDetailViewModel
import com.example.composeplayground.ui.screen.pokemon.PokemonListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Pokémon 功能的 Koin 模組，負責 Repository 與 ViewModel 的註冊。
 *
 * ## 網路層
 * 不再自建 HttpClient，改由 [networkModule] 提供的 [com.example.composeplayground.network.api.ApiService]
 * 注入至 [PokemonRepositoryImpl]。所有網路設定（base URL、快取、Logging）集中由 [networkModule] 管理。
 *
 * ## ViewModel 注入方式
 * [PokemonDetailViewModel] 需要 `pokemonId`，透過 Koin 的 `params` 在 call site 傳入：
 * ```kotlin
 * koinViewModel<PokemonDetailViewModel>(parameters = { parametersOf(pokemonId) })
 * ```
 */
val pokemonModule = module {

    // ── 資料層 ────────────────────────────────────────────────────────────────
    // get() 解析為 networkModule 提供的 ApiService（KtorApiService）
    single<PokemonRepository> { PokemonRepositoryImpl(get()) }

    // ── ViewModel ─────────────────────────────────────────────────────────────
    viewModel { PokemonListViewModel(get()) }
    // pokemonId 由導航層透過 parametersOf 傳入，對應 AppNavHost 的 entry<PokemonDetail>
    viewModel { params -> PokemonDetailViewModel(pokemonId = params.get(), repository = get()) }
}
