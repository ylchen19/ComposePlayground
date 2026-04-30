package com.example.composeplayground.data.paging

import androidx.paging.PagingConfig
import androidx.paging.PagingSource.LoadResult
import androidx.paging.testing.TestPager
import com.example.composeplayground.data.model.PicsumPhoto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryPicsumPagingSourceTest {

    private val pageSize = PicsumPagingSource.PAGE_SIZE

    private fun makePhotos(count: Int) = (1..count).map { i ->
        PicsumPhoto(
            id = "$i",
            author = "Author $i",
            originalWidth = i * 10,
            originalHeight = i * 10,
            sourceUrl = "https://example.com/$i",
        )
    }

    private fun makePager(photos: List<PicsumPhoto>) = TestPager(
        config = PagingConfig(pageSize = pageSize, enablePlaceholders = false),
        pagingSource = InMemoryPicsumPagingSource(photos),
    )

    @Test
    fun `empty list returns empty page with no keys`() = runTest {
        val pager = makePager(emptyList())

        val result = pager.refresh() as LoadResult.Page

        assertTrue(result.data.isEmpty())
        assertNull(result.prevKey)
        assertNull(result.nextKey)
    }

    @Test
    fun `first page has no prevKey and correct nextKey`() = runTest {
        val pager = makePager(makePhotos(pageSize + 5))

        val result = pager.refresh() as LoadResult.Page

        assertEquals(pageSize, result.data.size)
        assertNull(result.prevKey)
        assertEquals(1, result.nextKey)
    }

    @Test
    fun `list smaller than page size loads all items with no nextKey`() = runTest {
        val photos = makePhotos(10)
        val pager = makePager(photos)

        val result = pager.refresh() as LoadResult.Page

        assertEquals(10, result.data.size)
        assertNull(result.nextKey)
    }

    @Test
    fun `exactly one page loads all items with no nextKey`() = runTest {
        val photos = makePhotos(pageSize)
        val pager = makePager(photos)

        val result = pager.refresh() as LoadResult.Page

        assertEquals(pageSize, result.data.size)
        assertNull(result.nextKey)
    }

    @Test
    fun `append loads remaining items on second page`() = runTest {
        val photos = makePhotos(pageSize + 10)
        val pager = makePager(photos)

        pager.refresh()
        val appendResult = pager.append() as LoadResult.Page

        assertEquals(10, appendResult.data.size)
        assertEquals(photos[pageSize], appendResult.data.first())
        assertEquals(0, appendResult.prevKey)
        assertNull(appendResult.nextKey)
    }

    @Test
    fun `preserves input order across pages`() = runTest {
        val photos = makePhotos(pageSize * 2)
        val pager = makePager(photos)

        val firstPage = pager.refresh() as LoadResult.Page
        val secondPage = pager.append() as LoadResult.Page

        val allLoaded = firstPage.data + secondPage.data
        assertEquals(photos, allLoaded)
    }

    @Test
    fun `sorted list preserves sort order on first page`() = runTest {
        val photos = makePhotos(pageSize + 5)
        val sorted = photos.sortedByDescending { it.pixelCount }
        val pager = makePager(sorted)

        val result = pager.refresh() as LoadResult.Page

        assertEquals(sorted.first(), result.data.first())
        assertEquals(sorted[pageSize - 1], result.data.last())
    }
}
