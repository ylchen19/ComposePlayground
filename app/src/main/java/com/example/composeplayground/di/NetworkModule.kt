package com.example.composeplayground.di

import com.example.composeplayground.network.api.ApiService
import com.example.composeplayground.network.api.KtorApiService
import com.example.composeplayground.network.client.HttpClientFactory
import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * App 層的網路設定，提供「特定第三方 API」的 Base URL、HttpClient 與 ApiService 實例。
 *
 * 通用基礎設施（HttpClientFactory、TokenProvider、CacheConfig、ConnectivityObserver）由
 * [com.example.composeplayground.network.di.coreNetworkModule] 提供。
 *
 * ## 使用說明
 * - 預設 [HttpClient] / [ApiService] 指向 PokéAPI（公開 API，無需 Bearer Token）
 * - Picsum Photos 透過 `named("picsumClient")` 與 `named("picsumApi")` 取得獨立實例
 * - PokéAPI 為公開 API，[com.example.composeplayground.network.auth.TokenProvider] 的 Token
 *   永遠為 null，Auth 插件不會附加任何 Authorization header
 */
val appNetworkModule = module {

    // ── URL 設定 ──────────────────────────────────────────────────────────────
    single(named("baseUrl")) { "https://pokeapi.co/api/v2/" }
    single(named("picsumBaseUrl")) { "https://picsum.photos/" }
    // PokéAPI 無需 Token 刷新；refreshUrl 保留作為未來串接需認證 API 時的擴充點
    single(named("refreshUrl")) { "" }

    // ── HttpClient 實例 ───────────────────────────────────────────────────────

    // 由工廠建立已套用 Auth / Logging / ContentNegotiation 的完整客戶端（PokéAPI）
    single { get<HttpClientFactory>().create(baseUrl = get(named("baseUrl"))) }

    // 第二個 HttpClient 指向 Picsum Photos，用於圖庫模組。
    // Auth 插件對 Picsum 無害（TokenProvider 回傳 null 不會附 header），故重用同一工廠。
    single<HttpClient>(named("picsumClient")) {
        get<HttpClientFactory>().create(baseUrl = get(named("picsumBaseUrl")))
    }

    // ── API 服務 ──────────────────────────────────────────────────────────────
    single<ApiService> { KtorApiService(get(), get()) }

    // 給 Picsum 用的獨立 ApiService 實例，使用 named qualifier 區分
    single<ApiService>(named("picsumApi")) {
        KtorApiService(get(named("picsumClient")), get())
    }
}
