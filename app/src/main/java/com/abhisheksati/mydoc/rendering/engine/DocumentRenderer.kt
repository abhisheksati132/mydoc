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

interface DocumentRenderer {
    val pageCount: Int
    val isPasswordProtected: Boolean
    fun authenticatePassword(password: String): Boolean
    fun getPageSize(pageIndex: Int): Pair<Float, Float>
    fun renderPage(pageIndex: Int, scale: Float, mode: ReadingMode): Bitmap?
    fun renderRegion(
        pageIndex: Int,
        region: RectF,
        pageWidthPx: Float,
        outWidth: Int,
        outHeight: Int,
        mode: ReadingMode = ReadingMode.NORMAL
    ): Bitmap? = null
    fun search(query: String): List<SearchMatch>
    fun getOutline(): List<OutlineItem>
    fun close()
}
