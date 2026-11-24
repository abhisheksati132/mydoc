package com.abhisheksati.mydoc.ui.viewer.controls

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.ui.viewer.pdf.PdfViewerScreen
import com.abhisheksati.mydoc.viewmodel.ViewerViewModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Exercises page changes through the actual PDF screen, including its zoom reset effects. */
class PdfReaderZoomOnDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun continuousPageChangeKeepsSelectedZoom() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as MyDocApp
        val file = File.createTempFile("reader-zoom-", ".pdf", app.cacheDir)
        val pdf = PdfDocument()
        try {
            repeat(5) { index ->
                val page = pdf.startPage(PdfDocument.PageInfo.Builder(300, 400, index + 1).create())
                page.canvas.drawText("Reader page ${index + 1}", 30f, 60f, Paint().apply { textSize = 20f })
                pdf.finishPage(page)
            }
            file.outputStream().use(pdf::writeTo)
        } finally { pdf.close() }
        val store = ViewModelStore()
        lateinit var viewer: ViewerViewModel
        val document = DocumentFile(Uri.fromFile(file).toString(), file.name, file.length(), "application/pdf", 0L)
        try {
            instrumentation.runOnMainSync {
                viewer = ViewModelProvider(store, ViewerViewModel.Factory(app, app.documentRepository, document))[ViewerViewModel::class.java]
            }
            compose.waitUntil(20_000) { !viewer.uiState.value.isLoading && viewer.uiState.value.pageCount == 5 }
            instrumentation.runOnMainSync { viewer.setContinuousScroll(true) }
            compose.setContent {
                AppDesignSystemTheme {
                    PdfViewerScreen(document, viewer, {}, {}, modifier = Modifier.testTag("pdf-reader"))
                }
            }
            compose.onNodeWithContentDescription("Zoom, 100%").performClick()
            compose.onNodeWithText("200%").performScrollTo().performClick()
            compose.onNodeWithText("Done").performClick()
            repeat(5) {
                compose.onNodeWithTag("pdf-reader").performTouchInput { swipeUp(durationMillis = 350) }
                compose.waitForIdle()
            }
            compose.runOnIdle { assertTrue("Scrolling must reach another PDF page", viewer.uiState.value.currentPage > 0) }
            compose.onNodeWithContentDescription("Zoom, 200%").assertExists()
        } finally {
            instrumentation.runOnMainSync { store.clear() }
            file.delete()
        }
    }
}
