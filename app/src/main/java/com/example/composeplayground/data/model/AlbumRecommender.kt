package com.example.composeplayground.data.model

import androidx.compose.runtime.Immutable
import kotlin.random.Random

/**
 * 隨機專輯推薦的排序演算法。
 *
 * ## 為什麼是 Thompson sampling
 *
 * 這是典型的 multi-armed bandit 問題：每個類別是一支拉桿，右滑＝獎勵、左滑＝沒有獎勵，
 * 而我們並不知道使用者的真實喜好，只能邊推薦邊學。純貪婪（只推目前勝率最高的類別）會
 * 在前幾張卡就鎖死在某個類別上，永遠試不出其他可能喜歡的；純亂數則完全學不到東西。
 *
 * Thompson sampling 的做法是：為每個類別維護 Beta(1+喜歡, 1+跳過) 後驗分布，每次排序時
 * 從各分布**抽樣**一個親和度再排序。樣本少的類別後驗很寬、抽樣結果變異大，自然會不時
 * 被抽到高分而獲得曝光（探索）；樣本多且勝率高的類別則穩定拿高分（利用）。探索與利用
 * 的平衡是分布本身給的，不需要額外調參數。
 *
 * ## 分數組成
 *
 * `分數 = 類別親和度抽樣值 + 藝人加成 + 熱門度先驗`
 *
 * 類別親和度是主導項（值域 0..1），另外兩項刻意壓在小權重，只作為同分時的傾向。
 */

@Immutable
data class GenreStat(
    val likes: Int = 0,
    val skips: Int = 0,
) {
    val observations: Int get() = likes + skips
}

@Immutable
data class TasteProfile(
    val genreStats: Map<String, GenreStat> = emptyMap(),
    val likedArtistIds: Set<String> = emptySet(),
) {
    val observationCount: Int get() = genreStats.values.sumOf { it.observations }

    fun recordLike(album: Album): TasteProfile = copy(
        genreStats = genreStats.updated(album) { it.copy(likes = it.likes + 1) },
        likedArtistIds = album.artistId?.let { likedArtistIds + it } ?: likedArtistIds,
    )

    fun recordSkip(album: Album): TasteProfile = copy(
        genreStats = genreStats.updated(album) { it.copy(skips = it.skips + 1) },
    )

    private fun Map<String, GenreStat>.updated(
        album: Album,
        transform: (GenreStat) -> GenreStat,
    ): Map<String, GenreStat> {
        if (album.genres.isEmpty()) return this
        return toMutableMap().apply {
            for (genre in album.genres) {
                this[genre.id] = transform(this[genre.id] ?: GenreStat()).decayed()
            }
        }
    }

    /**
     * 觀測數達上限時對半衰減。這讓近期的滑動權重高於很久以前的，口味改變時能跟著轉向；
     * 同時把 Beta 抽樣的成本壓在常數範圍內（見 [sampleBeta] 是 O(alpha+beta)）。
     */
    private fun GenreStat.decayed(): GenreStat =
        if (observations <= MAX_OBSERVATIONS_PER_GENRE) this else GenreStat(likes / 2, skips / 2)
}

@Immutable
sealed interface RecommendationReason {
    @Immutable
    data class FavoriteGenre(val label: String) : RecommendationReason

    @Immutable
    data class FavoriteArtist(val name: String) : RecommendationReason
}

/**
 * 依 [taste] 為 [albums] 排序，最推薦的排在最前面。
 *
 * 親和度**每個類別只抽樣一次**（而非每張專輯一次）——同一次排序內所有 Pop 專輯共用同一個
 * Pop 樣本，這才是 Thompson sampling 的正確形式；每次重排會重新抽樣，順序自然會變化。
 *
 * 排序用 stable sort，故同分專輯維持 [albums] 傳入時的順序（呼叫端傳入的是已洗過牌的池，
 * 等於保留了隨機性）。
 */
fun rankAlbums(
    albums: List<Album>,
    taste: TasteProfile,
    random: Random = Random.Default,
): List<Album> {
    if (albums.size < 2) return albums
    val sampledAffinity = albums
        .flatMapTo(mutableSetOf()) { album -> album.genres.map { it.id } }
        .associateWith { genreId ->
            val stat = taste.genreStats[genreId] ?: GenreStat()
            sampleBeta(alpha = 1 + stat.likes, beta = 1 + stat.skips, random = random)
        }
    return albums.sortedByDescending { it.score(sampledAffinity, taste) }
}

/**
 * 說明目前這張為何被推薦，[taste] 樣本還太少時回傳 null（此時排序本來就近似隨機，
 * 硬給理由只會誤導）。
 */
fun explainRecommendation(album: Album, taste: TasteProfile): RecommendationReason? {
    album.artistId
        ?.takeIf { it in taste.likedArtistIds }
        ?.let { return RecommendationReason.FavoriteArtist(album.artistName) }

    return album.genres
        .mapNotNull { genre -> taste.genreStats[genre.id]?.let { genre to it } }
        .filter { (_, stat) -> stat.likes >= MIN_LIKES_FOR_EXPLANATION && stat.likes > stat.skips }
        .maxByOrNull { (_, stat) -> stat.likes }
        ?.let { (genre, _) -> RecommendationReason.FavoriteGenre(genre.label) }
}

private fun Album.score(sampledAffinity: Map<String, Double>, taste: TasteProfile): Double {
    val genreScore = genres
        .mapNotNull { sampledAffinity[it.id] }
        .takeIf { it.isNotEmpty() }
        ?.average()
        ?: NO_GENRE_AFFINITY
    val artistBonus = if (artistId != null && artistId in taste.likedArtistIds) ARTIST_BONUS else 0.0
    val popularityPrior = POPULARITY_WEIGHT * (1.0 - chartRank.coerceIn(0, CHART_DEPTH) / CHART_DEPTH.toDouble())
    return genreScore + artistBonus + popularityPrior
}

/**
 * 從 Beta(alpha, beta) 抽樣，alpha/beta 均為正整數。
 *
 * 用順序統計量的性質：Beta(a, b) 等同 (a+b-1) 個 U(0,1) 樣本中第 a 小的那個。這比引入
 * Gamma 抽樣器單純得多，而 alpha+beta 因 [MAX_OBSERVATIONS_PER_GENRE] 有上限，成本可控。
 */
internal fun sampleBeta(alpha: Int, beta: Int, random: Random): Double {
    require(alpha >= 1 && beta >= 1) { "alpha/beta must be >= 1, got $alpha/$beta" }
    val uniforms = DoubleArray(alpha + beta - 1) { random.nextDouble() }
    uniforms.sort()
    return uniforms[alpha - 1]
}

/** 超過這個觀測數就對半衰減，讓近期偏好權重更高。 */
private const val MAX_OBSERVATIONS_PER_GENRE = 20

/** 沒有類別資訊的專輯給中性親和度，不獎不懲。 */
private const val NO_GENRE_AFFINITY = 0.5

private const val ARTIST_BONUS = 0.25
private const val POPULARITY_WEIGHT = 0.10
private const val CHART_DEPTH = 100
private const val MIN_LIKES_FOR_EXPLANATION = 3
