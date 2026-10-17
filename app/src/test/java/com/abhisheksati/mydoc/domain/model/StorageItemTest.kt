package com.abhisheksati.mydoc.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StorageItemTest {

    @Test
    fun testFileTypesMapping() {
        val pdfItem = StorageItem(File("/fake/document.pdf"), isDirectory = false)
        assertEquals(StorageFileType.PDF, pdfItem.fileType)
        assertEquals("pdf", pdfItem.extension)

        val docxItem = StorageItem(File("/fake/resume.docx"), isDirectory = false)
        assertEquals(StorageFileType.WORD, docxItem.fileType)

        val xlsxItem = StorageItem(File("/fake/sheet.xlsx"), isDirectory = false)
        assertEquals(StorageFileType.EXCEL, xlsxItem.fileType)

        val pptxItem = StorageItem(File("/fake/slides.pptx"), isDirectory = false)
        assertEquals(StorageFileType.PPT, pptxItem.fileType)

        val imgItem = StorageItem(File("/fake/photo.jpg"), isDirectory = false)
        assertEquals(StorageFileType.IMAGE, imgItem.fileType)

        val vidItem = StorageItem(File("/fake/video.mp4"), isDirectory = false)
        assertEquals(StorageFileType.VIDEO, vidItem.fileType)

        val audioItem = StorageItem(File("/fake/song.mp3"), isDirectory = false)
        assertEquals(StorageFileType.AUDIO, audioItem.fileType)

        val zipItem = StorageItem(File("/fake/backup.zip"), isDirectory = false)
        assertEquals(StorageFileType.ARCHIVE, zipItem.fileType)

        val apkItem = StorageItem(File("/fake/app.apk"), isDirectory = false)
        assertEquals(StorageFileType.APK, apkItem.fileType)

        val codeItem = StorageItem(File("/fake/main.kt"), isDirectory = false)
        assertEquals(StorageFileType.CODE_TEXT, codeItem.fileType)

        val dirItem = StorageItem(File("/fake/my_folder"), isDirectory = true, itemCount = 5)
        assertEquals(StorageFileType.DIRECTORY, dirItem.fileType)
        assertEquals("5 items", dirItem.formattedSize)
    }

    @Test
    fun testHiddenFileDetection() {
        val hidden = StorageItem(File("/fake/.secret_config"), isDirectory = false)
        assertTrue(hidden.isHidden)

        val normal = StorageItem(File("/fake/normal.txt"), isDirectory = false)
        assertFalse(normal.isHidden)
    }

    @Test
    fun testStorageVolumeCalculations() {
        val volume = StorageVolume(
            title = "Internal",
            path = "/storage/emulated/0",
            totalBytes = 100_000_000_000L,
            freeBytes = 40_000_000_000L,
            isPrimary = true
        )
        assertEquals(60_000_000_000L, volume.usedBytes)
        assertEquals(0.6f, volume.usedRatio, 0.01f)
    }
}
