package com.abhisheksati.mydoc.ui.viewer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    val isLibreOfficeEnabled by app.settingsRepository.isLibreOfficeEngineEnabled.collectAsState()
    val isLibreOfficeReady = app.libreOfficeEngineManager.isReady()
    val isOfficeDocument = document.category in setOf(DocumentCategory.WORD, DocumentCategory.PPT, DocumentCategory.EXCEL) ||
            app.libreOfficeEngineManager.isSupportedFormat(ext)

    when {
        isLibreOfficeEnabled && isLibreOfficeReady && isOfficeDocument -> {
            // Render with LibreOffice Core Desktop Engine via unified document viewer
            val viewerViewModel: ViewerViewModel = viewModel(
                key = "lo_${document.uri}",
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

        document.category == DocumentCategory.EXCEL || ext == "csv" || ext == "tsv" -> {
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

        document.category == DocumentCategory.WORD || ext == "docx" || ext == "doc" -> {
            WordViewerScreen(
                document = document,
                onBackClick = onBackClick,
                onShareClick = onShareClick,
                modifier = modifier.fillMaxSize()
            )
        }

        document.category == DocumentCategory.PPT || ext == "pptx" || ext == "ppt" -> {
            PresentationViewerScreen(
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
