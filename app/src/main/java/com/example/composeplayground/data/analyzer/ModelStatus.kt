package com.example.composeplayground.data.analyzer

import androidx.compose.runtime.Immutable

/**
 * Gemini Nano 模型在裝置上的狀態。由 [GeminiNanoModelManager] 對外暴露為 StateFlow，
 * Settings 與 PicsumDetail 共享同一份狀態。
 */
@Immutable
sealed interface ModelStatus {
    /** 尚未呼叫 checkStatus()，初始狀態 */
    data object Unknown : ModelStatus

    /** 裝置不支援 Gemini Nano（無 AICore 或硬體不符） */
    data object NotSupported : ModelStatus

    /** 可下載但尚未下載 */
    data object Downloadable : ModelStatus

    /** 下載進行中；progress 為 0f..1f，總大小未知時維持 0f */
    @Immutable
    data class Downloading(val progress: Float) : ModelStatus

    /** 模型已就緒，可用於推論 */
    data object Ready : ModelStatus

    /** 下載或檢查失敗 */
    @Immutable
    data class Failed(val message: String) : ModelStatus
}
