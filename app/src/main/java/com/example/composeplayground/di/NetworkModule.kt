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

val networkModule = module {

    // Connectivity observer
    single<ConnectivityObserver> { NetworkConnectivityObserver(androidContext()) }

    // Bare HttpClient for token refresh (no Auth plugin to avoid recursion)
    single(named("bareClient")) {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    // Token management
    single<TokenProvider> {
        InMemoryTokenProvider(
            bareClient = get(named("bareClient")),
            refreshUrl = get(named("refreshUrl")),
        )
    }

    // Cache configuration
    single { CacheConfig(cacheDirectory = File(androidContext().cacheDir, "http_cache")) }

    // HttpClient factory & instance
    single { HttpClientFactory(get(), get()) }
    single { get<HttpClientFactory>().create(baseUrl = get(named("baseUrl"))) }

    // API service
    single<ApiService> { KtorApiService(get(), get()) }

    // Base URL & refresh URL — override these in your app configuration
    single(named("baseUrl")) { "https://api.example.com/" }
    single(named("refreshUrl")) { "https://api.example.com/auth/refresh" }
}
