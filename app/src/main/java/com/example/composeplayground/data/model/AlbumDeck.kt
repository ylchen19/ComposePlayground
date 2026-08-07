package com.example.composeplayground.data.model

import com.example.composeplayground.data.repository.MusicRegion

/**
 * 隨機專輯推薦的牌堆推導——刻意寫成純函式，讓排除邏輯可以獨立於 ViewModel 測試。
 */

/**
 * 從專輯池推導出目前該顯示的牌堆。
 *
 * **不在這裡洗牌**：`pool` 在寫入時就已經洗過一次，這裡只做過濾以保留既有順序。
 * 若在此處 `shuffled()`，每滑掉一張（[seenIds] 變動）都會讓剩下的卡片順序整副跳掉。
 */
fun buildDeck(
    pool: List<Album>,
    excludedGenreIds: Set<String>,
    seenIds: Set<Long>,
): List<Album> = pool.filterNot { album ->
    album.id in seenIds || album.genres.any { it.id in excludedGenreIds }
}

/**
 * 推導類別 chip row 的選項。
 *
 * 同一個 genreId 在不同 storefront 會有不同的在地化名稱，故以 id 去重，並取
 * [MusicRegion] enum 順序中**最先**出現該 id 的地區所提供的名稱當 label——Global(us)
 * 排第一，大多數類別因此拿到英文名，只有 `1253 華語流行樂` 這種地區限定的類別才會
 * 落回在地化名稱，結果至少是穩定且可預期的。
 *
 * 已被排除但目前池子裡沒有的類別（例如排除後又切換地區）仍會列出，否則使用者將無法
 * 把它取消排除。
 */
fun buildGenreOptions(
    pool: List<Album>,
    excludedGenreIds: Set<String>,
): List<AlbumGenre> {
    val regionRank = MusicRegion.entries.withIndex().associate { (index, region) -> region to index }
    val bestByGenreId = mutableMapOf<String, Pair<Int, AlbumGenre>>()
    for (album in pool) {
        val rank = regionRank[album.region] ?: Int.MAX_VALUE
        for (genre in album.genres) {
            val existing = bestByGenreId[genre.id]
            if (existing == null || rank < existing.first) {
                bestByGenreId[genre.id] = rank to genre
            }
        }
    }
    // 不在池中的已排除類別沒有 label 可用，退回顯示 id，至少仍可被點掉。
    for (id in excludedGenreIds) {
        bestByGenreId.getOrPut(id) { Int.MAX_VALUE to AlbumGenre(id = id, label = id) }
    }
    return bestByGenreId.values.map { it.second }.sortedBy { it.label }
}
