package com.example.composeplayground.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── DTO（Jikan REST API v4 回應結構）────────────────────────────────────────────
// Jikan 回傳結構深，這裡只保留 UI 真正需要的欄位；ContentNegotiation 設定為
// ignoreUnknownKeys = true，故未列出的欄位會被靜默忽略。

@Serializable
data class AnimeListResponse(
    val data: List<AnimeDto> = emptyList(),
    val pagination: PaginationDto = PaginationDto(),
)

@Serializable
data class PaginationDto(
    @SerialName("last_visible_page") val lastVisiblePage: Int = 1,
    @SerialName("has_next_page") val hasNextPage: Boolean = false,
    @SerialName("current_page") val currentPage: Int = 1,
)

@Serializable
data class AnimeDto(
    @SerialName("mal_id") val malId: Int,
    val title: String? = null,
    @SerialName("title_english") val titleEnglish: String? = null,
    val images: AnimeImagesDto? = null,
    val type: String? = null,
    val episodes: Int? = null,
    val score: Double? = null,
    val year: Int? = null,
    val status: String? = null,
    val synopsis: String? = null,
    val genres: List<NamedEntityDto> = emptyList(),
)

@Serializable
data class AnimeImagesDto(val jpg: AnimeImageUrlsDto? = null)

@Serializable
data class AnimeImageUrlsDto(
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("large_image_url") val largeImageUrl: String? = null,
)

@Serializable
data class NamedEntityDto(
    @SerialName("mal_id") val malId: Int,
    val name: String,
)

@Serializable
data class AnimeDetailResponse(val data: AnimeDto)

@Serializable
data class AnimeCharactersResponse(val data: List<AnimeCharacterEntryDto> = emptyList())

@Serializable
data class AnimeCharacterEntryDto(
    val character: CharacterDto,
    val role: String? = null,
)

@Serializable
data class CharacterDto(
    @SerialName("mal_id") val malId: Int,
    val name: String,
    val images: AnimeImagesDto? = null,
)

@Serializable
data class AnimeRecommendationsResponse(val data: List<AnimeRecommendationEntryDto> = emptyList())

@Serializable
data class AnimeRecommendationEntryDto(val entry: AnimeDto)

@Serializable
data class GenresResponse(val data: List<NamedEntityDto> = emptyList())

// ── Domain Model ──────────────────────────────────────────────────────────────

@Immutable
data class Anime(
    val id: Int,
    val title: String,
    val imageUrl: String,
    val type: String?,
    val episodes: Int?,
    val score: Double?,
    val year: Int?,
)

@Immutable
data class AnimeDetail(
    val id: Int,
    val title: String,
    val imageUrl: String,
    val type: String?,
    val episodes: Int?,
    val score: Double?,
    val year: Int?,
    val status: String?,
    val synopsis: String,
    val genres: List<String>,
)

@Immutable
data class AnimePage(
    val anime: List<Anime>,
    val hasNext: Boolean,
)

@Immutable
data class AnimeCharacter(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val role: String,
)

@Immutable
data class AnimeRecommendation(
    val id: Int,
    val title: String,
    val imageUrl: String,
)

@Immutable
data class AnimeGenre(
    val id: Int,
    val name: String,
)

// ── DTO → Domain 映射 ─────────────────────────────────────────────────────────

fun AnimeDto.toAnime(): Anime = Anime(
    id = malId,
    title = displayTitle(),
    imageUrl = images?.jpg?.imageUrl.orEmpty(),
    type = type,
    episodes = episodes,
    score = score,
    year = year,
)

fun AnimeDto.toAnimeDetail(): AnimeDetail = AnimeDetail(
    id = malId,
    title = displayTitle(),
    imageUrl = images?.jpg?.largeImageUrl ?: images?.jpg?.imageUrl.orEmpty(),
    type = type,
    episodes = episodes,
    score = score,
    year = year,
    status = status,
    synopsis = synopsis.orEmpty(),
    genres = genres.map { it.name },
)

fun AnimeCharacterEntryDto.toDomain(): AnimeCharacter = AnimeCharacter(
    id = character.malId,
    name = character.name,
    imageUrl = character.images?.jpg?.imageUrl.orEmpty(),
    role = role.orEmpty(),
)

fun AnimeRecommendationEntryDto.toDomain(): AnimeRecommendation = AnimeRecommendation(
    id = entry.malId,
    title = entry.displayTitle(),
    imageUrl = entry.images?.jpg?.imageUrl.orEmpty(),
)

fun NamedEntityDto.toGenre(): AnimeGenre = AnimeGenre(id = malId, name = name)

private fun AnimeDto.displayTitle(): String =
    titleEnglish?.takeIf { it.isNotBlank() } ?: title.orEmpty()
