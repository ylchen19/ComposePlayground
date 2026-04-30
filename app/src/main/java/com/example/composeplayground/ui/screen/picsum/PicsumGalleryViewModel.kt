package com.example.composeplayground.ui.screen.picsum

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.composeplayground.data.model.PicsumPhoto
import com.example.composeplayground.data.paging.PicsumPagingSource
import com.example.composeplayground.data.repository.PicsumRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

enum class PicsumViewMode { Grid, List, StaggeredGrid }

@Immutable
data class PicsumGalleryUiState(
    val viewMode: PicsumViewMode = PicsumViewMode.Grid,
)

class PicsumGalleryViewModel(
    private val repository: PicsumRepository,
) : ViewModel() {

    private val randomSeed: Long = Random.nextLong()

    val uiState: StateFlow<PicsumGalleryUiState>
        field = MutableStateFlow(PicsumGalleryUiState())

    val photos: Flow<PagingData<PicsumPhoto>> = Pager(
        config = PagingConfig(
            pageSize = PicsumPagingSource.PAGE_SIZE,
            prefetchDistance = 10,
            initialLoadSize = PicsumPagingSource.PAGE_SIZE,
            enablePlaceholders = false,
        ),
    ) {
        PicsumPagingSource(repository, randomSeed)
    }.flow.cachedIn(viewModelScope)

    fun cycleViewMode() {
        uiState.update {
            val next = when (it.viewMode) {
                PicsumViewMode.Grid -> PicsumViewMode.List
                PicsumViewMode.List -> PicsumViewMode.StaggeredGrid
                PicsumViewMode.StaggeredGrid -> PicsumViewMode.Grid
            }
            it.copy(viewMode = next)
        }
    }
}
