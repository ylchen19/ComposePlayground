package com.example.composeplayground.data.paging

import androidx.paging.PagingConfig
import androidx.paging.PagingSource.LoadResult
import androidx.paging.testing.TestPager
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

class DailyChartsPagingSourceTest {

    private fun makeTracks(count: Int) = (1..count).map { i ->
        Track(
            id = i.toLong(),
            trackName = "Track $i",
            artistName = "Artist $i",
            collectionName = "Album $i",
            artworkUrl = "https://example.com/art/$i.jpg",
            previewUrl = "https://example.com/preview/$i.m4a",
            trackTimeMillis = 30000L,
            releaseDate = "2020-01-01T00:00:00Z",
            genre = "Pop",
        )
    }

    private class FakeMusicRepository(private val charts: List<Track>) : MusicRepository {
        override suspend fun searchTracks(
            term: String,
            offset: Int,
            limit: Int,
            genre: MusicGenre,
            region: MusicRegion,
        ): TrackPage = TrackPage(tracks = emptyList(), hasNext = false)

        override suspend fun fetchDailyCharts(limit: Int, genre: MusicGenre, region: MusicRegion): List<Track> =
            charts
    }

    private fun makePager(repository: MusicRepository) = TestPager(
        config = PagingConfig(pageSize = MusicRepository.DAILY_CHARTS_LIMIT, enablePlaceholders = false),
        pagingSource = DailyChartsPagingSource(repository, MusicGenre.All, MusicRegion.Global),
    )

    @Test
    fun `single page returns all chart tracks with no next key`() = runTest {
        val repository = FakeMusicRepository(makeTracks(50))
        val pager = makePager(repository)

        val result = pager.refresh() as LoadResult.Page

        assertEquals(50, result.data.size)
        assertNull(result.prevKey)
        assertNull(result.nextKey)
    }

    @Test
    fun `empty charts returns empty page`() = runTest {
        val repository = FakeMusicRepository(emptyList())
        val pager = makePager(repository)

        val result = pager.refresh() as LoadResult.Page

        assertTrue(result.data.isEmpty())
        assertNull(result.nextKey)
    }
}
