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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.AppBottomSheet
import com.abhisheksati.mydoc.designsystem.components.AppCircularProgress
import com.abhisheksati.mydoc.designsystem.components.AppDialog
import com.abhisheksati.mydoc.designsystem.components.AppEmptyState
import com.abhisheksati.mydoc.designsystem.components.AppFilterChip
import com.abhisheksati.mydoc.designsystem.components.AppLinearProgress
import com.abhisheksati.mydoc.designsystem.components.AppSearchField
import com.abhisheksati.mydoc.designsystem.components.AppSectionHeader
import com.abhisheksati.mydoc.designsystem.components.AppTopBar
import com.abhisheksati.mydoc.designsystem.components.BrowseSkeleton
import com.abhisheksati.mydoc.designsystem.components.GlassEffectMode
import com.abhisheksati.mydoc.designsystem.components.GlassSurface
import com.abhisheksati.mydoc.designsystem.components.HomeFeedSkeleton
import com.abhisheksati.mydoc.designsystem.components.LocalGlassEffectMode
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DesignSystemPreviewScreen(
    onBackClick: () -> Unit = {}
) {
    var isDarkTheme by remember { mutableStateOf(true) }
    var glassMode by remember { mutableStateOf(GlassEffectMode.AUTO) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedChip by remember { mutableStateOf("All") }
    var isSheetOpen by remember { mutableStateOf(false) }
    var isDialogOpen by remember { mutableStateOf(false) }

    val hazeState = remember { HazeState() }

    CompositionLocalProvider(LocalGlassEffectMode provides glassMode) {
        AppDesignSystemTheme(darkTheme = isDarkTheme) {
            val colors = AppTheme.colors
            val typography = AppTheme.typography

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = colors.background
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    AppTopBar(
                        title = "Paper under Glass",
                        onBack = onBackClick,
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
                            .hazeSource(state = hazeState)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = AppSpacing.xxLarge)
                    ) {
                        // SECTION 1: IDENTITY & PALETTE
                        AppSectionHeader(
                            title = "1. Identity: ${if (isDarkTheme) "Deep Ink-Navy (Hero Dark)" else "Warm Ivory Paper (Light)"}"
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppSpacing.medium),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.xSmall)
                        ) {
                            ColorSwatchRow("background", colors.background, if (isDarkTheme) "#0B0E14 (Ink-navy)" else "#FAF7F2 (Warm ivory)")
                            ColorSwatchRow("surface", colors.surface, if (isDarkTheme) "#141923 (Substrate)" else "#FFFDF9 (Paper sheet)")
                            ColorSwatchRow("line / rule", colors.line, if (isDarkTheme) "#263043 (1dp rule)" else "#E5DFD3 (Dry rule)")
                            ColorSwatchRow("ink (textPrimary)", colors.textPrimary, if (isDarkTheme) "#F2F5F8 (Bleached)" else "#191614 (Carbon ink)")
                            ColorSwatchRow("inkMuted", colors.textSecondary, if (isDarkTheme) "#8C9AB0 (Slate mist)" else "#6B645C (Pencil)")
                            ColorSwatchRow("accent (Stamp Vermilion)", colors.accent, if (isDarkTheme) "#F05435 (Warm seal)" else "#D94324 (Red stamp)")
                            ColorSwatchRow("glassTint", colors.glassTint, "Refraction backing")
                            ColorSwatchRow("glassRimTop", colors.glassRimTop, "1dp Specular highlight")
                        }

                        Spacer(modifier = Modifier.height(AppSpacing.large))

                        // SECTION 2: GLASS SYSTEM
                        AppSectionHeader(title = "2. Optical Glass System (GlassSurface)")

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppSpacing.medium),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
                        ) {
                            // Glass Mode Switcher
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Glass mode:", style = typography.label, color = colors.inkMuted)
                                GlassEffectMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = glassMode == mode,
                                        onClick = { glassMode = mode },
                                        label = { Text(mode.name, style = typography.caption) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = colors.accent,
                                            selectedLabelColor = colors.onAccent
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(AppSpacing.xSmall))

                            // GlassSurface Floating Card Showcase
                            GlassSurface(
                                hazeState = hazeState,
                                shape = AppRadius.largeShape,
                                elevation = 8.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Floating Glass Chrome",
                                            style = typography.titleSmall,
                                            color = colors.ink
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(colors.accent)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "1dp Specular Rim",
                                                style = typography.metadataStrong,
                                                color = colors.onAccent
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Backdrop blur via Haze with a vertical specular highlight gradient and soft inner caustics. Opaque fallback on battery saver or API < 31.",
                                        style = typography.body,
                                        color = colors.inkMuted
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Contrast: WCAG AA Certified", style = typography.caption, color = colors.inkMuted)
                                        Text("Active: ${glassMode.name}", style = typography.metadata, color = colors.accent)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(AppSpacing.large))

                        // SECTION 3: EDITORIAL TYPE SCALE
                        AppSectionHeader(title = "3. Typography Scale (Fraunces + Inter + JetBrains Mono)")

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppSpacing.medium),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
                        ) {
                            Text(text = "Fraunces Display (32sp Bold)", style = typography.display, color = colors.textPrimary)
                            Text(text = "Fraunces Title (24sp SemiBold)", style = typography.title, color = colors.textPrimary)
                            Text(text = "Fraunces Heading (20sp SemiBold)", style = typography.heading, color = colors.textPrimary)
                            Text(text = "Document Title: Annual_Report_2026.pdf", style = typography.documentName, color = colors.textPrimary)
                            Text(text = "OVERLINE (11sp Inter SemiBold, tracked)", style = typography.overline, color = colors.accent)
                            Text(text = "Inter Body (14sp) — Fast, tactile reader chrome with authentic paper substrate.", style = typography.body, color = colors.textPrimary)
                            Text(text = "Inter Body Strong (14sp SemiBold)", style = typography.bodyStrong, color = colors.textPrimary)

                            // Monospace Metadata Showcase
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(AppRadius.cardShape)
                                    .background(colors.surfaceMuted)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("p. 14 / 128", style = typography.metadataStrong, color = colors.accent)
                                Text("4.8 MB", style = typography.metadata, color = colors.inkMuted)
                                Text("1920 × 1080", style = typography.metadata, color = colors.inkMuted)
                                Text("2026-11-02 21:50", style = typography.metadata, color = colors.inkMuted)
                            }
                        }

                        Spacer(modifier = Modifier.height(AppSpacing.large))

                        // SECTION 4: PHYSICAL PAPER CARD PREVIEW
                        AppSectionHeader(title = "4. Physical Paper Card (Solid Content)")

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppSpacing.medium)
                        ) {
                            // Paper card mock
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(AppRadius.cardShape)
                                    .background(colors.surface)
                                    .border(1.dp, colors.line, AppRadius.cardShape)
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Stamp Vermilion PDF Badge
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(colors.documentContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.PictureAsPdf,
                                            contentDescription = null,
                                            tint = colors.accent,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Quarterly Strategic Review",
                                            style = typography.documentName,
                                            color = colors.ink
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "PDF · 24 pages · 1.4 MB · 12m ago",
                                            style = typography.metadata,
                                            color = colors.inkMuted
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(AppSpacing.large))

                        // SECTION 5: INTERACTIVE SHEETS & PROGRESS
                        AppSectionHeader(title = "5. Interactive Elements & Progress")

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

                        Spacer(modifier = Modifier.height(AppSpacing.medium))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppSpacing.medium),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
                        ) {
                            OutlinedButton(
                                onClick = { isSheetOpen = true },
                                modifier = Modifier.weight(1f),
                                shape = AppRadius.cardShape
                            ) {
                                Text("Open Glass Sheet", style = typography.label, color = colors.textPrimary)
                            }

                            Button(
                                onClick = { isDialogOpen = true },
                                modifier = Modifier.weight(1f),
                                shape = AppRadius.cardShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.accent,
                                    contentColor = colors.onAccent
                                )
                            ) {
                                Text("Open Dialog", style = typography.label)
                            }
                        }
                        Spacer(modifier = Modifier.height(AppSpacing.large))

                        // SECTION 6: LOADING SKELETONS
                        AppSectionHeader(title = "6. Loading Skeletons (first paint)")
                        HomeFeedSkeleton(
                            modifier = Modifier.padding(horizontal = AppSpacing.medium)
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.large))
                        BrowseSkeleton()
                    }
                }

                // BottomSheet Preview
                AppBottomSheet(
                    isOpen = isSheetOpen,
                    onDismiss = { isSheetOpen = false },
                    title = "Glass Bottom Sheet"
                ) {
                    Text(
                        text = "Backdrop blur with 1dp specular rim highlight. Preserves optical hierarchy beneath glass controls.",
                        style = typography.body,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.medium))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
                    ) {
                        AppFilterChip(label = "Paper View", isSelected = true, onClick = {})
                        AppFilterChip(label = "Reflow", isSelected = false, onClick = {})
                        AppFilterChip(label = "Two-Page", isSelected = false, onClick = {})
                    }
                    Spacer(modifier = Modifier.height(AppSpacing.large))
                }

                // Dialog Preview
                AppDialog(
                    isOpen = isDialogOpen,
                    onDismissRequest = { isDialogOpen = false },
                    title = "Delete document?",
                    message = "This removes 'Quarterly Strategic Review' from your device storage.",
                    confirmLabel = "Delete",
                    onConfirm = { isDialogOpen = false },
                    dismissLabel = "Cancel",
                    onDismiss = { isDialogOpen = false },
                    isDestructive = true
                )
            }
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
            .border(AppSpacing.dividerThickness, colors.divider, AppRadius.roundShape)
            .background(colors.surface, AppRadius.roundShape)
            .padding(horizontal = AppSpacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(color, AppRadius.microShape)
                .border(1.dp, colors.divider, AppRadius.microShape)
        )
        Spacer(modifier = Modifier.width(AppSpacing.small))
        Text(text = name, style = typography.body, color = colors.textPrimary, modifier = Modifier.weight(1f))
        Text(text = hexDesc, style = typography.metadata, color = colors.textSecondary)
    }
}
