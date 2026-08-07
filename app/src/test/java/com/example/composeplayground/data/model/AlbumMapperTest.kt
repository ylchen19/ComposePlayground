package com.example.composeplayground.data.model

import com.example.composeplayground.data.repository.MusicRegion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlbumMapperTest {

    private fun entry(
        id: String? = "1895420989",
        name: String? = "petal",
        artistName: String? = "Ariana Grande",
        artworkUrl100: String? = "https://is1-ssl.mzstatic.com/image/thumb/abc.jpg/100x100bb.jpg",
        genres: List<AlbumGenreDto> = emptyList(),
    ) = AlbumEntryDto(
        id = id,
        name = name,
        artistName = artistName,
        artworkUrl100 = artworkUrl100,
        releaseDate = "2026-07-31",
        url = "https://music.apple.com/us/album/petal/1895420989",
        genres = genres,
    )

    @Test
    fun `artwork url is upscaled for the full-size card`() {
        val album = entry().toAlbum(MusicRegion.Global, chartRank = 0)

        assertEquals("https://is1-ssl.mzstatic.com/image/thumb/abc.jpg/600x600bb.jpg", album?.artworkUrl)
    }

    @Test
    fun `umbrella music genre is dropped`() {
        val album = entry(
            genres = listOf(
                AlbumGenreDto(genreId = "14", name = "Pop"),
                AlbumGenreDto(genreId = "34", name = "Music"),
            ),
        ).toAlbum(MusicRegion.Global, chartRank = 0)

        assertEquals(listOf(AlbumGenre("14", "Pop")), album?.genres)
    }

    @Test
    fun `genres missing an id or name are skipped`() {
        val album = entry(
            genres = listOf(
                AlbumGenreDto(genreId = null, name = "Pop"),
                AlbumGenreDto(genreId = "21", name = null),
                AlbumGenreDto(genreId = "11", name = "Jazz"),
            ),
        ).toAlbum(MusicRegion.Global, chartRank = 0)

        assertEquals(listOf(AlbumGenre("11", "Jazz")), album?.genres)
    }

    @Test
    fun `entries missing required fields map to null`() {
        assertNull(entry(id = null).toAlbum(MusicRegion.Global, chartRank = 0))
        assertNull(entry(id = "not-a-number").toAlbum(MusicRegion.Global, chartRank = 0))
        assertNull(entry(name = " ").toAlbum(MusicRegion.Global, chartRank = 0))
        assertNull(entry(artistName = null).toAlbum(MusicRegion.Global, chartRank = 0))
    }

    @Test
    fun `region is carried onto the domain model`() {
        assertEquals(MusicRegion.Taiwan, entry().toAlbum(MusicRegion.Taiwan, chartRank = 0)?.region)
    }
}
