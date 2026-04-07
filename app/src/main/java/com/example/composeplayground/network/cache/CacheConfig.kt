package com.example.composeplayground.network.cache

import java.io.File

/**
 * OkHttp 磁碟快取設定。
 *
 * 由 [com.example.composeplayground.di.NetworkModule] 提供，注入至 [com.example.composeplayground.network.client.HttpClientFactory]。
 * 快取目錄通常設為 `context.cacheDir`，由 Android 系統在儲存空間不足時自動清理。
 *
 * @param cacheDirectory 快取檔案存放目錄
 * @param cacheSize 最大快取大小（位元組），預設 10 MB
 */
data class CacheConfig(
    val cacheDirectory: File,
    val cacheSize: Long = 10L * 1024 * 1024, // 10 MB
)
