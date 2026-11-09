package com.abhisheksati.mydoc.ui.viewer.pdf

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.ui.viewer.controls.SearchOverlay
import com.abhisheksati.mydoc.ui.viewer.controls.ThumbnailStrip
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerBottomBar
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerTopBar
import com.abhisheksati.mydoc.viewmodel.ViewerUiState
import com.abhisheksati.mydoc.viewmodel.ViewerViewModel

/**
 * Top bar, search bar, thumbnail scrubber, and bottom bar controls for PDF viewer.
 */
@Composable
fun BoxScope.PdfViewerControlsOverlay(
    document: DocumentFile,
    state: ViewerUiState,
    viewModel: ViewerViewModel,
    isCurrentBookmarked: Boolean,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToolsClick: () -> Unit,
    onOcrClick: () -> Unit,
    onTtsClick: () -> Unit,
    onSlideshowClick: (() -> Unit)?,
    onReflowClick: (() -> Unit)?,
    onDocumentDetailsClick: () -> Unit,
    onOpenOutlineDrawer: () -> Unit,
    onOpenThumbnailGrid: () -> Unit,
    onOpenReadingControls: () -> Unit,
    onPageSelected: (Int) -> Unit,
    isCondensed: Boolean = false,
    hazeState: dev.chrisbanes.haze.HazeState? = null
) {
    // Clean, condensing Top Toolbar on a GlassSurface
    com.abhisheksati.mydoc.ui.viewer.controls.AdaptiveInkProvider(
        enabled=state.areControlsVisible && !state.isSearchOpen
    ) {
    ViewerTopBar(
        title = document.displayName,
        currentPage = state.currentPage,
        pageCount = state.pageCount,
        visible = state.areControlsVisible && !state.isSearchOpen,
        isBookmarked = isCurrentBookmarked,
        onBackClick = onBackClick,
        onSearchClick = viewModel::openSearch,
        onToggleBookmark = onToggleBookmark,
        onShareClick = onShareClick,
        isCondensed = isCondensed,
        onToolsClick = onToolsClick,
        onOcrClick = onOcrClick,
        onTtsClick = onTtsClick,
        onSlideshowClick = onSlideshowClick,
        onReflowClick = onReflowClick,
        reflowLabel = if (com.abhisheksati.mydoc.domain.model.DocumentFormats.familyOf(document.extension) ==
            com.abhisheksati.mydoc.domain.model.DocumentFamily.SPREADSHEET)
            androidx.compose.ui.res.stringResource(com.abhisheksati.mydoc.R.string.viewer_action_cell_grid) else null,
        onDocumentDetailsClick = onDocumentDetailsClick,
        hazeState = hazeState,
        modifier = Modifier.align(Alignment.TopCenter)
    )
    }

    // Search Overlay
    SearchOverlay(        visible = state.isSearchOpen,
        query = state.searchQuery,
        matchCount = state.searchMatches.size,
        currentMatchIndex = state.currentMatchIndex,
        onQueryChanged = viewModel::onSearchQueryChanged,
        onNextMatch = viewModel::nextSearchMatch,
        onPreviousMatch = viewModel::previousSearchMatch,
        onCloseSearch = viewModel::closeSearch,
        modifier = Modifier.align(Alignment.TopCenter)
    )

    // Ergonomic Glass Bottom Controls
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
    ) {
        ThumbnailStrip(
            pageCount = state.pageCount,
            currentPage = state.currentPage,
            visible = state.isThumbnailStripVisible && state.areControlsVisible,
            onPageSelected = onPageSelected,
            renderThumbnail = { p, s -> viewModel.renderPageBitmap(p, s) }
        )

        ViewerBottomBar(
            currentPage = state.currentPage,
            pageCount = state.pageCount,
            visible = state.areControlsVisible,
            hasOutline = state.outline.isNotEmpty(),
            onOutlineClick = onOpenOutlineDrawer,
            onPagesClick = onOpenThumbnailGrid,
            onAppearanceClick = onOpenReadingControls,
            onPageScrubbed = onPageSelected,
            sheetItems = state.outline,
            onSheetSelect = onPageSelected,
            hazeState = hazeState
        )
    }
}
