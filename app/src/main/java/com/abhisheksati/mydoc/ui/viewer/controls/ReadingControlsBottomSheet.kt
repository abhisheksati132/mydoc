package com.abhisheksati.mydoc.ui.viewer.controls

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.StayCurrentLandscape
import androidx.compose.material.icons.outlined.StayCurrentPortrait
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.AppDivider
import com.abhisheksati.mydoc.designsystem.components.AppSectionHeader
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.ReadingMode

/**
 * Consolidated reading controls bottom sheet.
 * Consolidates reading theme, brightness, scroll mode, page margin layout,
 * orientation lock, and utility actions into a single calm, accessible sheet.
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
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = AppSpacing.cornerRadius, topEnd = AppSpacing.cornerRadius),
        tonalElevation = AppSpacing.elevation,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = AppSpacing.paddingSmall, bottom = AppSpacing.paddingSmall)
                    .size(width = 32.dp, height = 4.dp)
                    .background(colors.divider, RoundedCornerShape(2.dp))
            )
        },
        modifier = modifier
    ) {
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
                    text = "Reading settings",
                    style = typography.title,
                    color = colors.textPrimary
                )
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDismiss()
                    },
                    modifier = Modifier.size(AppSpacing.minTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close settings",
                        tint = colors.textSecondary
                    )
                }
            }

            AppDivider()
            Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))

            // 1. Reading Theme Section
            AppSectionHeader(title = "READING THEME")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
            ) {
                ThemeOptionButton(
                    label = "Normal",
                    isSelected = readingMode == ReadingMode.NORMAL,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onReadingModeChange(ReadingMode.NORMAL)
                    },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionButton(
                    label = "Sepia",
                    isSelected = readingMode == ReadingMode.SEPIA,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onReadingModeChange(ReadingMode.SEPIA)
                    },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionButton(
                    label = "Dark",
                    isSelected = readingMode == ReadingMode.INVERTED_DARK,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onReadingModeChange(ReadingMode.INVERTED_DARK)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.paddingLarge))

            // 2. Brightness Section
            AppSectionHeader(title = "BRIGHTNESS")
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
                Slider(
                    value = if (currentBrightness < 0f) 0.5f else currentBrightness,
                    onValueChange = { newVal ->
                        onBrightnessChange(newVal)
                    },
                    onValueChangeFinished = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    valueRange = 0.05f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accent,
                        activeTrackColor = colors.accent,
                        inactiveTrackColor = colors.divider
                    ),
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
            AppSectionHeader(title = "LAYOUT & SCROLL MODE")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
            ) {
                ThemeOptionButton(
                    label = "Continuous",
                    icon = Icons.Outlined.SwapVert,
                    isSelected = isContinuousScroll && !isDualPageView,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDualPageViewChange(false)
                        onScrollModeChange(true)
                    },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionButton(
                    label = "Page flip",
                    icon = Icons.Outlined.ViewDay,
                    isSelected = !isContinuousScroll && !isDualPageView,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDualPageViewChange(false)
                        onScrollModeChange(false)
                    },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionButton(
                    label = "Two-page",
                    icon = Icons.Outlined.ViewWeek,
                    isSelected = isDualPageView,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDualPageViewChange(true)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.paddingLarge))

            // 4. Orientation Lock
            AppSectionHeader(title = "SCREEN ORIENTATION")
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
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionButton(
                    label = "Portrait",
                    icon = Icons.Outlined.StayCurrentPortrait,
                    isSelected = currentOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionButton(
                    label = "Landscape",
                    icon = Icons.Outlined.StayCurrentLandscape,
                    isSelected = currentOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.paddingLarge))

            // 5. Keep Screen On
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSpacing.minTouchTarget)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onKeepScreenOnChange(!isKeepScreenOn)
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Keep screen awake",
                        style = typography.body,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Prevent screen from sleeping while reading",
                        style = typography.caption,
                        color = colors.textSecondary
                    )
                }
                Switch(
                    checked = isKeepScreenOn,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onKeepScreenOnChange(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.accent,
                        checkedTrackColor = colors.surfaceVariant,
                        uncheckedThumbColor = colors.textSecondary,
                        uncheckedTrackColor = colors.surface
                    )
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))
            AppDivider()
            Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))

            // 6. Utility Actions (Open With & Print)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
            ) {
                ActionCardButton(
                    icon = Icons.AutoMirrored.Outlined.OpenInNew,
                    label = "Open with",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDismiss()
                        com.abhisheksati.mydoc.ui.viewer.controls.PrintHelper.printDocument(context, document)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Box(
        modifier = modifier
            .height(AppSpacing.minTouchTarget)
            .clip(RoundedCornerShape(AppSpacing.chipCornerRadius))
            .background(if (isSelected) colors.surfaceVariant else colors.surface)
            .border(
                width = AppSpacing.dividerThickness,
                color = if (isSelected) colors.accent else colors.divider,
                shape = RoundedCornerShape(AppSpacing.chipCornerRadius)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = AppSpacing.paddingSmall)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) colors.accent else colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(AppSpacing.paddingSmall))
            }
            Text(
                text = label,
                style = typography.label,
                color = if (isSelected) colors.accent else colors.textPrimary
            )
        }
    }
}

@Composable
private fun ActionCardButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Box(
        modifier = modifier
            .height(AppSpacing.minTouchTarget)
            .clip(RoundedCornerShape(AppSpacing.chipCornerRadius))
            .background(colors.surfaceVariant)
            .border(
                width = AppSpacing.dividerThickness,
                color = colors.divider,
                shape = RoundedCornerShape(AppSpacing.chipCornerRadius)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = AppSpacing.paddingSmall)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.paddingSmall))
            Text(
                text = label,
                style = typography.body,
                color = colors.textPrimary
            )
        }
    }
}
