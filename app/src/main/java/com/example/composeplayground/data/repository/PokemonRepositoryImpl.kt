package com.example.composeplayground.data.repository

import com.example.composeplayground.data.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class PokemonRepositoryImpl(
    private val httpClient: HttpClient,
) : PokemonRepository {

    override suspend fun fetchPokemonList(offset: Int, limit: Int): PokemonPage {
        val response: PokemonListResponse = httpClient.get("pokemon") {
            parameter("offset", offset)
            parameter("limit", limit)
        }.body()
        return PokemonPage(
            pokemon = response.results.map { item ->
                val id = extractIdFromUrl(item.url)
                Pokemon(id = id, name = item.name, imageUrl = spriteUrl(id), types = emptyList())
            },
            hasNext = response.next != null,
        )
    }

    override suspend fun fetchPokemonDetail(id: Int): PokemonDetail {
        val response: PokemonDetailResponse = httpClient.get("pokemon/$id").body()
        return response.toDomain()
    }

    override suspend fun fetchPokemonByType(typeName: String): List<Pokemon> {
        val response: PokemonTypeResponse = httpClient.get("type/$typeName").body()
        return response.pokemon.map { entry ->
            val id = extractIdFromUrl(entry.pokemon.url)
            Pokemon(id = id, name = entry.pokemon.name, imageUrl = spriteUrl(id), types = listOf(typeName))
        }.sortedBy { it.id }
    }

    override suspend fun fetchAllPokemonNames(): List<Pokemon> {
        val response: PokemonListResponse = httpClient.get("pokemon") {
            parameter("limit", 100000)
            parameter("offset", 0)
        }.body()
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
        fun extractIdFromUrl(url: String): Int =
            url.trimEnd('/').substringAfterLast('/').toInt()

        fun spriteUrl(id: Int): String =
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/$id.png"

        fun artworkUrl(id: Int): String =
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"
    }
}
