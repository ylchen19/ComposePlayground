package com.example.composeplayground.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.composeplayground.data.model.Anime
import com.example.composeplayground.data.repository.AnimeOrderBy
import com.example.composeplayground.data.repository.AnimeRepository
import com.example.composeplayground.data.repository.SortDirection

/**
 * 動畫列表的伺服器端分頁 PagingSource。
 *
 * 直接使用 Jikan 的 `page` / `limit` / `q` / `genres` / `order_by` / `sort` 參數，
 * 不像 Picsum 那樣 bulk-load — Jikan 上限 25/頁且有 rate limit，無法承受批次抓取。
 */
data class AnimePagingArgs(
    val query: String,
    val genreIds: Set<Int>,
    val orderBy: AnimeOrderBy,
    val sortDirection: SortDirection,
)

class AnimePagingSource(
    private val repository: AnimeRepository,
    private val args: AnimePagingArgs,
) : PagingSource<Int, Anime>() {

    private val seenIds = mutableSetOf<Int>()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Anime> {
        val page = params.key ?: 1
        return try {
            // 永遠以 PAGE_SIZE（= Jikan 上限）為 limit；不能用 params.loadSize，
            // 否則初次載入會放大成 initialLoadSize 觸發跨頁重複/超出 API 限制。
            val result = repository.fetchAnime(
                page = page,
                limit = PAGE_SIZE,
                query = args.query.takeIf { it.isNotBlank() },
                genreIds = args.genreIds.toList(),
                orderBy = args.orderBy,
                sortDirection = args.sortDirection,
            )
            // Jikan API 在不同 page 間可能回傳重複 anime ID，按 ID 過濾
            val deduplicated = result.anime.filter { anime -> seenIds.add(anime.id) }
            LoadResult.Page(
                data = deduplicated,
                prevKey = if (page > 1) page - 1 else null,
                nextKey = if (result.hasNext && deduplicated.isNotEmpty()) page + 1 else null,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Anime>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

    companion object {
        const val PAGE_SIZE = AnimeRepository.MAX_PAGE_SIZE
    }
}
