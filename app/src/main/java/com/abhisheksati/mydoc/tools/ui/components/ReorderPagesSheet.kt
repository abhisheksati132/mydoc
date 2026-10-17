package com.abhisheksati.mydoc.tools.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.AppBottomSheet
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.artifex.mupdf.fitz.ColorSpace
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material3.ExperimentalMaterial3Api
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderPagesSheet(
    pdfUri: Uri,
    onDismiss: () -> Unit,
    onApplyOrder: (List<Int>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val haptic = LocalHapticFeedback.current

    val pageOrder = remember { mutableStateListOf<Int>() }
    val thumbnails = remember { mutableStateListOf<Bitmap?>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(pdfUri) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                val tempFile = File.createTempFile("reorder_preview_", ".pdf", context.cacheDir)
                context.contentResolver.openInputStream(pdfUri)?.use { input ->
                    FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                }

                val doc = Document.openDocument(tempFile.absolutePath)
                val total = doc.countPages()
                val list = mutableListOf<Bitmap?>()

                for (p in 0 until total) {
                    val page = doc.loadPage(p)
                    val pixmap = page.toPixmap(Matrix(0.3f, 0.3f), ColorSpace.DeviceRGB, false)
                    val bmp = Bitmap.createBitmap(pixmap.pixels, pixmap.width, pixmap.height, Bitmap.Config.ARGB_8888)
                    list.add(bmp)
                    pixmap.destroy()
                    page.destroy()
                }
                doc.destroy()
                tempFile.delete()

                withContext(Dispatchers.Main) {
                    pageOrder.clear()
                    pageOrder.addAll(0 until total)
                    thumbnails.clear()
                    thumbnails.addAll(list)
                    isLoading = false
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    AppBottomSheet(
        isOpen = true,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                Text(text = "Reorder Pages", style = typography.title, color = colors.textPrimary)
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(AppSpacing.minTouchTarget)
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = colors.textSecondary)
                }
            }

            Text(
                text = "Use arrows to reorder or delete pages.",
                style = typography.caption,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Loading page previews...", style = typography.body, color = colors.textSecondary)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
                ) {
                    itemsIndexed(pageOrder) { index, origPage ->
                        val bmp = thumbnails.getOrNull(origPage)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, colors.divider, RoundedCornerShape(AppSpacing.chipCornerRadius))
                                .padding(AppSpacing.paddingSmall),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Page ${index + 1}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.7f)
                                        .clip(RoundedCornerShape(2.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.7f)
                                        .background(colors.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "${origPage + 1}", style = typography.body, color = colors.textSecondary)
                                }
                            }

                            Spacer(modifier = Modifier.height(AppSpacing.paddingSmall))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (index > 0) {
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val temp = pageOrder[index]
                                            pageOrder[index] = pageOrder[index - 1]
                                            pageOrder[index - 1] = temp
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Move left", tint = colors.textPrimary)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(28.dp))
                                }

                                Text(text = "${index + 1}", style = typography.caption, color = colors.textPrimary)

                                if (index < pageOrder.size - 1) {
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val temp = pageOrder[index]
                                            pageOrder[index] = pageOrder[index + 1]
                                            pageOrder[index + 1] = temp
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Move right", tint = colors.textPrimary)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(28.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))

            // Apply Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSpacing.minTouchTarget)
                    .clip(RoundedCornerShape(AppSpacing.chipCornerRadius))
                    .background(colors.accent)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onApplyOrder(pageOrder.toList())
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Apply Order", style = typography.body, color = colors.background)
            }
        }
    }
}
