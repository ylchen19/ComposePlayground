package com.example.composeplayground.ui.screen.picsum

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.composeplayground.data.model.PicsumPhoto
import com.example.composeplayground.data.paging.InMemoryPicsumPagingSource
import com.example.composeplayground.data.paging.PicsumPagingSource
import com.example.composeplayground.data.repository.PicsumRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlin.random.Random

enum class PicsumViewMode { Grid, List, StaggeredGrid }

/**
 * 排序模式。
 * - [Random]：使用原本的 lazy paging（每頁從 API 隨機抓），首次顯示瞬時。
 * - [ResolutionDesc] / [ResolutionAsc]：先一次性載入全部相片並按 width × height 排序，
 *   首次切換時會有一段「載入全部」的等待。
 */
enum class PicsumSortMode { Random, ResolutionDesc, ResolutionAsc, AuthorAsc }

@Immutable
data class SortLoadProgress(
    val loadedPages: Int,
    val totalPagesEstimate: Int?,
)

@Immutable
data class PicsumGalleryUiState(
    val viewMode: PicsumViewMode = PicsumViewMode.Grid,
    val sortMode: PicsumSortMode = PicsumSortMode.Random,
    val isLoadingAllForSort: Boolean = false,
    val sortLoadProgress: SortLoadProgress? = null,
    val sortLoadError: String? = null,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val authorSuggestions: List<String> = emptyList(),
)

class PicsumGalleryViewModel(
    private val repository: PicsumRepository,
) : ViewModel() {

    private val randomSeed: Long = Random.nextLong()

    val uiState: StateFlow<PicsumGalleryUiState>
        field = MutableStateFlow(PicsumGalleryUiState())

    private val sortMode: MutableStateFlow<PicsumSortMode> = MutableStateFlow(PicsumSortMode.Random)
    private val searchQuery: MutableStateFlow<String> = MutableStateFlow("")
    private val isSearchActive: MutableStateFlow<Boolean> = MutableStateFlow(false)

    // 完整相片列表的單次載入快取，避免在排序模式間切換時重複拉取
    private val allPhotosMutex = Mutex()
    @Volatile private var allPhotosCache: List<PicsumPhoto>? = null
    @Volatile private var allAuthorsCache: List<String> = emptyList()

    private val pagingConfig = PagingConfig(
        pageSize = PicsumPagingSource.PAGE_SIZE,
        prefetchDistance = 10,
        initialLoadSize = PicsumPagingSource.PAGE_SIZE,
        enablePlaceholders = false,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val photos: Flow<PagingData<PicsumPhoto>> = kotlinx.coroutines.flow.combine(
        sortMode,
        searchQuery,
        isSearchActive
    ) { sort, query, searchActive ->
        Triple(sort, query, searchActive)
    }.flatMapLatest { (mode, query, searchActive) ->
        val needsAllData = mode != PicsumSortMode.Random || query.isNotEmpty() || searchActive

        if (!needsAllData) {
            Pager(pagingConfig) {
                PicsumPagingSource(repository, randomSeed)
            }.flow
        } else {
            flow {
                setLoadingAll(true)
                clearSortError()
                try {
                    val all = loadAllPhotos()
                    var filtered = all
                    if (query.isNotEmpty()) {
                        filtered = all.filter { it.author.contains(query, ignoreCase = true) }
                    }
                    val sorted = when (mode) {
                        PicsumSortMode.ResolutionDesc -> filtered.sortedByDescending { it.pixelCount }
                        PicsumSortMode.ResolutionAsc -> filtered.sortedBy { it.pixelCount }
                        PicsumSortMode.AuthorAsc -> filtered.sortedBy { it.author }
                        PicsumSortMode.Random -> filtered
                    }
                    // 更新建議列表
                    val suggestions = if (query.isEmpty()) {
                         allAuthorsCache
                    } else {
                         allAuthorsCache.filter { it.contains(query, ignoreCase = true) }
                    }
                    uiState.update { it.copy(authorSuggestions = suggestions) }

                    // 資料就緒，先關掉遮罩再開始 emit Pager.flow（hot flow，永不完成）
                    setLoadingAll(false)
                    emitAll(Pager(pagingConfig) { InMemoryPicsumPagingSource(sorted) }.flow)
                } catch (ce: kotlinx.coroutines.CancellationException) {
                    throw ce
                } catch (e: Throwable) {
                    setSortError(e.localizedMessage ?: "載入全部相片失敗")
                    emit(PagingData.empty())
                } finally {
                    // 錯誤/取消路徑的兜底
                    setLoadingAll(false)
                }
            }
        }
    }.cachedIn(viewModelScope)

    fun toggleSearchActive() {
        val current = isSearchActive.value
        val next = !current
        isSearchActive.value = next
        uiState.update { it.copy(isSearchActive = next) }
        if (!next) {
            searchQuery.value = ""
            uiState.update { it.copy(searchQuery = "") }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
        uiState.update { it.copy(searchQuery = query) }
    }

    fun selectAuthor(author: String) {
        searchQuery.value = author
        uiState.update { it.copy(searchQuery = author) }
    }

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

    fun setSortMode(mode: PicsumSortMode) {
        if (uiState.value.sortMode == mode) return
        uiState.update { it.copy(sortMode = mode) }
        sortMode.value = mode
    }

    private fun setLoadingAll(loading: Boolean) {
        uiState.update {
            it.copy(
                isLoadingAllForSort = loading,
                sortLoadProgress = if (loading) SortLoadProgress(0, null) else null,
            )
        }
    }

    private fun setLoadProgress(loadedPages: Int, totalPagesEstimate: Int?) {
        uiState.update { it.copy(sortLoadProgress = SortLoadProgress(loadedPages, totalPagesEstimate)) }
    }

    private fun setSortError(message: String) {
        uiState.update { it.copy(sortLoadError = message) }
    }

    private fun clearSortError() {
        uiState.update { if (it.sortLoadError == null) it else it.copy(sortLoadError = null) }
    }

    fun dismissSortError() = clearSortError()

    private suspend fun loadAllPhotos(): List<PicsumPhoto> = allPhotosMutex.withLock {
        allPhotosCache ?: run {
            val accumulated = mutableListOf<PicsumPhoto>()
            val semaphore = Semaphore(BULK_CONCURRENCY)
            var nextPage = 1
            var reachedEnd = false
            // 批次平行抓取：每批送 BULK_CONCURRENCY 個 page，全部回來後判斷是否到尾。
            // 任一頁回傳空清單 → 後續不再送出新批次。
            while (!reachedEnd && nextPage <= MAX_BULK_PAGES) {
                val batchEnd = minOf(nextPage + BULK_CONCURRENCY - 1, MAX_BULK_PAGES)
                val pageRange = nextPage..batchEnd
                val results = coroutineScope {
                    pageRange.map { page ->
                        async {
                            semaphore.withPermit {
                                page to repository.fetchPhotos(page = page, limit = BULK_PAGE_LIMIT)
                            }
                        }
                    }.awaitAll()
                }.sortedBy { it.first }

                var goodPagesInBatch = 0
                for ((_, batch) in results) {
                    if (batch.isEmpty()) {
                        reachedEnd = true
                        break
                    }
                    accumulated.addAll(batch)
                    goodPagesInBatch++
                }
                // 若最後一個成功頁已短於 BULK_PAGE_LIMIT，後面必然是空頁，視為到尾。
                val lastGood = results.take(goodPagesInBatch).lastOrNull()?.second
                if (lastGood != null && lastGood.size < BULK_PAGE_LIMIT) {
                    reachedEnd = true
                }

                val totalLoadedPages = (nextPage - 1) + goodPagesInBatch
                val estimate = if (reachedEnd) totalLoadedPages else null
                setLoadProgress(loadedPages = totalLoadedPages, totalPagesEstimate = estimate)

                nextPage = batchEnd + 1
            }
            accumulated.toList().also { photos -> 
                allPhotosCache = photos
                allAuthorsCache = photos.map { it.author }.distinct().sorted()
            }
        }
    }

    private companion object {
        const val BULK_PAGE_LIMIT = 100
        const val MAX_BULK_PAGES = 30
        const val BULK_CONCURRENCY = 6
    }
}
