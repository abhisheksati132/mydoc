package com.abhisheksati.mydoc.rendering.engine.word

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.abhisheksati.mydoc.rendering.engine.OfficeConversion
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class WordFidelityOnDeviceTest {
    @Test fun everyRegisteredWordFormatOpensThroughThePageViewer() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        for (name in listOf("fidelity.docx", "template.dotx", "legacy-image.doc", "legacy-template.dot", "sample.odt", "sample.ott", "sample.fodt", "sample.rtf")) {
            val source = File.createTempFile("word-format-", ".${name.substringAfterLast('.')}", context.cacheDir)
            try {
                instrumentation.context.assets.open("word/$name").use { input -> source.outputStream().use { input.copyTo(it) } }
                val result = DocxConverter.convertDocxToPdf(context, Uri.fromFile(source), source.name)
                assertTrue("$name must produce readable pages", result is OfficeConversion.Success)
                val renderer = com.abhisheksati.mydoc.rendering.engine.MuPdfRenderer(filePath = requireNotNull(result.pdfFile).absolutePath)
                try { assertTrue(renderer.pageCount > 0) } finally { renderer.close() }
            } finally { source.delete() }
        }
        // The native worker's network restriction must never affect the reader process.
        android.system.Os.close(android.system.Os.socket(android.system.OsConstants.AF_INET,
            android.system.OsConstants.SOCK_STREAM, 0))
    }

    @Test fun tablePictureAndTextColorRemainVisibleOnTheRenderedPage() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val source = File.createTempFile("word-pixels-", ".docx", context.cacheDir)
        try {
            instrumentation.context.assets.open("word/fidelity.docx").use { input -> source.outputStream().use { input.copyTo(it) } }
            val result = DocxConverter.convertDocxToPdf(context, Uri.fromFile(source), source.name)
            assertTrue(result is OfficeConversion.Success)
            val renderer = com.abhisheksati.mydoc.rendering.engine.MuPdfRenderer(filePath = requireNotNull(result.pdfFile).absolutePath)
            try {
                val bitmap = requireNotNull(renderer.renderPage(0, 1f, com.abhisheksati.mydoc.domain.model.ReadingMode.NORMAL))
                try {
                    var red = 0
                    var blue = 0
                    for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
                        val pixel = bitmap.getPixel(x, y)
                        val r = android.graphics.Color.red(pixel)
                        val g = android.graphics.Color.green(pixel)
                        val b = android.graphics.Color.blue(pixel)
                        if (r > 200 && g < 70 && b < 70) red++
                        if (b > 90 && b > r * 1.5f && b > g * 1.2f) blue++
                    }
                    assertTrue("The red picture inside the table must be painted", red > 500)
                    assertTrue("The title must retain its blue text", blue > 100)
                } finally { bitmap.recycle() }
            } finally { renderer.close() }
        } finally { source.delete() }
    }
    @Test fun protectedWordUnlocksWithoutLeavingAnUnlockedCachedPreview() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val source = File.createTempFile("locked-word-", ".docx", context.cacheDir)
        var preview: File? = null
        try {
            instrumentation.context.assets.open("word/protected.docx").use { input -> source.outputStream().use { input.copyTo(it) } }
            val wrong = DocxConverter.convertDocxToPdf(context, Uri.fromFile(source), source.name, "wrong")
            assertEquals(com.abhisheksati.mydoc.domain.model.LoadFailure.PASSWORD_PROTECTED, (wrong as OfficeConversion.Failure).failure)
            val result = DocxConverter.convertDocxToPdf(context, Uri.fromFile(source), source.name, "Word-test-42")
            assertTrue("The correct Word password must open the document", result is OfficeConversion.Success)
            result as OfficeConversion.Success
            preview = result.pdf
            assertTrue(result.deleteWhenClosed)
            assertNotNull(result.renderPassword)
            val renderer = com.abhisheksati.mydoc.rendering.engine.MuPdfRenderer(filePath = result.pdf.absolutePath)
            try {
                assertTrue("A protected Word preview must remain encrypted on disk", renderer.isPasswordProtected)
                assertTrue(renderer.authenticatePassword(requireNotNull(result.renderPassword)))
                assertFalse("A status check must preserve the unlocked state", renderer.isPasswordProtected)
                assertTrue(renderer.getDocumentText().orEmpty().contains("HEADER RETAINED BY WORD ENGINE"))
                assertTrue(renderer.pageCount > 0)
            } finally { renderer.close() }
            val reopened = DocxConverter.convertDocxToPdf(context, Uri.fromFile(source), source.name)
            assertEquals(com.abhisheksati.mydoc.domain.model.LoadFailure.PASSWORD_PROTECTED, (reopened as OfficeConversion.Failure).failure)
        } finally { source.delete(); preview?.delete() }
    }
    @Test fun passwordProtectedWordRequestsAuthenticationRatherThanReportingDamage() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val source = File.createTempFile("locked-word-", ".docx", context.cacheDir)
        try {
            instrumentation.context.assets.open("word/protected.docx").use { input ->
                source.outputStream().use { input.copyTo(it) }
            }
            val result = DocxConverter.convertDocxToPdf(context, Uri.fromFile(source), source.name)
            assertTrue(result is OfficeConversion.Failure)
            assertEquals(com.abhisheksati.mydoc.domain.model.LoadFailure.PASSWORD_PROTECTED,
                (result as OfficeConversion.Failure).failure)
        } finally { source.delete() }
    }
    @Test fun headersFootersAndTableContentSurviveWordOpening() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val source = File.createTempFile("word-fidelity-", ".docx", context.cacheDir)
        try {
            instrumentation.context.assets.open("word/fidelity.docx").use { input ->
                source.outputStream().use(input::copyTo)
            }
            val before = source.readBytes()
            val result = DocxConverter.convertDocxToPdf(context, Uri.fromFile(source), source.name)
            assertTrue("Word conversion must produce pages", result is OfficeConversion.Success)
            PDFBoxResourceLoader.init(context)
            PDDocument.load(requireNotNull(result.pdfFile)).use { pdf ->
                val text = PDFTextStripper().getText(pdf)
                assertTrue("The source header must be retained", text.contains("HEADER RETAINED BY WORD ENGINE"))
                assertTrue("The source footer must be retained", text.contains("FOOTER RETAINED BY WORD ENGINE"))
                assertTrue(text.contains("MERGED TABLE HEADER"))
                assertTrue(text.contains("Final row right"))
            }
            assertArrayEquals("Viewing must not modify the source", before, source.readBytes())
        } finally { source.delete() }
    }
}
