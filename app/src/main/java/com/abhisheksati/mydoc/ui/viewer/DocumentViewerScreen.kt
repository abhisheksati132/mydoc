package com.abhisheksati.mydoc.ui.viewer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.domain.model.DocumentFamily
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.DocumentFormats
import com.abhisheksati.mydoc.domain.model.LoadFailure
import com.abhisheksati.mydoc.domain.model.ViewerKind
import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.ui.viewer.pdf.PdfViewerScreen
import com.abhisheksati.mydoc.ui.viewer.presentation.PresentationViewerScreen
import com.abhisheksati.mydoc.ui.viewer.spreadsheet.SpreadsheetViewerScreen
import com.abhisheksati.mydoc.ui.viewer.text.TextViewerScreen
import com.abhisheksati.mydoc.ui.viewer.word.WordViewerScreen
import com.abhisheksati.mydoc.viewmodel.SpreadsheetViewModel
import com.abhisheksati.mydoc.viewmodel.ViewerViewModel

/**
 * Routes a document to the engine that can actually read it.
 *
 * Dispatch comes from [DocumentFormats], not from local extension lists: one table decides the
 * viewer, whether a converter is required, what the thumbnail can show, and what the reader must
 * disclose about fidelity. A format with no engine lands on [DocumentProblemScreen] instead of
 * being handed to a parser that cannot read it.
 *
 * Files whose extension is unknown still go to MuPDF, which reports its own error — a mislabelled
 * PDF should not become "unsupported" merely because nobody declared its extension.
 */
@Composable
fun DocumentViewerScreen(
    document: DocumentFile,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToolsClick: (() -> Unit)? = null,
    onSelectTool: ((PdfTool) -> Unit)? = null,
    onDocumentUpdated: ((DocumentFile) -> Unit)? = null,
    onSwipeToNextDocument: (() -> Unit)? = null,
    onSwipeToPreviousDocument: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as MyDocApp
    val ext = document.extension.lowercase()
    val descriptor = DocumentFormats.forExtension(ext)
    val declaredFailure = DocumentFormats.failureFor(ext)
    var isSlideshowActive by rememberSaveable { mutableStateOf(false) }
    var isWordReflowActive by rememberSaveable { mutableStateOf(false) }

    val viewer = when {
        descriptor == null -> ViewerKind.PDF_PAGES
        declaredFailure != null -> ViewerKind.UNSUPPORTED
        else -> descriptor.viewer
    }

    if (viewer == ViewerKind.UNSUPPORTED) {
        DocumentProblemScreen(
            failure = declaredFailure ?: LoadFailure.UNSUPPORTED_FORMAT,
            extension = ext,
            onBackClick = onBackClick,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    // Slideshow projection is a mode of the presentation viewer, not a format route.
    if (isSlideshowActive) {
        PresentationViewerScreen(
            document = document,
            onBackClick = { isSlideshowActive = false },
            onShareClick = onShareClick,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    when (viewer) {
        ViewerKind.SPREADSHEET_GRID -> {
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

        ViewerKind.PRESENTATION_SLIDES -> PresentationViewerScreen(
            document = document,
            onBackClick = onBackClick,
            onShareClick = onShareClick,
            modifier = modifier.fillMaxSize()
        )

        ViewerKind.WORD_REFLOW -> WordViewerScreen(
            document = document,
            onBackClick = onBackClick,
            onShareClick = onShareClick,
            modifier = modifier.fillMaxSize()
        )

        ViewerKind.TEXT_VIEWER -> TextViewerScreen(
            document = document,
            onBackClick = onBackClick,
            onShareClick = onShareClick,
            onSwipeToNextDocument = onSwipeToNextDocument,
            onSwipeToPreviousDocument = onSwipeToPreviousDocument,
            modifier = modifier.fillMaxSize()
        )

        ViewerKind.CONVERTED_PAGES, ViewerKind.PDF_PAGES -> {
            val viewerViewModel: ViewerViewModel = viewModel(
                key = document.uri,
                factory = ViewerViewModel.Factory(context, app.documentRepository, document)
            )
            val viewerState by viewerViewModel.uiState.collectAsStateWithLifecycle()
            val family = descriptor?.family
            val isWord = family == DocumentFamily.WORD

            if (isWord && (isWordReflowActive || viewerState.needsReflowFallback)) {
                WordViewerScreen(
                    document = document,
                    onBackClick = {
                        if (isWordReflowActive) isWordReflowActive = false else onBackClick()
                    },
                    onShareClick = onShareClick,
                    modifier = modifier.fillMaxSize()
                )
            } else {
                PdfViewerScreen(
                    document = document,
                    viewModel = viewerViewModel,
                    onBackClick = onBackClick,
                    onShareClick = onShareClick,
                    onToolsClick = onToolsClick,
                    onSelectTool = onSelectTool,
                    onDocumentUpdated = onDocumentUpdated,
                    onReflowClick = if (isWord) {
                        { isWordReflowActive = true }
                    } else {
                        null
                    },
                    onSlideshowClick = if (family == DocumentFamily.PRESENTATION) {
                        { isSlideshowActive = true }
                    } else {
                        null
                    },
                    onSwipeToNextDocument = onSwipeToNextDocument,
                    onSwipeToPreviousDocument = onSwipeToPreviousDocument,
                    modifier = modifier.fillMaxSize()
                )
            }
        }

        ViewerKind.UNSUPPORTED -> Unit
    }
}
