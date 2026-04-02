package com.example.composeplayground.network.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicReference

@Serializable
private data class RefreshRequest(val refreshToken: String)

@Serializable
private data class TokenResponse(val accessToken: String, val refreshToken: String)

class InMemoryTokenProvider(
    private val bareClient: HttpClient,
    private val refreshUrl: String,
) : TokenProvider {

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
