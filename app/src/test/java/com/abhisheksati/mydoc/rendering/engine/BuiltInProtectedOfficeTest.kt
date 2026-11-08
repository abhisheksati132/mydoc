package com.abhisheksati.mydoc.rendering.engine

import com.abhisheksati.mydoc.domain.model.DocumentLoadException
import com.abhisheksati.mydoc.domain.model.LoadFailure
import com.abhisheksati.mydoc.rendering.engine.word.DocxParser
import com.abhisheksati.mydoc.rendering.engine.spreadsheet.SpreadsheetParser
import com.abhisheksati.mydoc.rendering.engine.presentation.PptxParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class BuiltInProtectedOfficeTest {
    @Test fun protectedWordDoesNotBecomeRecoveredEncryptionMetadata() = runBlocking {
        assertLocked { File("src/androidTest/assets/word/protected.docx").inputStream().use { DocxParser().parse(it) } }
    }
    @Test fun protectedWorkbookDoesNotBecomeAnEmptySuccessfulGrid() = runBlocking {
        assertLocked { File("src/androidTest/assets/office/protected.xlsx").inputStream().use { SpreadsheetParser().parse(it, "xlsx") } }
    }
    @Test fun protectedPresentationDoesNotBecomeInventedSlides() = runBlocking {
        assertLocked { File("src/androidTest/assets/office/protected.pptx").inputStream().use { PptxParser().parse(it) } }
    }
    private suspend fun assertLocked(parse: suspend () -> Any) {
        try { parse(); fail("Built-in readers must report encrypted Office containers") }
        catch (error: DocumentLoadException) { assertEquals(LoadFailure.PASSWORD_PROTECTED, error.failure) }
    }
}
