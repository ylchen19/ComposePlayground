package com.example.composeplayground.data.model

import com.example.composeplayground.data.repository.MusicRegion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumDeckTest {

    private fun album(
        id: Long,
        region: MusicRegion = MusicRegion.Global,
        genres: List<AlbumGenre> = emptyList(),
    ) = Album(
        id = id,
        name = "Album $id",
        artistName = "Artist $id",
        artistId = "artist-$id",
        artworkUrl = "https://example.com/$id/600x600bb.jpg",
        releaseDate = "2026-01-01",
        appleMusicUrl = "https://music.apple.com/us/album/$id",
        genres = genres,
        region = region,
        chartRank = 0,
    )

    private val pop = AlbumGenre(id = "14", label = "Pop")
    private val rock = AlbumGenre(id = "21", label = "Rock")

    @Test
    fun `excluded genre is removed from deck`() {
        val pool = listOf(
            album(1, genres = listOf(pop)),
            album(2, genres = listOf(rock)),
            album(3, genres = listOf(pop, rock)),
        )

        val deck = buildDeck(pool, excludedGenreIds = setOf("14"), seenIds = emptySet())

        assertEquals(listOf(2L), deck.map { it.id })
    }

    @Test
    fun `seen albums are consumed from the deck in order`() {
        val pool = listOf(album(1), album(2), album(3))

        val afterFirst = buildDeck(pool, excludedGenreIds = emptySet(), seenIds = setOf(1L))
        val afterSecond = buildDeck(pool, excludedGenreIds = emptySet(), seenIds = setOf(1L, 2L))

        assertEquals(listOf(2L, 3L), afterFirst.map { it.id })
        assertEquals(listOf(3L), afterSecond.map { it.id })
    }

    @Test
    fun `deck preserves pool order so remaining cards do not jump`() {
        val pool = listOf(album(5), album(3), album(9), album(1))

        val deck = buildDeck(pool, excludedGenreIds = emptySet(), seenIds = setOf(3L))

        assertEquals(listOf(5L, 9L, 1L), deck.map { it.id })
    }

    @Test
    fun `excluding every genre yields an empty deck`() {
        val pool = listOf(album(1, genres = listOf(pop)), album(2, genres = listOf(rock)))

        val deck = buildDeck(pool, excludedGenreIds = setOf("14", "21"), seenIds = emptySet())

        assertTrue(deck.isEmpty())
    }

    @Test
    fun `genre options deduplicate localized labels by genre id`() {
        val pool = listOf(
            album(1, region = MusicRegion.Taiwan, genres = listOf(AlbumGenre("14", "流行樂"))),
            album(2, region = MusicRegion.Global, genres = listOf(AlbumGenre("14", "Pop"))),
            album(3, region = MusicRegion.Japan, genres = listOf(AlbumGenre("14", "ポップ"))),
        )

        val options = buildGenreOptions(pool, excludedGenreIds = emptySet())

        assertEquals(1, options.size)
        // Global 在 MusicRegion enum 中排最前面，故其名稱勝出
        assertEquals(AlbumGenre("14", "Pop"), options.single())
    }

    @Test
    fun `region exclusive genre falls back to its localized label`() {
        val pool = listOf(
            album(1, region = MusicRegion.Global, genres = listOf(pop)),
            album(2, region = MusicRegion.Taiwan, genres = listOf(AlbumGenre("1253", "華語流行樂"))),
        )

        val options = buildGenreOptions(pool, excludedGenreIds = emptySet())

        assertEquals(listOf("Pop", "華語流行樂"), options.map { it.label })
    }

    @Test
    fun `excluded genre absent from pool is still listed so it can be un-excluded`() {
        val pool = listOf(album(1, genres = listOf(pop)))

        val options = buildGenreOptions(pool, excludedGenreIds = setOf("9999"))

        assertEquals(setOf("14", "9999"), options.map { it.id }.toSet())
    }
}
