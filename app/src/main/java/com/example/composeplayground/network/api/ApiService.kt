package com.example.composeplayground.network.api

import com.example.composeplayground.network.NetworkResult
import io.ktor.util.reflect.TypeInfo

interface ApiService {
    suspend fun <T> get(
        endpoint: String,
        typeInfo: TypeInfo,
        queryParams: Map<String, String> = emptyMap(),
    ): NetworkResult<T>

    suspend fun <T> post(
        endpoint: String,
        body: Any,
        typeInfo: TypeInfo,
    ): NetworkResult<T>

    suspend fun <T> put(
        endpoint: String,
        body: Any,
        typeInfo: TypeInfo,
    ): NetworkResult<T>

    suspend fun <T> delete(
        endpoint: String,
        typeInfo: TypeInfo,
    ): NetworkResult<T>
}

suspend inline fun <reified T> ApiService.get(
    endpoint: String,
    queryParams: Map<String, String> = emptyMap(),
): NetworkResult<T> = get(endpoint, io.ktor.util.reflect.typeInfo<T>(), queryParams)

suspend inline fun <reified T> ApiService.post(
    endpoint: String,
    body: Any,
): NetworkResult<T> = post(endpoint, body, io.ktor.util.reflect.typeInfo<T>())

suspend inline fun <reified T> ApiService.put(
    endpoint: String,
    body: Any,
): NetworkResult<T> = put(endpoint, body, io.ktor.util.reflect.typeInfo<T>())

suspend inline fun <reified T> ApiService.delete(
    endpoint: String,
): NetworkResult<T> = delete(endpoint, io.ktor.util.reflect.typeInfo<T>())
