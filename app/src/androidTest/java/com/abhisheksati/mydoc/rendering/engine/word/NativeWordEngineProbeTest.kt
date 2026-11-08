package com.abhisheksati.mydoc.rendering.engine.word

import androidx.test.platform.app.InstrumentationRegistry
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class NativeWordEngineProbeTest {
    @Test fun nativeEngineExportsWordHeadersAndFooters() = kotlinx.coroutines.runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val jobs = File(context.cacheDir, "word_engine_jobs").apply { mkdirs() }
        val job = java.nio.file.Files.createTempDirectory(jobs.toPath(), "word-probe-").toFile()
        val source = File(job, "input.docx")
        val output = File(job, "output.pdf")
        try {
            instrumentation.context.assets.open("word/fidelity.docx").use { input ->
                source.outputStream().use { input.copyTo(it) }
            }
            val result = WordEngineConnection.convert(context, source, output)
            assertTrue(result.success)
            assertNotEquals("Native conversion must run outside the reader process", android.os.Process.myPid(), result.processId)
            PDFBoxResourceLoader.init(context)
            PDDocument.load(output).use { pdf ->
                val text = PDFTextStripper().getText(pdf)
                assertTrue(text.contains("HEADER RETAINED BY WORD ENGINE"))
                assertTrue(text.contains("FOOTER RETAINED BY WORD ENGINE"))
                assertTrue(text.contains("MERGED TABLE HEADER"))
            }
        } finally { source.delete(); output.delete(); job.delete() }
    }
}
