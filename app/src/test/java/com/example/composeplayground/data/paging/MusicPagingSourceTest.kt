package com.example.composeplayground.data.paging

import androidx.paging.PagingConfig
import androidx.paging.PagingSource.LoadResult
import androidx.paging.testing.TestPager
import com.example.composeplayground.data.model.Album
import com.example.composeplayground.data.model.Track
import com.example.composeplayground.data.model.TrackPage
import com.example.composeplayground.data.repository.MusicGenre
import com.example.composeplayground.data.repository.MusicRegion
import com.example.composeplayground.data.repository.MusicRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicPagingSourceTest {

    private val pageSize = MusicPagingSource.PAGE_SIZE

    private fun makeTracks(count: Int, idOffset: Int = 0) = (1..count).map { i ->
        val id = (idOffset + i).toLong()
        Track(
            id = id,
            trackName = "Track $id",
            artistName = "Artist $id",
            collectionName = "Album $id",
            artworkUrl = "https://example.com/art/$id.jpg",
            previewUrl = "https://example.com/preview/$id.m4a",
            trackTimeMillis = 30000L,
            releaseDate = "2020-01-01T00:00:00Z",
            genre = "Pop",
        )
    }

    private class FakeMusicRepository(
        private val pages: Map<Int, List<Track>>,
        private val totalHint: Int,
    ) : MusicRepository {
        override suspend fun searchTracks(
            term: String,
            offset: Int,
            limit: Int,
            genre: MusicGenre,
            region: MusicRegion,
        ): TrackPage {
            val tracks = pages[offset].orEmpty()
            return TrackPage(
                tracks = tracks,
                hasNext = tracks.size == limit && offset + limit < totalHint,
            )
        }

        override suspend fun fetchDailyCharts(limit: Int, genre: MusicGenre, region: MusicRegion): List<Track> =
            emptyList()

        override suspend fun fetchTopAlbums(region: MusicRegion, limit: Int): List<Album> = emptyList()
    }

    private fun makePager(repository: MusicRepository) = TestPager(
        config = PagingConfig(pageSize = pageSize, enablePlaceholders = false),
        pagingSource = MusicPagingSource(
            repository,
            MusicPagingArgs(query = "query", genre = MusicGenre.All, region = MusicRegion.Global),
        ),
    )

    @Test
    fun `empty result returns empty page with no keys`() = runTest {
        val repository = FakeMusicRepository(pages = emptyMap(), totalHint = 0)
        val pager = makePager(repository)

        val result = pager.refresh() as LoadResult.Page

        assertTrue(result.data.isEmpty())
        assertNull(result.prevKey)
        assertNull(result.nextKey)
    }

    @Test
    fun `first page has no prevKey and correct nextKey when more results exist`() = runTest {
        val repository = FakeMusicRepository(
            pages = mapOf(0 to makeTracks(pageSize)),
            totalHint = MusicRepository.MAX_RESULTS,
        )
        val pager = makePager(repository)

        val result = pager.refresh() as LoadResult.Page

        assertEquals(pageSize, result.data.size)
        assertNull(result.prevKey)
        assertEquals(1, result.nextKey)
    }

    @Test
    fun `partial last page has no nextKey`() = runTest {
        val repository = FakeMusicRepository(
            pages = mapOf(0 to makeTracks(pageSize - 3)),
            totalHint = MusicRepository.MAX_RESULTS,
        )
        val pager = makePager(repository)

        val result = pager.refresh() as LoadResult.Page

        assertEquals(pageSize - 3, result.data.size)
        assertNull(result.nextKey)
    }

    @Test
    fun `empty filtered page still advances nextKey when raw hasNext is true`() = runTest {
        // Simulates a genre-filtered page where every raw result was filtered out client-side,
        // but the underlying API still has more raw pages — pagination must not stop early.
        val repository = object : MusicRepository {
            override suspend fun searchTracks(
                term: String,
                offset: Int,
                limit: Int,
                genre: MusicGenre,
                region: MusicRegion,
            ): TrackPage = TrackPage(tracks = emptyList(), hasNext = true)

            override suspend fun fetchDailyCharts(limit: Int, genre: MusicGenre, region: MusicRegion): List<Track> =
                emptyList()

            override suspend fun fetchTopAlbums(region: MusicRegion, limit: Int): List<Album> = emptyList()
        }
        val pager = makePager(repository)

        val result = pager.refresh() as LoadResult.Page

        assertTrue(result.data.isEmpty())
        assertEquals(1, result.nextKey)
    }

    @Test
    fun `duplicate track ids across pages are filtered out`() = runTest {
        val firstPage = makeTracks(pageSize)
        // second page repeats the last 2 ids of the first page
        val secondPage = makeTracks(pageSize, idOffset = pageSize - 2)
        val repository = FakeMusicRepository(
            pages = mapOf(0 to firstPage, pageSize to secondPage),
            totalHint = MusicRepository.MAX_RESULTS,
        )
        val pager = makePager(repository)

        pager.refresh()
        val secondResult = pager.append() as LoadResult.Page

        assertEquals(pageSize - 2, secondResult.data.size)
    }
}
