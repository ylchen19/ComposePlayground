package com.example.composeplayground.data.model

data class Pokemon(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val types: List<String>,
)

data class PokemonDetail(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val height: Int,
    val weight: Int,
    val types: List<String>,
    val abilities: List<PokemonAbility>,
    val stats: List<PokemonStatInfo>,
)

data class PokemonAbility(
    val name: String,
    val isHidden: Boolean,
)

data class PokemonStatInfo(
    val name: String,
    val baseStat: Int,
)
