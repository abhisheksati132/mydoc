package com.abhisheksati.mydoc.rendering.engine

import android.graphics.Bitmap
import android.graphics.RectF
import com.abhisheksati.mydoc.domain.model.ReadingMode

data class SearchMatch(
    val pageIndex: Int,
    val bounds: RectF,
    val textSnippet: String = ""
)

data class OutlineItem(
    val title: String,
    val pageIndex: Int,
    val level: Int = 0,
    val children: List<OutlineItem> = emptyList()
)

data class TextSelectionResult(
    val selectedText: String,
    val highlightRects: List<RectF>
)

interface DocumentRenderer {
    val pageCount: Int
    val isPasswordProtected: Boolean

    /**
     * Whether this engine can be handed a password at all. The OS `PdfRenderer` cannot open an
     * encrypted file under any password, so a locked document that reaches it is not damaged — it
     * is simply beyond this engine. Callers must not offer a password prompt from a renderer that
     * answers false, and must not call the document corrupt merely because it would not open.
     */
    val supportsPasswordAuthentication: Boolean
        get() = false

    fun authenticatePassword(password: String): Boolean
    fun getPageSize(pageIndex: Int): Pair<Float, Float>
    fun renderPage(pageIndex: Int, scale: Float, mode: ReadingMode): Bitmap?
    fun getCachedPage(pageIndex: Int, scale: Float, mode: ReadingMode): Bitmap? = null
    fun renderRegion(
        pageIndex: Int,
        region: RectF,
        pageWidthPx: Float,
        outWidth: Int,
        outHeight: Int,
        mode: ReadingMode = ReadingMode.NORMAL
    ): Bitmap? = null
    /**
     * A full-document search. [isCancelled] is consulted between pages: the scan is long enough to
     * be worth abandoning when the reader types another character, and the caller must not publish
     * results for a query nobody asked for any more.
     */
    fun search(query: String, isCancelled: () -> Boolean = { false }): List<SearchMatch>
    fun getOutline(): List<OutlineItem>
    /**
     * Null when the text layer could not be read at all; an empty string when the page or
     * document genuinely carries no text. Collapsing the two makes "no text on this page" and
     * "we could not read this page" indistinguishable to search, OCR fallback and copy.
     */
    fun getPageText(pageIndex: Int): String? = null
    fun getDocumentText(): String? = null
    fun selectTextAt(pageIndex: Int, point: android.graphics.PointF): TextSelectionResult? = null
    fun selectTextBetween(pageIndex: Int, start: android.graphics.PointF, end: android.graphics.PointF): TextSelectionResult? = null
    fun close()
}
