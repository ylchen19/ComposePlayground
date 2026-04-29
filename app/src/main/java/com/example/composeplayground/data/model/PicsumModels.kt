package com.example.composeplayground.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Picsum Photos `/v2/list` 端點回傳的單筆 DTO。
 *
 * 範例：
 * ```json
 * {"id":"0","author":"Alejandro Escamilla","width":5616,"height":3744,
 *  "url":"https://unsplash.com/...","download_url":"https://picsum.photos/id/0/5616/3744"}
 * ```
 */
@Serializable
data class PicsumPhotoDto(
    val id: String,
    val author: String,
    val width: Int,
    val height: Int,
    val url: String,
    @SerialName("download_url") val downloadUrl: String,
)

/**
 * Picsum 圖片的 domain model。
 *
 * 提供兩種尺寸的 URL 產生方式：
 * - [thumbnailUrl]：固定方形大圖（預設 1080×1080），用於 grid 縮圖以實際感受「大檔案」載入效果
 * - [fullSizeUrl]：原始尺寸大圖，用於詳細頁
 */
@Immutable
data class PicsumPhoto(
    val id: String,
    val author: String,
    val originalWidth: Int,
    val originalHeight: Int,
    val sourceUrl: String,
) {
    fun thumbnailUrl(size: Int = 1080) = "https://picsum.photos/id/$id/$size/$size"
    fun fullSizeUrl() = "https://picsum.photos/id/$id/$originalWidth/$originalHeight"
}

fun PicsumPhotoDto.toDomain(): PicsumPhoto = PicsumPhoto(
    id = id,
    author = author,
    originalWidth = width,
    originalHeight = height,
    sourceUrl = url,
)
