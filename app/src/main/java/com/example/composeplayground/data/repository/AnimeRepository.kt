package com.example.composeplayground.data.repository

import com.example.composeplayground.data.model.AnimeCharacter
import com.example.composeplayground.data.model.AnimeDetail
import com.example.composeplayground.data.model.AnimeGenre
import com.example.composeplayground.data.model.AnimePage
import com.example.composeplayground.data.model.AnimeRecommendation

/**
 * Anime（動畫）功能的資料層介面，由 [AnimeRepositoryImpl] 串接 Jikan REST API v4。
 *
 * Jikan 限制：`limit` 上限 25，rate limit 為 3 req/s。
 * Repository 不暴露 DTO，所有方法皆回傳 domain model。
 */
interface AnimeRepository {

    /**
     * 取得動畫列表，支援搜尋、類型篩選與排序。
     *
     * @param page 頁數（1-indexed）
     * @param limit 每頁筆數，建議使用 [AnimeRepositoryImpl.MAX_PAGE_SIZE]（25）
     * @param query 標題關鍵字；空字串或 null 表示不過濾
     * @param genreIds 多選類型 ID（Jikan 以 CSV 串接後送出）
     * @param orderBy 排序欄位
     * @param sortDirection 升冪 / 降冪
     */
    suspend fun fetchAnime(
        page: Int,
        limit: Int,
        query: String?,
        genreIds: List<Int>,
        orderBy: AnimeOrderBy,
        sortDirection: SortDirection,
    ): AnimePage

    suspend fun fetchAnimeDetail(id: Int): AnimeDetail
    suspend fun fetchAnimeCharacters(id: Int): List<AnimeCharacter>
    suspend fun fetchAnimeRecommendations(id: Int): List<AnimeRecommendation>
    suspend fun fetchGenres(): List<AnimeGenre>

    companion object {
        const val MAX_PAGE_SIZE = 25
    }
}

/** Jikan `order_by` 的合法值子集，覆蓋常見排序需求。 */
enum class AnimeOrderBy(val apiValue: String) {
    Score("score"),
    Popularity("popularity"),
    Rank("rank"),
    Title("title"),
    StartDate("start_date"),
    Episodes("episodes"),
}

enum class SortDirection(val apiValue: String) {
    Asc("asc"),
    Desc("desc"),
}
