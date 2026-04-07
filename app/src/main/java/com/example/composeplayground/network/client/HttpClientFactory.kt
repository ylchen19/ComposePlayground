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

/**
 * Ktor [HttpClient] 工廠，負責組裝所有網路層插件。
 *
 * 建立的 HttpClient 包含：
 * - **OkHttp 引擎**：使用 [CacheConfig] 設定磁碟快取
 * - **ContentNegotiation**：JSON 序列化（寬鬆模式，忽略未知欄位）
 * - **Logging**：Log level BODY，輸出完整請求與回應（正式環境建議降為 NONE）
 * - **Auth（Bearer）**：自動附加 Access Token，並在收到 401 時觸發 Token 刷新
 *
 * @param tokenProvider Token 讀取與刷新的來源
 * @param cacheConfig OkHttp 磁碟快取設定
 */
class HttpClientFactory(
    private val tokenProvider: TokenProvider,
    private val cacheConfig: CacheConfig,
) {
    /**
     * 建立並回傳已設定好的 [HttpClient]。
     *
     * @param baseUrl 所有請求的基礎 URL（例如 `https://pokeapi.co/api/v2/`）
     */
    fun create(baseUrl: String): HttpClient {
        // 以 OkHttpClient 為底層引擎，啟用磁碟快取以節省頻寬
        val okHttpClient = OkHttpClient.Builder()
            .cache(Cache(cacheConfig.cacheDirectory, cacheConfig.cacheSize))
            .build()

        return HttpClient(OkHttp) {
            engine {
                preconfigured = okHttpClient
            }

            // JSON 反序列化設定：忽略多餘欄位、允許寬鬆格式、序列化預設值
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = false
                    encodeDefaults = true
                })
            }

            // 輸出完整請求/回應日誌，便於開發期除錯
            install(Logging) {
                level = LogLevel.BODY
            }

            install(Auth) {
                bearer {
                    // 每次請求前從 TokenProvider 讀取目前的 Token
                    loadTokens {
                        val access = tokenProvider.getAccessToken()
                        val refresh = tokenProvider.getRefreshToken()
                        if (access != null) BearerTokens(access, refresh ?: "") else null
                    }

                    // 收到 401 時嘗試刷新 Token；失敗則清除 Token（強制重新登入）
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

                    // 路徑含 "auth" 的端點（如登入、刷新）不附加 Bearer Token，避免循環
                    sendWithoutRequest { request ->
                        request.url.pathSegments.any { it == "auth" }
                    }
                }
            }

            // 設定所有請求的預設 base URL 與 Content-Type
            defaultRequest {
                url(baseUrl)
                contentType(ContentType.Application.Json)
            }
        }
    }
}
