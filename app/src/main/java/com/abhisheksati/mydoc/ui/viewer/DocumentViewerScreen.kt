package com.abhisheksati.mydoc.ui.viewer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.domain.model.DocumentCategory
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.ui.viewer.pdf.PdfViewerScreen
import com.abhisheksati.mydoc.ui.viewer.presentation.PresentationViewerScreen
import com.abhisheksati.mydoc.ui.viewer.spreadsheet.SpreadsheetViewerScreen
import com.abhisheksati.mydoc.ui.viewer.text.TextViewerScreen
import com.abhisheksati.mydoc.ui.viewer.word.WordViewerScreen
import com.abhisheksati.mydoc.viewmodel.SpreadsheetViewModel
import com.abhisheksati.mydoc.viewmodel.ViewerViewModel

/**
 * Universal document viewer dispatcher routing documents to their optimal rendering engine.
 * Ordered strictly by format specificity with zero unreachable or duplicated branches.
 */
@Composable
fun DocumentViewerScreen(
    document: DocumentFile,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    onToolsClick: (() -> Unit)? = null,
    onSelectTool: ((PdfTool) -> Unit)? = null,
    onDocumentUpdated: ((DocumentFile) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as MyDocApp
    val ext = document.extension.lowercase()
    var isSlideshowActive by rememberSaveable { mutableStateOf(false) }

    when {
        // 1. Spreadsheets: Multi-sheet grid viewer supporting cell formulas, borders, and frozen panes.
        // Handled via SpreadsheetViewerScreen using Apache POI / CSV stream parsing.
        document.category == DocumentCategory.EXCEL || ext in setOf("xls", "xlsx", "xlsm", "xlsb", "csv", "tsv", "ods", "fods", "ots") -> {
            val spreadsheetViewModel: SpreadsheetViewModel = viewModel(
                key = document.uri,
                factory = SpreadsheetViewModel.Factory(context, document)
            )
            SpreadsheetViewerScreen(
                document = document,
                viewModel = spreadsheetViewModel,
                onBackClick = onBackClick,
                onShareClick = onShareClick,
                modifier = modifier.fillMaxSize()
            )
        }

        // 2. Presentations (Slideshow mode): Full-screen slide projection mode.
        // Activated explicitly from the top bar overflow menu or when opening standalone presentations.
        isSlideshowActive -> {
            PresentationViewerScreen(
                document = document,
                onBackClick = { isSlideshowActive = false },
                onShareClick = onShareClick,
                modifier = modifier.fillMaxSize()
            )
        }

        // 3. Presentations (Document reading mode): Rendered page-by-page through vector pipeline
        // with pinch-to-zoom, search, and page scrubber, plus top-bar option to start slideshow.
        document.category == DocumentCategory.PPT || ext in setOf("ppt", "pptx", "pot", "potx", "pps", "ppsx", "odp", "fodp", "otp") -> {
            val viewerViewModel: ViewerViewModel = viewModel(
                key = document.uri,
                factory = ViewerViewModel.Factory(context, app.documentRepository, document)
            )
            PdfViewerScreen(
                document = document,
                viewModel = viewerViewModel,
                onBackClick = onBackClick,
                onShareClick = onShareClick,
                onToolsClick = onToolsClick,
                onSelectTool = onSelectTool,
                onDocumentUpdated = onDocumentUpdated,
                onSlideshowClick = { isSlideshowActive = true },
                modifier = modifier.fillMaxSize()
            )
        }

        // 4. Modern Word processing documents (.docx, .dotx):
        // Converted and rendered through high-fidelity PDF/vector engine for crisp text and exact layout.
        ext in setOf("docx", "dotx") -> {
            val viewerViewModel: ViewerViewModel = viewModel(
                key = document.uri,
                factory = ViewerViewModel.Factory(context, app.documentRepository, document)
            )
            PdfViewerScreen(
                document = document,
                viewModel = viewerViewModel,
                onBackClick = onBackClick,
                onShareClick = onShareClick,
                onToolsClick = onToolsClick,
                onSelectTool = onSelectTool,
                onDocumentUpdated = onDocumentUpdated,
                modifier = modifier.fillMaxSize()
            )
        }

        // 5. Legacy Word documents (.doc, .rtf, .odt):
        // Handled via WordViewerScreen using streaming POI DocxParser block layout.
        document.category == DocumentCategory.WORD || ext in setOf("doc", "dot", "rtf", "odt", "fodt", "ott", "wps", "wpd") -> {
            WordViewerScreen(
                document = document,
                onBackClick = onBackClick,
                onShareClick = onShareClick,
                modifier = modifier.fillMaxSize()
            )
        }

        // 6. Plain text, logs, configuration files, and source code:
        // Handled via TextViewerScreen with virtualized line index, monospace typography, and syntax line numbers.
        document.category == DocumentCategory.TEXT || ext in setOf(
            "txt", "log", "json", "xml", "md", "yaml", "yml", "ini", "conf", "sh",
            "sql", "kt", "java", "py", "html", "css", "js", "ts", "c", "cpp", "h",
            "gradle", "properties", "bat"
        ) -> {
            TextViewerScreen(
                document = document,
                onBackClick = onBackClick,
                onShareClick = onShareClick,
                modifier = modifier.fillMaxSize()
            )
        }

        // 7. Fixed-layout vector documents (Default: PDF, EPUB, XPS, CBZ, and all others):
        // Handled via PdfViewerScreen with MuPDF native renderer, centroid pinch-to-zoom, and sharp tiles.
        else -> {
            val viewerViewModel: ViewerViewModel = viewModel(
                key = document.uri,
                factory = ViewerViewModel.Factory(context, app.documentRepository, document)
            )
            PdfViewerScreen(
                document = document,
                viewModel = viewerViewModel,
                onBackClick = onBackClick,
                onShareClick = onShareClick,
                onToolsClick = onToolsClick,
                onSelectTool = onSelectTool,
                onDocumentUpdated = onDocumentUpdated,
                modifier = modifier.fillMaxSize()
            )
        }
    }
}
