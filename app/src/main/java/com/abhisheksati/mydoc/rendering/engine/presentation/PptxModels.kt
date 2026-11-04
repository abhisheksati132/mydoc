package com.abhisheksati.mydoc.rendering.engine.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign

data class SlideBounds(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
}

enum class VisualElementType {
    IMAGE,
    SHAPE,
    TEXT_BOX,
    TABLE
}

data class SlideTextRun(
    val text: String,
    val fontSizeSp: Float = 14f,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikeThrough: Boolean = false,
    val fontFamily: String? = null,
    val color: Color? = null
)

data class SlideParagraph(
    val runs: List<SlideTextRun> = emptyList(),
    val alignment: TextAlign = TextAlign.Start,
    val isBullet: Boolean = false,
    val bulletChar: String? = null,
    val indentLevel: Int = 0
) {
    val plainText: String get() = runs.joinToString("") { it.text }
}

data class VisualElement(
    val type: VisualElementType,
    val bounds: SlideBounds,
    val imageBytes: ByteArray? = null,
    val backgroundColor: Color? = null,
    val borderColor: Color? = null,
    val borderWidthDp: Float = 0f,
    val isRounded: Boolean = false,
    val isEllipse: Boolean = false,
    val shapePreset: String? = null,
    val paragraphs: List<SlideParagraph> = emptyList(),
    val tableRows: List<List<String>> = emptyList()
)

data class PptxSlide(
    val slideNumber: Int,
    val title: String,
    val content: List<String>,
    val visualElements: List<VisualElement> = emptyList(),
    val backgroundColor: Color? = null,
    val aspectRatio: Float = 16f / 9f,
    val notes: String? = null
)

data class PptxPresentation(
    val slides: List<PptxSlide>,
    val aspectRatio: Float = 16f / 9f,
    /**
     * Why nothing was laid out. Previously an empty result was encoded as a fabricated slide
     * titled "Empty Presentation", which a genuine slide with that title would defeat, and which
     * callers then had to string-compare to decide whether the deck loaded.
     */
    val emptyReason: String? = null,
    /** True when slides came from a byte-level text scrape rather than a real parse. */
    val recovered: Boolean = false,
    /**
     * Why the deck may be shorter than the file. A parse that stopped halfway used to return the
     * slides it had managed with no signal at all, which is an incomplete document presented as a
     * complete one - the exact failure this release set out to remove.
     */
    val incompleteReason: String? = null
) {
    val isEmpty: Boolean get() = slides.isEmpty()
}
