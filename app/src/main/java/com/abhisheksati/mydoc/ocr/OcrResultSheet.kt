package com.abhisheksati.mydoc.ocr

import com.abhisheksati.mydoc.designsystem.components.AppModalSheet
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.util.UserMessages
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.components.OverlaySystemBars
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.DocumentCanvasColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.abhisheksati.mydoc.designsystem.components.showMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrResultSheet(
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = AppTheme.colors
    val typography = AppTheme.typography

    val wordCount = remember(text) { ocrWordCount(text) }
    val charCount = text.length
    val resources = LocalResources.current
    val clipLabel = stringResource(R.string.ocr_clip_label)

    AppModalSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier.fillMaxHeight(0.85f)
    ) {
        OverlaySystemBars()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.TextFields,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.ocr_title),
                            style = typography.heading,
                            color = colors.textPrimary
                        )
                        Text(
                            text = stringResource(R.string.ocr_counts, wordCount, charCount),
                            style = typography.caption,
                            color = colors.textSecondary
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close), tint = colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(clipLabel, text))
                        context.showMessage(resources.getString(R.string.ocr_copied))
                    },
                    modifier = Modifier.weight(1f),
                    shape = AppRadius.pillShape,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.Black)
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_copy), style = AppTheme.typography.captionStrong)
                }

                OutlinedButton(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Extracted Text"))
                    },
                    modifier = Modifier.weight(1f),
                    shape = AppRadius.pillShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary)
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_share), style = AppTheme.typography.caption)
                }

                OutlinedButton(
                    onClick = {
                        try {
                            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                            if (!docsDir.exists()) docsDir.mkdirs()
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            val file = File(docsDir, "OCR_Text_$timestamp.txt")
                            file.writeText(text)
                            context.showMessage("Saved to Documents/${file.name}")
                        } catch (e: Exception) {
                            context.showMessage(UserMessages.actionFailed(R.string.action_save, e, "Ocr").resolve(context))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = AppRadius.pillShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary)
                ) {
                    Icon(Icons.Outlined.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_save_txt), style = AppTheme.typography.caption)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Extracted text box
            Surface(
                color = DocumentCanvasColors.blackPage,
                shape = AppRadius.pillShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                SelectionContainer {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = text.ifBlank { "No text detected in this image." },
                            style = typography.body,
                            // The plate is always black, so the ink must be the on-dark token; the
                            // theme's primary ink is near-black in light mode (~1.2:1 here).
                            color = DocumentCanvasColors.onDarkInk
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * The sheet used to split the whole OCR result on every recomposition — a regex compile plus one
 * substring per word, per scroll, per button press, for a text that never changes while it is open.
 * Splitting is now done once per text, and the counting itself is exposed so it can be tested.
 */
internal fun ocrWordCount(text: String): Int = text.split(OCR_WHITESPACE).count { it.isNotBlank() }

private val OCR_WHITESPACE = Regex("\\s+")
