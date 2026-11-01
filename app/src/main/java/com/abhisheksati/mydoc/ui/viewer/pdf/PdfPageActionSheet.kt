package com.abhisheksati.mydoc.ui.viewer.pdf

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.AppBottomSheet
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography

/**
 * Bottom action sheet displaying contextual tools for a specific PDF page:
 * copy text, read aloud with TTS, share text, and perform OCR text extraction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPageActionSheet(
    isOpen: Boolean,
    pageNum: Int,
    onDismiss: () -> Unit,
    onCopyPageText: () -> Unit,
    onReadPageAloud: () -> Unit,
    onSharePageText: () -> Unit,
    onExtractOcr: () -> Unit
) {
    if (!isOpen) return

    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    AppBottomSheet(
        isOpen = true,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.paddingLarge)
        ) {
            Text(
                text = "Page ${pageNum + 1} Actions",
                style = typography.title,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Copy Page Text
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppRadius.pillShape)
                    .clickable(onClick = onCopyPageText)
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = colors.accent)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Copy Page Text", style = typography.body, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Text("Copy all digital text from this page to clipboard", style = typography.caption, color = colors.textSecondary)
                }
            }

            // 2. Read Page Aloud (TTS)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppRadius.pillShape)
                    .clickable(onClick = onReadPageAloud)
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = null, tint = colors.accent)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Read Page Aloud (TTS)", style = typography.body, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Text("Listen to this page with offline speech synthesizer", style = typography.caption, color = colors.textSecondary)
                }
            }

            // 3. Share Page Text
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppRadius.pillShape)
                    .clickable(onClick = onSharePageText)
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Share, contentDescription = null, tint = colors.accent)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Share Page Text", style = typography.body, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Text("Share page text to WhatsApp, Messages, or Email", style = typography.caption, color = colors.textSecondary)
                }
            }

            // 4. OCR Extract Text
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppRadius.pillShape)
                    .clickable(onClick = onExtractOcr)
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.TextFields, contentDescription = null, tint = colors.accent)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Extract Text with OCR", style = typography.body, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Text("Recognize printed or handwritten text on images", style = typography.caption, color = colors.textSecondary)
                }
            }
        }
    }
}
