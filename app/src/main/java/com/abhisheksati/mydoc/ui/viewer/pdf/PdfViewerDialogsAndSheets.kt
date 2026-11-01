package com.abhisheksati.mydoc.ui.viewer.pdf

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.ocr.OcrResultSheet
import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.tools.tts.DocumentTtsManager
import com.abhisheksati.mydoc.ui.home.DocumentDetailsDialog
import com.abhisheksati.mydoc.ui.tts.DocumentTtsBar
import com.abhisheksati.mydoc.ui.viewer.controls.DocumentToolsSheet
import com.abhisheksati.mydoc.ui.viewer.controls.OutlineDrawer
import com.abhisheksati.mydoc.ui.viewer.controls.PageGridSheet
import com.abhisheksati.mydoc.ui.viewer.controls.PageJumpDialog
import com.abhisheksati.mydoc.ui.viewer.controls.PasswordDialog
import com.abhisheksati.mydoc.ui.viewer.controls.ReadingControlsBottomSheet
import com.abhisheksati.mydoc.viewmodel.ViewerUiState

/**
 * Encapsulates all modal sheets, drawers, and overlay dialogs for the PDF viewer.
 */
@Composable
fun PdfViewerDialogsAndSheets(
    document: DocumentFile,
    state: ViewerUiState,
    bookmarks: Set<Int>,
    isReadingControlsOpen: Boolean,
    onDismissReadingControls: () -> Unit,
    onReadingModeChange: (ReadingMode) -> Unit,
    onScrollModeChange: (Boolean) -> Unit,
    onDualPageViewChange: (Boolean) -> Unit,
    currentBrightness: Float,
    onBrightnessChange: (Float) -> Unit,
    isKeepScreenOn: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onFitPageChange: (Boolean) -> Unit,
    isDocumentDetailsOpen: Boolean,
    onDismissDocumentDetails: () -> Unit,
    onShareClick: () -> Unit,
    isThumbnailGridOpen: Boolean,
    onDismissThumbnailGrid: () -> Unit,
    onPageSelected: (Int) -> Unit,
    renderThumbnail: (Int, Float) -> Bitmap?,
    isPageJumpDialogOpen: Boolean,
    onDismissPageJumpDialog: () -> Unit,
    onJumpToPage: (Int) -> Unit,
    onDismissOutlineDrawer: () -> Unit,
    onSelectOutlineItem: (Int) -> Unit,
    onRemoveBookmark: (Int) -> Unit,
    isToolsSheetOpen: Boolean,
    onDismissToolsSheet: () -> Unit,
    onSelectTool: (PdfTool) -> Unit,
    onAuthenticatePassword: (String) -> Unit,
    onCancelPassword: () -> Unit,
    ocrExtractedText: String?,
    onDismissOcr: () -> Unit,
    isTtsVisible: Boolean,
    onCloseTts: () -> Unit,
    currentTtsText: String,
    ttsManager: DocumentTtsManager,
    modifier: Modifier = Modifier
) {
    // Consolidated Reading Controls Bottom Sheet
    ReadingControlsBottomSheet(
        isOpen = isReadingControlsOpen,
        onDismiss = onDismissReadingControls,
        readingMode = state.readingMode,
        onReadingModeChange = onReadingModeChange,
        isContinuousScroll = state.isContinuousScroll,
        onScrollModeChange = onScrollModeChange,
        isDualPageView = state.isDualPageView,
        onDualPageViewChange = onDualPageViewChange,
        currentBrightness = currentBrightness,
        onBrightnessChange = onBrightnessChange,
        isKeepScreenOn = isKeepScreenOn,
        onKeepScreenOnChange = onKeepScreenOnChange,
        document = document,
        isFitPage = state.isFitPage,
        onFitPageChange = onFitPageChange
    )

    // Document Details Dialog
    if (isDocumentDetailsOpen) {
        DocumentDetailsDialog(
            document = document,
            onDismiss = onDismissDocumentDetails,
            onOpen = { onDismissDocumentDetails() },
            onShare = { onShareClick() },
            onRemove = { onDismissDocumentDetails() }
        )
    }

    // 3-Column Page Thumbnail Grid Overview Sheet
    PageGridSheet(
        isOpen = isThumbnailGridOpen,
        pageCount = state.pageCount,
        currentPage = state.currentPage,
        bookmarks = bookmarks,
        onDismiss = onDismissThumbnailGrid,
        onPageSelected = onPageSelected,
        renderThumbnail = renderThumbnail
    )

    // Page Jump Dialog
    PageJumpDialog(
        isOpen = isPageJumpDialogOpen,
        currentPage = state.currentPage,
        pageCount = state.pageCount,
        onJump = onJumpToPage,
        onDismiss = onDismissPageJumpDialog
    )

    // Table of Contents & Bookmarks Drawer
    OutlineDrawer(
        outline = state.outline,
        bookmarks = bookmarks,
        isOpen = state.isOutlineDrawerOpen,
        onDismiss = onDismissOutlineDrawer,
        onItemSelected = onSelectOutlineItem,
        onRemoveBookmark = onRemoveBookmark
    )

    // Document In-Viewer Tools Sheet
    DocumentToolsSheet(
        isOpen = isToolsSheetOpen,
        onDismiss = onDismissToolsSheet,
        document = document,
        onSelectTool = onSelectTool
    )

    // Password Dialog
    PasswordDialog(
        isOpen = state.isPasswordRequired,
        isIncorrect = state.isPasswordIncorrect,
        onSubmit = onAuthenticatePassword,
        onCancel = onCancelPassword
    )

    // OCR Result Modal Sheet
    if (ocrExtractedText != null) {
        OcrResultSheet(
            text = ocrExtractedText,
            onDismiss = onDismissOcr
        )
    }

    // Text-to-Speech (TTS) Read Aloud floating playback controller
    DocumentTtsBar(
        ttsManager = ttsManager,
        documentText = currentTtsText,
        isVisible = isTtsVisible,
        onClose = onCloseTts,
        modifier = modifier
    )
}
