package com.example.composeplayground.data.repository

import android.content.Context
import com.example.composeplayground.data.model.*
import com.example.composeplayground.network.NetworkResult
import com.example.composeplayground.network.api.ApiService
import com.example.composeplayground.network.api.get
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class PokemonRepositoryImpl(
    private val apiService: ApiService,
    private val context: Context,
) : PokemonRepository {

    private val nameMapping: Map<String, String> by lazy {
        loadPokemonNames()
    }

    private fun loadPokemonNames(): Map<String, String> {
        return try {
            val jsonString = context.assets.open("pokemon_names_zh_hant.json").bufferedReader().use { it.readText() }
            val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
            jsonObject.mapValues { it.value.jsonPrimitive.content }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    override suspend fun fetchPokemonList(offset: Int, limit: Int): PokemonPage {
        val response = apiService.get<PokemonListResponse>(
            endpoint = "pokemon",
            queryParams = mapOf("offset" to "$offset", "limit" to "$limit"),
        ).getOrThrow()
        return PokemonPage(
            pokemon = response.results.map { item ->
                val id = extractIdFromUrl(item.url)
                val localizedName = nameMapping[id.toString()] ?: item.name
                Pokemon(id = id, name = localizedName, imageUrl = spriteUrl(id), types = emptyList())
            },
            hasNext = response.next != null,
        )
    }

    override suspend fun fetchPokemonDetail(id: Int): PokemonDetail = coroutineScope {
        val detailDeferred = async { apiService.get<PokemonDetailResponse>("pokemon/$id").getOrThrow() }
        val speciesDeferred = async { apiService.get<PokemonSpeciesResponse>("pokemon-species/$id").getOrThrow() }

        val detailResponse = detailDeferred.await()
        val speciesResponse = speciesDeferred.await()

        detailResponse.toDomain(speciesResponse)
    }

    override suspend fun fetchPokemonByType(typeName: String): List<Pokemon> {
        val response = apiService.get<PokemonTypeResponse>("type/$typeName").getOrThrow()
        return response.pokemon.map { entry ->
            val id = extractIdFromUrl(entry.pokemon.url)
            val localizedName = nameMapping[id.toString()] ?: entry.pokemon.name
            Pokemon(id = id, name = localizedName, imageUrl = spriteUrl(id), types = listOf(typeName))
        }.sortedBy { it.id }
    }

    override suspend fun fetchAllPokemonNames(): List<Pokemon> {
        val response = apiService.get<PokemonListResponse>(
            endpoint = "pokemon",
            queryParams = mapOf("limit" to "100000", "offset" to "0"),
        ).getOrThrow()
        return response.results.map { item ->
            val id = extractIdFromUrl(item.url)
            val localizedName = nameMapping[id.toString()] ?: item.name
            Pokemon(id = id, name = localizedName, imageUrl = spriteUrl(id), types = emptyList())
        }
    }

    override suspend fun fetchEvolutionChain(pokemonId: Int): List<EvolutionNode> = coroutineScope {
        val speciesResponse = apiService.get<PokemonSpeciesResponse>("pokemon-species/$pokemonId").getOrThrow()
        val chainId = extractIdFromUrl(speciesResponse.evolutionChain.url)
        val chainResponse = apiService.get<EvolutionChainResponse>("evolution-chain/$chainId").getOrThrow()

        val result = mutableListOf<EvolutionNode>()
        
        fun traverse(link: ChainLink) {
            val id = extractIdFromUrl(link.species.url)
            val localizedName = nameMapping[id.toString()] ?: link.species.name
            result.add(EvolutionNode(id = id, name = localizedName, imageUrl = artworkUrl(id)))
            link.evolvesTo.forEach { traverse(it) }
        }
        traverse(chainResponse.chain)

        result.sortBy { it.id }
        result
    }

    private fun PokemonDetailResponse.toDomain(species: PokemonSpeciesResponse): PokemonDetail {
        return PokemonDetail(
            id = id,
            name = species.names.filterNameChinese() ?: nameMapping[id.toString()] ?: name,
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
            flavorText = species.flavorTextEntries.filterFlavorTextChinese() ?: "",
        )
    }

    private fun List<ApiName>.filterNameChinese(): String? {
        return find { it.language.name == "zh-Hant" }?.name
            ?: find { it.language.name == "en" }?.name // Fallback to English if no Traditional Chinese
    }

    private fun List<FlavorTextEntry>.filterFlavorTextChinese(): String? {
        return find { it.language.name == "zh-Hant" }?.flavorText?.replace("\n", " ")
            ?: find { it.language.name == "en" }?.flavorText?.replace("\n", " ")
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
