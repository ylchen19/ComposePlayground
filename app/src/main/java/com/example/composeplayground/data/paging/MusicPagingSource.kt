package com.example.composeplayground.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.composeplayground.data.model.Track
import com.example.composeplayground.data.repository.MusicGenre
import com.example.composeplayground.data.repository.MusicRegion
import com.example.composeplayground.data.repository.MusicRepository

data class MusicPagingArgs(
    val query: String,
    val genre: MusicGenre,
    val region: MusicRegion,
)

/**
 * 音樂搜尋列表的伺服器端分頁 PagingSource。
 *
 * 以 page（0-indexed）換算 iTunes 的 `offset` 參數，key 即為 page 數。
 */
class MusicPagingSource(
    private val repository: MusicRepository,
    private val args: MusicPagingArgs,
) : PagingSource<Int, Track>() {

    private val seenIds = mutableSetOf<Long>()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Track> {
        val page = params.key ?: 0
        return try {
            // 永遠以 PAGE_SIZE 為 limit；不能用 params.loadSize，否則初次載入會
            // 放大成 initialLoadSize 觸發跨頁重複。
            val result = repository.searchTracks(
                term = args.query,
                offset = page * PAGE_SIZE,
                limit = PAGE_SIZE,
                genre = args.genre,
                region = args.region,
            )
            // iTunes 在不同 offset 間可能回傳重複 track，按 ID 過濾
            val deduplicated = result.tracks.filter { track -> seenIds.add(track.id) }
            LoadResult.Page(
                data = deduplicated,
                prevKey = if (page > 0) page - 1 else null,
                // hasNext 以「過濾前」原始筆數判斷；曲風篩選可能讓某頁篩選後為空，
                // 但只要原始 API 還有下一頁就必須繼續嘗試，否則會誤判為已無更多資料。
                nextKey = if (result.hasNext) page + 1 else null,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Track>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

    companion object {
        const val PAGE_SIZE = MusicRepository.MAX_PAGE_SIZE
    }
}
