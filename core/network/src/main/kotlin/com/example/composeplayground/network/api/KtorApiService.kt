package com.example.composeplayground.network.api

import com.example.composeplayground.network.NetworkResult
import com.example.composeplayground.network.connectivity.ConnectivityObserver
import com.example.composeplayground.network.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.util.reflect.TypeInfo

/**
 * [ApiService] 的 Ktor 實作。
 *
 * 每個方法在發送請求前先檢查 [ConnectivityObserver.isConnected]，
 * 有連線才透過 [safeApiCall] 執行實際請求，統一將例外轉換為 [NetworkResult.Error]。
 *
 * `@Suppress("UNCHECKED_CAST")` 來自 Ktor 的 `body(typeInfo)` 回傳 `Any`，
 * 但型別安全由 [TypeInfo] 在執行期保證，強制轉型不會造成實際風險。
 *
 * @param httpClient 已設定 Auth、Logging、ContentNegotiation 插件的 Ktor HttpClient
 * @param connectivityObserver 用於在無網路時提前短路，避免不必要的請求
 */
class KtorApiService(
    private val httpClient: HttpClient,
    private val connectivityObserver: ConnectivityObserver,
) : ApiService {

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> get(
        endpoint: String,
        typeInfo: TypeInfo,
        queryParams: Map<String, String>,
    ): NetworkResult<T> {
        if (!connectivityObserver.isConnected.value) {
            return NetworkResult.Error(message = "No internet connection")
        }
        return safeApiCall {
            httpClient.get(endpoint) {
                queryParams.forEach { (key, value) -> parameter(key, value) }
            }.body(typeInfo) as T
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> post(
        endpoint: String,
        body: Any,
        typeInfo: TypeInfo,
    ): NetworkResult<T> {
        if (!connectivityObserver.isConnected.value) {
            return NetworkResult.Error(message = "No internet connection")
        }
        return safeApiCall {
            httpClient.post(endpoint) {
                setBody(body)
            }.body(typeInfo) as T
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> put(
        endpoint: String,
        body: Any,
        typeInfo: TypeInfo,
    ): NetworkResult<T> {
        if (!connectivityObserver.isConnected.value) {
            return NetworkResult.Error(message = "No internet connection")
        }
        return safeApiCall {
            httpClient.put(endpoint) {
                setBody(body)
            }.body(typeInfo) as T
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> delete(
        endpoint: String,
        typeInfo: TypeInfo,
    ): NetworkResult<T> {
        if (!connectivityObserver.isConnected.value) {
            return NetworkResult.Error(message = "No internet connection")
        }
        return safeApiCall {
            httpClient.delete(endpoint).body(typeInfo) as T
        }
    }
}
