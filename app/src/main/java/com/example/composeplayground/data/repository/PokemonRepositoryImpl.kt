package com.example.composeplayground.data.repository

import com.example.composeplayground.data.model.*
import com.example.composeplayground.network.NetworkResult
import com.example.composeplayground.network.api.ApiService
import com.example.composeplayground.network.api.get

class PokemonRepositoryImpl(
    private val apiService: ApiService,
) : PokemonRepository {

    override suspend fun fetchPokemonList(offset: Int, limit: Int): PokemonPage {
        val response = apiService.get<PokemonListResponse>(
            endpoint = "pokemon",
            queryParams = mapOf("offset" to "$offset", "limit" to "$limit"),
        ).getOrThrow()
        return PokemonPage(
            pokemon = response.results.map { item ->
                val id = extractIdFromUrl(item.url)
                Pokemon(id = id, name = item.name, imageUrl = spriteUrl(id), types = emptyList())
            },
            hasNext = response.next != null,
        )
    }

    override suspend fun fetchPokemonDetail(id: Int): PokemonDetail {
        val response = apiService.get<PokemonDetailResponse>("pokemon/$id").getOrThrow()
        return response.toDomain()
    }

    override suspend fun fetchPokemonByType(typeName: String): List<Pokemon> {
        val response = apiService.get<PokemonTypeResponse>("type/$typeName").getOrThrow()
        return response.pokemon.map { entry ->
            val id = extractIdFromUrl(entry.pokemon.url)
            Pokemon(id = id, name = entry.pokemon.name, imageUrl = spriteUrl(id), types = listOf(typeName))
        }.sortedBy { it.id }
    }

    override suspend fun fetchAllPokemonNames(): List<Pokemon> {
        val response = apiService.get<PokemonListResponse>(
            endpoint = "pokemon",
            queryParams = mapOf("limit" to "100000", "offset" to "0"),
        ).getOrThrow()
        return response.results.map { item ->
            val id = extractIdFromUrl(item.url)
            Pokemon(id = id, name = item.name, imageUrl = spriteUrl(id), types = emptyList())
        }
    }

    private fun PokemonDetailResponse.toDomain(): PokemonDetail {
        return PokemonDetail(
            id = id,
            name = name,
            imageUrl = sprites.other?.officialArtwork?.frontDefault
                ?: sprites.frontDefault
                ?: artworkUrl(id),
            height = height,
            weight = weight,
            types = types.sortedBy { it.slot }.map { it.type.name },
            abilities = abilities.sortedBy { it.slot }.map {
                PokemonAbility(name = it.ability.name, isHidden = it.isHidden)
            },
            stats = stats.map {
                PokemonStatInfo(name = it.stat.name, baseStat = it.baseStat)
            },
        )
    }

    companion object {
        private const val BASE_SPRITE_URL = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon"
        private const val BASE_ARTWORK_URL = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork"

        fun extractIdFromUrl(url: String): Int =
            url.trimEnd('/').substringAfterLast('/').toInt()

        fun spriteUrl(id: Int): String = "$BASE_SPRITE_URL/$id.png"

        fun artworkUrl(id: Int): String = "$BASE_ARTWORK_URL/$id.png"
    }
}

/**
 * [NetworkResult] 的解包輔助函式，成功時回傳資料，失敗時拋出例外由 PagingSource 統一捕捉。
 * 定義為檔案私有，不對外暴露，僅限本 Repository 使用。
 */
private fun <T> NetworkResult<T>.getOrThrow(): T = when (this) {
    is NetworkResult.Success -> data
    is NetworkResult.Error -> throw Exception(message ?: "Network error (code=$code)")
    is NetworkResult.Loading -> error("Unexpected Loading state in repository")
}
