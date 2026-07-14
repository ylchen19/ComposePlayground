package com.example.composeplayground.data.repository

import com.example.composeplayground.data.model.Track
import com.example.composeplayground.data.model.TrackPage

/**
 * 音樂搜尋功能的資料層介面，由 [MusicRepositoryImpl] 串接 iTunes Search API。
 *
 * iTunes 限制：`limit` 建議 ≤ 25，`offset + limit` 不可超過 [MAX_RESULTS]（API 硬上限 200）。
 * Repository 不暴露 DTO，回傳 domain model。
 */
interface MusicRepository {

    /**
     * 搜尋歌曲。
     *
     * @param term 搜尋關鍵字（歌手 / 歌曲 / 專輯名稱），空字串不應呼叫
     * @param offset 起始位移（0-indexed）
     * @param limit 筆數上限，建議使用 [MAX_PAGE_SIZE]
     * @param genre 曲風篩選；[MusicGenre.All] 表示不篩選
     * @param region 地區／語言篩選，對應 iTunes storefront
     */
    suspend fun searchTracks(term: String, offset: Int, limit: Int, genre: MusicGenre, region: MusicRegion): TrackPage

    /**
     * 取得當日 Apple Music 排行榜（依熱門程度排序），供未輸入搜尋字時的推薦清單使用。
     *
     * @param limit 榜單筆數上限，建議使用 [DAILY_CHARTS_LIMIT]
     * @param genre 曲風篩選；[MusicGenre.All] 表示不篩選
     * @param region 地區／語言篩選，對應 iTunes storefront
     */
    suspend fun fetchDailyCharts(limit: Int, genre: MusicGenre, region: MusicRegion): List<Track>

    companion object {
        const val MAX_PAGE_SIZE = 25
        const val MAX_RESULTS = 200
        const val DAILY_CHARTS_LIMIT = 50
    }
}

/**
 * 曲風篩選。iTunes Search API 的 `genreId` 參數實測對 `/search` 與排行榜 RSS Feed
 * 皆無效（server 端會忽略），故改以回傳結果的 `primaryGenreName` 在 client 端比對篩選
 * （見 [MusicRepositoryImpl]）。[itunesGenreName] 對應 iTunes 回傳的英文曲風名稱。
 */
enum class MusicGenre(val itunesGenreName: String?) {
    All(null),
    Pop("Pop"),
    Rock("Rock"),
    HipHopRap("Hip-Hop/Rap"),
    Country("Country"),
    Alternative("Alternative"),
    RnbSoul("R&B/Soul"),
    Electronic("Electronic"),
    Jazz("Jazz"),
    Classical("Classical"),
    Latin("Latin"),
    World("World"),
    Reggae("Reggae"),
}

/**
 * 地區／語言篩選，以 iTunes storefront（[storefront]，`country` 查詢參數）作為代理——
 * iTunes API 沒有真正的「語言」欄位，但切換 storefront 會改變該地區的主流語言曲目內容。
 */
enum class MusicRegion(val storefront: String) {
    Global("us"),
    Taiwan("tw"),
    Japan("jp"),
    Korea("kr"),
    France("fr"),
    Germany("de"),
    Spain("es"),
}
