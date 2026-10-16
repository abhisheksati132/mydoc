package com.abhisheksati.mydoc.ui.viewer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.domain.model.DocumentCategory
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.ui.viewer.pdf.PdfViewerScreen
import com.abhisheksati.mydoc.ui.viewer.spreadsheet.SpreadsheetViewerScreen
import com.abhisheksati.mydoc.viewmodel.SpreadsheetViewModel
import com.abhisheksati.mydoc.viewmodel.ViewerViewModel

@Composable
fun DocumentViewerScreen(
    document: DocumentFile,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as MyDocApp

    if (document.category == DocumentCategory.EXCEL || document.extension.equals("csv", ignoreCase = true)) {
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
            modifier = modifier.fillMaxSize()
        )
    }
}
