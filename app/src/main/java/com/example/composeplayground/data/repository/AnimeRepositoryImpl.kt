package com.example.composeplayground.data.repository

import com.example.composeplayground.data.model.AnimeCharacter
import com.example.composeplayground.data.model.AnimeCharactersResponse
import com.example.composeplayground.data.model.AnimeDetail
import com.example.composeplayground.data.model.AnimeDetailResponse
import com.example.composeplayground.data.model.AnimeGenre
import com.example.composeplayground.data.model.AnimeListResponse
import com.example.composeplayground.data.model.AnimePage
import com.example.composeplayground.data.model.AnimeRecommendation
import com.example.composeplayground.data.model.AnimeRecommendationsResponse
import com.example.composeplayground.data.model.GenresResponse
import com.example.composeplayground.data.model.toAnime
import com.example.composeplayground.data.model.toAnimeDetail
import com.example.composeplayground.data.model.toDomain
import com.example.composeplayground.data.model.toGenre
import com.example.composeplayground.network.NetworkResult
import com.example.composeplayground.network.api.ApiService
import com.example.composeplayground.network.api.get

class AnimeRepositoryImpl(
    private val apiService: ApiService,
) : AnimeRepository {

    override suspend fun fetchAnime(
        page: Int,
        limit: Int,
        query: String?,
        genreIds: List<Int>,
        orderBy: AnimeOrderBy,
        sortDirection: SortDirection,
    ): AnimePage {
        val params = buildMap {
            put("page", page.toString())
            put("limit", limit.coerceAtMost(AnimeRepository.MAX_PAGE_SIZE).toString())
            put("order_by", orderBy.apiValue)
            put("sort", sortDirection.apiValue)
            if (!query.isNullOrBlank()) put("q", query.trim())
            if (genreIds.isNotEmpty()) put("genres", genreIds.joinToString(","))
            put("sfw", "true")
        }
        val response = apiService.get<AnimeListResponse>(
            endpoint = "anime",
            queryParams = params,
        ).getOrThrow()
        return AnimePage(
            anime = response.data.map { it.toAnime() },
            hasNext = response.pagination.hasNextPage,
        )
    }

    override suspend fun fetchAnimeDetail(id: Int): AnimeDetail {
        val response = apiService.get<AnimeDetailResponse>("anime/$id/full").getOrThrow()
        return response.data.toAnimeDetail()
    }

    override suspend fun fetchAnimeCharacters(id: Int): List<AnimeCharacter> {
        val response = apiService.get<AnimeCharactersResponse>("anime/$id/characters").getOrThrow()
        return response.data.map { it.toDomain() }
    }

    override suspend fun fetchAnimeRecommendations(id: Int): List<AnimeRecommendation> {
        val response = apiService.get<AnimeRecommendationsResponse>("anime/$id/recommendations").getOrThrow()
        return response.data.map { it.toDomain() }
    }

    override suspend fun fetchGenres(): List<AnimeGenre> {
        val response = apiService.get<GenresResponse>("genres/anime").getOrThrow()
        return response.data.map { it.toGenre() }.sortedBy { it.name }
    }
}

private fun <T> NetworkResult<T>.getOrThrow(): T = when (this) {
    is NetworkResult.Success -> data
    is NetworkResult.Error -> throw Exception(message ?: "Network error (code=$code)")
    is NetworkResult.Loading -> error("Unexpected Loading state in repository")
}
