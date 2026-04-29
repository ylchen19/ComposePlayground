package com.example.composeplayground.network.di

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
 * 提供與 feature 無關的網路層基礎設施。
 *
 * 包含：連線監聽、Token 管理、HTTP 快取設定、HttpClient 工廠。
 *
 * 使用此 module 的應用程式需在自己的 Koin module 中提供：
 * - `single(named("refreshUrl")) { ... }`：Token 刷新端點，公開 API 可填空字串
 * - 自己使用 [HttpClientFactory] 建立面向特定 baseUrl 的 [HttpClient]
 */
val coreNetworkModule = module {

    single<ConnectivityObserver> { NetworkConnectivityObserver(androidContext()) }

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

    single { CacheConfig(cacheDirectory = File(androidContext().cacheDir, "http_cache")) }

    single { HttpClientFactory(get(), get()) }
}
