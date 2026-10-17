package com.abhisheksati.mydoc.designsystem.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.AppBottomSheet
import com.abhisheksati.mydoc.designsystem.components.AppCircularProgress
import com.abhisheksati.mydoc.designsystem.components.AppDialog
import com.abhisheksati.mydoc.designsystem.components.AppEmptyState
import com.abhisheksati.mydoc.designsystem.components.AppFileRow
import com.abhisheksati.mydoc.designsystem.components.AppFilterChip
import com.abhisheksati.mydoc.designsystem.components.AppLinearProgress
import com.abhisheksati.mydoc.designsystem.components.AppSearchField
import com.abhisheksati.mydoc.designsystem.components.AppSectionHeader
import com.abhisheksati.mydoc.designsystem.components.AppToolbar
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DesignSystemPreviewScreen(
    onBackClick: () -> Unit = {}
) {
    var isDarkTheme by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedChip by remember { mutableStateOf("All") }
    var isSheetOpen by remember { mutableStateOf(false) }
    var isDialogOpen by remember { mutableStateOf(false) }

    AppDesignSystemTheme(darkTheme = isDarkTheme) {
        val colors = AppTheme.colors
        val typography = AppTheme.typography

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colors.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                AppToolbar(
                    title = "Design System Showcase",
                    navigationIcon = Icons.AutoMirrored.Outlined.ArrowBack,
                    onNavigationClick = onBackClick,
                    actions = {
                        IconButton(
                            onClick = { isDarkTheme = !isDarkTheme },
                            modifier = Modifier.size(AppSpacing.minTouchTarget)
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                contentDescription = "Toggle Theme",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = AppSpacing.xxLarge)
                ) {
                    // SECTION 1: COLOR TOKENS
                    AppSectionHeader(title = "1. Color Tokens (${if (isDarkTheme) "Dark: Soft Near-Black" else "Light: Soft Near-White"})")

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppSpacing.medium),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.xSmall)
                    ) {
                        ColorSwatchRow("background", colors.background, if (isDarkTheme) "#0F0F10" else "#F9F9FB")
                        ColorSwatchRow("surface", colors.surface, if (isDarkTheme) "#18181A" else "#FFFFFF")
                        ColorSwatchRow("surfaceVariant", colors.surfaceVariant, if (isDarkTheme) "#222226" else "#EFEFF2")
                        ColorSwatchRow("divider", colors.divider, if (isDarkTheme) "#2B2B30" else "#E0E0E5")
                        ColorSwatchRow("textPrimary", colors.textPrimary, if (isDarkTheme) "#F0F0F2 (off-white)" else "#111114 (near-black)")
                        ColorSwatchRow("textSecondary", colors.textSecondary, if (isDarkTheme) "#A0A0A6" else "#666670")
                        ColorSwatchRow("accent", colors.accent, if (isDarkTheme) "#7FA4DE (muted)" else "#2B5BB8")
                        ColorSwatchRow("error", colors.error, if (isDarkTheme) "#E57373" else "#D32F2F")
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.large))

                    // SECTION 2: TYPE SCALE (4 STYLES, INTER FONT ONLY)
                    AppSectionHeader(title = "2. Type Scale (Inter font, 4 styles only)")

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppSpacing.medium),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
                    ) {
                        Text(text = "Title (18sp SemiBold) — mydoc Fast Calm Reader", style = typography.title, color = colors.textPrimary)
                        Text(text = "Body (14sp Normal) — Document viewing with zero bloat and 100ms instant feedback.", style = typography.body, color = colors.textPrimary)
                        Text(text = "Label (12sp Medium) — ALL DOCUMENTS • RECENT HISTORY • FAVORITES", style = typography.label, color = colors.textSecondary)
                        Text(text = "Caption (11sp Normal) — 2.4 MB  •  Page 14 of 42  •  Opened 2h ago", style = typography.caption, color = colors.textSecondary)
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.large))

                    // SECTION 3: SEARCH FIELD & FILTER CHIPS
                    AppSectionHeader(title = "3. SearchField & FilterChips")

                    Box(modifier = Modifier.padding(horizontal = AppSpacing.medium)) {
                        AppSearchField(
                            query = searchQuery,
                            onQueryChanged = { searchQuery = it },
                            placeholder = "Search documents"
                        )
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.small))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppSpacing.medium),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
                    ) {
                        listOf("All", "PDF", "Word", "Excel", "PPT").forEach { chip ->
                            AppFilterChip(
                                selected = selectedChip == chip,
                                onClick = { selectedChip = chip },
                                label = chip
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.large))

                    // SECTION 4: FILE ROW WITH MIDDLE TRUNCATION
                    AppSectionHeader(title = "4. FileRow (Middle-Truncation & Progress)")

                    AppFileRow(
                        displayName = "Quarterly_Financial_Statement_And_Audited_Report_2026_Q3_Final.pdf",
                        typeBadge = "PDF",
                        metadataText = "4.8 MB  •  Yesterday",
                        isFavorite = true,
                        progressFraction = 0.45f,
                        progressText = "Page 19/42 (45%)",
                        onClick = {},
                        onToggleFavorite = {},
                        onShare = {},
                        onDetails = {}
                    )

                    AppFileRow(
                        displayName = "Engineering_Architecture_Design_Specifications_v2_Release.docx",
                        typeBadge = "DOCX",
                        metadataText = "1.2 MB  •  2h ago",
                        isFavorite = false,
                        onClick = {},
                        onToggleFavorite = {},
                        onShare = {},
                        onDetails = {}
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.large))

                    // SECTION 5: PROGRESS INDICATORS
                    AppSectionHeader(title = "5. ProgressIndicator (Linear & Circular)")

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppSpacing.medium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
                    ) {
                        AppCircularProgress()
                        Box(modifier = Modifier.weight(1f)) {
                            AppLinearProgress(progress = 0.65f)
                        }
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.large))

                    // SECTION 6: INTERACTIVE BOTTOMSHEET & DIALOG
                    AppSectionHeader(title = "6. BottomSheet & Dialog")

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppSpacing.medium),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
                    ) {
                        OutlinedButton(
                            onClick = { isSheetOpen = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(AppSpacing.cornerRadius)
                        ) {
                            Text("Open BottomSheet", style = typography.label, color = colors.textPrimary)
                        }

                        Button(
                            onClick = { isDialogOpen = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(AppSpacing.cornerRadius),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.surfaceVariant,
                                contentColor = colors.textPrimary
                            )
                        ) {
                            Text("Open Dialog", style = typography.label)
                        }
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.large))

                    // SECTION 7: EMPTY STATE
                    AppSectionHeader(title = "7. EmptyState (Calm, Plain, No Illustrations)")

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .border(AppSpacing.dividerThickness, colors.divider, RoundedCornerShape(AppSpacing.cornerRadius))
                            .padding(AppSpacing.medium)
                    ) {
                        AppEmptyState(
                            message = "No documents found in storage.",
                            actionLabel = "Open file",
                            onActionClick = {}
                        )
                    }
                }
            }

            // BottomSheet Preview
            AppBottomSheet(
                isOpen = isSheetOpen,
                onDismiss = { isSheetOpen = false },
                title = "Reading controls"
            ) {
                Text(
                    text = "Consolidated one-hand reading controls (theme, brightness, scroll mode, margins, and orientation lock).",
                    style = typography.body,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(AppSpacing.medium))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
                ) {
                    AppFilterChip(selected = true, onClick = {}, label = "Continuous")
                    AppFilterChip(selected = false, onClick = {}, label = "Single Page")
                    AppFilterChip(selected = false, onClick = {}, label = "Two Page")
                }
                Spacer(modifier = Modifier.height(AppSpacing.large))
            }

            // Dialog Preview
            AppDialog(
                isOpen = isDialogOpen,
                onDismissRequest = { isDialogOpen = false },
                title = "Clear recent documents?",
                message = "This removes recent document history from mydoc without deleting files from storage.",
                confirmLabel = "Clear history",
                onConfirm = { isDialogOpen = false },
                dismissLabel = "Cancel",
                onDismiss = { isDialogOpen = false },
                isDestructive = true
            )
        }
    }
}

@Composable
private fun ColorSwatchRow(name: String, color: Color, hexDesc: String) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .border(AppSpacing.dividerThickness, colors.divider, RoundedCornerShape(AppSpacing.chipCornerRadius))
            .background(colors.surface, RoundedCornerShape(AppSpacing.chipCornerRadius))
            .padding(horizontal = AppSpacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(color, RoundedCornerShape(4.dp))
                .border(1.dp, colors.divider, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(AppSpacing.small))
        Text(text = name, style = typography.body, color = colors.textPrimary, modifier = Modifier.weight(1f))
        Text(text = hexDesc, style = typography.caption, color = colors.textSecondary)
    }
}
