package com.example.composeplayground.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.composeplayground.data.model.PicsumPhoto

/**
 * 以一份已排序好的記憶體清單作為資料來源的 Paging 來源。
 *
 * 用於 Picsum Gallery「依解析度排序」模式：先一次性載入並排序所有圖片後，
 * 再透過此 source 切片成 [PicsumPagingSource.PAGE_SIZE] 大小的分頁，
 * 維持與隨機模式一致的 LazyPagingItems API。
 *
 * @param photos 完整的、已排序的相片清單
 */
class InMemoryPicsumPagingSource(
    private val photos: List<PicsumPhoto>,
) : PagingSource<Int, PicsumPhoto>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PicsumPhoto> {
        val page = params.key ?: 0
        val from = page * PicsumPagingSource.PAGE_SIZE
        if (from >= photos.size) {
            return LoadResult.Page(emptyList(), prevKey = null, nextKey = null)
        }
        val to = minOf(from + PicsumPagingSource.PAGE_SIZE, photos.size)
        return LoadResult.Page(
            data = photos.subList(from, to),
            prevKey = if (page > 0) page - 1 else null,
            nextKey = if (to < photos.size) page + 1 else null,
        )
    }

    override fun getRefreshKey(state: PagingState<Int, PicsumPhoto>): Int? {
        return state.anchorPosition?.let { anchor ->
            val page = state.closestPageToPosition(anchor) ?: return@let null
            page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
        }
    }
}
