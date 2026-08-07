package com.example.composeplayground.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.composeplayground.data.model.Album
import com.example.composeplayground.data.model.AlbumGenre
import com.example.composeplayground.data.model.GenreStat
import com.example.composeplayground.data.model.TasteProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "album_roulette_preferences")

class DataStoreAlbumRoulettePreferencesRepository(
    private val context: Context,
) : AlbumRoulettePreferencesRepository {

    private companion object {
        val EXCLUDED_GENRE_IDS_KEY = stringSetPreferencesKey("excluded_genre_ids")
        val EXCLUDED_REGIONS_KEY = stringSetPreferencesKey("excluded_regions")
        val LIKED_ALBUMS_KEY = stringPreferencesKey("liked_albums")
        val TASTE_KEY = stringPreferencesKey("taste_profile")

        val json = Json { ignoreUnknownKeys = true }
    }

    override val preferencesFlow: Flow<AlbumRoulettePreferences> = context.dataStore.data.map { prefs ->
        AlbumRoulettePreferences(
            excludedGenreIds = prefs[EXCLUDED_GENRE_IDS_KEY].orEmpty(),
            excludedRegions = prefs[EXCLUDED_REGIONS_KEY].orEmpty()
                .mapNotNull { runCatching { MusicRegion.valueOf(it) }.getOrNull() }
                .toSet(),
            likedAlbums = prefs[LIKED_ALBUMS_KEY].decodeLikedAlbums(),
            taste = prefs[TASTE_KEY].decodeTaste(),
        )
    }

    override suspend fun saveTaste(taste: TasteProfile) {
        context.dataStore.edit { prefs ->
            prefs[TASTE_KEY] = json.encodeToString(taste.toEntity())
        }
    }

    override suspend fun toggleGenreExclusion(genreId: String) {
        context.dataStore.edit { prefs ->
            prefs[EXCLUDED_GENRE_IDS_KEY] = prefs[EXCLUDED_GENRE_IDS_KEY].orEmpty().toggled(genreId)
        }
    }

    override suspend fun toggleRegionExclusion(region: MusicRegion) {
        context.dataStore.edit { prefs ->
            val next = prefs[EXCLUDED_REGIONS_KEY].orEmpty().toggled(region.name)
            // 全部地區都被排除就沒有任何榜單可抓，直接忽略這次操作以保留至少一個來源。
            if (next.size < MusicRegion.entries.size) {
                prefs[EXCLUDED_REGIONS_KEY] = next
            }
        }
    }

    override suspend fun clearGenreExclusions() {
        context.dataStore.edit { prefs ->
            prefs[EXCLUDED_GENRE_IDS_KEY] = emptySet()
        }
    }

    override suspend fun like(album: Album) {
        context.dataStore.edit { prefs ->
            val current = prefs[LIKED_ALBUMS_KEY].decodeLikedAlbums()
            if (current.any { it.id == album.id }) return@edit
            prefs[LIKED_ALBUMS_KEY] = (current + album).encodeLikedAlbums()
        }
    }

    override suspend fun unlike(albumId: Long) {
        context.dataStore.edit { prefs ->
            val current = prefs[LIKED_ALBUMS_KEY].decodeLikedAlbums()
            prefs[LIKED_ALBUMS_KEY] = current.filterNot { it.id == albumId }.encodeLikedAlbums()
        }
    }

    // Preferences DataStore 只能存純量，且 stringSet 不保序；收藏要保留加入順序，
    // 故序列化成單一 JSON 字串存放。
    private fun String?.decodeLikedAlbums(): List<Album> {
        if (this.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<LikedAlbumEntity>>(this) }
            .getOrDefault(emptyList())
            .mapNotNull { it.toAlbum() }
    }

    private fun List<Album>.encodeLikedAlbums(): String =
        json.encodeToString(map { it.toEntity() })

    private fun String?.decodeTaste(): TasteProfile {
        if (this.isNullOrBlank()) return TasteProfile()
        return runCatching { json.decodeFromString<TasteProfileEntity>(this) }
            .getOrNull()
            ?.toTasteProfile()
            ?: TasteProfile()
    }
}

private fun <T> Set<T>.toggled(value: T): Set<T> =
    if (value in this) this - value else this + value

// ── 持久化 entity ↔ domain 映射 ────────────────────────────────────────────────
// Album 是 @Immutable domain model，不掛 @Serializable；持久化格式獨立一份，避免
// domain model 被儲存格式綁死。

@Serializable
private data class LikedAlbumEntity(
    val id: Long,
    val name: String,
    val artistName: String,
    val artistId: String? = null,
    val artworkUrl: String,
    val releaseDate: String? = null,
    val appleMusicUrl: String? = null,
    val genres: List<LikedAlbumGenreEntity> = emptyList(),
    val region: String,
)

@Serializable
private data class LikedAlbumGenreEntity(val id: String, val label: String)

@Serializable
private data class TasteProfileEntity(
    val genres: Map<String, GenreStatEntity> = emptyMap(),
    val likedArtistIds: Set<String> = emptySet(),
)

@Serializable
private data class GenreStatEntity(val likes: Int = 0, val skips: Int = 0)

private fun TasteProfile.toEntity() = TasteProfileEntity(
    genres = genreStats.mapValues { (_, stat) -> GenreStatEntity(stat.likes, stat.skips) },
    likedArtistIds = likedArtistIds,
)

private fun TasteProfileEntity.toTasteProfile() = TasteProfile(
    genreStats = genres.mapValues { (_, stat) -> GenreStat(stat.likes, stat.skips) },
    likedArtistIds = likedArtistIds,
)

private fun Album.toEntity() = LikedAlbumEntity(
    id = id,
    name = name,
    artistName = artistName,
    artistId = artistId,
    artworkUrl = artworkUrl,
    releaseDate = releaseDate,
    appleMusicUrl = appleMusicUrl,
    genres = genres.map { LikedAlbumGenreEntity(it.id, it.label) },
    region = region.name,
)

private fun LikedAlbumEntity.toAlbum(): Album? {
    val parsedRegion = runCatching { MusicRegion.valueOf(region) }.getOrNull() ?: return null
    return Album(
        id = id,
        name = name,
        artistName = artistName,
        artistId = artistId,
        artworkUrl = artworkUrl,
        releaseDate = releaseDate,
        appleMusicUrl = appleMusicUrl,
        genres = genres.map { AlbumGenre(id = it.id, label = it.label) },
        region = parsedRegion,
        // 收藏清單不參與推薦排序，名次沒有意義。
        chartRank = 0,
    )
}
