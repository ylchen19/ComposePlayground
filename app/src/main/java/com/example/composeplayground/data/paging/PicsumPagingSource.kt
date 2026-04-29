package com.example.composeplayground.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.composeplayground.data.model.PicsumPhoto
import com.example.composeplayground.data.repository.PicsumRepository

/**
 * Picsum 圖庫的 PagingSource。
 *
 * Picsum 的 page 從 1 開始，總計約 1084 張。回傳空陣列代表已抵達最後一頁。
 */
class PicsumPagingSource(
    private val repository: PicsumRepository,
) : PagingSource<Int, PicsumPhoto>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PicsumPhoto> {
        val page = params.key ?: 1
        return try {
            val photos = repository.fetchPhotos(page = page, limit = params.loadSize)
            LoadResult.Page(
                data = photos,
                prevKey = if (page > 1) page - 1 else null,
                nextKey = if (photos.isEmpty()) null else page + 1,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, PicsumPhoto>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }
}
