package com.example.composeplayground.data.analyzer

/**
 * 對 Picsum 圖片做內容摘要分析。實作層自行決定如何取得圖像（如 Coil + 縮圖）
 * 與如何產生 label，ViewModel 只認此契約。
 */
interface PicsumImageAnalyzer {
    /**
     * @param photoId 用作 cache key
     * @param imageUrl 通常傳 [com.example.composeplayground.data.model.PicsumPhoto.thumbnailUrl]
     *                 的小尺寸版本即可，模型不需要原圖解析度
     */
    suspend fun summarize(photoId: String, imageUrl: String): Result<List<String>>
}
