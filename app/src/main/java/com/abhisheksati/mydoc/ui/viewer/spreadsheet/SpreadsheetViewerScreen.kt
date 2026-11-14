package com.abhisheksati.mydoc.ui.viewer.spreadsheet

import com.abhisheksati.mydoc.designsystem.components.AppModalSheet
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.components.AppProgressIndicator
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
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
import com.abhisheksati.mydoc.ui.viewer.controls.OutlineDrawer
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

    BackHandler(enabled = isSearchOpen) {
        isSearchOpen = false
        viewModel.onSearchQueryChanged("")
    }

    var showSheets by remember { mutableStateOf(false) }
    OutlineDrawer(
        outline = outlineItems,
        isOpen = showSheets,
        onDismiss = { showSheets = false },
        onItemSelected = viewModel::selectSheet
    )

    ViewerScaffold(
        document = document,
        capabilities = ViewerCapabilities(
            supportsSearch = true,
            supportsOutline = state.model.sheets.size > 1,
            supportsPageGrid = false,
            supportsSlideshow = false,
            supportsTts = false,
            supportsZoom = false,
            supportsContinuousScroll = false,
            supportsDualPageView = false
        ),
        currentPage = state.selectedSheetIndex,
        pageCount = state.model.sheets.size.coerceAtLeast(1),
        isControlsVisible = !isSearchOpen,
        onBackClick = onBackClick,
        onShareClick = onShareClick,
        onSearchClick = { isSearchOpen = true },
        onOutlineClick = { showSheets = true },
        onPagesClick = if (outlineItems.isNotEmpty()) { { showSheets = true } } else null,
        sheetItems = outlineItems,
        onSheetSelect = { viewModel.selectSheet(it) },
        readingPrefs = readingPrefs,
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
                    placeholder = { Text(stringResource(R.string.sheet_search_placeholder)) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (state.searchQuery.isNotEmpty()) {
                                    viewModel.onSearchQueryChanged("")
                                } else {
                                    isSearchOpen = false
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.cd_close_search))
                        }
                    },
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
                val parseFailure = currentSheet?.parseFailure
                if (parseFailure != null) {
                    Text(
                        stringResource(R.string.sheet_read_failed, parseFailure),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer).padding(16.dp)
                    )
                } else if (currentSheet != null) {
                    val maxRow = maxOf(25, currentSheet.maxRow + 5)
                    val maxCol = maxOf(10, currentSheet.maxCol + 3)
                    val rowHeight = 36.dp
                    val headerColWidth = 44.dp
                    val gridBorderColor = MaterialTheme.colorScheme.outlineVariant
                    // Widths come from the workbook itself; 110dp is only the fallback for
                    // columns the file never sized.
                    fun widthFor(c: Int) = currentSheet.columnWidths[c]?.dp ?: 110.dp
                    fun heightFor(r: Int) = (currentSheet.rowHeightsDp[r] ?: currentSheet.defaultRowHeightDp).dp

                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (currentSheet.truncated) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.sheet_truncated_banner, currentSheet.maxRow + 1),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
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
                                    Row(modifier = Modifier.height(heightFor(r))) {
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
                                                val cellStyle = currentSheet.cellStyles[r to c]
                                                val isSelected = state.selectedCell == (r to c)
                                                val isMatch = state.searchQuery.isNotBlank() && cellText.contains(state.searchQuery, ignoreCase = true)

                                                val isCovered = currentSheet.isMergedNonAnchor(r, c)
                                                val cellBg = when {
                                                    isSelected -> colors.accent.copy(alpha = 0.25f)
                                                    isMatch -> DocumentCanvasColors.searchHighlight
                                                    currentSheet.conditionalFills[r to c] != null -> Color(currentSheet.conditionalFills.getValue(r to c))
                                                    // A merged range keeps its value in the anchor cell;
                                                    // shading the rest shows the span rather than a gap.
                                                    isCovered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                    else -> cellStyle?.fillArgb?.let(::Color) ?: Color.Transparent
                                                }
                                                val cellTextColor = cellStyle?.fontArgb?.let(::Color) ?: textColor
                                                val cellTextAlign = when (cellStyle?.horizontalAlignment) {
                                                    "center", "centerContinuous" -> TextAlign.Center
                                                    "right" -> TextAlign.End
                                                    else -> TextAlign.Start
                                                }
                                                val cellVerticalAlignment = when (cellStyle?.verticalAlignment) {
                                                    "top" -> Alignment.TopStart
                                                    "center", "distributed", "justify" -> Alignment.CenterStart
                                                    else -> Alignment.BottomStart
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
                                                        .drawBehind { drawSpreadsheetBorders(cellStyle, gridBorderColor) }
                                                        .clickable { viewModel.selectCell(r, c) }
                                                        .padding(horizontal = 6.dp, vertical = if (cellStyle?.wrapText == true) 2.dp else 0.dp),
                                                    contentAlignment = cellVerticalAlignment
                                                ) {
                                                    Text(
                                                        text = cellText,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = cellTextColor,
                                                        fontWeight = if (cellStyle?.bold == true) FontWeight.Bold else FontWeight.Normal,
                                                        fontStyle = if (cellStyle?.italic == true) FontStyle.Italic else FontStyle.Normal,
                                                        textDecoration = when {
                                                            cellStyle?.underline == true && cellStyle.strike -> TextDecoration.combine(
                                                                listOf(TextDecoration.Underline, TextDecoration.LineThrough)
                                                            )
                                                            cellStyle?.underline == true -> TextDecoration.Underline
                                                            cellStyle?.strike == true -> TextDecoration.LineThrough
                                                            else -> TextDecoration.None
                                                        },
                                                        maxLines = if (cellStyle?.wrapText == true) {
                                                            (heightFor(r).value / 18f).toInt().coerceIn(1, 12)
                                                        } else 1,
                                                        softWrap = cellStyle?.wrapText == true,
                                                        overflow = if (cellStyle?.wrapText == true) TextOverflow.Clip else TextOverflow.Ellipsis,
                                                        textAlign = cellTextAlign
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
            val sheet = state.currentSheet
            val cellVal = sheet?.getCellValue(r, c).orEmpty()
            val formula = sheet?.formulas?.get(r to c)
            val cellRef = "${SheetData.columnIndexToLetter(c)}${r + 1}"

            AppModalSheet(
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
                        text = stringResource(R.string.cell_label, cellRef),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.cell_value),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = cellVal.ifEmpty { stringResource(R.string.cell_empty) },
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor
                    )
                    if (formula != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.cell_formula),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "=$formula",
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

private fun DrawScope.drawSpreadsheetBorders(style: com.abhisheksati.mydoc.rendering.engine.spreadsheet.SpreadsheetCellStyle?, fallback: Color) {
    style ?: return
    fun draw(edge: com.abhisheksati.mydoc.rendering.engine.spreadsheet.SpreadsheetBorder?, horizontal: Boolean, leading: Boolean) {
        edge ?: return
        val stroke = edge.widthDp.coerceAtLeast(0.5f).dp.toPx()
        val color = edge.colorArgb?.let(::Color) ?: fallback
        val dash = if (edge.dashed) PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 3.dp.toPx())) else null
        val gap = stroke.coerceAtLeast(1.dp.toPx())
        val center = when {
            horizontal && leading -> 0f
            horizontal -> size.height
            leading -> 0f
            else -> size.width
        }
        fun line(position: Float) {
            val start: Offset
            val end: Offset
            if (horizontal) {
                start = Offset(0f, position)
                end = Offset(size.width, position)
            } else {
                start = Offset(position, 0f)
                end = Offset(position, size.height)
            }
            drawLine(color, start, end, stroke, pathEffect = dash)
        }
        if (edge.double) {
            line(center + if (leading) gap / 2f else -gap / 2f)
            line(center + if (leading) gap * 1.5f else -gap * 1.5f)
        } else line(center)
    }
    draw(style.topBorder, horizontal = true, leading = true)
    draw(style.bottomBorder, horizontal = true, leading = false)
    draw(style.leftBorder, horizontal = false, leading = true)
    draw(style.rightBorder, horizontal = false, leading = false)
}
