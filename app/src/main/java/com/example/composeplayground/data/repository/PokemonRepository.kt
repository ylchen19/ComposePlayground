package com.example.composeplayground.data.repository

import com.example.composeplayground.data.model.Pokemon
import com.example.composeplayground.data.model.PokemonDetail
import com.example.composeplayground.data.model.PokemonListResponse
import com.example.composeplayground.data.model.PokemonTypeResponse

interface PokemonRepository {
    suspend fun fetchPokemonList(offset: Int, limit: Int): PokemonListResponse
    suspend fun fetchPokemonDetail(id: Int): PokemonDetail
    suspend fun fetchPokemonByType(typeName: String): PokemonTypeResponse
    suspend fun fetchAllPokemonNames(): List<Pokemon>
}
