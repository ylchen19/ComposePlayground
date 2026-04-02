package com.example.composeplayground.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object Home : NavKey

@Serializable
data class PokemonDetail(val pokemonId: Int) : NavKey

@Serializable
data object Settings : NavKey
