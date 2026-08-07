package com.example.composeplayground.ui.screen.music

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.composeplayground.data.model.Album
import com.example.composeplayground.data.model.AlbumGenre
import com.example.composeplayground.data.model.RecommendationReason
import com.example.composeplayground.data.model.TasteProfile
import com.example.composeplayground.data.model.buildDeck
import com.example.composeplayground.data.model.buildGenreOptions
import com.example.composeplayground.data.model.explainRecommendation
import com.example.composeplayground.data.model.rankAlbums
import com.example.composeplayground.data.repository.AlbumRoulettePreferencesRepository
import com.example.composeplayground.data.repository.MusicRegion
import com.example.composeplayground.data.repository.MusicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class AlbumRouletteUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val deck: List<Album> = emptyList(),
    val availableGenres: List<AlbumGenre> = emptyList(),
    val excludedGenreIds: Set<String> = emptySet(),
    val excludedRegions: Set<MusicRegion> = emptySet(),
    val likedAlbums: List<Album> = emptyList(),
    val showLikedSheet: Boolean = false,
    /** 目前這張為何被推薦；樣本不足時為 null。 */
    val recommendationReason: RecommendationReason? = null,
    /** 已學習的滑動次數，讓使用者知道推薦有沒有東西可依據。 */
    val tasteObservationCount: Int = 0,
) {
    val topAlbum: Album? get() = deck.firstOrNull()

    /** 疊在上層卡片後面預先繪製，滑掉上一張時不會有空白閃爍。 */
    val nextAlbum: Album? get() = deck.getOrNull(1)

    val isDeckEmpty: Boolean get() = !isLoading && errorMessage == null && deck.isEmpty()
}

/**
 * 隨機專輯推薦。抓取未被排除地區的 Apple Music 專輯榜組成抽選池，以 Thompson sampling
 * 依使用者的左右滑歷史排序後逐張發牌（演算法見 `AlbumRecommender.kt`）。
 */
class AlbumRouletteViewModel(
    private val repository: MusicRepository,
    private val preferencesRepository: AlbumRoulettePreferencesRepository,
) : ViewModel() {

    private val pool = MutableStateFlow<List<Album>>(emptyList())
    private val seenIds = MutableStateFlow<Set<Long>>(emptySet())

    /** 已排序的待發牌堆；順序由 [rankAlbums] 決定，不再是單純的洗牌結果。 */
    private val orderedDeck = MutableStateFlow<List<Album>>(emptyList())

    /**
     * 學到的推薦偏好。記憶體副本是權威來源、以純函式即時更新，DataStore 只做寫回——
     * 否則每滑一張卡都要等寫入往返才能重排牌堆。
     */
    private val taste = MutableStateFlow(TasteProfile())

    private val screenState = MutableStateFlow(ScreenState())

    private var fetchJob: Job? = null

    val uiState: StateFlow<AlbumRouletteUiState> = combine(
        orderedDeck,
        pool,
        screenState,
        preferencesRepository.preferencesFlow,
        taste,
    ) { deck, pool, screen, preferences, taste ->
        AlbumRouletteUiState(
            isLoading = screen.isLoading,
            errorMessage = screen.errorMessage,
            deck = deck,
            availableGenres = buildGenreOptions(pool, preferences.excludedGenreIds),
            excludedGenreIds = preferences.excludedGenreIds,
            excludedRegions = preferences.excludedRegions,
            likedAlbums = preferences.likedAlbums,
            showLikedSheet = screen.showLikedSheet,
            recommendationReason = deck.firstOrNull()?.let { explainRecommendation(it, taste) },
            tasteObservationCount = taste.observationCount,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AlbumRouletteUiState())

    init {
        viewModelScope.launch {
            taste.value = preferencesRepository.preferencesFlow.first().taste
        }
        // 只有地區排除會改變抽選池；類別排除純粹是 client 端過濾，不需要重抓。
        viewModelScope.launch {
            preferencesRepository.preferencesFlow
                .map { it.excludedRegions }
                .distinctUntilChanged()
                .collect { excluded -> loadPool(MusicRegion.entries.filterNot { it in excluded }) }
        }
        viewModelScope.launch {
            preferencesRepository.preferencesFlow
                .map { it.excludedGenreIds }
                .distinctUntilChanged()
                .collect { excluded -> rebuildDeck(excluded) }
        }
    }

    fun skipTop() {
        val album = uiState.value.topAlbum ?: return
        learn { it.recordSkip(album) }
        advanceDeck()
    }

    fun likeTop() {
        val album = uiState.value.topAlbum ?: return
        learn { it.recordLike(album) }
        advanceDeck()
        viewModelScope.launch { preferencesRepository.like(album) }
    }

    fun reshuffle() {
        seenIds.value = emptySet()
        pool.update { it.shuffled() }
        rebuildDeck()
    }

    fun toggleGenreExclusion(genreId: String) {
        viewModelScope.launch { preferencesRepository.toggleGenreExclusion(genreId) }
    }

    fun toggleRegionExclusion(region: MusicRegion) {
        viewModelScope.launch { preferencesRepository.toggleRegionExclusion(region) }
    }

    fun clearGenreExclusions() {
        viewModelScope.launch { preferencesRepository.clearGenreExclusions() }
    }

    fun resetTaste() {
        taste.value = TasteProfile()
        viewModelScope.launch { preferencesRepository.saveTaste(TasteProfile()) }
        rebuildDeck()
    }

    fun unlike(albumId: Long) {
        viewModelScope.launch { preferencesRepository.unlike(albumId) }
    }

    fun openLikedSheet() {
        screenState.update { it.copy(showLikedSheet = true) }
    }

    fun closeLikedSheet() {
        screenState.update { it.copy(showLikedSheet = false) }
    }

    fun retry() {
        val excluded = uiState.value.excludedRegions
        loadPool(MusicRegion.entries.filterNot { it in excluded })
    }

    private fun learn(update: (TasteProfile) -> TasteProfile) {
        taste.update(update)
        viewModelScope.launch { preferencesRepository.saveTaste(taste.value) }
    }

    /**
     * 滑掉目前這張後推進牌堆。
     *
     * **下一張已經以背景卡的形式露出過，所以固定不動**，只重排它後面的部分——否則剛學到的
     * 偏好會把使用者眼前看到的那張換掉，畫面會突兀地跳一下。
     */
    private fun advanceDeck() {
        val current = orderedDeck.value
        val album = current.firstOrNull() ?: return
        seenIds.update { it + album.id }

        val remaining = current.drop(1)
        val pinned = remaining.firstOrNull() ?: run {
            orderedDeck.value = emptyList()
            return
        }
        orderedDeck.value = listOf(pinned) + rankAlbums(remaining.drop(1), taste.value)
    }

    /** 整副重排。用於抽選池變動、排除條件變動、重新洗牌與重設偏好。 */
    private fun rebuildDeck(excludedGenreIds: Set<String> = uiState.value.excludedGenreIds) {
        orderedDeck.value = rankAlbums(
            albums = buildDeck(pool.value, excludedGenreIds, seenIds.value),
            taste = taste.value,
        )
    }

    private fun loadPool(regions: List<MusicRegion>) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            screenState.update { it.copy(isLoading = true, errorMessage = null) }
            val results = coroutineScope {
                regions
                    .map { region ->
                        async { runCatching { repository.fetchTopAlbums(region, MusicRepository.TOP_ALBUMS_LIMIT) } }
                    }
                    .awaitAll()
            }
            val albums = results.mapNotNull { it.getOrNull() }.flatten()
            if (albums.isEmpty()) {
                screenState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = results.firstNotNullOfOrNull { result ->
                            result.exceptionOrNull()?.localizedMessage
                        } ?: EMPTY_POOL_MESSAGE,
                    )
                }
                pool.value = emptyList()
                orderedDeck.value = emptyList()
                return@launch
            }
            // 同一張專輯可能同時出現在多個地區榜上；洗牌決定同分專輯的先後（rankAlbums 是
            // stable sort，會保留這裡的隨機順序）。
            seenIds.value = emptySet()
            pool.value = albums.distinctBy { it.id }.shuffled()
            rebuildDeck()
            screenState.update { it.copy(isLoading = false, errorMessage = null) }
        }
    }

    private data class ScreenState(
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
        val showLikedSheet: Boolean = false,
    )

    private companion object {
        const val EMPTY_POOL_MESSAGE = "Apple Music 專輯榜目前沒有資料"
    }
}
