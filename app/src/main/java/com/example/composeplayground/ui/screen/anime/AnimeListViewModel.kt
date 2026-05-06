package com.example.composeplayground.ui.screen.anime

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.composeplayground.data.model.Anime
import com.example.composeplayground.data.model.AnimeGenre
import com.example.composeplayground.data.paging.AnimePagingArgs
import com.example.composeplayground.data.paging.AnimePagingSource
import com.example.composeplayground.data.repository.AnimeOrderBy
import com.example.composeplayground.data.repository.AnimeRepository
import com.example.composeplayground.data.repository.SortDirection
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
import kotlinx.coroutines.launch

enum class AnimeViewMode { Grid, List }

@Immutable
data class AnimeListUiState(
    val viewMode: AnimeViewMode = AnimeViewMode.Grid,
    val searchQuery: String = "",
    val selectedGenreIds: Set<Int> = emptySet(),
    val orderBy: AnimeOrderBy = AnimeOrderBy.Score,
    val sortDirection: SortDirection = SortDirection.Desc,
    val genres: List<AnimeGenre> = emptyList(),
    val genresError: String? = null,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class AnimeListViewModel(
    private val repository: AnimeRepository,
) : ViewModel() {

    val uiState: StateFlow<AnimeListUiState>
        field = MutableStateFlow(AnimeListUiState())

    val animePagingFlow: Flow<PagingData<Anime>> = combine(
        uiState.map { it.searchQuery }.distinctUntilChanged().debounce(SEARCH_DEBOUNCE_MS),
        uiState.map { it.selectedGenreIds }.distinctUntilChanged(),
        uiState.map { it.orderBy }.distinctUntilChanged(),
        uiState.map { it.sortDirection }.distinctUntilChanged(),
    ) { query, genres, orderBy, dir ->
        AnimePagingArgs(
            query = query,
            genreIds = genres,
            orderBy = orderBy,
            sortDirection = dir,
        )
    }.flatMapLatest { args ->
        Pager(
            config = PagingConfig(
                pageSize = AnimePagingSource.PAGE_SIZE,
                prefetchDistance = PREFETCH_DISTANCE,
                // Jikan 有 rate limit，刻意讓 initialLoadSize == pageSize，
                // 避免初次載入觸發兩次 API 呼叫。
                initialLoadSize = AnimePagingSource.PAGE_SIZE,
                enablePlaceholders = false,
            ),
        ) { AnimePagingSource(repository, args) }.flow
    }.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            runCatching { repository.fetchGenres() }
                .onSuccess { genres -> uiState.update { it.copy(genres = genres, genresError = null) } }
                .onFailure { e ->
                    uiState.update {
                        it.copy(genresError = e.localizedMessage ?: "Failed to load genres")
                    }
                }
        }
    }

    fun toggleViewMode() {
        uiState.update {
            it.copy(
                viewMode = if (it.viewMode == AnimeViewMode.Grid) AnimeViewMode.List else AnimeViewMode.Grid,
            )
        }
    }

    fun updateSearchQuery(query: String) {
        uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleGenre(genreId: Int) {
        uiState.update { current ->
            val next = current.selectedGenreIds.toMutableSet().apply {
                if (!add(genreId)) remove(genreId)
            }
            current.copy(selectedGenreIds = next)
        }
    }

    fun clearGenres() {
        uiState.update { it.copy(selectedGenreIds = emptySet()) }
    }

    fun setOrderBy(orderBy: AnimeOrderBy) {
        uiState.update { it.copy(orderBy = orderBy) }
    }

    fun toggleSortDirection() {
        uiState.update {
            it.copy(
                sortDirection = if (it.sortDirection == SortDirection.Desc) SortDirection.Asc else SortDirection.Desc,
            )
        }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L
        private const val PREFETCH_DISTANCE = 10
    }
}
