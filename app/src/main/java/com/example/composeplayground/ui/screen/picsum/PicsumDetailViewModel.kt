package com.example.composeplayground.ui.screen.picsum

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.composeplayground.data.analyzer.GeminiNanoPicsumImageAnalyzer
import com.example.composeplayground.data.analyzer.PicsumImageAnalyzer
import com.example.composeplayground.data.model.PicsumPhoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
sealed interface ImageSummaryState {
    data object Idle : ImageSummaryState
    data object Loading : ImageSummaryState
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
 * 圖片描述在 [init] 交給 [PicsumImageAnalyzer]——以縮圖為輸入、結果走
 * [ImageSummaryState] 暴露給 UI；analyzer 內部以 photoId 做 in-memory cache。
 */
class PicsumDetailViewModel(
    photoId: String,
    author: String,
    originalWidth: Int,
    originalHeight: Int,
    private val analyzer: PicsumImageAnalyzer,
) : ViewModel() {

    val uiState: StateFlow<PicsumDetailUiState>
        field = MutableStateFlow(
            PicsumDetailUiState(
                photo = PicsumPhoto(
                    id = photoId,
                    author = author,
                    originalWidth = originalWidth,
                    originalHeight = originalHeight,
                    sourceUrl = "",
                ),
            ),
        )

    init {
        viewModelScope.launch {
            uiState.update { it.copy(summary = ImageSummaryState.Loading) }
            val photo = uiState.value.photo
            val url = photo.thumbnailUrl(ANALYZE_THUMB_SIZE)
            analyzer.summarize(photo.id, url)
                .onSuccess { description ->
                    uiState.update { it.copy(summary = ImageSummaryState.Success(description)) }
                }
                .onFailure { e ->
                    uiState.update {
                        it.copy(summary = ImageSummaryState.Error(e.message.orEmpty()))
                    }
                }
        }
    }

    private companion object {
        // Gemini Nano / ML Kit 的輸入解析度需求都低於原圖；384 px 已足夠
        const val ANALYZE_THUMB_SIZE = GeminiNanoPicsumImageAnalyzer.ANALYZE_THUMB_SIZE
    }
}
