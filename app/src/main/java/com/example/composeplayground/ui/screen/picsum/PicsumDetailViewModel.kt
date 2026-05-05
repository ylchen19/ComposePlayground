package com.example.composeplayground.ui.screen.picsum

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.composeplayground.data.analyzer.GeminiNanoModelManager
import com.example.composeplayground.data.analyzer.GeminiNanoPicsumImageAnalyzer
import com.example.composeplayground.data.analyzer.ModelStatus
import com.example.composeplayground.data.analyzer.PicsumImageAnalyzer
import com.example.composeplayground.data.model.PicsumPhoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
sealed interface ImageSummaryState {
    data object Idle : ImageSummaryState
    data object Loading : ImageSummaryState
    /** Gemini Nano 模型下載中；[progress] 為 0f..1f，totalBytes 未知時維持 0f */
    @Immutable data class Downloading(val progress: Float) : ImageSummaryState
    @Immutable data class Success(val description: String) : ImageSummaryState
    @Immutable data class Error(val message: String) : ImageSummaryState
}

@Immutable
data class PicsumDetailUiState(
    val photo: PicsumPhoto,
    val summary: ImageSummaryState = ImageSummaryState.Idle,
)

/**
 * 詳細頁 ViewModel。所需欄位由導航層透過 NavKey 直接攜帶（id/author/width/height），
 * 不再回打 API，避免單張圖片再做一次列表查詢。
 *
 * 圖片描述在 [init] 交給 [PicsumImageAnalyzer]。模型下載狀態統一由
 * [GeminiNanoModelManager] 觀察——若使用者已在 Settings 預先下載完成，
 * 詳細頁直接顯示分析結果；若尚未下載，會在分析期間自動觸發下載並顯示進度。
 */
class PicsumDetailViewModel(
    photoId: String,
    author: String,
    originalWidth: Int,
    originalHeight: Int,
    private val analyzer: PicsumImageAnalyzer,
    private val modelManager: GeminiNanoModelManager,
) : ViewModel() {

    private val photo = PicsumPhoto(
        id = photoId,
        author = author,
        originalWidth = originalWidth,
        originalHeight = originalHeight,
        sourceUrl = "",
    )

    /** Analyzer 本身的進度（不含模型下載），由 init 流程更新 */
    private val analysisState = MutableStateFlow<ImageSummaryState>(ImageSummaryState.Idle)

    /**
     * 對外狀態：當分析仍在 Loading 期間，若 manager 正在下載模型，
     * 顯示 Downloading 進度；否則照分析狀態顯示。
     * 分析完成（Success/Error）後 manager 狀態不再影響顯示。
     */
    val uiState: StateFlow<PicsumDetailUiState> =
        combine(analysisState, modelManager.status) { local, model ->
            val summary = when {
                local is ImageSummaryState.Loading && model is ModelStatus.Downloading ->
                    ImageSummaryState.Downloading(model.progress)
                else -> local
            }
            PicsumDetailUiState(photo = photo, summary = summary)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = PicsumDetailUiState(photo = photo),
        )

    init {
        viewModelScope.launch {
            analysisState.value = ImageSummaryState.Loading
            val url = photo.thumbnailUrl(ANALYZE_THUMB_SIZE)
            analyzer.summarize(photoId = photo.id, imageUrl = url)
                .onSuccess { analysisState.value = ImageSummaryState.Success(it) }
                .onFailure { analysisState.value = ImageSummaryState.Error(it.message.orEmpty()) }
        }
    }

    private companion object {
        // Gemini Nano / ML Kit 的輸入解析度需求都低於原圖；384 px 已足夠
        const val ANALYZE_THUMB_SIZE = GeminiNanoPicsumImageAnalyzer.ANALYZE_THUMB_SIZE
    }
}
