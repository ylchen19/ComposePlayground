package com.example.composeplayground.data.analyzer

import android.content.Context
import android.util.Log
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Gemini Nano 模型生命週期單一管理者（singleton）。
 *
 * - **狀態唯一來源**：[status] 由 Settings 與 PicsumDetail 共同觀察
 * - **避免重複下載**：[Mutex] + [Deferred] 確保 detail 與 settings 同時觸發只跑一次下載 job
 * - **獨立 scope**：使用 SupervisorJob + Dispatchers.Default，等同 application scope，
 *   不依賴 ViewModel 生命週期；因為下載一旦開始就應該跑完，使用者離開頁面也不該中斷
 *
 * 兩個對外 API：
 * - [ensureReady]：給 analyzer 用，回傳就緒模型；若需下載會自動觸發並等候
 * - [startDownload]：給 Settings 用，主動觸發下載但不等候完成
 */
class GeminiNanoModelManager(
    @Suppress("unused") private val appContext: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _status = MutableStateFlow<ModelStatus>(ModelStatus.Unknown)
    val status: StateFlow<ModelStatus> = _status.asStateFlow()

    private val mutex = Mutex()

    /** 共享下載 job：避免並發呼叫 ensureReady/startDownload 觸發雙下載 */
    private var inFlight: Deferred<GenerativeModel?>? = null

    /**
     * 給 analyzer 用：取得就緒模型，需要時自動下載並等候完成。
     * 不支援裝置回傳 null，由 analyzer 降到 ML Kit 模板。
     */
    suspend fun ensureReady(): GenerativeModel? = obtainOrShareJob().await()

    /**
     * 給 Settings 用：主動觸發下載，立即返回（不等候完成）。
     * 進度會反映在 [status] StateFlow，由 UI 觀察。
     */
    fun startDownload() {
        scope.launch {
            runCatching { obtainOrShareJob().await() }
                .onFailure { Log.w(TAG, "startDownload failed: ${it.message}") }
        }
    }

    /**
     * 從零檢查當前狀態（不觸發下載）。
     * Settings 進入時呼叫，更新 UI 顯示模型是否已下載。
     */
    fun refresh() {
        scope.launch {
            runCatching {
                val model = Generation.getClient()
                _status.value = when (model.checkStatus()) {
                    FeatureStatus.AVAILABLE -> ModelStatus.Ready
                    FeatureStatus.DOWNLOADABLE -> ModelStatus.Downloadable
                    FeatureStatus.UNAVAILABLE -> ModelStatus.NotSupported
                    else -> ModelStatus.Unknown
                }
            }.onFailure {
                Log.w(TAG, "refresh failed: ${it.message}")
                _status.value = ModelStatus.Failed(it.message.orEmpty())
            }
        }
    }

    private suspend fun obtainOrShareJob(): Deferred<GenerativeModel?> = mutex.withLock {
        val current = inFlight
        if (current != null && !current.isCompleted) return@withLock current
        val newJob = scope.async { performDownloadAndReady() }
        inFlight = newJob
        newJob
    }

    private suspend fun performDownloadAndReady(): GenerativeModel? {
        return try {
            val model = Generation.getClient()
            val featureStatus = model.checkStatus()
            Log.d(TAG, "Gemini Nano feature status: $featureStatus")
            when (featureStatus) {
                FeatureStatus.AVAILABLE -> {
                    _status.value = ModelStatus.Ready
                    model
                }
                FeatureStatus.DOWNLOADABLE -> {
                    _status.value = ModelStatus.Downloading(0f)
                    awaitDownload(model)
                    _status.value = ModelStatus.Ready
                    Log.d(TAG, "Gemini Nano download complete")
                    model
                }
                else -> {
                    _status.value = ModelStatus.NotSupported
                    Log.w(TAG, "Gemini Nano not available (status=$featureStatus)")
                    null
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            // AICore 內部 exception 無公開型別，但 message 含 "error code XXX"。
            // 把「裝置根本不支援」歸類為 NotSupported，讓 UI 顯示為灰色說明而非紅色錯誤。
            // 詳見 AICore 內部錯誤碼：606 FEATURE_NOT_FOUND（Samsung 等非 Pixel 裝置常見）、
            // 16 NOT_SUPPORTED、8 NOT_AVAILABLE
            val msg = e.message.orEmpty()
            val unsupported = listOf("606", "FEATURE_NOT_FOUND", "NOT_SUPPORTED", "NOT_AVAILABLE")
                .any { it in msg }
            _status.value = if (unsupported) ModelStatus.NotSupported else ModelStatus.Failed(msg)
            Log.w(TAG, "performDownloadAndReady failed: ${e.javaClass.simpleName}: $msg")
            null
        }
    }

    private suspend fun awaitDownload(model: GenerativeModel) {
        var totalBytes = 0L
        val result = model.download().first { status ->
            when (status) {
                is DownloadStatus.DownloadStarted -> {
                    totalBytes = status.bytesToDownload
                    false
                }
                is DownloadStatus.DownloadProgress -> {
                    if (totalBytes > 0L) {
                        val progress = (status.totalBytesDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f)
                        _status.value = ModelStatus.Downloading(progress)
                    }
                    false
                }
                is DownloadStatus.DownloadCompleted -> true
                is DownloadStatus.DownloadFailed -> true
            }
        }
        if (result is DownloadStatus.DownloadFailed) throw result.e
    }

    private companion object {
        const val TAG = "GeminiNanoManager"
    }
}
