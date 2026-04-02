package com.example.composeplayground.network.client

import com.example.composeplayground.network.auth.TokenProvider
import com.example.composeplayground.network.cache.CacheConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.OkHttpClient

class HttpClientFactory(
    private val tokenProvider: TokenProvider,
    private val cacheConfig: CacheConfig,
) {
    fun create(baseUrl: String): HttpClient {
        val okHttpClient = OkHttpClient.Builder()
            .cache(Cache(cacheConfig.cacheDirectory, cacheConfig.cacheSize))
            .build()

        return HttpClient(OkHttp) {
            engine {
                preconfigured = okHttpClient
            }

            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = false
                    encodeDefaults = true
                })
            }

            install(Logging) {
                level = LogLevel.BODY
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        val access = tokenProvider.getAccessToken()
                        val refresh = tokenProvider.getRefreshToken()
                        if (access != null) BearerTokens(access, refresh ?: "") else null
                    }

                    refreshTokens {
                        val success = tokenProvider.refreshTokens()
                        if (success) {
                            val access = tokenProvider.getAccessToken()
                            val refresh = tokenProvider.getRefreshToken()
                            if (access != null) BearerTokens(access, refresh ?: "") else null
                        } else {
                            tokenProvider.clearTokens()
                            null
                        }
                    }

                    sendWithoutRequest { request ->
                        request.url.pathSegments.any { it == "auth" }
                    }
                }
            }

            defaultRequest {
                url(baseUrl)
                contentType(ContentType.Application.Json)
            }
        }
    }
}
