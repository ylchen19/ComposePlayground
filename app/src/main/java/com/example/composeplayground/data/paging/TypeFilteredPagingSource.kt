package com.example.composeplayground.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.composeplayground.data.model.Pokemon
import com.example.composeplayground.data.repository.PokemonRepository
import com.example.composeplayground.data.repository.PokemonRepositoryImpl

class TypeFilteredPagingSource(
    private val repository: PokemonRepository,
    private val typeName: String,
    private val searchQuery: String = "",
) : PagingSource<Int, Pokemon>() {

    private var cachedList: List<Pokemon>? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Pokemon> {
        val offset = params.key ?: 0
        return try {
            val allPokemon = cachedList ?: run {
                val response = repository.fetchPokemonByType(typeName)
                response.pokemon.map { entry ->
                    val id = PokemonRepositoryImpl.extractIdFromUrl(entry.pokemon.url)
                    Pokemon(
                        id = id,
                        name = entry.pokemon.name,
                        imageUrl = PokemonRepositoryImpl.spriteUrl(id),
                        types = listOf(typeName),
                    )
                }.sortedBy { it.id }.also { cachedList = it }
            }

            val filtered = if (searchQuery.isBlank()) allPokemon
                else allPokemon.filter { it.name.contains(searchQuery, ignoreCase = true) }

            val pageData = filtered.drop(offset).take(params.loadSize)
            val nextOffset = if (offset + params.loadSize < filtered.size) offset + params.loadSize else null
            val prevOffset = if (offset > 0) maxOf(0, offset - params.loadSize) else null

            LoadResult.Page(
                data = pageData,
                prevKey = prevOffset,
                nextKey = nextOffset,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Pokemon>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(state.config.pageSize)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(state.config.pageSize)
        }
    }
}
