package com.abhisheksati.mydoc.ui.viewer.pdf

import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.ocr.OcrEngine
import com.abhisheksati.mydoc.tools.tts.DocumentTtsManager
import com.abhisheksati.mydoc.viewmodel.ViewerViewModel
import kotlinx.coroutines.launch
import com.abhisheksati.mydoc.designsystem.components.showMessage

@Composable
fun PdfContextualActionsHost(
    pageNum: Int?,
    activeSelectionText: String?,
    readingMode: ReadingMode,
    viewModel: ViewerViewModel,
    ocrEngine: OcrEngine,
    ttsManager: DocumentTtsManager,
    onStartTts: (String) -> Unit,
    onOcrResult: (String) -> Unit,
    onClose: () -> Unit
) {
    if (pageNum == null) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    PdfFloatingContextualBar(
        pageNum = pageNum,
        activeSelectionText = activeSelectionText,
        readingMode = readingMode,
        onCopy = {
            coroutineScope.launch {
                val text = activeSelectionText ?: viewModel.getPageText(pageNum)
                if (!text.isNullOrBlank()) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("PDF Text", text))
                    context.showMessage(if (activeSelectionText != null) "Text copied to clipboard" else "Page ${pageNum + 1} text copied")
                } else {
                    context.showMessage(text.unreadableTextLayerMessage() ?: "No text detected on this page")
                }
                onClose()
            }
        },
        onShare = {
            coroutineScope.launch {
                val text = activeSelectionText ?: viewModel.getPageText(pageNum)
                if (!text.isNullOrBlank()) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Text"))
                } else {
                    context.showMessage(text.unreadableTextLayerMessage() ?: "No text to share")
                }
                onClose()
            }
        },
        onSearch = {
            coroutineScope.launch {
                val text = activeSelectionText ?: viewModel.getPageText(pageNum)
                if (!text.isNullOrBlank()) {
                    val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                        putExtra(SearchManager.QUERY, text.take(150))
                    }
                    try {
                        context.startActivity(searchIntent)
                    } catch (_: Exception) {
                        context.showMessage("No browser found")
                    }
                } else {
                    context.showMessage(text.unreadableTextLayerMessage() ?: "No text to search")
                }
                onClose()
            }
        },
        onSpeak = {
            coroutineScope.launch {
                val text = activeSelectionText ?: viewModel.getPageText(pageNum)
                if (!text.isNullOrBlank()) {
                    onStartTts(text)
                    ttsManager.startReading(text)
                } else {
                    context.showMessage(text.unreadableTextLayerMessage() ?: "No text to read")
                }
                onClose()
            }
        },
        onClose = onClose
    )

    // Page Contextual Action Sheet (Copy, Read, Share, OCR)
    PdfPageActionSheet(
        isOpen = true,
        pageNum = pageNum,
        onDismiss = onClose,
        onCopyPageText = {
            coroutineScope.launch {
                val text = viewModel.getPageText(pageNum)
                if (!text.isNullOrBlank()) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Page ${pageNum + 1}", text))
                    context.showMessage("Page ${pageNum + 1} text copied to clipboard")
                } else {
                    context.showMessage(text.unreadableTextLayerMessage() ?: "No digital text on this page. Try OCR extract.")
                }
                onClose()
            }
        },
        onReadPageAloud = {
            coroutineScope.launch {
                val text = viewModel.getPageText(pageNum)
                if (!text.isNullOrBlank()) {
                    onStartTts(text)
                } else {
                    context.showMessage(text.unreadableTextLayerMessage() ?: "No text on this page to read")
                }
                onClose()
            }
        },
        onSharePageText = {
            coroutineScope.launch {
                val text = viewModel.getPageText(pageNum)
                if (!text.isNullOrBlank()) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Page ${pageNum + 1} Text"))
                } else {
                    context.showMessage(text.unreadableTextLayerMessage() ?: "No text on this page to share")
                }
                onClose()
            }
        },
        onExtractOcr = {
            onClose()
            coroutineScope.launch {
                val pageBmp = viewModel.renderPageBitmap(pageNum, 2.0f)
                if (pageBmp != null) {
                    val ocr = ocrEngine.recognizeText(pageBmp)
                    if (ocr.isNotBlank()) {
                        onOcrResult(ocr)
                    } else {
                        context.showMessage("No text found with OCR")
                    }
                }
            }
        }
    )
}

/**
 * A null page text means the text layer could not be read; an empty one means the page has no
 * text. Telling a reader "there is nothing here" about a page we failed to parse sends them to
 * OCR for a document that only needs re-opening, so the two get different sentences.
 */
private fun String?.unreadableTextLayerMessage(): String? =
    if (this == null) "MyDoc could not read this page's text layer." else null
