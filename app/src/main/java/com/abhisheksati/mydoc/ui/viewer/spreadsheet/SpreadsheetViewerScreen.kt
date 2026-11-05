package com.abhisheksati.mydoc.ui.viewer.spreadsheet

import com.abhisheksati.mydoc.designsystem.components.AppProgressIndicator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.designsystem.components.OverlaySystemBars
import com.abhisheksati.mydoc.designsystem.tokens.DocumentCanvasColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.LoadFailure
import com.abhisheksati.mydoc.ui.viewer.DocumentProblemScreen
import com.abhisheksati.mydoc.domain.model.ReadingPrefs
import com.abhisheksati.mydoc.rendering.engine.OutlineItem
import com.abhisheksati.mydoc.rendering.engine.spreadsheet.SheetData
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerCapabilities
import com.abhisheksati.mydoc.ui.viewer.controls.edgeFlingToDocument
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerChrome
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerScaffold
import com.abhisheksati.mydoc.ui.viewer.controls.ViewerThemeTokens
import com.abhisheksati.mydoc.viewmodel.SpreadsheetViewModel

/**
 * Modern spreadsheet table viewer wrapped in unified ViewerScaffold.
 * Features frozen column and row headers, cell selection, multi-sheet tabs, and in-sheet cell search.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpreadsheetViewerScreen(
    document: DocumentFile,
    viewModel: SpreadsheetViewModel,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSwipeToNextDocument: (() -> Unit)? = null,
    onSwipeToPreviousDocument: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as? MyDocApp
    val colors = LocalAppColors.current

    val state by viewModel.uiState.collectAsState()
    var isSearchOpen by remember { mutableStateOf(false) }

    val gridState = rememberLazyListState()
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    val defaultPrefs = remember { ReadingPrefs() }
    val readingPrefs by (app?.settingsRepository?.readingPrefs ?: remember { kotlinx.coroutines.flow.MutableStateFlow(defaultPrefs) }).collectAsState()

    val textColor = ViewerThemeTokens.getTextColor(readingPrefs.readingMode, colors.textPrimary)

    val outlineItems = remember(state.model.sheets) {
        state.model.sheets.mapIndexed { idx, s ->
            OutlineItem(title = s.name, pageIndex = idx, level = 0)
        }
    }

    ViewerScaffold(
        document = document,
        capabilities = ViewerCapabilities(
            supportsSearch = true,
            supportsOutline = state.model.sheets.size > 1,
            supportsPageGrid = false,
            supportsSlideshow = false,
            supportsTts = false,
            supportsZoom = true
        ),
        currentPage = state.selectedSheetIndex,
        pageCount = state.model.sheets.size.coerceAtLeast(1),
        isControlsVisible = !isSearchOpen,
        onBackClick = onBackClick,
        onShareClick = onShareClick,
        onSearchClick = { isSearchOpen = true },
        sheetItems = outlineItems,
        onSheetSelect = { viewModel.selectSheet(it) },
        readingPrefs = readingPrefs,
        fidelityNotes = buildList {
            if (state.currentSheet?.truncated == true) {
                add("This sheet hit our read limit — later rows are not shown")
            }
            // A worksheet whose XML could not be parsed reads as an empty sheet unless it says
            // otherwise, which is how a damaged workbook got presented as a blank one.
            state.currentSheet?.parseFailure?.let { reason ->
                add("This sheet could not be read: $reason")
            }
            val failed = state.model.failedSheets
            if (failed.isNotEmpty()) {
                val total = state.model.sheets.size
                add(
                    if (failed.size == 1) "1 of $total sheets could not be read: ${failed.first()}"
                    else "${failed.size} of $total sheets could not be read: ${failed.joinToString(", ")}"
                )
            }
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .edgeFlingToDocument(
                    atFirstPage = {
                        state.selectedSheetIndex == 0 &&
                            gridState.firstVisibleItemIndex == 0 &&
                            gridState.firstVisibleItemScrollOffset == 0 &&
                            horizontalScrollState.value == 0
                    },
                    atLastPage = {
                        state.selectedSheetIndex == state.model.sheets.size - 1 &&
                            !gridState.canScrollForward
                    },
                    onPrevious = onSwipeToPreviousDocument,
                    onNext = onSwipeToNextDocument
                )
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = ViewerChrome.topBarHeight, bottom = ViewerChrome.bottomBarHeight)
        ) {
            // In-sheet search input
            if (isSearchOpen) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    placeholder = { Text("Search sheet cells…") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AppProgressIndicator()
                }
            } else if (state.errorMessage != null) {
                DocumentProblemScreen(
                    failure = state.failure ?: LoadFailure.CORRUPT_DOCUMENT,
                    extension = document.extension,
                    onBackClick = onBackClick
                )
            } else {
                val currentSheet = state.currentSheet
                if (currentSheet != null) {
                    val maxRow = maxOf(25, currentSheet.maxRow + 5)
                    val maxCol = maxOf(10, currentSheet.maxCol + 3)
                    val rowHeight = 36.dp
                    val headerColWidth = 44.dp
                    // Widths come from the workbook itself; 110dp is only the fallback for
                    // columns the file never sized.
                    fun widthFor(c: Int) = currentSheet.columnWidths[c]?.dp ?: 110.dp

                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Top Frozen Header (Column names: A, B, C...)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                // Corner box
                                Box(
                                    modifier = Modifier
                                        .width(headerColWidth)
                                        .height(rowHeight)
                                        .border(0.5.dp, MaterialTheme.colorScheme.outline)
                                )

                                // Column Headers
                                Row(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                                    for (c in 0..maxCol) {
                                        Box(
                                            modifier = Modifier
                                                .width(widthFor(c))
                                                .height(rowHeight)
                                                .border(0.5.dp, MaterialTheme.colorScheme.outline),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = SheetData.columnIndexToLetter(c),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Normal,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Body Grid with Virtualized Rows and Frozen Row Header
                            LazyColumn(state = gridState, modifier = Modifier.fillMaxSize()) {
                                items(maxRow + 1) { r ->
                                    Row(modifier = Modifier.height(rowHeight)) {
                                        // Row Header (frozen horizontally)
                                        Box(
                                            modifier = Modifier
                                                .width(headerColWidth)
                                                .fillMaxHeight()
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .border(0.5.dp, MaterialTheme.colorScheme.outline),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${r + 1}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Cells Row (horizontal scroll synchronized across all rows)
                                        Row(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                                            for (c in 0..maxCol) {
                                                val cellText = currentSheet.getCellValue(r, c)
                                                val isSelected = state.selectedCell == (r to c)
                                                val isMatch = state.searchQuery.isNotBlank() && cellText.contains(state.searchQuery, ignoreCase = true)

                                                val isCovered = currentSheet.isMergedNonAnchor(r, c)
                                                val cellBg = when {
                                                    isSelected -> colors.accent.copy(alpha = 0.25f)
                                                    isMatch -> DocumentCanvasColors.searchHighlight
                                                    // A merged range keeps its value in the anchor cell;
                                                    // shading the rest shows the span rather than a gap.
                                                    isCovered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                    else -> Color.Transparent
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .width(widthFor(c))
                                                        .fillMaxHeight()
                                                        .border(
                                                            width = if (isSelected) 1.5.dp else 0.5.dp,
                                                            color = if (isSelected) colors.accent else MaterialTheme.colorScheme.outlineVariant
                                                        )
                                                        .background(cellBg)
                                                        .clickable { viewModel.selectCell(r, c) }
                                                        .padding(horizontal = 6.dp),
                                                    contentAlignment = Alignment.CenterStart
                                                ) {
                                                    Text(
                                                        text = cellText,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = textColor,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        textAlign = TextAlign.Start
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Cell Inspection Bottom Sheet
        val selected = state.selectedCell
        if (selected != null) {
            val (r, c) = selected
            val cellVal = state.currentSheet?.getCellValue(r, c) ?: ""
            val cellRef = "${SheetData.columnIndexToLetter(c)}${r + 1}"

            ModalBottomSheet(
                onDismissRequest = viewModel::clearSelectedCell,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                OverlaySystemBars()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "Cell $cellRef",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = cellVal.ifEmpty { "(Empty cell)" },
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
