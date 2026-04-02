package com.example.composeplayground.network

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerializationException
import java.io.IOException

sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>
    data class Error(
        val code: Int? = null,
        val message: String? = null,
        val throwable: Throwable? = null,
    ) : NetworkResult<Nothing>

    data object Loading : NetworkResult<Nothing>
}

suspend fun <T> safeApiCall(block: suspend () -> T): NetworkResult<T> {
    return try {
        NetworkResult.Success(block())
    } catch (e: ClientRequestException) {
        // 4xx errors
        val errorBody = try { e.response.bodyAsText() } catch (_: Exception) { null }
        NetworkResult.Error(
            code = e.response.status.value,
            message = errorBody ?: e.message,
            throwable = e,
        )
    } catch (e: ServerResponseException) {
        // 5xx errors
        NetworkResult.Error(
            code = e.response.status.value,
            message = e.message,
            throwable = e,
        )
    } catch (e: IOException) {
        NetworkResult.Error(
            message = "Network error: ${e.localizedMessage}",
            throwable = e,
        )
    } catch (e: SerializationException) {
        NetworkResult.Error(
            message = "Parsing error: ${e.localizedMessage}",
            throwable = e,
        )
    }
}
