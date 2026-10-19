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
import com.abhisheksati.mydoc.ui.viewer.pdf.PdfViewerScreen
import com.abhisheksati.mydoc.ui.viewer.presentation.PresentationViewerScreen
import com.abhisheksati.mydoc.ui.viewer.spreadsheet.SpreadsheetViewerScreen
import com.abhisheksati.mydoc.ui.viewer.text.TextViewerScreen
import com.abhisheksati.mydoc.ui.viewer.word.WordViewerScreen
import com.abhisheksati.mydoc.viewmodel.SpreadsheetViewModel
import com.abhisheksati.mydoc.viewmodel.ViewerViewModel

@Composable
fun DocumentViewerScreen(
    document: DocumentFile,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    onToolsClick: (() -> Unit)? = null,
    onDocumentUpdated: ((DocumentFile) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as MyDocApp
    val ext = document.extension.lowercase()
    var isSlideshowActive by rememberSaveable { mutableStateOf(false) }

    when {
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

        // PowerPoint presentations: support slideshow mode toggle and default to high-fidelity PDF vertical viewer
        document.category == DocumentCategory.PPT || ext in setOf("ppt", "pptx", "pot", "potx", "pps", "ppsx", "odp", "fodp", "otp") -> {
            if (isSlideshowActive || ext == "ppt") {
                PresentationViewerScreen(
                    document = document,
                    onBackClick = {
                        if (isSlideshowActive) {
                            isSlideshowActive = false
                        } else {
                            onBackClick()
                        }
                    },
                    onShareClick = onShareClick,
                    modifier = modifier.fillMaxSize()
                )
            } else {
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
                    onDocumentUpdated = onDocumentUpdated,
                    onSlideshowClick = { isSlideshowActive = true },
                    modifier = modifier.fillMaxSize()
                )
            }
        }

        // Modern Word documents (.docx): route through high-fidelity PDF engine with fallback for legacy .doc
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
                onDocumentUpdated = onDocumentUpdated,
                modifier = modifier.fillMaxSize()
            )
        }

        // Excel spreadsheets (.xlsx, .xls): route through vector PDF pipeline
        document.category == DocumentCategory.EXCEL || ext in setOf("xlsx", "xls", "xlsm", "xltx", "xlt") -> {
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
                onDocumentUpdated = onDocumentUpdated,
                modifier = modifier.fillMaxSize()
            )
        }

        document.category == DocumentCategory.WORD || ext in setOf("doc", "dot", "rtf", "odt", "fodt", "ott", "wps", "wpd") -> {
            WordViewerScreen(
                document = document,
                onBackClick = onBackClick,
                onShareClick = onShareClick,
                modifier = modifier.fillMaxSize()
            )
        }

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

        else -> {
            // PDF and all other document formats
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
                onDocumentUpdated = onDocumentUpdated,
                modifier = modifier.fillMaxSize()
            )
        }
    }
}
