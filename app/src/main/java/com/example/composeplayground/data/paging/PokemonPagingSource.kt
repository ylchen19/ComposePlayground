package com.example.composeplayground.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.composeplayground.data.model.Pokemon
import com.example.composeplayground.data.repository.PokemonRepository
import com.example.composeplayground.data.repository.PokemonRepositoryImpl

class PokemonPagingSource(
    private val repository: PokemonRepository,
    private val searchQuery: String = "",
) : PagingSource<Int, Pokemon>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Pokemon> {
        val offset = params.key ?: 0
        return try {
            val response = repository.fetchPokemonList(offset = offset, limit = params.loadSize)
            val pokemonList = response.results.map { item ->
                val id = PokemonRepositoryImpl.extractIdFromUrl(item.url)
                Pokemon(
                    id = id,
                    name = item.name,
                    imageUrl = PokemonRepositoryImpl.spriteUrl(id),
                    types = emptyList(),
                )
            }.let { list ->
                if (searchQuery.isBlank()) list
                else list.filter { it.name.contains(searchQuery, ignoreCase = true) }
            }

            val nextOffset = if (response.next != null) offset + params.loadSize else null
            val prevOffset = if (offset > 0) maxOf(0, offset - params.loadSize) else null

            LoadResult.Page(
                data = pokemonList,
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
