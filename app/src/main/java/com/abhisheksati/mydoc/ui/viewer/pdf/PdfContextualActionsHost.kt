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
                if (text.isNotBlank()) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("PDF Text", text))
                    Toast.makeText(context, if (activeSelectionText != null) "Text copied to clipboard" else "Page ${pageNum + 1} text copied", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No text detected on this page", Toast.LENGTH_SHORT).show()
                }
                onClose()
            }
        },
        onShare = {
            coroutineScope.launch {
                val text = activeSelectionText ?: viewModel.getPageText(pageNum)
                if (text.isNotBlank()) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Text"))
                } else {
                    Toast.makeText(context, "No text to share", Toast.LENGTH_SHORT).show()
                }
                onClose()
            }
        },
        onSearch = {
            coroutineScope.launch {
                val text = activeSelectionText ?: viewModel.getPageText(pageNum)
                if (text.isNotBlank()) {
                    val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                        putExtra(SearchManager.QUERY, text.take(150))
                    }
                    try {
                        context.startActivity(searchIntent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "No text to search", Toast.LENGTH_SHORT).show()
                }
                onClose()
            }
        },
        onSpeak = {
            coroutineScope.launch {
                val text = activeSelectionText ?: viewModel.getPageText(pageNum)
                if (text.isNotBlank()) {
                    onStartTts(text)
                    ttsManager.startReading(text)
                } else {
                    Toast.makeText(context, "No text to read", Toast.LENGTH_SHORT).show()
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
                if (text.isNotBlank()) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Page ${pageNum + 1}", text))
                    Toast.makeText(context, "Page ${pageNum + 1} text copied to clipboard", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No digital text on this page. Try OCR extract.", Toast.LENGTH_SHORT).show()
                }
                onClose()
            }
        },
        onReadPageAloud = {
            coroutineScope.launch {
                val text = viewModel.getPageText(pageNum)
                if (text.isNotBlank()) {
                    onStartTts(text)
                } else {
                    Toast.makeText(context, "No text on this page to read", Toast.LENGTH_SHORT).show()
                }
                onClose()
            }
        },
        onSharePageText = {
            coroutineScope.launch {
                val text = viewModel.getPageText(pageNum)
                if (text.isNotBlank()) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Page ${pageNum + 1} Text"))
                } else {
                    Toast.makeText(context, "No text on this page to share", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(context, "No text found with OCR", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )
}
