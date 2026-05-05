package com.example.composeplayground.data.repository

import com.example.composeplayground.data.model.EvolutionNode
import com.example.composeplayground.data.model.Pokemon
import com.example.composeplayground.data.model.PokemonDetail
import com.example.composeplayground.data.model.PokemonPage

/** 所有方法一律回傳 domain model，呼叫端不接觸任何 API DTO。 */
interface PokemonRepository {
    suspend fun fetchPokemonList(offset: Int, limit: Int): PokemonPage
    suspend fun fetchPokemonDetail(id: Int): PokemonDetail
    suspend fun fetchPokemonByType(typeName: String): List<Pokemon>
    suspend fun fetchAllPokemonNames(): List<Pokemon>
    suspend fun fetchEvolutionChain(pokemonId: Int): List<EvolutionNode>
}
