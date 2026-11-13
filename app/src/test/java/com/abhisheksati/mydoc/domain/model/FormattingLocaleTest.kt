package com.abhisheksati.mydoc.domain.model

import com.abhisheksati.mydoc.tools.model.formatByteSize
import com.abhisheksati.mydoc.ui.home.formatByteCount
import java.io.File
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattingLocaleTest {

    @Test
    fun byteFormats_followDefaultLocale() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("1,5 GB", formatBytesPrecise(1_610_612_736L))
            assertEquals("1,5 GB", StorageVolume.formatBytes(1_610_612_736L))
            assertEquals("1,5 GB", formatByteSize(1_610_612_736L))
            assertEquals(
                "1,5 MB",
                StorageItem(File("/fake/a.bin"), sizeBytes = 1_572_864L).formattedSize
            )
            assertEquals(
                "1,5 MB",
                TrashItem(
                    originalPath = "/fake/a.bin",
                    trashPath = "/fake/trash/a.bin",
                    displayName = "a.bin",
                    sizeBytes = 1_572_864L,
                    deletedTimestamp = 0L,
                    isDirectory = false
                ).formattedSize
            )
            assertEquals("12,5000° N, 55,2500° E", ExifMetadata(latitude = 12.5, longitude = 55.25).locationText)
            assertEquals("1.234.567", formatByteCount(1_234_567L))

            Locale.setDefault(Locale.US)
            assertEquals("1.5 GB", formatBytesPrecise(1_610_612_736L))
            assertEquals("1.5 GB", StorageVolume.formatBytes(1_610_612_736L))
            assertEquals("1.5 GB", formatByteSize(1_610_612_736L))
            assertEquals("12.5000° N, 55.2500° E", ExifMetadata(latitude = 12.5, longitude = 55.25).locationText)
            assertEquals("1,234,567", formatByteCount(1_234_567L))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun byteFormats_keepUnitBoundaries() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertEquals("512 B", formatBytesPrecise(512L))
            assertEquals("1.0 KB", formatBytesPrecise(1024L))
            assertEquals("1.0 MB", formatBytesPrecise(1024L * 1024L))
            assertEquals("Unknown size", formatBytesPrecise(0L))
        } finally {
            Locale.setDefault(previous)
        }
    }
}