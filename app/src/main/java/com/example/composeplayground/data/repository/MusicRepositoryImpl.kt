package com.example.composeplayground.data.repository

import com.example.composeplayground.data.model.ChartsFeedResponse
import com.example.composeplayground.data.model.ItunesSearchResponse
import com.example.composeplayground.data.model.Track
import com.example.composeplayground.data.model.TrackPage
import com.example.composeplayground.data.model.toTrack
import com.example.composeplayground.network.NetworkResult
import com.example.composeplayground.network.api.ApiService
import com.example.composeplayground.network.api.get

/**
 * @param apiService iTunes Search/Lookup API（`https://itunes.apple.com/`）
 * @param chartsApiService Apple Music 排行榜 RSS Feed（`https://rss.marketingtools.apple.com/`）
 */
class MusicRepositoryImpl(
    private val apiService: ApiService,
    private val chartsApiService: ApiService,
) : MusicRepository {

    override suspend fun searchTracks(
        term: String,
        offset: Int,
        limit: Int,
        genre: MusicGenre,
        region: MusicRegion,
    ): TrackPage {
        val boundedLimit = limit.coerceAtMost(MusicRepository.MAX_PAGE_SIZE)
        val params = mapOf(
            "term" to term,
            "media" to "music",
            "entity" to "song",
            "limit" to boundedLimit.toString(),
            "offset" to offset.toString(),
            "country" to region.storefront,
        )
        val response = apiService.get<ItunesSearchResponse>(
            endpoint = "search",
            queryParams = params,
        ).getOrThrow()
        // genreId 查詢參數對 /search 實測無效（server 端忽略），改在 client 端依
        // primaryGenreName 過濾；hasNext 仍以「過濾前」的原始筆數判斷是否還有下一頁，
        // 否則篩選後筆數變少會誤判為已無更多資料。
        val rawTracks = response.results.mapNotNull { it.toTrack() }
        val filtered = rawTracks.filterByGenre(genre)
        return TrackPage(
            tracks = filtered,
            hasNext = rawTracks.size == boundedLimit && offset + boundedLimit < MusicRepository.MAX_RESULTS,
        )
    }

    override suspend fun fetchDailyCharts(limit: Int, genre: MusicGenre, region: MusicRegion): List<Track> {
        val feed = chartsApiService.get<ChartsFeedResponse>(
            endpoint = "api/v2/${region.storefront}/music/most-played/$limit/songs.json",
        ).getOrThrow()
        val rankedIds = feed.feed.results.mapNotNull { it.id?.toLongOrNull() }
        if (rankedIds.isEmpty()) return emptyList()

        // RSS feed 只給 id 排名，不含 previewUrl；用 iTunes lookup 一次批次補齊完整欄位。
        // lookup 需帶相同 storefront，否則其他地區的曲目 id 可能查無資料或缺少試聽連結。
        val lookup = apiService.get<ItunesSearchResponse>(
            endpoint = "lookup",
            queryParams = mapOf("id" to rankedIds.joinToString(","), "country" to region.storefront),
        ).getOrThrow()
        val tracksById = lookup.results.mapNotNull { it.toTrack() }.associateBy { it.id }

        // lookup 回傳順序不保證與排行榜名次一致，依原始排名重新排序後再套用曲風篩選。
        return rankedIds.mapNotNull { tracksById[it] }.filterByGenre(genre)
    }

    private fun List<Track>.filterByGenre(genre: MusicGenre): List<Track> {
        val target = genre.itunesGenreName ?: return this
        return filter { it.genre.equals(target, ignoreCase = true) }
    }
}

private fun <T> NetworkResult<T>.getOrThrow(): T = when (this) {
    is NetworkResult.Success -> data
    is NetworkResult.Error -> throw Exception(message ?: "Network error (code=$code)")
    is NetworkResult.Loading -> error("Unexpected Loading state in repository")
}
