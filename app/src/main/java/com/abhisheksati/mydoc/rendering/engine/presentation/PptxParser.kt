package com.abhisheksati.mydoc.rendering.engine.presentation

import android.graphics.RectF
import android.util.Xml
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

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
    val color: Color? = null
)

data class SlideParagraph(
    val runs: List<SlideTextRun> = emptyList(),
    val alignment: TextAlign = TextAlign.Start,
    val isBullet: Boolean = false
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
    val aspectRatio: Float = 16f / 9f
)

class PptxParser {

    suspend fun parse(inputStream: InputStream): PptxPresentation = withContext(Dispatchers.IO) {
        val bytes = inputStream.readBytes()
        val mediaMap = mutableMapOf<String, ByteArray>()
        val relsMap = mutableMapOf<Int, MutableMap<String, String>>()
        val slideXmlMap = mutableMapOf<Int, ByteArray>()
        val notesMap = mutableMapOf<String, String>()
        var slideWidth = 9144000L
        var slideHeight = 5143500L

        // Step 1: Scan entire ZIP archive to extract media files, rels, and slide XMLs
        val zip = ZipInputStream(ByteArrayInputStream(bytes))
        var entry: ZipEntry? = zip.nextEntry

        while (entry != null) {
            val name = entry.name

            when {
                // Presentation dimensions
                name == "ppt/presentation.xml" -> {
                    val presXmlBytes = zip.readBytes()
                    val (w, h) = parsePresentationDimensions(presXmlBytes)
                    slideWidth = w
                    slideHeight = h
                }

                // Media files (pictures, graphics, photos, diagrams)
                name.startsWith("ppt/media/") -> {
                    val mediaBytes = zip.readBytes()
                    mediaMap[name] = mediaBytes
                    // Also store with relative name
                    val shortName = name.removePrefix("ppt/")
                    mediaMap[shortName] = mediaBytes
                    mediaMap[name.substringAfterLast('/')] = mediaBytes
                }

                // Slide relationships (maps rId to media/image targets)
                name.startsWith("ppt/slides/_rels/slide") && name.endsWith(".xml.rels") -> {
                    val numStr = name.removePrefix("ppt/slides/_rels/slide").removeSuffix(".xml.rels")
                    val slideNum = numStr.toIntOrNull() ?: 1
                    val relsBytes = zip.readBytes()
                    val map = parseRelationshipsXml(relsBytes)
                    relsMap[slideNum] = map
                }

                // Slide content XML
                name.startsWith("ppt/slides/slide") && name.endsWith(".xml") -> {
                    val numStr = name.removePrefix("ppt/slides/slide").removeSuffix(".xml")
                    val slideNum = numStr.toIntOrNull() ?: 1
                    val slideBytes = zip.readBytes()
                    slideXmlMap[slideNum] = slideBytes
                }

                // Speaker Notes XML
                name.startsWith("ppt/notesSlides/notesSlide") && name.endsWith(".xml") -> {
                    val notesBytes = zip.readBytes()
                    val noteText = parseNotesXml(notesBytes)
                    if (noteText.isNotEmpty()) {
                        notesMap[name] = noteText
                        notesMap[name.removePrefix("ppt/")] = noteText
                        notesMap["../notesSlides/" + name.substringAfterLast('/')] = noteText
                    }
                }
            }

            zip.closeEntry()
            entry = zip.nextEntry
        }

        val presAspectRatio = if (slideHeight > 0) slideWidth.toFloat() / slideHeight.toFloat() else 16f / 9f

        // Step 2: Parse each slide XML into rich visual elements
        val slidesMap = mutableMapOf<Int, PptxSlide>()
        for ((slideNum, slideBytes) in slideXmlMap) {
            val slideRels = relsMap[slideNum] ?: emptyMap()
            val noteTarget = slideRels.values.find { it.contains("notesSlide") }
            val slideNotes = (if (noteTarget != null) notesMap[noteTarget] else null)
                ?: notesMap["ppt/notesSlides/notesSlide$slideNum.xml"]
                ?: notesMap["notesSlides/notesSlide$slideNum.xml"]
            val slide = parseSlideXml(
                stream = ByteArrayInputStream(slideBytes),
                slideNum = slideNum,
                slideWidth = slideWidth,
                slideHeight = slideHeight,
                aspectRatio = presAspectRatio,
                mediaMap = mediaMap,
                relsMap = slideRels,
                notes = slideNotes
            )
            slidesMap[slideNum] = slide
        }

        val sortedSlides = slidesMap.toSortedMap().values.toList()
        if (sortedSlides.isEmpty()) {
            PptxPresentation(
                slides = listOf(
                    PptxSlide(
                        slideNumber = 1,
                        title = "Empty Presentation",
                        content = listOf("No slides found in presentation."),
                        aspectRatio = presAspectRatio
                    )
                ),
                aspectRatio = presAspectRatio
            )
        } else {
            PptxPresentation(
                slides = sortedSlides,
                aspectRatio = presAspectRatio
            )
        }
    }

    private fun parsePresentationDimensions(xmlBytes: ByteArray): Pair<Long, Long> {
        return try {
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(ByteArrayInputStream(xmlBytes), "UTF-8")
            }
            var eventType = parser.eventType
            var w = 9144000L
            var h = 5143500L

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && (parser.name == "p:sldSz" || parser.name == "sldSz")) {
                    val cx = parser.getAttributeValue(null, "cx")?.toLongOrNull()
                    val cy = parser.getAttributeValue(null, "cy")?.toLongOrNull()
                    if (cx != null && cx > 0L) w = cx
                    if (cy != null && cy > 0L) h = cy
                    break
                }
                eventType = parser.next()
            }
            Pair(w, h)
        } catch (_: Exception) {
            Pair(9144000L, 5143500L)
        }
    }

    private fun parseRelationshipsXml(xmlBytes: ByteArray): MutableMap<String, String> {
        val rels = mutableMapOf<String, String>()
        try {
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(ByteArrayInputStream(xmlBytes), "UTF-8")
            }
            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && (parser.name == "Relationship" || parser.name?.endsWith(":Relationship") == true)) {
                    val id = parser.getAttributeValue(null, "Id")
                    val target = parser.getAttributeValue(null, "Target")
                    if (id != null && target != null) {
                        // Normalize target: e.g. "../media/image1.png" -> "ppt/media/image1.png"
                        val cleanTarget = if (target.startsWith("../")) {
                            "ppt/" + target.removePrefix("../")
                        } else if (!target.startsWith("ppt/")) {
                            "ppt/slides/$target"
                        } else {
                            target
                        }
                        rels[id] = cleanTarget
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
        return rels
    }

    private fun parseNotesXml(xmlBytes: ByteArray): String {
        return try {
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(ByteArrayInputStream(xmlBytes), "UTF-8")
            }
            var eventType = parser.eventType
            val sb = StringBuilder()
            var currentTag = ""
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name ?: ""
                    }
                    XmlPullParser.TEXT -> {
                        if (currentTag == "a:t" || currentTag == "t") {
                            val text = parser.text?.trim()
                            if (!text.isNullOrEmpty() && !text.all { it.isDigit() }) {
                                if (sb.isNotEmpty()) sb.append(" ")
                                sb.append(text)
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
            sb.toString().trim()
        } catch (_: Exception) {
            ""
        }
    }

    private fun parseSlideXml(
        stream: InputStream,
        slideNum: Int,
        slideWidth: Long,
        slideHeight: Long,
        aspectRatio: Float,
        mediaMap: Map<String, ByteArray>,
        relsMap: Map<String, String>,
        notes: String? = null
    ): PptxSlide {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(stream, "UTF-8")
        }

        val visualElements = mutableListOf<VisualElement>()
        val plainTextParagraphs = mutableListOf<String>()
        var slideBgColor: Color? = null

        // Temporary state while traversing XML
        var currentTag = ""
        var inShape = false
        var inPic = false
        var inGraphicFrame = false
        var inTable = false

        var curOffX = 0L
        var curOffY = 0L
        var curExtX = 0L
        var curExtY = 0L
        var curFillColor: Color? = null
        var curBorderColor: Color? = null
        var curBorderWidth = 0f
        var curIsRounded = false
        var curIsEllipse = false
        var curShapePreset: String? = null
        var curBlipEmbed: String? = null

        val curParagraphs = mutableListOf<SlideParagraph>()
        val curRuns = mutableListOf<SlideTextRun>()
        val currentRunText = StringBuilder()
        var curRunSize = 14f
        var curRunBold = false
        var curRunItalic = false
        var curRunColor: Color? = null
        var curParaAlign = TextAlign.Start

        val curTableRows = mutableListOf<MutableList<String>>()
        var curTableRow = mutableListOf<String>()
        val curCellText = StringBuilder()

        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name ?: ""

            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = tagName

                    when (tagName) {
                        "p:sp", "sp" -> {
                            inShape = true
                            curOffX = 0L; curOffY = 0L; curExtX = 0L; curExtY = 0L
                            curFillColor = null; curBorderColor = null; curBorderWidth = 0f
                            curIsRounded = false; curIsEllipse = false
                            curShapePreset = null
                            curParagraphs.clear()
                        }

                        "p:pic", "pic" -> {
                            inPic = true
                            curOffX = 0L; curOffY = 0L; curExtX = 0L; curExtY = 0L
                            curBlipEmbed = null
                        }

                        "p:graphicFrame", "graphicFrame" -> {
                            inGraphicFrame = true
                            curOffX = 0L; curOffY = 0L; curExtX = 0L; curExtY = 0L
                            curTableRows.clear()
                        }

                        "a:tbl", "tbl" -> {
                            inTable = true
                            curTableRows.clear()
                        }

                        "a:tr", "tr" -> {
                            curTableRow = mutableListOf()
                        }

                        "a:tc", "tc" -> {
                            curCellText.clear()
                        }

                        "a:off", "off" -> {
                            val x = parser.getAttributeValue(null, "x")?.toLongOrNull() ?: 0L
                            val y = parser.getAttributeValue(null, "y")?.toLongOrNull() ?: 0L
                            curOffX = x
                            curOffY = y
                        }

                        "a:ext", "ext" -> {
                            val cx = parser.getAttributeValue(null, "cx")?.toLongOrNull() ?: 0L
                            val cy = parser.getAttributeValue(null, "cy")?.toLongOrNull() ?: 0L
                            if (cx > 0L && cy > 0L) {
                                curExtX = cx
                                curExtY = cy
                            }
                        }

                        "a:prstGeom", "prstGeom" -> {
                            val prst = parser.getAttributeValue(null, "prst") ?: ""
                            curShapePreset = prst
                            if (prst.contains("round", ignoreCase = true)) {
                                curIsRounded = true
                            } else if (prst == "ellipse" || prst == "circle") {
                                curIsEllipse = true
                            }
                        }

                        "a:srgbClr", "srgbClr" -> {
                            val hex = parser.getAttributeValue(null, "val")
                            val color = parseHexColor(hex)
                            if (color != null) {
                                if (inShape && curFillColor == null) {
                                    curFillColor = color
                                } else if (curRunColor == null) {
                                    curRunColor = color
                                }
                            }
                        }

                        "a:schemeClr", "schemeClr" -> {
                            val scheme = parser.getAttributeValue(null, "val") ?: ""
                            val color = parseSchemeColor(scheme)
                            if (color != null) {
                                if (inShape && curFillColor == null) {
                                    curFillColor = color
                                } else if (curRunColor == null) {
                                    curRunColor = color
                                }
                            }
                        }

                        "a:blip", "blip" -> {
                            var embed = parser.getAttributeValue(null, "r:embed")
                            if (embed == null) {
                                // Try without namespace prefix
                                for (i in 0 until parser.attributeCount) {
                                    if (parser.getAttributeName(i).contains("embed")) {
                                        embed = parser.getAttributeValue(i)
                                        break
                                    }
                                }
                            }
                            if (embed != null) {
                                curBlipEmbed = embed
                            }
                        }

                        "a:p", "p" -> {
                            curRuns.clear()
                            curParaAlign = TextAlign.Start
                        }

                        "a:pPr", "pPr" -> {
                            val algn = parser.getAttributeValue(null, "algn")
                            curParaAlign = when (algn) {
                                "ctr" -> TextAlign.Center
                                "r" -> TextAlign.End
                                "just" -> TextAlign.Justify
                                else -> TextAlign.Start
                            }
                        }

                        "a:r", "r" -> {
                            currentRunText.clear()
                            curRunSize = 14f
                            curRunBold = false
                            curRunItalic = false
                            curRunColor = null
                        }

                        "a:rPr", "rPr" -> {
                            val sz = parser.getAttributeValue(null, "sz")?.toFloatOrNull()
                            if (sz != null) {
                                curRunSize = (sz / 100f).coerceIn(8f, 72f)
                            }
                            curRunBold = parser.getAttributeValue(null, "b") == "1"
                            curRunItalic = parser.getAttributeValue(null, "i") == "1"
                        }

                        "a:t", "t" -> {
                            val text = parser.nextText()
                            if (text.isNotEmpty()) {
                                currentRunText.append(text)
                                if (inTable) {
                                    if (curCellText.isNotEmpty()) curCellText.append(" ")
                                    curCellText.append(text)
                                }
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    when (tagName) {
                        "a:r", "r" -> {
                            val text = currentRunText.toString()
                            if (text.isNotEmpty()) {
                                curRuns.add(
                                    SlideTextRun(
                                        text = text,
                                        fontSizeSp = curRunSize,
                                        isBold = curRunBold,
                                        isItalic = curRunItalic,
                                        color = curRunColor
                                    )
                                )
                            }
                        }

                        "a:p", "p" -> {
                            if (curRuns.isNotEmpty()) {
                                val para = SlideParagraph(
                                    runs = curRuns.toList(),
                                    alignment = curParaAlign
                                )
                                curParagraphs.add(para)
                                val plain = para.plainText.trim()
                                if (plain.isNotEmpty()) {
                                    plainTextParagraphs.add(plain)
                                }
                            }
                        }

                        "a:tc", "tc" -> {
                            curTableRow.add(curCellText.toString().trim())
                        }

                        "a:tr", "tr" -> {
                            if (curTableRow.isNotEmpty()) {
                                curTableRows.add(curTableRow.toList().toMutableList())
                            }
                        }

                        "a:tbl", "tbl" -> {
                            inTable = false
                            if (curTableRows.isNotEmpty()) {
                                val bounds = calculateBounds(curOffX, curOffY, curExtX, curExtY, slideWidth, slideHeight)
                                visualElements.add(
                                    VisualElement(
                                        type = VisualElementType.TABLE,
                                        bounds = bounds,
                                        tableRows = curTableRows.map { it.toList() }
                                    )
                                )
                            }
                        }

                        "p:graphicFrame", "graphicFrame" -> {
                            inGraphicFrame = false
                        }

                        "p:sp", "sp" -> {
                            inShape = false
                            val bounds = calculateBounds(curOffX, curOffY, curExtX, curExtY, slideWidth, slideHeight)
                            val type = if (curFillColor != null || curBorderColor != null || curShapePreset != null) {
                                VisualElementType.SHAPE
                            } else {
                                VisualElementType.TEXT_BOX
                            }

                            if (curParagraphs.isNotEmpty() || curFillColor != null || curBorderColor != null || curShapePreset != null) {
                                visualElements.add(
                                    VisualElement(
                                        type = type,
                                        bounds = bounds,
                                        backgroundColor = curFillColor,
                                        borderColor = curBorderColor,
                                        borderWidthDp = curBorderWidth,
                                        isRounded = curIsRounded,
                                        isEllipse = curIsEllipse,
                                        shapePreset = curShapePreset,
                                        paragraphs = curParagraphs.toList()
                                    )
                                )
                            }
                        }

                        "p:pic", "pic" -> {
                            inPic = false
                            val embedId = curBlipEmbed
                            if (embedId != null) {
                                val targetPath = relsMap[embedId]
                                val imageBytes = if (targetPath != null) {
                                    mediaMap[targetPath]
                                        ?: mediaMap[targetPath.substringAfterLast('/')]
                                        ?: mediaMap["ppt/" + targetPath.removePrefix("../")]
                                } else null

                                val bounds = calculateBounds(curOffX, curOffY, curExtX, curExtY, slideWidth, slideHeight)

                                if (imageBytes != null && imageBytes.isNotEmpty()) {
                                    visualElements.add(
                                        VisualElement(
                                            type = VisualElementType.IMAGE,
                                            bounds = bounds,
                                            imageBytes = imageBytes
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            eventType = parser.next()
        }

        val title = plainTextParagraphs.firstOrNull() ?: "Slide $slideNum"
        val content = if (plainTextParagraphs.size > 1) plainTextParagraphs.drop(1) else emptyList()

        return PptxSlide(
            slideNumber = slideNum,
            title = title,
            content = content,
            visualElements = visualElements,
            backgroundColor = slideBgColor,
            aspectRatio = aspectRatio,
            notes = notes
        )
    }

    private fun calculateBounds(
        offX: Long,
        offY: Long,
        extX: Long,
        extY: Long,
        slideWidth: Long,
        slideHeight: Long
    ): SlideBounds {
        val w = if (slideWidth > 0) slideWidth.toFloat() else 9144000f
        val h = if (slideHeight > 0) slideHeight.toFloat() else 5143500f

        val left = (offX.toFloat() / w).coerceIn(0f, 1f)
        val top = (offY.toFloat() / h).coerceIn(0f, 1f)
        val width = (extX.toFloat() / w).coerceIn(0f, 1f - left)
        val height = (extY.toFloat() / h).coerceIn(0f, 1f - top)

        return SlideBounds(
            left = left,
            top = top,
            width = if (width > 0f) width else 0.5f,
            height = if (height > 0f) height else 0.2f
        )
    }

    private fun parseHexColor(hex: String?): Color? {
        if (hex.isNullOrBlank()) return null
        val cleanHex = hex.trim().removePrefix("#")
        return try {
            if (cleanHex.length == 6) {
                val rgb = cleanHex.toLong(16)
                Color(0xFF000000 or rgb)
            } else if (cleanHex.length == 8) {
                val argb = cleanHex.toLong(16)
                Color(argb)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun parseSchemeColor(scheme: String): Color? {
        return when (scheme.lowercase()) {
            "accent1" -> Color(0xFF2563EB)
            "accent2" -> Color(0xFFDC2626)
            "accent3" -> Color(0xFF16A34A)
            "accent4" -> Color(0xFFD97706)
            "accent5" -> Color(0xFF7C3AED)
            "accent6" -> Color(0xFF0D9488)
            "dk1", "tx1" -> Color(0xFF1E293B)
            "lt1", "bg1" -> Color(0xFFFFFFFF)
            "dk2", "tx2" -> Color(0xFF475569)
            "lt2", "bg2" -> Color(0xFFF1F5F9)
            else -> null
        }
    }
}
