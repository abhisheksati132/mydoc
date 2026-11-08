package com.abhisheksati.mydoc.rendering.engine.word

import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.viewmodel.ViewerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class WordViewerSessionOnDeviceTest {
    @Test fun wordPasswordUnlocksTheViewerOnceAndPreviewIsRemovedOnClose() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as MyDocApp
        val source = File.createTempFile("word-session-", ".docx", app.cacheDir)
        val store = ViewModelStore()
        val previews = File(app.cacheDir, "converted_docs")
        fun privatePreviews() = previews.listFiles().orEmpty().filter { it.name.startsWith("word_private_") }.toSet()
        val before = privatePreviews()
        try {
            instrumentation.context.assets.open("word/protected.docx").use { input -> source.outputStream().use { input.copyTo(it) } }
            val document = DocumentFile(Uri.fromFile(source).toString(), source.name, source.length(),
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 0L)
            lateinit var viewer: ViewerViewModel
            instrumentation.runOnMainSync {
                viewer = ViewModelProvider(store, ViewerViewModel.Factory(app, app.documentRepository, document))[ViewerViewModel::class.java]
            }
            withTimeout(20_000) { while (!viewer.uiState.value.isPasswordRequired) delay(25) }
            instrumentation.runOnMainSync { viewer.authenticatePassword("wrong") }
            withTimeout(20_000) { while (!viewer.uiState.value.isPasswordIncorrect) delay(25) }
            instrumentation.runOnMainSync { viewer.authenticatePassword("Word-test-42") }
            withTimeout(20_000) { while (viewer.uiState.value.isLoading || viewer.uiState.value.pageCount == 0) delay(25) }
            val state = viewer.uiState.value
            assertFalse("The derived PDF must not ask for a second password", state.isPasswordRequired)
            assertNull(state.errorMessage)
            assertTrue(state.isWordEncrypted)
            val text = viewer.getDocumentText().orEmpty()
            assertTrue("Unlocked synthetic fixture text: $text", text.contains("HEADER RETAINED BY WORD ENGINE"))
            assertTrue(privatePreviews().minus(before).isNotEmpty())
            instrumentation.runOnMainSync { store.clear() }
            assertTrue("Closing the viewer must delete its encrypted preview", privatePreviews().minus(before).isEmpty())
        } finally {
            instrumentation.runOnMainSync { store.clear() }
            source.delete()
        }
    }
}
