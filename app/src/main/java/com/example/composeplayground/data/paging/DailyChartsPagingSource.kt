package com.example.composeplayground.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.composeplayground.data.model.Track
import com.example.composeplayground.data.repository.MusicGenre
import com.example.composeplayground.data.repository.MusicRegion
import com.example.composeplayground.data.repository.MusicRepository

/**
 * 每日排行榜為固定筆數的單頁資料（不像 [MusicPagingSource] 支援無限捲動），
 * nextKey 恆為 null，載入一次後捲到底即自然停止。
 */
class DailyChartsPagingSource(
    private val repository: MusicRepository,
    private val genre: MusicGenre,
    private val region: MusicRegion,
) : PagingSource<Int, Track>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Track> {
        return try {
            val tracks = repository.fetchDailyCharts(
                limit = MusicRepository.DAILY_CHARTS_LIMIT,
                genre = genre,
                region = region,
            )
            LoadResult.Page(data = tracks, prevKey = null, nextKey = null)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Track>): Int? = null
}
