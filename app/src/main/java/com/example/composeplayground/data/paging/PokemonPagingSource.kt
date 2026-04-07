package com.example.composeplayground.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.composeplayground.data.model.Pokemon
import com.example.composeplayground.data.repository.PokemonRepository

class PokemonPagingSource(
    private val repository: PokemonRepository,
    private val searchQuery: String = "",
) : PagingSource<Int, Pokemon>() {

    // 搜尋模式：第一次 load 時取得全部名稱並快取，後續頁直接從快取取
    private var cachedSearchResults: List<Pokemon>? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Pokemon> {
        val offset = params.key ?: 0
        return try {
            if (searchQuery.isNotBlank()) {
                // 搜尋模式：一次取得所有名稱，client-side filter 後手動分頁
                // 避免每頁從 API 拿 20 筆再 filter 幾乎全空的問題
                val allPokemon = cachedSearchResults
                    ?: repository.fetchAllPokemonNames().also { cachedSearchResults = it }
                val filtered = allPokemon.filter { it.name.contains(searchQuery, ignoreCase = true) }
                val pageData = filtered.drop(offset).take(params.loadSize)
                val nextOffset = if (offset + params.loadSize < filtered.size) offset + params.loadSize else null
                val prevOffset = if (offset > 0) maxOf(0, offset - params.loadSize) else null
                LoadResult.Page(data = pageData, prevKey = prevOffset, nextKey = nextOffset)
            } else {
                // 一般模式：直接使用 API 分頁，repository 負責 DTO → domain 映射
                val page = repository.fetchPokemonList(offset = offset, limit = params.loadSize)
                val nextOffset = if (page.hasNext) offset + params.loadSize else null
                val prevOffset = if (offset > 0) maxOf(0, offset - params.loadSize) else null
                LoadResult.Page(data = page.pokemon, prevKey = prevOffset, nextKey = nextOffset)
            }
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
