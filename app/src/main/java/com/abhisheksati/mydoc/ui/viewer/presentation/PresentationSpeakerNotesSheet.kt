package com.abhisheksati.mydoc.ui.viewer.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.components.OverlaySystemBars
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing

/**
 * Bottom sheet displaying presenter/speaker notes for the active slide.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresentationSpeakerNotesSheet(
    isOpen: Boolean,
    currentSlideIndex: Int,
    notes: String,
    onDismiss: () -> Unit,
    cardBgColor: Color,
    textColor: Color
) {
    if (!isOpen) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = cardBgColor
    ) {
        OverlaySystemBars()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.paddingLarge)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Speaker Notes — Slide ${currentSlideIndex + 1}",
                    style = AppTheme.typography.bodyLargeStrong,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = textColor)
                }
            }
            Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))
            Text(
                text = notes.ifBlank { "No speaker notes for this slide." },
                style = AppTheme.typography.bodyLarge,
                color = textColor,
                lineHeight = 24.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}
