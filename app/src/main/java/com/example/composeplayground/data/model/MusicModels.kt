package com.example.composeplayground.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

// ── DTO（iTunes Search API 回應結構）────────────────────────────────────────────
// ContentNegotiation 設定為 ignoreUnknownKeys = true，故未列出的欄位會被靜默忽略。

@Serializable
data class ItunesSearchResponse(
    val resultCount: Int = 0,
    val results: List<TrackDto> = emptyList(),
)

@Serializable
data class TrackDto(
    val trackId: Long? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val collectionName: String? = null,
    val artworkUrl100: String? = null,
    val previewUrl: String? = null,
    val trackTimeMillis: Long? = null,
    val releaseDate: String? = null,
    val primaryGenreName: String? = null,
)

// ── DTO（Apple Music 排行榜 RSS Feed 回應結構）──────────────────────────────────
// 只用來取得當日熱門曲目的 id 排序；曲目完整欄位（含 previewUrl）另以
// iTunes lookup API 批次補齊，見 MusicRepositoryImpl.fetchDailyCharts。

@Serializable
data class ChartsFeedResponse(val feed: ChartsFeedDto = ChartsFeedDto())

@Serializable
data class ChartsFeedDto(val results: List<ChartEntryDto> = emptyList())

@Serializable
data class ChartEntryDto(val id: String? = null)

// ── Domain Model ──────────────────────────────────────────────────────────────

@Immutable
data class Track(
    val id: Long,
    val trackName: String,
    val artistName: String,
    val collectionName: String?,
    val artworkUrl: String,
    val previewUrl: String?,
    val trackTimeMillis: Long?,
    val releaseDate: String?,
    val genre: String?,
)

@Immutable
data class TrackPage(
    val tracks: List<Track>,
    val hasNext: Boolean,
)

// ── DTO → Domain 映射 ─────────────────────────────────────────────────────────

/** iTunes 偶爾回傳缺少關鍵欄位的項目，回傳 null 由呼叫端過濾而非拋例外。 */
fun TrackDto.toTrack(): Track? {
    val id = trackId ?: return null
    if (trackName.isNullOrBlank() || artistName.isNullOrBlank()) return null
    return Track(
        id = id,
        trackName = trackName,
        artistName = artistName,
        collectionName = collectionName,
        artworkUrl = artworkUrl100.orEmpty(),
        previewUrl = previewUrl,
        trackTimeMillis = trackTimeMillis,
        releaseDate = releaseDate,
        genre = primaryGenreName,
    )
}
