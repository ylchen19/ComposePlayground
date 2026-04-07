package com.example.composeplayground.di

import com.example.composeplayground.network.api.ApiService
import com.example.composeplayground.network.api.KtorApiService
import com.example.composeplayground.network.auth.InMemoryTokenProvider
import com.example.composeplayground.network.auth.TokenProvider
import com.example.composeplayground.network.cache.CacheConfig
import com.example.composeplayground.network.client.HttpClientFactory
import com.example.composeplayground.network.connectivity.ConnectivityObserver
import com.example.composeplayground.network.connectivity.NetworkConnectivityObserver
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File

/**
 * 網路層的 Koin 模組，提供具備快取能力的 [HttpClient] 與 [ApiService]，
 * 目前指向 PokéAPI（公開 API，無需 Bearer Token）。
 *
 * ## 依賴圖
 * ```
 * baseUrl / refreshUrl (named)
 *      ↓
 * bareClient (named)  →  TokenProvider  →  HttpClientFactory  →  HttpClient
 *                                                                      ↓
 * ConnectivityObserver  ─────────────────────────────────────→  ApiService (KtorApiService)
 *                                                                      ↑
 *                                                              pokemonModule 使用此 ApiService
 * ```
 *
 * ## 使用說明
 * - [ApiService] 由 [pokemonModule] 的 [com.example.composeplayground.data.repository.PokemonRepositoryImpl] 注入使用
 * - PokéAPI 為公開 API，[TokenProvider] 的 Token 永遠為 null，Auth 插件不會附加任何 Authorization header
 * - `bareClient` 為不含 Auth 插件的裸客戶端，供日後有認證需求的 API 刷新 Token 使用
 * - 快取目錄設為 `context.cacheDir/http_cache`，由 Android 在儲存空間不足時自動清理
 */
val networkModule = module {

    // ── 連線監聽 ──────────────────────────────────────────────────────────────
    single<ConnectivityObserver> { NetworkConnectivityObserver(androidContext()) }

    // ── Token 管理 ────────────────────────────────────────────────────────────

    // 不含 Auth 插件的裸客戶端，專供 Token 刷新使用，防止遞迴授權
    single(named("bareClient")) {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    single<TokenProvider> {
        InMemoryTokenProvider(
            bareClient = get(named("bareClient")),
            refreshUrl = get(named("refreshUrl")),
        )
    }

    // ── HttpClient ────────────────────────────────────────────────────────────

    // OkHttp 磁碟快取，存放於 cacheDir/http_cache，由系統自動管理容量
    single { CacheConfig(cacheDirectory = File(androidContext().cacheDir, "http_cache")) }

    single { HttpClientFactory(get(), get()) }

    // 由工廠建立已套用 Auth / Logging / ContentNegotiation 的完整客戶端
    single { get<HttpClientFactory>().create(baseUrl = get(named("baseUrl"))) }

    // ── API 服務 ──────────────────────────────────────────────────────────────
    single<ApiService> { KtorApiService(get(), get()) }

    // ── URL 設定 ──────────────────────────────────────────────────────────────
    single(named("baseUrl")) { "https://pokeapi.co/api/v2/" }
    // PokéAPI 無需 Token 刷新；refreshUrl 保留作為未來串接需認證 API 時的擴充點
    single(named("refreshUrl")) { "" }
}
