package com.example.composeplayground.network.api

import com.example.composeplayground.network.NetworkResult
import io.ktor.util.reflect.TypeInfo

/**
 * HTTP API 操作的抽象介面，封裝 GET / POST / PUT / DELETE 四種方法。
 *
 * 介面方法接收 [TypeInfo] 以支援執行期泛型反序列化（Ktor 需要此資訊還原型別）。
 * 實際呼叫請使用下方 `inline reified` 擴充函式，編譯器會自動推導 [TypeInfo]，
 * 無需手動傳入型別資訊。
 *
 * 所有方法回傳 [NetworkResult]，錯誤處理已在實作層統一透過 [safeApiCall] 完成。
 */
interface ApiService {
    /**
     * 發送 GET 請求。
     *
     * @param endpoint 相對於 baseUrl 的路徑（例如 `/pokemon/1`）
     * @param typeInfo 回應型別的執行期資訊，供 Ktor 反序列化使用
     * @param queryParams URL query 參數
     */
    suspend fun <T> get(
        endpoint: String,
        typeInfo: TypeInfo,
        queryParams: Map<String, String> = emptyMap(),
    ): NetworkResult<T>

    /**
     * 發送 POST 請求。
     *
     * @param endpoint 相對於 baseUrl 的路徑
     * @param body 請求 body，會被序列化為 JSON
     * @param typeInfo 回應型別的執行期資訊
     */
    suspend fun <T> post(
        endpoint: String,
        body: Any,
        typeInfo: TypeInfo,
    ): NetworkResult<T>

    /**
     * 發送 PUT 請求。
     *
     * @param endpoint 相對於 baseUrl 的路徑
     * @param body 請求 body，會被序列化為 JSON
     * @param typeInfo 回應型別的執行期資訊
     */
    suspend fun <T> put(
        endpoint: String,
        body: Any,
        typeInfo: TypeInfo,
    ): NetworkResult<T>

    /**
     * 發送 DELETE 請求。
     *
     * @param endpoint 相對於 baseUrl 的路徑
     * @param typeInfo 回應型別的執行期資訊
     */
    suspend fun <T> delete(
        endpoint: String,
        typeInfo: TypeInfo,
    ): NetworkResult<T>
}

// ── 便利擴充函式（inline reified）────────────────────────────────────────────
// 呼叫端直接指定型別參數即可，TypeInfo 由編譯器在 call site 自動生成。
// 範例：apiService.get<PokemonDetail>("/pokemon/1")

/** GET 的 reified 便利版本，自動推導 [TypeInfo]。 */
suspend inline fun <reified T> ApiService.get(
    endpoint: String,
    queryParams: Map<String, String> = emptyMap(),
): NetworkResult<T> = get(endpoint, io.ktor.util.reflect.typeInfo<T>(), queryParams)

/** POST 的 reified 便利版本，自動推導 [TypeInfo]。 */
suspend inline fun <reified T> ApiService.post(
    endpoint: String,
    body: Any,
): NetworkResult<T> = post(endpoint, body, io.ktor.util.reflect.typeInfo<T>())

/** PUT 的 reified 便利版本，自動推導 [TypeInfo]。 */
suspend inline fun <reified T> ApiService.put(
    endpoint: String,
    body: Any,
): NetworkResult<T> = put(endpoint, body, io.ktor.util.reflect.typeInfo<T>())

/** DELETE 的 reified 便利版本，自動推導 [TypeInfo]。 */
suspend inline fun <reified T> ApiService.delete(
    endpoint: String,
): NetworkResult<T> = delete(endpoint, io.ktor.util.reflect.typeInfo<T>())
