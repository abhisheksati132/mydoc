package com.abhisheksati.mydoc.ui.viewer.spreadsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.rendering.engine.spreadsheet.SheetData
import com.abhisheksati.mydoc.ui.common.AppDivider
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing as Spacing
import com.abhisheksati.mydoc.viewmodel.SpreadsheetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpreadsheetViewerScreen(
    document: DocumentFile,
    viewModel: SpreadsheetViewModel,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var isSearchOpen by remember { mutableStateOf(false) }

    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    val colors = com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors.current
    val isAppDark = colors.background == com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors.background
    val bgColor = if (isAppDark) androidx.compose.ui.graphics.Color(0xFF000000) else colors.background

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = document.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { isSearchOpen = !isSearchOpen }) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = if (isSearchOpen) colors.accent else colors.textSecondary
                        )
                    }
                    IconButton(onClick = onShareClick) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = stringResource(R.string.share),
                            tint = colors.textSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = bgColor
                )
            )
        },
        bottomBar = {
            // Sheet Tabs Selector
            if (state.model.sheets.size > 1) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface)
                        .navigationBarsPadding()
                ) {
                    AppDivider()
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.medium, vertical = Spacing.extraSmall)
                    ) {
                        items(state.model.sheets.size) { index ->
                            val sheet = state.model.sheets[index]
                            val isSelected = index == state.selectedSheetIndex
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectSheet(index) },
                                label = { Text(sheet.name) },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.padding(end = Spacing.small),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.accent,
                                    selectedLabelColor = if (isAppDark) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White
                                )
                            )
                        }
                    }
                }
            }
        },
        containerColor = bgColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // In-sheet search input
            if (isSearchOpen) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    placeholder = { Text("Search sheet cells…") },
                    singleLine = true,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.medium, vertical = Spacing.small),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                AppDivider()
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (state.errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.errorMessage ?: "Failed to open sheet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(Spacing.large)
                    )
                }
            } else {
                val currentSheet = state.currentSheet
                if (currentSheet != null) {
                    val maxRow = maxOf(25, currentSheet.maxRow + 5)
                    val maxCol = maxOf(10, currentSheet.maxCol + 3)
                    val colWidth = 110.dp
                    val rowHeight = 36.dp
                    val headerColWidth = 44.dp

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
                                                .width(colWidth)
                                                .height(rowHeight)
                                                .border(0.5.dp, MaterialTheme.colorScheme.outline),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = SheetData.columnIndexToLetter(c),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Body Grid with Frozen Row Header Column
                            Row(modifier = Modifier.fillMaxSize()) {
                                // Frozen Row Headers (1, 2, 3...)
                                Column(
                                    modifier = Modifier
                                        .width(headerColWidth)
                                        .verticalScroll(verticalScrollState)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    for (r in 0..maxRow) {
                                        Box(
                                            modifier = Modifier
                                                .width(headerColWidth)
                                                .height(rowHeight)
                                                .border(0.5.dp, MaterialTheme.colorScheme.outline),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${r + 1}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                // 2D Scrollable Cell Grid
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .horizontalScroll(horizontalScrollState)
                                        .verticalScroll(verticalScrollState)
                                ) {
                                    Column {
                                        for (r in 0..maxRow) {
                                            Row {
                                                for (c in 0..maxCol) {
                                                    val cellVal = currentSheet.getCellValue(r, c)
                                                    val isMatch = state.searchQuery.isNotBlank() && cellVal.contains(state.searchQuery, ignoreCase = true)
                                                    val isSelected = state.selectedCell?.first == r && state.selectedCell?.second == c

                                                    Box(
                                                        modifier = Modifier
                                                            .width(colWidth)
                                                            .height(rowHeight)
                                                            .border(
                                                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                                            )
                                                            .background(
                                                                if (isMatch) Color(0x66FFEB3B)
                                                                else if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                                else Color.Transparent
                                                            )
                                                            .clickable { viewModel.selectCell(r, c) }
                                                            .padding(horizontal = 6.dp),
                                                        contentAlignment = Alignment.CenterStart
                                                    ) {
                                                        Text(
                                                            text = cellVal,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onBackground,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
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
        }

        // Cell Inspection Bottom Sheet
        val selected = state.selectedCell
        if (selected != null) {
            val (r, c) = selected
            val cellVal = state.currentSheet?.getCellValue(r, c) ?: ""
            val cellRef = "${SheetData.columnIndexToLetter(c)}${r + 1}"

            ModalBottomSheet(
                onDismissRequest = viewModel::clearSelectedCell,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.medium)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "CELL $cellRef",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(Spacing.small))
                    Text(
                        text = cellVal.ifEmpty { "(Empty cell)" },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(Spacing.large))
                }
            }
        }
    }
}
