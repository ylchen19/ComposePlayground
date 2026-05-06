package com.example.composeplayground.ui.screen.anime

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.composeplayground.data.model.AnimeCharacter
import com.example.composeplayground.data.model.AnimeDetail
import com.example.composeplayground.data.model.AnimeRecommendation
import com.example.composeplayground.data.repository.AnimeRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface AnimeDetailUiState {
    data object Loading : AnimeDetailUiState

    @Immutable
    data class Success(
        val detail: AnimeDetail,
        val characters: List<AnimeCharacter> = emptyList(),
        val recommendations: List<AnimeRecommendation> = emptyList(),
    ) : AnimeDetailUiState

    data class Error(val message: String) : AnimeDetailUiState
}

class AnimeDetailViewModel(
    private val animeId: Int,
    private val repository: AnimeRepository,
) : ViewModel() {

    val uiState: StateFlow<AnimeDetailUiState>
        field = MutableStateFlow<AnimeDetailUiState>(AnimeDetailUiState.Loading)

    init {
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        uiState.value = AnimeDetailUiState.Loading
        viewModelScope.launch {
            try {
                val detailDeferred = async { repository.fetchAnimeDetail(animeId) }
                // 角色 / 推薦失敗不應阻擋整個詳細頁，使用 runCatching 降級成空列表。
                val charactersDeferred = async {
                    runCatching { repository.fetchAnimeCharacters(animeId) }.getOrDefault(emptyList())
                }
                val recommendationsDeferred = async {
                    runCatching { repository.fetchAnimeRecommendations(animeId) }.getOrDefault(emptyList())
                }

                val detail = detailDeferred.await()
                val characters = charactersDeferred.await()
                val recommendations = recommendationsDeferred.await()

                uiState.value = AnimeDetailUiState.Success(
                    detail = detail,
                    characters = characters.take(MAX_CHARACTERS),
                    recommendations = recommendations.take(MAX_RECOMMENDATIONS),
                )
            } catch (e: Exception) {
                uiState.value = AnimeDetailUiState.Error(
                    e.localizedMessage ?: "Failed to load anime details",
                )
            }
        }
    }

    companion object {
        private const val MAX_CHARACTERS = 20
        private const val MAX_RECOMMENDATIONS = 12
    }
}
