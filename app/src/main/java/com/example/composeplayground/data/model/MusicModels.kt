package com.example.composeplayground.data.model

import androidx.compose.runtime.Immutable
import com.example.composeplayground.data.repository.MusicRegion
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

// ── DTO（Apple Music 專輯排行榜 RSS Feed 回應結構）──────────────────────────────
// 與上面的 songs feed 同一個 host 但 payload 完整得多：專輯 feed 直接帶回名稱、
// 藝人、封面與 genres，故不需要像 fetchDailyCharts 那樣再打一次 lookup 補欄位。

@Serializable
data class AlbumFeedResponse(val feed: AlbumFeedDto = AlbumFeedDto())

@Serializable
data class AlbumFeedDto(val results: List<AlbumEntryDto> = emptyList())

@Serializable
data class AlbumEntryDto(
    val id: String? = null,
    val name: String? = null,
    val artistName: String? = null,
    val artistId: String? = null,
    val artworkUrl100: String? = null,
    val releaseDate: String? = null,
    val url: String? = null,
    val genres: List<AlbumGenreDto> = emptyList(),
)

@Serializable
data class AlbumGenreDto(
    val genreId: String? = null,
    val name: String? = null,
)

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

/**
 * 專輯類別。以 [id]（iTunes genreId）而非 [label] 作為識別，因為**類別名稱會隨
 * storefront 在地化**——genreId 14 在 us 是 "Pop"、tw 是「流行樂」、jp 是「ポップ」、
 * kr 是「팝」，只有 genreId 跨區穩定。
 */
@Immutable
data class AlbumGenre(
    val id: String,
    val label: String,
)

@Immutable
data class Album(
    val id: Long,
    val name: String,
    val artistName: String,
    val artistId: String?,
    val artworkUrl: String,
    val releaseDate: String?,
    val appleMusicUrl: String?,
    val genres: List<AlbumGenre>,
    val region: MusicRegion,
    /** 在該地區榜單中的名次（0 起算），推薦排序拿來當熱門度先驗。 */
    val chartRank: Int,
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

/** 掛在幾乎每張專輯上的傘狀類別（us "Music" / tw「音樂」/ jp「ミュージック」）。 */
private const val UMBRELLA_GENRE_ID = "34"

fun AlbumEntryDto.toAlbum(region: MusicRegion, chartRank: Int): Album? {
    val albumId = id?.toLongOrNull() ?: return null
    if (name.isNullOrBlank() || artistName.isNullOrBlank()) return null
    return Album(
        id = albumId,
        name = name,
        artistName = artistName,
        artistId = artistId,
        artworkUrl = artworkUrl100.orEmpty().toHighResArtwork(),
        releaseDate = releaseDate,
        appleMusicUrl = url,
        // 濾掉傘狀類別，否則它會在排除清單裡變成一個「勾了就整副牌全空」的無用選項。
        genres = genres.mapNotNull { dto ->
            val genreId = dto.genreId?.takeIf { it != UMBRELLA_GENRE_ID } ?: return@mapNotNull null
            val label = dto.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            AlbumGenre(id = genreId, label = label)
        },
        region = region,
        chartRank = chartRank,
    )
}

/**
 * RSS feed 只給 100x100 縮圖，但推薦頁是整張大卡片。iTunes CDN 的圖片路徑允許直接
 * 替換尺寸片段取得高解析版本（非公開 API，故做字串比對而非無條件替換）。
 */
private fun String.toHighResArtwork(): String = replace("100x100bb", "600x600bb")
