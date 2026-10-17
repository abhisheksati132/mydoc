package com.abhisheksati.mydoc.rendering.engine.presentation

import android.util.Xml
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

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

/**
 * Resilient multi-format presentation engine.
 * Supports:
 * 1. Modern OOXML PowerPoint presentations (.pptx) via safe random-access ZipFile, layout placeholder mapping,
 *    slide relationships, speaker notes, and vector shapes.
 * 2. Legacy binary PowerPoint presentations (.ppt, PowerPoint 97-2003 OLE2) via binary record parsing
 *    (RT_Slide, RT_TextCharsAtom, RT_TextBytesAtom) and string stream extraction fallback.
 */
class PptxParser {

    suspend fun parse(inputStream: InputStream): PptxPresentation = withContext(Dispatchers.IO) {
        val bytes = inputStream.readBytes()
        if (bytes.isEmpty()) {
            return@withContext emptyPresentation()
        }

        // Check file magic bytes
        val isZip = bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() // PK..
        val isOle2 = bytes.size >= 8 &&
            bytes[0] == 0xD0.toByte() && bytes[1] == 0xCF.toByte() &&
            bytes[2] == 0x11.toByte() && bytes[3] == 0xE0.toByte() // D0 CF 11 E0

        if (isOle2) {
            // Legacy binary .ppt format
            return@withContext parseBinaryPpt(bytes)
        }

        if (isZip) {
            // Modern .pptx format
            val pres = parsePptxZip(bytes)
            if (pres.slides.isNotEmpty()) {
                return@withContext pres
            }
        }

        // Fallback: Attempt both parsers if magic detection was ambiguous
        try {
            val pres = parsePptxZip(bytes)
            if (pres.slides.isNotEmpty()) return@withContext pres
        } catch (_: Exception) {}

        parseBinaryPpt(bytes)
    }

    private fun emptyPresentation(): PptxPresentation {
        return PptxPresentation(
            slides = listOf(
                PptxSlide(
                    slideNumber = 1,
                    title = "Empty Presentation",
                    content = listOf("No slides found in this file."),
                    aspectRatio = 16f / 9f
                )
            ),
            aspectRatio = 16f / 9f
        )
    }

    /**
     * Modern .pptx parser using temporary ZipFile for random-access, immune to streaming ZipInputStream bugs.
     */
    private fun parsePptxZip(bytes: ByteArray): PptxPresentation {
        val tempFile = File.createTempFile("pptx_doc_", ".pptx")
        try {
            tempFile.writeBytes(bytes)
            val zip = ZipFile(tempFile)
            try {
                return parseZipFile(zip)
            } finally {
                zip.close()
            }
        } finally {
            tempFile.delete()
        }
    }

    private fun parseZipFile(zip: ZipFile): PptxPresentation {
        val mediaMap = mutableMapOf<String, ByteArray>()
        val relsMap = mutableMapOf<String, MutableMap<String, String>>()
        val notesMap = mutableMapOf<String, String>()
        var slideWidth = 9144000L
        var slideHeight = 5143500L

        // Index all entries with normalized paths (lowercase, forward slashes)
        val entries = zip.entries().asSequence().toList()
        val entryMap = entries.associateBy { it.name.replace('\\', '/').trimStart('/') }

        // 1. Parse presentation dimensions and slide ordering from ppt/presentation.xml
        val presEntry = entryMap["ppt/presentation.xml"]
            ?: entries.firstOrNull { it.name.endsWith("presentation.xml", ignoreCase = true) }

        val orderedSlideRelIds = mutableListOf<String>()
        if (presEntry != null) {
            val presXml = zip.getInputStream(presEntry).readBytes()
            val (dims, slideRels) = parsePresentationInfo(presXml)
            slideWidth = dims.first
            slideHeight = dims.second
            orderedSlideRelIds.addAll(slideRels)
        }

        // 2. Parse presentation relationships (maps rId -> slide path)
        val presRelsEntry = entryMap["ppt/_rels/presentation.xml.rels"]
            ?: entries.firstOrNull { it.name.endsWith("presentation.xml.rels", ignoreCase = true) }
        val presRelsMap = if (presRelsEntry != null) {
            parseRelationshipsXml(zip.getInputStream(presRelsEntry).readBytes())
        } else emptyMap()

        // 3. Extract media files (pictures, photos, diagrams)
        for (entry in entries) {
            val normName = entry.name.replace('\\', '/').trimStart('/')
            if (normName.contains("ppt/media/", ignoreCase = true)) {
                val mediaBytes = zip.getInputStream(entry).readBytes()
                mediaMap[normName] = mediaBytes
                val shortName = normName.removePrefix("ppt/")
                mediaMap[shortName] = mediaBytes
                mediaMap[normName.substringAfterLast('/')] = mediaBytes
            }
        }

        // 4. Extract slide relationships (slide*.xml.rels)
        for (entry in entries) {
            val normName = entry.name.replace('\\', '/').trimStart('/')
            if (normName.contains("_rels/") && normName.endsWith(".rels", ignoreCase = true)) {
                val relsBytes = zip.getInputStream(entry).readBytes()
                val map = parseRelationshipsXml(relsBytes)
                relsMap[normName] = map
                // Also store with slide base name
                val base = normName.substringAfterLast('/').removeSuffix(".rels")
                relsMap[base] = map
            }
        }

        // 5. Extract Speaker Notes (notesSlide*.xml)
        for (entry in entries) {
            val normName = entry.name.replace('\\', '/').trimStart('/')
            if (normName.contains("notesSlide", ignoreCase = true) && normName.endsWith(".xml", ignoreCase = true)) {
                val notesBytes = zip.getInputStream(entry).readBytes()
                val noteText = parseNotesXml(notesBytes)
                if (noteText.isNotEmpty()) {
                    notesMap[normName] = noteText
                    notesMap[normName.removePrefix("ppt/")] = noteText
                    notesMap[normName.substringAfterLast('/')] = noteText
                }
            }
        }

        // 6. Determine slide entries in true presentation order
        val slideEntries = mutableListOf<java.util.zip.ZipEntry>()
        if (orderedSlideRelIds.isNotEmpty()) {
            for (relId in orderedSlideRelIds) {
                val target = presRelsMap[relId]
                if (target != null) {
                    val targetNorm = target.trimStart('/').let {
                        if (it.startsWith("slides/")) "ppt/$it"
                        else if (!it.startsWith("ppt/")) "ppt/slides/$it"
                        else it
                    }
                    val found = entryMap[targetNorm]
                        ?: entryMap[targetNorm.lowercase()]
                        ?: entries.firstOrNull { it.name.replace('\\', '/').endsWith(target.substringAfterLast('/'), ignoreCase = true) }
                    if (found != null && !slideEntries.contains(found)) {
                        slideEntries.add(found)
                    }
                }
            }
        }

        // Fallback: If ordered list is empty, scan all slide XML files and sort them numerically
        if (slideEntries.isEmpty()) {
            val candidateSlides = entries.filter { entry ->
                val n = entry.name.replace('\\', '/').lowercase()
                n.contains("ppt/slides/slide") && n.endsWith(".xml") && !n.contains("_rels")
            }.sortedBy { entry ->
                val numStr = entry.name.filter { it.isDigit() }
                numStr.toIntOrNull() ?: 0
            }
            slideEntries.addAll(candidateSlides)
        }

        val presAspectRatio = if (slideHeight > 0) slideWidth.toFloat() / slideHeight.toFloat() else 16f / 9f

        // 7. Parse each slide XML into rich visual elements
        val slidesList = mutableListOf<PptxSlide>()
        for ((index, slideEntry) in slideEntries.withIndex()) {
            val slideNum = index + 1
            val normName = slideEntry.name.replace('\\', '/').trimStart('/')
            val slideBase = normName.substringAfterLast('/')
            val relsEntryName = "ppt/slides/_rels/$slideBase.rels"

            val slideRels = relsMap[relsEntryName]
                ?: relsMap[slideBase]
                ?: relsMap["slide$slideNum.xml"]
                ?: emptyMap()

            val noteTarget = slideRels.values.find { it.contains("notesSlide", ignoreCase = true) }
            val slideNotes = (if (noteTarget != null) {
                notesMap[noteTarget] ?: notesMap[noteTarget.substringAfterLast('/')]
            } else null)
                ?: notesMap["ppt/notesSlides/notesSlide$slideNum.xml"]
                ?: notesMap["notesSlide$slideNum.xml"]

            val slideXmlBytes = zip.getInputStream(slideEntry).readBytes()
            val slide = parseSlideXml(
                stream = ByteArrayInputStream(slideXmlBytes),
                slideNum = slideNum,
                slideWidth = slideWidth,
                slideHeight = slideHeight,
                aspectRatio = presAspectRatio,
                mediaMap = mediaMap,
                relsMap = slideRels,
                notes = slideNotes
            )
            slidesList.add(slide)
        }

        return if (slidesList.isNotEmpty()) {
            PptxPresentation(slides = slidesList, aspectRatio = presAspectRatio)
        } else {
            emptyPresentation()
        }
    }

    private fun parsePresentationInfo(xmlBytes: ByteArray): Pair<Pair<Long, Long>, List<String>> {
        var w = 9144000L
        var h = 5143500L
        val relIds = mutableListOf<String>()

        try {
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(ByteArrayInputStream(xmlBytes), "UTF-8")
            }
            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val name = parser.name ?: ""
                    if (name.endsWith("sldSz", ignoreCase = true)) {
                        val cx = parser.getAttributeValue(null, "cx")?.toLongOrNull()
                        val cy = parser.getAttributeValue(null, "cy")?.toLongOrNull()
                        if (cx != null && cx > 0L) w = cx
                        if (cy != null && cy > 0L) h = cy
                    } else if (name.endsWith("sldId", ignoreCase = true)) {
                        // Attribute r:id
                        var rId: String? = null
                        for (i in 0 until parser.attributeCount) {
                            if (parser.getAttributeName(i).contains("id", ignoreCase = true)) {
                                val v = parser.getAttributeValue(i)
                                if (v.startsWith("rId", ignoreCase = true)) {
                                    rId = v
                                    break
                                }
                            }
                        }
                        if (rId != null) relIds.add(rId)
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}

        return Pair(Pair(w, h), relIds)
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
                if (eventType == XmlPullParser.START_TAG && parser.name?.endsWith("Relationship", ignoreCase = true) == true) {
                    val id = parser.getAttributeValue(null, "Id")
                    val target = parser.getAttributeValue(null, "Target")
                    if (id != null && target != null) {
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
                        if (currentTag.equals("a:t", ignoreCase = true) || currentTag.equals("t", ignoreCase = true)) {
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
        val visualElements = mutableListOf<VisualElement>()
        val plainTextParagraphs = mutableListOf<String>()
        var slideBgColor: Color? = null

        try {
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(stream, "UTF-8")
            }

            var currentTag = ""
            var inShape = false
            var inPic = false
            var inGraphicFrame = false
            var inTable = false
            var shapeCounter = 0

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
            var curPlaceholderType: String? = null
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

                        when {
                            tagName.endsWith("sp", ignoreCase = true) && !tagName.endsWith("grpSp", ignoreCase = true) -> {
                                inShape = true
                                curOffX = 0L; curOffY = 0L; curExtX = 0L; curExtY = 0L
                                curFillColor = null; curBorderColor = null; curBorderWidth = 0f
                                curIsRounded = false; curIsEllipse = false
                                curShapePreset = null
                                curPlaceholderType = null
                                curParagraphs.clear()
                            }

                            tagName.endsWith("pic", ignoreCase = true) -> {
                                inPic = true
                                curOffX = 0L; curOffY = 0L; curExtX = 0L; curExtY = 0L
                                curBlipEmbed = null
                            }

                            tagName.endsWith("graphicFrame", ignoreCase = true) -> {
                                inGraphicFrame = true
                                curOffX = 0L; curOffY = 0L; curExtX = 0L; curExtY = 0L
                                curTableRows.clear()
                            }

                            tagName.endsWith("tbl", ignoreCase = true) -> {
                                inTable = true
                                curTableRows.clear()
                            }

                            tagName.endsWith("tr", ignoreCase = true) -> {
                                curTableRow = mutableListOf()
                            }

                            tagName.endsWith("tc", ignoreCase = true) -> {
                                curCellText.clear()
                            }

                            tagName.endsWith("ph", ignoreCase = true) -> {
                                val phType = parser.getAttributeValue(null, "type")
                                curPlaceholderType = phType ?: "body"
                            }

                            tagName.endsWith("off", ignoreCase = true) -> {
                                val x = parser.getAttributeValue(null, "x")?.toLongOrNull() ?: 0L
                                val y = parser.getAttributeValue(null, "y")?.toLongOrNull() ?: 0L
                                curOffX = x
                                curOffY = y
                            }

                            tagName.endsWith("ext", ignoreCase = true) -> {
                                val cx = parser.getAttributeValue(null, "cx")?.toLongOrNull() ?: 0L
                                val cy = parser.getAttributeValue(null, "cy")?.toLongOrNull() ?: 0L
                                if (cx > 0L && cy > 0L) {
                                    curExtX = cx
                                    curExtY = cy
                                }
                            }

                            tagName.endsWith("prstGeom", ignoreCase = true) -> {
                                val prst = parser.getAttributeValue(null, "prst") ?: ""
                                curShapePreset = prst
                                if (prst.contains("round", ignoreCase = true)) {
                                    curIsRounded = true
                                } else if (prst == "ellipse" || prst == "circle") {
                                    curIsEllipse = true
                                }
                            }

                            tagName.endsWith("srgbClr", ignoreCase = true) -> {
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

                            tagName.endsWith("schemeClr", ignoreCase = true) -> {
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

                            tagName.endsWith("blip", ignoreCase = true) -> {
                                var embed: String? = null
                                for (i in 0 until parser.attributeCount) {
                                    if (parser.getAttributeName(i).contains("embed", ignoreCase = true)) {
                                        embed = parser.getAttributeValue(i)
                                        break
                                    }
                                }
                                if (embed != null) curBlipEmbed = embed
                            }

                            tagName.endsWith("p", ignoreCase = true) -> {
                                curRuns.clear()
                                curParaAlign = TextAlign.Start
                            }

                            tagName.endsWith("pPr", ignoreCase = true) -> {
                                val algn = parser.getAttributeValue(null, "algn")
                                curParaAlign = when (algn) {
                                    "ctr" -> TextAlign.Center
                                    "r" -> TextAlign.End
                                    "just" -> TextAlign.Justify
                                    else -> TextAlign.Start
                                }
                            }

                            tagName.endsWith("r", ignoreCase = true) -> {
                                currentRunText.clear()
                                curRunSize = 14f
                                curRunBold = false
                                curRunItalic = false
                                curRunColor = null
                            }

                            tagName.endsWith("rPr", ignoreCase = true) -> {
                                val sz = parser.getAttributeValue(null, "sz")?.toFloatOrNull()
                                if (sz != null) {
                                    curRunSize = (sz / 100f).coerceIn(8f, 72f)
                                }
                                curRunBold = parser.getAttributeValue(null, "b") == "1"
                                curRunItalic = parser.getAttributeValue(null, "i") == "1"
                            }

                            tagName.endsWith("br", ignoreCase = true) -> {
                                currentRunText.append("\n")
                            }
                        }
                    }

                    XmlPullParser.TEXT -> {
                        if (currentTag.endsWith(":t", ignoreCase = true) || currentTag.equals("t", ignoreCase = true)) {
                            val text = parser.text ?: ""
                            if (text.isNotEmpty()) {
                                currentRunText.append(text)
                                if (inTable) {
                                    if (curCellText.isNotEmpty()) curCellText.append(" ")
                                    curCellText.append(text)
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        when {
                            tagName.endsWith("r", ignoreCase = true) -> {
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

                            tagName.endsWith("p", ignoreCase = true) -> {
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

                            tagName.endsWith("tc", ignoreCase = true) -> {
                                curTableRow.add(curCellText.toString().trim())
                            }

                            tagName.endsWith("tr", ignoreCase = true) -> {
                                if (curTableRow.isNotEmpty()) {
                                    curTableRows.add(curTableRow.toList().toMutableList())
                                }
                            }

                            tagName.endsWith("tbl", ignoreCase = true) -> {
                                inTable = false
                                if (curTableRows.isNotEmpty()) {
                                    val bounds = calculateBounds(curOffX, curOffY, curExtX, curExtY, slideWidth, slideHeight, curPlaceholderType, shapeCounter++)
                                    visualElements.add(
                                        VisualElement(
                                            type = VisualElementType.TABLE,
                                            bounds = bounds,
                                            tableRows = curTableRows.map { it.toList() }
                                        )
                                    )
                                }
                            }

                            tagName.endsWith("graphicFrame", ignoreCase = true) -> {
                                inGraphicFrame = false
                            }

                            tagName.endsWith("sp", ignoreCase = true) && !tagName.endsWith("grpSp", ignoreCase = true) -> {
                                inShape = false
                                if (curParagraphs.isNotEmpty() || curFillColor != null || curBorderColor != null || curShapePreset != null) {
                                    val bounds = calculateBounds(curOffX, curOffY, curExtX, curExtY, slideWidth, slideHeight, curPlaceholderType, shapeCounter++)
                                    val type = if (curFillColor != null || curBorderColor != null || curShapePreset != null) {
                                        VisualElementType.SHAPE
                                    } else {
                                        VisualElementType.TEXT_BOX
                                    }

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

                            tagName.endsWith("pic", ignoreCase = true) -> {
                                inPic = false
                                val embedId = curBlipEmbed
                                if (embedId != null) {
                                    val targetPath = relsMap[embedId]
                                    val imageBytes = if (targetPath != null) {
                                        mediaMap[targetPath]
                                            ?: mediaMap[targetPath.substringAfterLast('/')]
                                            ?: mediaMap["ppt/" + targetPath.removePrefix("../")]
                                    } else null

                                    if (imageBytes != null && imageBytes.isNotEmpty()) {
                                        val bounds = calculateBounds(curOffX, curOffY, curExtX, curExtY, slideWidth, slideHeight, curPlaceholderType, shapeCounter++)
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
        } catch (_: Exception) {}

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

    /**
     * Computes bounds for a visual element.
     * If the slide omitted xfrm (very common for standard PowerPoint placeholders), assigns
     * standard layout coordinates so shapes and text do not stack on top of each other at (0, 0).
     */
    private fun calculateBounds(
        offX: Long,
        offY: Long,
        extX: Long,
        extY: Long,
        slideWidth: Long,
        slideHeight: Long,
        placeholderType: String?,
        shapeIndex: Int
    ): SlideBounds {
        val w = if (slideWidth > 0) slideWidth.toFloat() else 9144000f
        val h = if (slideHeight > 0) slideHeight.toFloat() else 5143500f

        if (extX > 0L && extY > 0L) {
            val left = (offX.toFloat() / w).coerceIn(0f, 1f)
            val top = (offY.toFloat() / h).coerceIn(0f, 1f)
            val width = (extX.toFloat() / w).coerceIn(0.01f, 1f - left)
            val height = (extY.toFloat() / h).coerceIn(0.01f, 1f - top)
            return SlideBounds(left = left, top = top, width = width, height = height)
        }

        // Layout fallback for placeholder shapes missing explicit xfrm
        return when (placeholderType?.lowercase()) {
            "title", "ctrtitle" -> SlideBounds(left = 0.06f, top = 0.08f, width = 0.88f, height = 0.18f)
            "subtitle" -> SlideBounds(left = 0.08f, top = 0.28f, width = 0.84f, height = 0.18f)
            "body", "obj" -> SlideBounds(left = 0.06f, top = 0.30f, width = 0.88f, height = 0.60f)
            "dt" -> SlideBounds(left = 0.06f, top = 0.92f, width = 0.25f, height = 0.05f)
            "ftr" -> SlideBounds(left = 0.35f, top = 0.92f, width = 0.30f, height = 0.05f)
            "sldnum" -> SlideBounds(left = 0.80f, top = 0.92f, width = 0.14f, height = 0.05f)
            else -> {
                // Stagger non-positioned shapes vertically
                val topOffset = (0.08f + (shapeIndex * 0.20f)).coerceIn(0.08f, 0.72f)
                SlideBounds(left = 0.06f, top = topOffset, width = 0.88f, height = 0.18f)
            }
        }
    }

    /**
     * Resilient parser for legacy PowerPoint 97-2003 binary files (.ppt).
     * Scans PowerPoint Document atom records (RT_Slide = 0x03EE, RT_TextCharsAtom = 0x0F9E,
     * RT_TextBytesAtom = 0x0F9F) and falls back to string extraction so every .ppt is readable.
     */
    private fun parseBinaryPpt(bytes: ByteArray): PptxPresentation {
        val slides = mutableListOf<PptxSlide>()
        var currentTitle = ""
        val currentContent = mutableListOf<String>()
        var slideCounter = 1

        fun flushSlide() {
            if (currentTitle.isNotBlank() || currentContent.isNotEmpty()) {
                val t = if (currentTitle.isNotBlank()) currentTitle else "Slide $slideCounter"
                val visuals = mutableListOf<VisualElement>()
                visuals.add(
                    VisualElement(
                        type = VisualElementType.TEXT_BOX,
                        bounds = SlideBounds(0.06f, 0.08f, 0.88f, 0.20f),
                        paragraphs = listOf(
                            SlideParagraph(
                                runs = listOf(SlideTextRun(text = t, fontSizeSp = 22f, isBold = true))
                            )
                        )
                    )
                )
                if (currentContent.isNotEmpty()) {
                    visuals.add(
                        VisualElement(
                            type = VisualElementType.TEXT_BOX,
                            bounds = SlideBounds(0.06f, 0.32f, 0.88f, 0.58f),
                            paragraphs = currentContent.map { line ->
                                SlideParagraph(
                                    runs = listOf(SlideTextRun(text = line, fontSizeSp = 15f)),
                                    isBullet = true
                                )
                            }
                        )
                    )
                }

                slides.add(
                    PptxSlide(
                        slideNumber = slideCounter++,
                        title = t,
                        content = currentContent.toList(),
                        visualElements = visuals,
                        aspectRatio = 4f / 3f
                    )
                )
                currentTitle = ""
                currentContent.clear()
            }
        }

        var i = 0
        val total = bytes.size

        while (i <= total - 8) {
            val recType = (bytes[i + 2].toInt() and 0xFF) or ((bytes[i + 3].toInt() and 0xFF) shl 8)
            val recLen = (bytes[i + 4].toLong() and 0xFF) or
                ((bytes[i + 5].toLong() and 0xFF) shl 8) or
                ((bytes[i + 6].toLong() and 0xFF) shl 16) or
                ((bytes[i + 7].toLong() and 0xFF) shl 24)

            if (recLen < 0 || recLen > 500_000 || i + 8 + recLen > total) {
                i += 2
                continue
            }

            when (recType) {
                0x03EE -> { // RT_Slide
                    flushSlide()
                }

                0x0F9F -> { // RT_TextBytesAtom (Latin-1 / ASCII)
                    if (recLen > 0) {
                        val text = String(bytes, i + 8, recLen.toInt(), Charsets.ISO_8859_1).trim()
                        if (text.isNotBlank() && text.any { it.isLetter() }) {
                            val lines = text.split("\r\n", "\r", "\n").map { it.trim() }.filter { it.isNotEmpty() }
                            for (line in lines) {
                                if (currentTitle.isEmpty()) {
                                    currentTitle = line
                                } else {
                                    currentContent.add(line)
                                }
                            }
                        }
                    }
                    i += 8 + recLen.toInt()
                    continue
                }

                0x0F9E -> { // RT_TextCharsAtom (UTF-16LE)
                    if (recLen >= 2) {
                        val text = String(bytes, i + 8, recLen.toInt(), Charsets.UTF_16LE).trim()
                        if (text.isNotBlank() && text.any { it.isLetter() }) {
                            val lines = text.split("\r\n", "\r", "\n").map { it.trim() }.filter { it.isNotEmpty() }
                            for (line in lines) {
                                if (currentTitle.isEmpty()) {
                                    currentTitle = line
                                } else {
                                    currentContent.add(line)
                                }
                            }
                        }
                    }
                    i += 8 + recLen.toInt()
                    continue
                }
            }
            i += 1
        }
        flushSlide()

        // Fallback: If binary record traversal returned no slides, extract printable text strings
        if (slides.isEmpty()) {
            val extractedStrings = extractReadableStrings(bytes)
            if (extractedStrings.isNotEmpty()) {
                val chunks = extractedStrings.chunked(6)
                for ((idx, chunk) in chunks.withIndex()) {
                    val t = chunk.first()
                    val c = if (chunk.size > 1) chunk.drop(1) else emptyList()
                    val visuals = mutableListOf<VisualElement>()
                    visuals.add(
                        VisualElement(
                            type = VisualElementType.TEXT_BOX,
                            bounds = SlideBounds(0.06f, 0.08f, 0.88f, 0.20f),
                            paragraphs = listOf(
                                SlideParagraph(
                                    runs = listOf(SlideTextRun(text = t, fontSizeSp = 20f, isBold = true))
                                )
                            )
                        )
                    )
                    if (c.isNotEmpty()) {
                        visuals.add(
                            VisualElement(
                                type = VisualElementType.TEXT_BOX,
                                bounds = SlideBounds(0.06f, 0.32f, 0.88f, 0.58f),
                                paragraphs = c.map { line ->
                                    SlideParagraph(
                                        runs = listOf(SlideTextRun(text = line, fontSizeSp = 15f)),
                                        isBullet = true
                                    )
                                }
                            )
                        )
                    }

                    slides.add(
                        PptxSlide(
                            slideNumber = idx + 1,
                            title = t,
                            content = c,
                            visualElements = visuals,
                            aspectRatio = 4f / 3f
                        )
                    )
                }
            }
        }

        return if (slides.isNotEmpty()) {
            PptxPresentation(slides = slides, aspectRatio = 4f / 3f)
        } else {
            PptxPresentation(
                slides = listOf(
                    PptxSlide(
                        slideNumber = 1,
                        title = "PowerPoint Presentation",
                        content = listOf("Legacy binary presentation opened in read-only mode."),
                        aspectRatio = 4f / 3f
                    )
                ),
                aspectRatio = 4f / 3f
            )
        }
    }

    private fun extractReadableStrings(bytes: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        val sb = StringBuilder()
        for (b in bytes) {
            val c = b.toInt().toChar()
            if (c in ' '..'~' || c == '\n') {
                sb.append(c)
            } else {
                if (sb.length >= 4) {
                    val candidate = sb.toString().trim()
                    if (candidate.length >= 4 && candidate.any { it.isLetter() }) {
                        strings.add(candidate)
                    }
                }
                sb.clear()
            }
        }
        return strings.take(60)
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
