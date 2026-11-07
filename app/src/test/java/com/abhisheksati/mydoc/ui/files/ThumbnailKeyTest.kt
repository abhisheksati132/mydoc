package com.abhisheksati.mydoc.ui.files

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ThumbnailKeyTest {

    @Test
    fun `same file version keeps the same key`() {
        assertEquals(thumbnailKey("/sdcard/a.pdf", 100L, 500L), thumbnailKey("/sdcard/a.pdf", 100L, 500L))
    }

    @Test
    fun `an in-place edit changes the key even though the path is identical`() {
        val before = thumbnailKey("/sdcard/a.pdf", 100L, 500L)
        assertNotEquals("mtime must invalidate the cached thumbnail",
            before, thumbnailKey("/sdcard/a.pdf", 100L, 501L))
        assertNotEquals("size must invalidate the cached thumbnail",
            before, thumbnailKey("/sdcard/a.pdf", 101L, 500L))
    }

    @Test
    fun `different files never share a key`() {
        assertNotEquals(thumbnailKey("/sdcard/a.pdf", 100L, 500L), thumbnailKey("/sdcard/b.pdf", 100L, 500L))
    }
}