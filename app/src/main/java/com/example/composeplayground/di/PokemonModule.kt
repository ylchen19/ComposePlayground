package com.example.composeplayground.di

import com.example.composeplayground.data.repository.PokemonRepository
import com.example.composeplayground.data.repository.PokemonRepositoryImpl
import com.example.composeplayground.ui.screen.pokemon.PokemonDetailViewModel
import com.example.composeplayground.ui.screen.pokemon.PokemonListViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Pokémon 功能的 Koin 模組，包含專屬 HttpClient、Repository 與 ViewModel 的註冊。
 *
 * ## 為何獨立一個 HttpClient？
 * PokéAPI 為公開 API，不需要 Bearer Token 認證，
 * 因此以 `named("pokemonClient")` 建立輕量客戶端，與 [networkModule] 的認證客戶端隔離，
 * 避免攜帶 Token 至第三方 API。
 *
 * ## JSON 設定
 * - `ignoreUnknownKeys`：忽略 API 回傳的多餘欄位，提升向後相容性
 * - `isLenient`：允許寬鬆 JSON 格式（如單引號、無引號的 key）
 * - `coerceInputValues`：型別不符時使用欄位預設值，而非拋出例外
 *
 * ## ViewModel 注入方式
 * [PokemonDetailViewModel] 需要 `pokemonId`，透過 Koin 的 `params` 在 call site 傳入：
 * ```kotlin
 * koinViewModel<PokemonDetailViewModel>(parameters = { parametersOf(pokemonId) })
 * ```
 */
val pokemonModule = module {

    // ── 網路客戶端 ────────────────────────────────────────────────────────────

    // 專用於 PokéAPI 的輕量客戶端，無 Auth 插件，Log level 僅記錄 Headers
    single(named("pokemonClient")) {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                })
            }

            install(Logging) {
                level = LogLevel.HEADERS
            }

            defaultRequest {
                url("https://pokeapi.co/api/v2/")
                contentType(ContentType.Application.Json)
            }
        }
    }

    // ── 資料層 ────────────────────────────────────────────────────────────────
    single<PokemonRepository> { PokemonRepositoryImpl(get(named("pokemonClient"))) }

    // ── ViewModel ─────────────────────────────────────────────────────────────
    viewModel { PokemonListViewModel(get()) }
    // pokemonId 由導航層透過 parametersOf 傳入，對應 AppNavHost 的 entry<PokemonDetail>
    viewModel { params -> PokemonDetailViewModel(pokemonId = params.get(), repository = get()) }
}
