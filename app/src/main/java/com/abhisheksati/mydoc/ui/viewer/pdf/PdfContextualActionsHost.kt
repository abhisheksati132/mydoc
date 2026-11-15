package com.abhisheksati.mydoc.ui.viewer.pdf

import com.abhisheksati.mydoc.R
import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.ocr.OcrEngine
import com.abhisheksati.mydoc.tools.tts.DocumentTtsManager
import com.abhisheksati.mydoc.viewmodel.ViewerViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    onStartAudiobook: (Int) -> Unit,
    onSaveHighlight: (Int, String) -> Unit,
    onOcrResult: (String) -> Unit,
    onClose: () -> Unit
) {
    if (pageNum == null) return

    val context = LocalContext.current

    val resources = LocalResources.current
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
                    clipboard.setPrimaryClip(ClipData.newPlainText(resources.getString(R.string.ctx_clip_label), text))
                    context.showMessage(
                        if (activeSelectionText != null) resources.getString(R.string.ctx_copied)
                        else resources.getString(R.string.ctx_page_copied, pageNum + 1)
                    )
                } else {
                    context.showMessage(text.unreadableTextLayerMessage() ?: resources.getString(R.string.ctx_no_text))
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
                    context.startActivity(Intent.createChooser(shareIntent, resources.getString(R.string.ctx_share_text)))
                } else {
                    context.showMessage(text.unreadableTextLayerMessage() ?: resources.getString(R.string.ctx_no_text_share))
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
                        context.showMessage(resources.getString(R.string.ctx_no_browser))
                    }
                } else {
                    context.showMessage(text.unreadableTextLayerMessage() ?: resources.getString(R.string.ctx_no_text_search))
                }
                onClose()
            }
        },
        onSpeak = {
            // A bare page action starts the audiobook at this page (the controller handles
            // scanned pages via OCR); a selection reads only the selected passage.
            if (activeSelectionText != null) {
                onStartTts(activeSelectionText)
                ttsManager.startReading(activeSelectionText)
            } else {
                onStartAudiobook(pageNum)
            }
            onClose()
        },
        onHighlight = activeSelectionText?.let { text ->
            { onSaveHighlight(pageNum, text); onClose() }
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
                    clipboard.setPrimaryClip(ClipData.newPlainText(resources.getString(R.string.ctx_page_label, pageNum + 1), text))
                    context.showMessage(resources.getString(R.string.ctx_page_copied_long, pageNum + 1))
                } else {
                    context.showMessage(text.unreadableTextLayerMessage() ?: resources.getString(R.string.ctx_no_digital_text))
                }
                onClose()
            }
        },
        onReadPageAloud = {
            onStartAudiobook(pageNum)
            onClose()
        },
        onSharePageText = {
            coroutineScope.launch {
                val text = viewModel.getPageText(pageNum)
                if (!text.isNullOrBlank()) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, resources.getString(R.string.ctx_share_page_text, pageNum + 1)))
                } else {
                    context.showMessage(text.unreadableTextLayerMessage() ?: resources.getString(R.string.ctx_no_text_on_page_share))
                }
                onClose()
            }
        },
        onExtractOcr = {
            onClose()
            viewModel.viewModelScope.launch {
                val pageBmp = viewModel.renderPageBitmap(pageNum, 2.0f)
                if (pageBmp != null) {
                    val ocr = ocrEngine.recognizeText(pageBmp)
                    if (ocr.isNotBlank()) {
                        onOcrResult(ocr)
                    } else {
                        context.showMessage(resources.getString(R.string.ctx_no_ocr_text))
                    }
                }
            }
        },
        onSavePageHighlight = {
            onClose()
            coroutineScope.launch {
                val digital = viewModel.getPageText(pageNum)
                val text = if (!digital.isNullOrBlank()) {
                    digital
                } else {
                    // Pages without a text layer (scans, fonts without ToUnicode) still get a
                    // highlight from what the offline OCR engine can read.
                    withContext(Dispatchers.Default) {
                        viewModel.renderPageBitmap(pageNum, 1.5f)?.let { bitmap ->
                            runCatching { ocrEngine.recognizeText(bitmap) }.getOrNull()
                        }
                    }
                }
                if (!text.isNullOrBlank()) {
                    onSaveHighlight(pageNum, text)
                } else {
                    context.showMessage(resources.getString(R.string.ctx_no_digital_text))
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
