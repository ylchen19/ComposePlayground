package com.example.composeplayground.network.auth

interface TokenProvider {
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun refreshTokens(): Boolean
    suspend fun clearTokens()
    fun updateTokens(accessToken: String, refreshToken: String)
}
