package com.example.composeplayground.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PicsumPhotoTest {

    private fun makePhoto(width: Int, height: Int) = PicsumPhoto(
        id = "1",
        author = "Test Author",
        originalWidth = width,
        originalHeight = height,
        sourceUrl = "https://example.com",
    )

    @Test
    fun `pixelCount multiplies width by height`() {
        val photo = makePhoto(1920, 1080)
        assertEquals(1920L * 1080L, photo.pixelCount)
    }

    @Test
    fun `pixelCount handles large dimensions without integer overflow`() {
        val photo = makePhoto(10000, 10000)
        assertEquals(100_000_000L, photo.pixelCount)
    }

    @Test
    fun `pixelCount for zero dimension is zero`() {
        val photo = makePhoto(0, 1080)
        assertEquals(0L, photo.pixelCount)
    }

    @Test
    fun `sort by pixelCount descending orders correctly`() {
        val photos = listOf(
            makePhoto(100, 100),   // 10_000
            makePhoto(1920, 1080), // 2_073_600
            makePhoto(300, 200),   // 60_000
        )
        val sorted = photos.sortedByDescending { it.pixelCount }

        assertEquals("1920x1080 should be first", 1920, sorted[0].originalWidth)
        assertEquals("300x200 should be second", 300, sorted[1].originalWidth)
        assertEquals("100x100 should be last", 100, sorted[2].originalWidth)
    }

    @Test
    fun `sort by pixelCount ascending orders correctly`() {
        val photos = listOf(
            makePhoto(1920, 1080),
            makePhoto(100, 100),
            makePhoto(300, 200),
        )
        val sorted = photos.sortedBy { it.pixelCount }

        assertEquals("100x100 should be first", 100, sorted[0].originalWidth)
        assertEquals("300x200 should be second", 300, sorted[1].originalWidth)
        assertEquals("1920x1080 should be last", 1920, sorted[2].originalWidth)
    }

    @Test
    fun `sort by author ascending orders alphabetically`() {
        val photos = listOf(
            PicsumPhoto("3", "Zara Smith", 100, 100, ""),
            PicsumPhoto("1", "Alice Chen", 100, 100, ""),
            PicsumPhoto("2", "Bob Lee", 100, 100, ""),
        )
        val sorted = photos.sortedBy { it.author }

        assertEquals("Alice Chen", sorted[0].author)
        assertEquals("Bob Lee", sorted[1].author)
        assertEquals("Zara Smith", sorted[2].author)
    }
}
