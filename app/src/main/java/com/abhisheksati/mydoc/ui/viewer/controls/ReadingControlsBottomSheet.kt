package com.abhisheksati.mydoc.ui.viewer.controls

import com.abhisheksati.mydoc.designsystem.components.AppModalSheet
import com.abhisheksati.mydoc.R
import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
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
import com.abhisheksati.mydoc.util.openFile
import com.abhisheksati.mydoc.designsystem.components.showMessage

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
    modifier: Modifier = Modifier,
    isFitPage: Boolean = false,
    onFitPageChange: ((Boolean) -> Unit)? = null
) {
    if (!isOpen) return

    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val context = LocalContext.current
    val resources = LocalResources.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    AppModalSheet(
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
                    .heightIn(min = AppSpacing.minTouchTarget),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.appearance_title),
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
                        contentDescription = stringResource(R.string.appearance_close),
                        tint = colors.textSecondary
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))

            SectionLabel(stringResource(R.string.appearance_reading_mode))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
            ) {
                ThemeOptionButton(
                    label = stringResource(R.string.appearance_paper),
                    isSelected = readingMode == ReadingMode.NORMAL,
                    onClick = {
                        onReadingModeChange(ReadingMode.NORMAL)
                    },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionButton(
                    label = stringResource(R.string.reading_mode_sepia),
                    isSelected = readingMode == ReadingMode.SEPIA,
                    onClick = {
                        onReadingModeChange(ReadingMode.SEPIA)
                    },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionButton(
                    label = stringResource(R.string.appearance_comfort),
                    isSelected = readingMode == ReadingMode.EYE_COMFORT,
                    onClick = {
                        onReadingModeChange(ReadingMode.EYE_COMFORT)
                    },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionButton(
                    label = stringResource(R.string.appearance_night),
                    isSelected = readingMode == ReadingMode.INVERTED_DARK,
                    onClick = {
                        onReadingModeChange(ReadingMode.INVERTED_DARK)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.paddingLarge))

            // 2. Brightness Section
            SectionLabel(stringResource(R.string.appearance_brightness))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AppSpacing.minTouchTarget),
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
                    modifier = Modifier.width(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.paddingLarge))

            // 3. Scroll & Page Layout Mode
            SectionLabel(stringResource(R.string.appearance_layout))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
            ) {
                ThemeOptionButton(
                    label = stringResource(R.string.appearance_scroll),
                    icon = Icons.Outlined.SwapVert,
                    isSelected = isContinuousScroll && !isDualPageView,
                    onClick = {
                        onDualPageViewChange(false)
                        onScrollModeChange(true)
                    },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionButton(
                    label = stringResource(R.string.appearance_paged),
                    icon = Icons.Outlined.ViewDay,
                    isSelected = !isContinuousScroll && !isDualPageView,
                    onClick = {
                        onDualPageViewChange(false)
                        onScrollModeChange(false)
                    },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionButton(
                    label = stringResource(R.string.appearance_dual_page),
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
                SectionLabel(stringResource(R.string.appearance_page_fit))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
                ) {
                    ThemeOptionButton(
                        label = stringResource(R.string.appearance_fit_width),
                        icon = Icons.Outlined.FitScreen,
                        isSelected = !isFitPage,
                        onClick = {
                            onFitPageChange(false)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionButton(
                        label = stringResource(R.string.appearance_fit_page),
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
            SectionLabel(stringResource(R.string.appearance_orientation))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
            ) {
                val activity = context as? Activity
                // The activity holds the real request, so it is mirrored into state here. Reading
                // `requestedOrientation` straight during composition gave the highlight no reason to
                // recompose: the orientation changed under the user's finger and the selected button
                // stayed on the old choice.
                var currentOrientation by remember(activity) {
                    mutableIntStateOf(
                        activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    )
                }
                val requestOrientation: (Int) -> Unit = { mode ->
                    activity?.requestedOrientation = mode
                    currentOrientation = mode
                }

                ThemeOptionButton(
                    label = stringResource(R.string.appearance_auto),
                    icon = Icons.Outlined.ScreenRotation,
                    isSelected = currentOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                    onClick = { requestOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionButton(
                    label = stringResource(R.string.appearance_portrait),
                    icon = Icons.Outlined.StayCurrentPortrait,
                    isSelected = currentOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                    onClick = { requestOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionButton(
                    label = stringResource(R.string.appearance_landscape),
                    icon = Icons.Outlined.StayCurrentLandscape,
                    isSelected = currentOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                    onClick = { requestOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.paddingLarge))

            // 6. Keep Screen On
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AppSpacing.minTouchTarget)
                    .clickable {
                        onKeepScreenOnChange(!isKeepScreenOn)
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.appearance_keep_awake),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.appearance_keep_awake_subtitle),
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
                    label = stringResource(R.string.appearance_open_with),
                    onClick = {
                        onDismiss()
                        val mime = document.mimeType.ifEmpty { "application/pdf" }
                        if (!openFile(context, Uri.parse(document.uri), mime)) {
                            context.showMessage(resources.getString(R.string.appearance_no_app))
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                ActionCardButton(
                    icon = Icons.Outlined.Print,
                    label = stringResource(R.string.appearance_print),
                    onClick = {
                        onDismiss()
                        PrintHelper.printDocument(context, document) { msg ->
                            context.showMessage(msg)
                        }
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
        label = {
            Text(
                text = label,
                maxLines = 1,
                style = MaterialTheme.typography.labelSmall,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        },
        leadingIcon = if (icon != null) {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
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
