package com.example.composeplayground.network.cache

import java.io.File

data class CacheConfig(
    val cacheDirectory: File,
    val cacheSize: Long = 10L * 1024 * 1024, // 10 MB
)
