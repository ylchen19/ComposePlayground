package com.example.composeplayground.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.composeplayground.data.model.PicsumPhoto
import com.example.composeplayground.data.repository.PicsumRepository
import kotlin.random.Random

/**
 * Picsum 圖庫的 PagingSource。
 *
 * Picsum 的 page 從 1 開始，總計約 1084 張。回傳空陣列代表已抵達最後一頁。
 *
 * @param randomSeed 由 ViewModel 於建立時產生的隨機種子，使每次進入頁面的排序都不同。
 * 每批載入（每頁）以 `seed xor page` 做局部洗牌，達到分區塊亂序的效果。
 */
class PicsumPagingSource(
    private val repository: PicsumRepository,
    private val randomSeed: Long,
) : PagingSource<Int, PicsumPhoto>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PicsumPhoto> {
        val page = params.key ?: 1
        return try {
            // Always use PAGE_SIZE (not params.loadSize) so that page boundaries align
            // consistently across initial and subsequent loads. Using params.loadSize would
            // cause the initial 60-item load (page=1, limit=60) and the following 30-item
            // load (page=2, limit=30) to overlap at items 31–60, producing duplicate IDs.
            val photos = repository.fetchPhotos(page = page, limit = PAGE_SIZE)
            LoadResult.Page(
                data = photos.shuffled(Random(randomSeed xor page.toLong())),
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

    companion object {
        const val PAGE_SIZE = 30
    }
}
