package com.example.composeplayground.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// GET /pokemon?limit=20&offset=0
@Serializable
data class PokemonListResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<PokemonListItem>,
)

@Serializable
data class PokemonListItem(
    val name: String,
    val url: String,
)

// GET /pokemon/{id}
@Serializable
data class PokemonDetailResponse(
    val id: Int,
    val name: String,
    val height: Int,
    val weight: Int,
    val types: List<PokemonTypeSlot>,
    val abilities: List<PokemonAbilitySlot>,
    val stats: List<PokemonStatEntry>,
    val sprites: PokemonSprites,
)

@Serializable
data class PokemonTypeSlot(
    val slot: Int,
    val type: NamedApiResource,
)

@Serializable
data class PokemonAbilitySlot(
    val ability: NamedApiResource,
    @SerialName("is_hidden") val isHidden: Boolean,
    val slot: Int,
)

@Serializable
data class PokemonStatEntry(
    @SerialName("base_stat") val baseStat: Int,
    val effort: Int,
    val stat: NamedApiResource,
)

@Serializable
data class PokemonSprites(
    @SerialName("front_default") val frontDefault: String?,
    val other: SpritesOther? = null,
)

@Serializable
data class SpritesOther(
    @SerialName("official-artwork") val officialArtwork: OfficialArtwork? = null,
)

@Serializable
data class OfficialArtwork(
    @SerialName("front_default") val frontDefault: String?,
)

@Serializable
data class NamedApiResource(
    val name: String,
    val url: String,
)

// GET /type/{name}
@Serializable
data class PokemonTypeResponse(
    val id: Int,
    val name: String,
    val pokemon: List<TypePokemonEntry>,
)

@Serializable
data class TypePokemonEntry(
    val pokemon: NamedApiResource,
    val slot: Int,
)
