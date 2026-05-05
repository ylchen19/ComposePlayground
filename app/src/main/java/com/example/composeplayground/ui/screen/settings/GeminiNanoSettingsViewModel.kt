package com.example.composeplayground.ui.screen.settings

import androidx.lifecycle.ViewModel
import com.example.composeplayground.data.analyzer.GeminiNanoModelManager
import com.example.composeplayground.data.analyzer.ModelStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Settings 中 Gemini Nano 區塊的 ViewModel：
 * - 直接代理 [GeminiNanoModelManager.status] 作為 UI 狀態
 * - 將下載/刷新動作委派給 manager（singleton，全 app 共享）
 */
class GeminiNanoSettingsViewModel(
    private val manager: GeminiNanoModelManager,
) : ViewModel() {

    val uiState: StateFlow<ModelStatus> = manager.status

    /** 進入 Settings 時呼叫，刷新模型狀態（不觸發下載） */
    fun refresh() = manager.refresh()

    /** 使用者按下「下載」時呼叫；重複呼叫安全（manager 內部 Mutex 防護） */
    fun startDownload() = manager.startDownload()
}
