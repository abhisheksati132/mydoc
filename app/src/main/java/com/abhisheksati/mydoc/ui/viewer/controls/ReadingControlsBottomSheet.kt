package com.abhisheksati.mydoc.ui.viewer.controls

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.StayCurrentLandscape
import androidx.compose.material.icons.outlined.StayCurrentPortrait
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.AppSlider
import com.abhisheksati.mydoc.designsystem.components.OverlaySystemBars
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.ReadingMode

/**
 * Consolidated Appearance Bottom Sheet.
 * Provides controls for:
 * 1. Reading Mode (Paper, Sepia, Eye comfort, Night)
 * 2. Brightness adjustment
 * 3. Layout (Continuous, Paged, Dual-page)
 * 4. Page Fit (Fit width, Fit page)
 * 5. Screen Orientation & Keep Screen Awake
 * 6. Utility actions (Open with, Print)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingControlsBottomSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    readingMode: ReadingMode,
    onReadingModeChange: (ReadingMode) -> Unit,
    isContinuousScroll: Boolean,
    onScrollModeChange: (Boolean) -> Unit,
    isDualPageView: Boolean,
    onDualPageViewChange: (Boolean) -> Unit,
    currentBrightness: Float,
    onBrightnessChange: (Float) -> Unit,
    isKeepScreenOn: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    document: DocumentFile,
    isFitPage: Boolean = false,
    onFitPageChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        OverlaySystemBars()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppSpacing.paddingLarge)
                .padding(bottom = AppSpacing.paddingLarge)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSpacing.minTouchTarget),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(
                    onClick = {
                        onDismiss()
                    },
                    modifier = Modifier.size(AppSpacing.minTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close appearance settings",
                        tint = colors.textSecondary
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))

            SectionLabel("Reading mode")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
            ) {
                ThemeOptionButton(
                    label = "Paper",
                    isSelected = readingMode == ReadingMode.NORMAL,
                    onClick = {
                        onReadingModeChange(ReadingMode.NORMAL)
                    },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionButton(
                    label = "Sepia",
                    isSelected = readingMode == ReadingMode.SEPIA,
                    onClick = {
                        onReadingModeChange(ReadingMode.SEPIA)
                    },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionButton(
                    label = "Comfort",
                    isSelected = readingMode == ReadingMode.EYE_COMFORT,
                    onClick = {
                        onReadingModeChange(ReadingMode.EYE_COMFORT)
                    },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionButton(
                    label = "Night",
                    isSelected = readingMode == ReadingMode.INVERTED_DARK,
                    onClick = {
                        onReadingModeChange(ReadingMode.INVERTED_DARK)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.paddingLarge))

            // 2. Brightness Section
            SectionLabel("Brightness")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSpacing.minTouchTarget),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.BrightnessMedium,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(AppSpacing.paddingMedium))
                AppSlider(
                    value = if (currentBrightness < 0f) 0.5f else currentBrightness,
                    onValueChange = { newVal ->
                        onBrightnessChange(newVal)
                    },
                    onValueChangeFinished = {
                    },
                    valueRange = 0.05f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accent,
                        activeTrackColor = colors.accent,
                        inactiveTrackColor = colors.divider
                    ),
                    thumbSize = 13.dp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(AppSpacing.paddingSmall))
                Text(
                    text = if (currentBrightness < 0f) "Auto" else "${(currentBrightness * 100).toInt()}%",
                    style = typography.caption,
                    color = colors.textSecondary,
                    modifier = Modifier.width(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.paddingLarge))

            // 3. Scroll & Page Layout Mode
            SectionLabel("Layout")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
            ) {
                ThemeOptionButton(
                    label = "Scroll",
                    icon = Icons.Outlined.SwapVert,
                    isSelected = isContinuousScroll && !isDualPageView,
                    onClick = {
                        onDualPageViewChange(false)
                        onScrollModeChange(true)
                    },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionButton(
                    label = "Paged",
                    icon = Icons.Outlined.ViewDay,
                    isSelected = !isContinuousScroll && !isDualPageView,
                    onClick = {
                        onDualPageViewChange(false)
                        onScrollModeChange(false)
                    },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionButton(
                    label = "Dual-page",
                    icon = Icons.Outlined.ViewWeek,
                    isSelected = isDualPageView,
                    onClick = {
                        onDualPageViewChange(true)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // 4. Page Fit (Width vs Page)
            if (onFitPageChange != null) {
                Spacer(modifier = Modifier.height(AppSpacing.paddingLarge))
                SectionLabel("Page fit")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
                ) {
                    ThemeOptionButton(
                        label = "Fit width",
                        icon = Icons.Outlined.FitScreen,
                        isSelected = !isFitPage,
                        onClick = {
                            onFitPageChange(false)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionButton(
                        label = "Fit page",
                        icon = Icons.Outlined.CropFree,
                        isSelected = isFitPage,
                        onClick = {
                            onFitPageChange(true)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.paddingLarge))

            // 5. Orientation Lock
            SectionLabel("Screen orientation")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
            ) {
                val activity = context as? Activity
                val currentOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

                ThemeOptionButton(
                    label = "Auto",
                    icon = Icons.Outlined.ScreenRotation,
                    isSelected = currentOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                    onClick = {
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionButton(
                    label = "Portrait",
                    icon = Icons.Outlined.StayCurrentPortrait,
                    isSelected = currentOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                    onClick = {
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionButton(
                    label = "Landscape",
                    icon = Icons.Outlined.StayCurrentLandscape,
                    isSelected = currentOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                    onClick = {
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.paddingLarge))

            // 6. Keep Screen On
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSpacing.minTouchTarget)
                    .clickable {
                        onKeepScreenOnChange(!isKeepScreenOn)
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Keep screen awake",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Prevent screen from sleeping while reading",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isKeepScreenOn,
                    onCheckedChange = {
                        onKeepScreenOnChange(it)
                    },
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))

            // 7. Utility Actions (Open With & Print)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
            ) {
                ActionCardButton(
                    icon = Icons.AutoMirrored.Outlined.OpenInNew,
                    label = "Open with",
                    onClick = {
                        onDismiss()
                        val sendIntent = Intent(Intent.ACTION_VIEW).apply {
                            val mime = document.mimeType.ifEmpty { "application/pdf" }
                            setDataAndType(Uri.parse(document.uri), mime)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        try {
                            context.startActivity(Intent.createChooser(sendIntent, "Open with"))
                        } catch (_: Exception) {
                            Toast.makeText(context, "No app available to open file", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                ActionCardButton(
                    icon = Icons.Outlined.Print,
                    label = "Print",
                    onClick = {
                        onDismiss()
                        PrintHelper.printDocument(context, document)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun ThemeOptionButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        leadingIcon = if (icon != null) {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else null,
        modifier = modifier
    )
}

@Composable
private fun ActionCardButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}

@Preview(name = "AppearanceSheet - Light")
@Composable
private fun AppearanceSheetLightPreview() {
    AppDesignSystemTheme(darkTheme = false, amoledBlack = false) {
        ReadingControlsBottomSheet(
            isOpen = true,
            onDismiss = {},
            readingMode = ReadingMode.NORMAL,
            onReadingModeChange = {},
            isContinuousScroll = true,
            onScrollModeChange = {},
            isDualPageView = false,
            onDualPageViewChange = {},
            currentBrightness = 0.6f,
            onBrightnessChange = {},
            isKeepScreenOn = true,
            onKeepScreenOnChange = {},
            document = DocumentFile(
                uri = "file:///sample.pdf",
                displayName = "Sample Document",
                sizeBytes = 1024L,
                mimeType = "application/pdf",
                lastOpenedTimestamp = System.currentTimeMillis()
            ),
            isFitPage = false,
            onFitPageChange = {}
        )
    }
}
