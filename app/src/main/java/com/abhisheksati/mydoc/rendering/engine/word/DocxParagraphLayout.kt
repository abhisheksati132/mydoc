package com.abhisheksati.mydoc.rendering.engine.word

enum class DocxAlignment { LEFT, CENTER, RIGHT, JUSTIFY, START, END }

/** Nullable values distinguish an absent property from an explicit zero or false override. */
data class DocxParagraphLayout(
    val alignment: DocxAlignment? = null,
    val before: Float? = null,
    val after: Float? = null,
    val left: Float? = null,
    val right: Float? = null,
    val pageBreakBefore: Boolean? = null
) {
    fun over(parent: DocxParagraphLayout) = DocxParagraphLayout(
        alignment ?: parent.alignment, before ?: parent.before, after ?: parent.after,
        left ?: parent.left, right ?: parent.right, pageBreakBefore ?: parent.pageBreakBefore
    )
}
