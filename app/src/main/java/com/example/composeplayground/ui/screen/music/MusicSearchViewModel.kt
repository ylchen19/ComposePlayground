package com.example.composeplayground.ui.screen.music

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.composeplayground.data.model.Track
import com.example.composeplayground.data.paging.DailyChartsPagingSource
import com.example.composeplayground.data.paging.MusicPagingArgs
import com.example.composeplayground.data.paging.MusicPagingSource
import com.example.composeplayground.data.repository.MusicGenre
import com.example.composeplayground.data.repository.MusicRegion
import com.example.composeplayground.data.repository.MusicRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

@Immutable
data class MusicSearchUiState(
    val searchQuery: String = "",
    val selectedGenre: MusicGenre = MusicGenre.All,
    val selectedRegion: MusicRegion = MusicRegion.Global,
) {
    val isRecommendation: Boolean get() = searchQuery.isBlank()
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class MusicSearchViewModel(
    private val repository: MusicRepository,
) : ViewModel() {

    val uiState: StateFlow<MusicSearchUiState>
        field = MutableStateFlow(MusicSearchUiState())

    val tracksPagingFlow: Flow<PagingData<Track>> = combine(
        uiState.map { it.searchQuery }.distinctUntilChanged()
            // 未輸入時立即載入排行榜，不需等待 debounce
            .debounce { query -> if (query.isBlank()) 0L else SEARCH_DEBOUNCE_MS },
        uiState.map { it.selectedGenre }.distinctUntilChanged(),
        uiState.map { it.selectedRegion }.distinctUntilChanged(),
    ) { query, genre, region -> Triple(query, genre, region) }
        .flatMapLatest { (query, genre, region) ->
            Pager(
                config = PagingConfig(
                    pageSize = MusicPagingSource.PAGE_SIZE,
                    initialLoadSize = MusicPagingSource.PAGE_SIZE,
                    enablePlaceholders = false,
                ),
            ) {
                if (query.isBlank()) {
                    DailyChartsPagingSource(repository, genre, region)
                } else {
                    MusicPagingSource(repository, MusicPagingArgs(query, genre, region))
                }
            }.flow
        }
        .cachedIn(viewModelScope)

    fun updateSearchQuery(query: String) {
        uiState.update { it.copy(searchQuery = query) }
    }

    fun selectGenre(genre: MusicGenre) {
        uiState.update { it.copy(selectedGenre = genre) }
    }

    fun selectRegion(region: MusicRegion) {
        uiState.update { it.copy(selectedRegion = region) }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L
    }
}
