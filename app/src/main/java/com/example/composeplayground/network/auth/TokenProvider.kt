package com.example.composeplayground.network.auth

/**
 * Token 管理介面，抽象化 Access Token / Refresh Token 的讀寫與刷新邏輯。
 *
 * 具體實作（如 [InMemoryTokenProvider]）決定 Token 的儲存方式。
 * Ktor [Auth] 插件在需要時會透過此介面自動處理 Token 刷新流程。
 */
interface TokenProvider {
    /** 取得目前的 Access Token；尚未登入或已清除時回傳 null。 */
    suspend fun getAccessToken(): String?

    /** 取得目前的 Refresh Token；尚未登入或已清除時回傳 null。 */
    suspend fun getRefreshToken(): String?

    /**
     * 使用 Refresh Token 向伺服器換取新的 Token 組合。
     *
     * @return `true` 表示刷新成功且新 Token 已儲存；`false` 表示刷新失敗（Refresh Token 過期或網路錯誤）
     */
    suspend fun refreshTokens(): Boolean

    /** 清除所有已儲存的 Token，通常在登出時呼叫。 */
    suspend fun clearTokens()

    /**
     * 直接更新 Token（例如登入成功後呼叫）。
     *
     * @param accessToken 新的 Access Token
     * @param refreshToken 新的 Refresh Token
     */
    fun updateTokens(accessToken: String, refreshToken: String)
}
