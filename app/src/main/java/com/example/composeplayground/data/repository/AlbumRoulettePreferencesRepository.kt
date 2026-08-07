package com.example.composeplayground.data.repository

import androidx.compose.runtime.Immutable
import com.example.composeplayground.data.model.Album
import com.example.composeplayground.data.model.TasteProfile
import kotlinx.coroutines.flow.Flow

/**
 * 隨機專輯推薦的使用者偏好：想排除的類別／地區，以及右滑收藏的專輯。
 *
 * [excludedGenreIds] 存的是 iTunes genreId 而非類別名稱——名稱會隨 storefront 在地化，
 * 存名稱會讓排除設定在切換地區後失效（見 `AlbumGenre`）。
 */
@Immutable
data class AlbumRoulettePreferences(
    val excludedGenreIds: Set<String> = emptySet(),
    val excludedRegions: Set<MusicRegion> = emptySet(),
    val likedAlbums: List<Album> = emptyList(),
    val taste: TasteProfile = TasteProfile(),
)

interface AlbumRoulettePreferencesRepository {

    val preferencesFlow: Flow<AlbumRoulettePreferences>

    suspend fun toggleGenreExclusion(genreId: String)

    suspend fun toggleRegionExclusion(region: MusicRegion)

    suspend fun clearGenreExclusions()

    suspend fun like(album: Album)

    suspend fun unlike(albumId: Long)

    /**
     * 覆寫學到的推薦偏好。ViewModel 在記憶體裡持有權威副本並以純函式更新，這裡只負責存放，
     * 避免每滑一張卡都要等 DataStore 寫回才能重排牌堆。
     */
    suspend fun saveTaste(taste: TasteProfile)
}
