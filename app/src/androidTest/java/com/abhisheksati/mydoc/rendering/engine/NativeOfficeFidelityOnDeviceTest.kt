package com.abhisheksati.mydoc.rendering.engine

import android.graphics.Color
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.LoadFailure
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.rendering.engine.presentation.PptxConverter
import com.abhisheksati.mydoc.rendering.engine.spreadsheet.XlsxConverter
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import com.abhisheksati.mydoc.viewmodel.ViewerViewModel
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Complete packages exercise Impress/Calc through the public converter and actual PDF reader. */
class NativeOfficeFidelityOnDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun impressRetainsBothSlidesTablePictureAndChart() = runBlocking {
        withFixture("fidelity.pptx") { source ->
            val before = source.readBytes()
            val result = PptxConverter.convertPptxToPdf(context, Uri.fromFile(source), source.name)
            checkPages(result, listOf("IMPRESS FIRST SLIDE", "IMPRESS SECOND SLIDE", "PRESENTATION TABLE", "North", "South"))
            assertRedPicture(result)
            assertArrayEquals("Opening must preserve the source package", before, source.readBytes())
            val again = PptxConverter.convertPptxToPdf(context, Uri.fromFile(source), source.name)
            assertEquals("Second open must reuse the validated PDF", result.pdfFile, again.pdfFile)
        }
    }

    @Test fun calcPrintsBothWorksheetsAndChartWithOriginalStyles() = runBlocking {
        withFixture("fidelity.xlsx") { source ->
            val before = source.readBytes()
            val result = XlsxConverter.convertToPages(context, Uri.fromFile(source), source.name)
            checkPages(result, listOf("CALC SUMMARY SHEET", "CALC DETAIL SHEET", "SECOND WORKSHEET CONTENT", "North", "South"))
            assertRedPicture(result)
            assertArrayEquals(before, source.readBytes())
            assertEquals(result.pdfFile,
                XlsxConverter.convertToPages(context, Uri.fromFile(source), source.name).pdfFile)
        }
    }

    @Test fun legacyBinaryWorkbookUsesCalcInsteadOfTheOoxmlParser() = runBlocking {
        withFixture("legacy.xls") { source ->
            checkPages(XlsxConverter.convertToPages(context, Uri.fromFile(source), source.name),
                listOf("LEGACY CALC FIRST", "LEGACY CALC SECOND", "42", "37.25"))
        }
    }

    @Test fun protectedDeckAndWorkbookRequirePasswordsAndKeepPreviewsEncrypted() = runBlocking {
        for (name in listOf("protected.pptx", "protected.xlsx")) withFixture(name) { source ->
            suspend fun convert(password: String? = null): OfficeConversion =
                if (name.endsWith("pptx")) PptxConverter.convertPptxToPdf(context, Uri.fromFile(source), source.name, password)
                else XlsxConverter.convertToPages(context, Uri.fromFile(source), source.name, password)
            assertEquals(LoadFailure.PASSWORD_PROTECTED, (convert() as OfficeConversion.Failure).failure)
            assertEquals(LoadFailure.PASSWORD_PROTECTED, (convert("wrong") as OfficeConversion.Failure).failure)
            val opened = convert("mydoc-test") as OfficeConversion.Success
            try {
                assertNotNull(opened.renderPassword)
                assertTrue(opened.deleteWhenClosed)
                val renderer = MuPdfRenderer(filePath = opened.pdf.absolutePath)
                try {
                    assertTrue("The preview must stay encrypted on disk", renderer.isPasswordProtected)
                    assertTrue(renderer.authenticatePassword(requireNotNull(opened.renderPassword)))
                    assertEquals(2, renderer.pageCount)
                } finally { renderer.close() }
                assertEquals("Unlocking must not create a reusable clear preview",
                    LoadFailure.PASSWORD_PROTECTED, (convert() as OfficeConversion.Failure).failure)
            } finally { opened.pdf.delete() }
        }
    }

    @Test fun protectedOfficeViewerUnlocksWithoutASecondPdfPromptAndCleansUpOnClose() = runBlocking {
        val app = context.applicationContext as MyDocApp
        val previews = File(app.cacheDir, "converted_docs")
        fun privatePreviews() = previews.listFiles().orEmpty().filter { it.name.startsWith("word_private_") }.toSet()
        for (name in listOf("protected.pptx", "protected.xlsx")) withFixture(name) { source ->
            val store = ViewModelStore()
            val before = privatePreviews()
            try {
                val document = DocumentFile(Uri.fromFile(source).toString(), source.name, source.length(),
                    "application/octet-stream", 0L)
                lateinit var viewer: ViewerViewModel
                instrumentation.runOnMainSync {
                    viewer = ViewModelProvider(store,
                        ViewerViewModel.Factory(app, app.documentRepository, document))[ViewerViewModel::class.java]
                }
                withTimeout(90_000) { while (!viewer.uiState.value.isPasswordRequired) delay(25) }
                instrumentation.runOnMainSync { viewer.authenticatePassword("mydoc-test") }
                withTimeout(90_000) { while (viewer.uiState.value.isLoading || viewer.uiState.value.pageCount == 0) delay(25) }
                val state = viewer.uiState.value
                assertNull(state.errorMessage)
                assertFalse("The generated PDF must unlock automatically", state.isPasswordRequired)
                assertTrue(state.isOfficeEncrypted)
                assertEquals(2, state.pageCount)
                assertTrue(privatePreviews().minus(before).isNotEmpty())
                instrumentation.runOnMainSync { store.clear() }
                assertTrue("Closing must remove this session's encrypted preview", privatePreviews().minus(before).isEmpty())
            } finally { instrumentation.runOnMainSync { store.clear() } }
        }
    }

    private fun checkPages(result: OfficeConversion, expected: List<String>) {
        assertTrue("Expected native page conversion, got $result", result is OfficeConversion.Success)
        val renderer = MuPdfRenderer(filePath = requireNotNull(result.pdfFile).absolutePath)
        try {
            assertEquals("Both slides/visible worksheets must reach the PDF", 2, renderer.pageCount)
            val text = renderer.getDocumentText().orEmpty()
            for (value in expected) assertTrue("Missing $value from rendered pages: $text", text.contains(value))
        } finally { renderer.close() }
    }

    private fun assertRedPicture(result: OfficeConversion) {
        val renderer = MuPdfRenderer(filePath = requireNotNull(result.pdfFile).absolutePath)
        try {
            val bitmap = requireNotNull(renderer.renderPage(0, 1f, ReadingMode.NORMAL))
            try {
                var red = 0
                for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
                    val pixel = bitmap.getPixel(x, y)
                    if (Color.red(pixel) > 180 && Color.green(pixel) < 70 && Color.blue(pixel) < 70) red++
                }
                assertTrue("Source pictures/fills must be visible ink ($red pixels)", red > 500)
            } finally { bitmap.recycle() }
        } finally { renderer.close() }
    }

    private suspend fun withFixture(name: String, block: suspend (File) -> Unit) {
        val source = File.createTempFile("native-office-", ".${name.substringAfterLast('.')}", context.cacheDir)
        try {
            instrumentation.context.assets.open("office/$name").use { input ->
                source.outputStream().use { input.copyTo(it) }
            }
            block(source)
        } finally { source.delete() }
    }
}
