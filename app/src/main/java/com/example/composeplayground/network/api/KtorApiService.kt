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
