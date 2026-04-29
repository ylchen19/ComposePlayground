package com.example.composeplayground.ui.screen.picsum

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

class PicsumGalleryViewModel(
    private val repository: PicsumRepository,
) : ViewModel() {

    val photos: Flow<PagingData<PicsumPhoto>> = Pager(
        config = PagingConfig(
            pageSize = 30,
            prefetchDistance = 10,
            initialLoadSize = 60,
            enablePlaceholders = false,
        ),
    ) {
        PicsumPagingSource(repository)
    }.flow.cachedIn(viewModelScope)
}
