package com.example.composeplayground.network

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerializationException
import java.io.IOException

/**
 * 封裝 API 呼叫結果的 sealed interface。
 *
 * 所有網路操作回傳此型別，由呼叫端以 when 表達式窮舉處理：
 * - [Success]：請求成功，攜帶解析後的資料
 * - [Error]：請求失敗，攜帶 HTTP 狀態碼、訊息及原始例外
 * - [Loading]：請求進行中（通常用於 UI 狀態初始值）
 */
sealed interface NetworkResult<out T> {
    /** 請求成功，[data] 為已反序列化的回應資料。 */
    data class Success<T>(val data: T) : NetworkResult<T>

    /**
     * 請求失敗。
     *
     * @param code HTTP 狀態碼（4xx/5xx），網路層錯誤時為 null
     * @param message 人類可讀的錯誤描述
     * @param throwable 原始例外，可供日誌或詳細追蹤使用
     */
    data class Error(
        val code: Int? = null,
        val message: String? = null,
        val throwable: Throwable? = null,
    ) : NetworkResult<Nothing>

    /** 請求尚未完成，常作為 StateFlow 的初始值。 */
    data object Loading : NetworkResult<Nothing>
}

/**
 * 將任意 suspend 呼叫包裝成 [NetworkResult]，統一處理常見例外。
 *
 * 例外對應規則：
 * - [ClientRequestException]（4xx）→ [NetworkResult.Error] 含 HTTP 狀態碼與回應 body
 * - [ServerResponseException]（5xx）→ [NetworkResult.Error] 含 HTTP 狀態碼
 * - [IOException] → [NetworkResult.Error]，網路層錯誤（無連線、逾時等）
 * - [SerializationException] → [NetworkResult.Error]，JSON 解析失敗
 *
 * @param block 實際執行 API 呼叫的 suspend lambda
 * @return 包裝後的 [NetworkResult]
 */
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
