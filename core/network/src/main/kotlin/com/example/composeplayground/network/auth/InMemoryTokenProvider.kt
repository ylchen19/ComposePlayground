package com.example.composeplayground.network.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicReference

/** Token 刷新請求的 JSON body。 */
@Serializable
private data class RefreshRequest(val refreshToken: String)

/** Token 刷新回應的 JSON 結構。 */
@Serializable
private data class TokenResponse(val accessToken: String, val refreshToken: String)

/**
 * [TokenProvider] 的記憶體實作，Token 僅存活於 App 程序生命週期內。
 *
 * - 使用 [AtomicReference] 保證讀寫的原子性，避免多協程同時存取產生競態條件
 * - [bareClient] 為不含 Auth 插件的裸 HttpClient，用於避免刷新 Token 時觸發遞迴授權
 *
 * @param bareClient 不含 Bearer Auth 插件的 Ktor HttpClient，專用於 Token 刷新請求
 * @param refreshUrl 刷新 Token 的完整 URL
 */
class InMemoryTokenProvider(
    private val bareClient: HttpClient,
    private val refreshUrl: String,
) : TokenProvider {

    /** 以不可變 data class 包裝 Token 組合，方便 AtomicReference 進行原子替換。 */
    private data class Tokens(val access: String?, val refresh: String?)

    private val tokens = AtomicReference(Tokens(null, null))

    override suspend fun getAccessToken(): String? = tokens.get().access

    override suspend fun getRefreshToken(): String? = tokens.get().refresh

    override suspend fun refreshTokens(): Boolean {
        val currentRefresh = tokens.get().refresh ?: return false
        return try {
            val response: TokenResponse = bareClient.post(refreshUrl) {
                contentType(ContentType.Application.Json)
                setBody(RefreshRequest(currentRefresh))
            }.body()
            updateTokens(response.accessToken, response.refreshToken)
            true
        } catch (_: Exception) {
            // 任何例外（網路錯誤、反序列化失敗）皆視為刷新失敗，由呼叫端決定後續處理
            false
        }
    }

    override suspend fun clearTokens() {
        tokens.set(Tokens(null, null))
    }

    override fun updateTokens(accessToken: String, refreshToken: String) {
        tokens.set(Tokens(accessToken, refreshToken))
    }
}
