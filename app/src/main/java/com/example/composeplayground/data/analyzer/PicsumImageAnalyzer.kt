package com.example.composeplayground.data.analyzer

/**
 * 對 Picsum 圖片產生一句話的自然語言描述。
 *
 * 實作策略（優先到 fallback）：
 * 1. Gemini Nano on-device (Pixel 9+ 支援多模態，需下載約 2 GB 模型)
 * 2. ML Kit Image Labeling + 中文句子模板（所有裝置均支援，即時可用）
 *
 * 模型下載狀態由 [GeminiNanoModelManager.status] 對外暴露，UI 自行觀察，
 * 不再透過 callback 傳遞進度。
 */
interface PicsumImageAnalyzer {
    /**
     * @param photoId 做 cache key，相同 photoId 不重複分析
     * @param imageUrl 縮圖 URL（呼叫端決定大小，建議 ≤ 384 px）
     */
    suspend fun summarize(
        photoId: String,
        imageUrl: String,
    ): Result<String>
}
