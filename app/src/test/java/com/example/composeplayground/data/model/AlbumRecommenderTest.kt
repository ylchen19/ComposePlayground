package com.example.composeplayground.data.model

import com.example.composeplayground.data.repository.MusicRegion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AlbumRecommenderTest {

    private val pop = AlbumGenre(id = "14", label = "Pop")
    private val metal = AlbumGenre(id = "1153", label = "Metal")

    private fun album(
        id: Long,
        genres: List<AlbumGenre> = emptyList(),
        artistId: String? = "artist-$id",
        chartRank: Int = 0,
    ) = Album(
        id = id,
        name = "Album $id",
        artistName = "Artist $id",
        artistId = artistId,
        artworkUrl = "https://example.com/$id.jpg",
        releaseDate = "2026-01-01",
        appleMusicUrl = null,
        genres = genres,
        region = MusicRegion.Global,
        chartRank = chartRank,
    )

    // ── Beta 抽樣 ──────────────────────────────────────────────────────────────

    @Test
    fun `beta sample always falls inside the unit interval`() {
        val random = Random(seed = 1)
        repeat(200) {
            val value = sampleBeta(alpha = 1 + it % 12, beta = 1 + (it * 3) % 9, random = random)
            assertTrue("out of range: $value", value in 0.0..1.0)
        }
    }

    @Test
    fun `beta mean tracks the like ratio`() {
        val random = Random(seed = 42)
        fun meanOf(alpha: Int, beta: Int) =
            (1..400).map { sampleBeta(alpha, beta, random) }.average()

        val liked = meanOf(alpha = 1 + 18, beta = 1 + 2)
        val skipped = meanOf(alpha = 1 + 2, beta = 1 + 18)
        val unseen = meanOf(alpha = 1, beta = 1)

        assertTrue("liked=$liked should beat unseen=$unseen", liked > unseen)
        assertTrue("unseen=$unseen should beat skipped=$skipped", unseen > skipped)
    }

    // ── 排序 ─────────────────────────────────────────────────────────────────

    @Test
    fun `liked genre outranks skipped genre over repeated draws`() {
        val taste = (1..15).fold(TasteProfile()) { profile, _ ->
            profile.recordLike(album(1, genres = listOf(pop)))
                .recordSkip(album(2, genres = listOf(metal)))
        }
        val albums = listOf(album(2, genres = listOf(metal)), album(1, genres = listOf(pop)))
        val random = Random(seed = 7)

        val popFirst = (1..100).count { rankAlbums(albums, taste, random).first().id == 1L }

        assertTrue("Pop only led $popFirst/100 draws", popFirst > 80)
    }

    @Test
    fun `unexplored genre still gets exposure so the deck keeps exploring`() {
        // Pop 有壓倒性的正評，Metal 完全沒被看過——純貪婪會讓 Metal 永遠不見天日。
        val taste = (1..15).fold(TasteProfile()) { profile, _ ->
            profile.recordLike(album(1, genres = listOf(pop)))
        }
        val albums = listOf(album(1, genres = listOf(pop)), album(2, genres = listOf(metal)))
        val random = Random(seed = 11)

        val metalFirst = (1..200).count { rankAlbums(albums, taste, random).first().id == 2L }

        assertTrue("Metal never surfaced", metalFirst > 0)
        assertTrue("Metal dominated ($metalFirst/200), exploitation is broken", metalFirst < 100)
    }

    @Test
    fun `chart position breaks ties within the same genre`() {
        val albums = listOf(
            album(1, genres = listOf(pop), chartRank = 90),
            album(2, genres = listOf(pop), chartRank = 0),
        )

        // 同類別共用同一個抽樣值，故只剩熱門度先驗有差別。
        val ranked = rankAlbums(albums, TasteProfile(), Random(seed = 3))

        assertEquals(listOf(2L, 1L), ranked.map { it.id })
    }

    @Test
    fun `liked artist is boosted above an equally ranked stranger`() {
        val taste = TasteProfile().recordLike(album(1, genres = listOf(pop), artistId = "loved"))
        val albums = listOf(
            album(2, genres = listOf(pop), artistId = "stranger"),
            album(3, genres = listOf(pop), artistId = "loved"),
        )

        val ranked = rankAlbums(albums, taste, Random(seed = 5))

        assertEquals(3L, ranked.first().id)
    }

    @Test
    fun `ranking a single album is a no-op`() {
        val albums = listOf(album(1, genres = listOf(pop)))

        assertEquals(albums, rankAlbums(albums, TasteProfile(), Random(seed = 1)))
    }

    // ── 觀測衰減 ─────────────────────────────────────────────────────────────

    @Test
    fun `genre observations decay so recent swipes weigh more`() {
        val taste = (1..30).fold(TasteProfile()) { profile, _ ->
            profile.recordLike(album(1, genres = listOf(pop)))
        }

        val stat = taste.genreStats.getValue(pop.id)

        assertTrue("observations grew unbounded: ${stat.observations}", stat.observations <= 20)
        assertTrue("likes should still dominate", stat.likes > stat.skips)
    }

    @Test
    fun `albums without genres do not pollute the profile`() {
        val taste = TasteProfile().recordSkip(album(1, genres = emptyList()))

        assertTrue(taste.genreStats.isEmpty())
        assertEquals(0, taste.observationCount)
    }

    // ── 推薦理由 ─────────────────────────────────────────────────────────────

    @Test
    fun `no reason is given while the profile is still thin`() {
        val taste = TasteProfile().recordLike(album(1, genres = listOf(pop)))

        assertNull(explainRecommendation(album(2, genres = listOf(pop), artistId = "other"), taste))
    }

    @Test
    fun `favourite genre is reported once enough likes accumulate`() {
        val taste = (1..3).fold(TasteProfile()) { profile, _ ->
            profile.recordLike(album(1, genres = listOf(pop)))
        }

        val reason = explainRecommendation(album(2, genres = listOf(pop), artistId = "other"), taste)

        assertEquals(RecommendationReason.FavoriteGenre("Pop"), reason)
    }

    @Test
    fun `liked artist takes precedence over genre as the reason`() {
        val taste = (1..3).fold(TasteProfile()) { profile, _ ->
            profile.recordLike(album(1, genres = listOf(pop), artistId = "loved"))
        }

        val reason = explainRecommendation(album(9, genres = listOf(pop), artistId = "loved"), taste)

        assertEquals(RecommendationReason.FavoriteArtist("Artist 9"), reason)
    }

    @Test
    fun `a genre skipped more than liked is not offered as a reason`() {
        var taste = TasteProfile()
        repeat(3) { taste = taste.recordLike(album(1, genres = listOf(metal))) }
        repeat(6) { taste = taste.recordSkip(album(2, genres = listOf(metal))) }

        assertNull(explainRecommendation(album(3, genres = listOf(metal), artistId = "other"), taste))
    }
}
