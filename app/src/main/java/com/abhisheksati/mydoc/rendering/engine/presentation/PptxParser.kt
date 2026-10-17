package com.abhisheksati.mydoc.rendering.engine.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.ArrayDeque
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
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
    val aspectRatio: Float = 16f / 9f
)

/**
 * Enterprise presentation engine.
 * Supports:
 * 1. Modern OpenXML PowerPoint (.pptx) with exact tag parsing (isTag), slideLayout and slideMaster
 *    placeholder inheritance, nested group transforms (<p:grpSp>), DrawingML theme palettes (<a:clrScheme>)
 *    and color modifiers (lumMod, lumOff, tint, shade), speaker notes, vector shapes, and embedded media.
 * 2. Legacy binary PowerPoint (PowerPoint 97-2003 OLE2 .ppt) via a native Compound File Binary Format
 *    (CFBF) structured storage stream extractor and PowerPoint Document record parser.
 */
class PptxParser {

    suspend fun parse(inputStream: InputStream, cacheDir: File? = null): PptxPresentation = withContext(Dispatchers.IO) {
        val bytes = inputStream.readBytes()
        if (bytes.isEmpty()) {
            return@withContext emptyPresentation()
        }

        val isZip = bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() // PK..
        val isOle2 = bytes.size >= 8 &&
            bytes[0] == 0xD0.toByte() && bytes[1] == 0xCF.toByte() &&
            bytes[2] == 0x11.toByte() && bytes[3] == 0xE0.toByte() // D0 CF 11 E0

        if (isOle2) {
            return@withContext parseBinaryPpt(bytes)
        }

        if (isZip) {
            val pres = parsePptxZip(bytes, cacheDir)
            if (pres.slides.isNotEmpty()) {
                return@withContext pres
            }
        }

        // Fallback: If not recognized or zip failed, attempt binary PPT extraction
        parseBinaryPpt(bytes)
    }

    private fun emptyPresentation(): PptxPresentation {
        val title = "Empty Presentation"
        val lines = listOf("No slides found in this file.")
        val visual = VisualElement(
            type = VisualElementType.TEXT_BOX,
            bounds = SlideBounds(0.08f, 0.35f, 0.84f, 0.30f),
            paragraphs = listOf(
                SlideParagraph(runs = listOf(SlideTextRun(text = title, fontSizeSp = 22f, isBold = true))),
                SlideParagraph(runs = listOf(SlideTextRun(text = lines.first(), fontSizeSp = 15f)))
            )
        )
        return PptxPresentation(
            slides = listOf(
                PptxSlide(
                    slideNumber = 1,
                    title = title,
                    content = lines,
                    visualElements = listOf(visual),
                    aspectRatio = 16f / 9f
                )
            ),
            aspectRatio = 16f / 9f
        )
    }

    /* =========================================================================
     * SECTION 1: MODERN OPENXML (.pptx) PARSER
     * ========================================================================= */

    private fun parsePptxZip(bytes: ByteArray, cacheDir: File?): PptxPresentation {
        if (cacheDir != null && cacheDir.exists()) {
            try {
                val tempFile = File.createTempFile("pptx_view_", ".pptx", cacheDir)
                try {
                    tempFile.writeBytes(bytes)
                    val zip = ZipFile(tempFile)
                    try {
                        val entryMap = mutableMapOf<String, ByteArray>()
                        val entries = zip.entries()
                        while (entries.hasMoreElements()) {
                            val entry = entries.nextElement()
                            if (!entry.isDirectory) {
                                val norm = entry.name.replace('\\', '/').trimStart('/')
                                val data = zip.getInputStream(entry).readBytes()
                                entryMap[norm] = data
                                entryMap[norm.lowercase()] = data
                            }
                        }
                        val pres = parseFromEntryMap(entryMap)
                        if (pres.slides.isNotEmpty()) return pres
                    } finally {
                        zip.close()
                    }
                } finally {
                    tempFile.delete()
                }
            } catch (_: Exception) {}
        }

        return parseZipInMemory(bytes)
    }

    private fun parseZipInMemory(bytes: ByteArray): PptxPresentation {
        val entryMap = mutableMapOf<String, ByteArray>()
        try {
            val zip = ZipInputStream(ByteArrayInputStream(bytes))
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val data = zip.readBytes()
                    val norm = entry.name.replace('\\', '/').trimStart('/')
                    entryMap[norm] = data
                    entryMap[norm.lowercase()] = data
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        } catch (_: Exception) {}

        return parseFromEntryMap(entryMap)
    }

    private data class GroupTransform(
        val offX: Long,
        val offY: Long,
        val extX: Long,
        val extY: Long,
        val chOffX: Long,
        val chOffY: Long,
        val chExtX: Long,
        val chExtY: Long
    )

    private data class PlaceholderLayout(
        val bounds: SlideBounds?,
        val fillColor: Color?,
        val borderColor: Color?,
        val borderWidthDp: Float?,
        val preset: String?,
        val defFont: String?,
        val defSizeSp: Float?,
        val defBold: Boolean?,
        val defItalic: Boolean?,
        val defColor: Color?,
        val defAlign: TextAlign?
    )

    private data class SlideLayoutInfo(
        val placeholders: Map<String, PlaceholderLayout>, // key: "type:idx" or "type" or "idx"
        val backgroundFill: Color?,
        val backgroundShapes: List<VisualElement>
    )

    private data class ThemeInfo(
        val colorScheme: Map<String, Color>,
        val majorFont: String,
        val minorFont: String
    )

    private fun parseFromEntryMap(entryMap: Map<String, ByteArray>): PptxPresentation {
        val mediaMap = mutableMapOf<String, ByteArray>()
        val relsMap = mutableMapOf<String, MutableMap<String, String>>()
        val notesMap = mutableMapOf<String, String>()
        var slideWidth = 9144000L
        var slideHeight = 5143500L

        // 1. Parse presentation dimensions and slide ordering from ppt/presentation.xml
        val presXmlBytes = entryMap["ppt/presentation.xml"]
            ?: entryMap.entries.firstOrNull { it.key.endsWith("presentation.xml", ignoreCase = true) }?.value

        val orderedSlideRelIds = mutableListOf<String>()
        if (presXmlBytes != null) {
            val (dims, slideRels) = parsePresentationInfo(presXmlBytes)
            slideWidth = dims.first
            slideHeight = dims.second
            orderedSlideRelIds.addAll(slideRels)
        }

        // 2. Parse Theme (Color Scheme + Major/Minor Fonts)
        val themeBytes = entryMap["ppt/theme/theme1.xml"]
            ?: entryMap.entries.firstOrNull { it.key.contains("theme/theme", ignoreCase = true) }?.value
        val theme = if (themeBytes != null) parseThemeXml(themeBytes) else defaultTheme()

        // 3. Parse presentation relationships (maps rId -> slide path)
        val presRelsBytes = entryMap["ppt/_rels/presentation.xml.rels"]
            ?: entryMap.entries.firstOrNull { it.key.endsWith("presentation.xml.rels", ignoreCase = true) }?.value
        val presRelsMap = if (presRelsBytes != null) {
            parseRelationshipsXml(presRelsBytes)
        } else emptyMap()

        // 4. Extract media files (pictures, photos, diagrams)
        for ((name, data) in entryMap) {
            if (name.contains("ppt/media/", ignoreCase = true)) {
                mediaMap[name] = data
                val shortName = name.removePrefix("ppt/")
                mediaMap[shortName] = data
                mediaMap[name.substringAfterLast('/')] = data
            }
        }

        // 5. Extract all package relationships
        for ((name, data) in entryMap) {
            if (name.contains("_rels/") && name.endsWith(".rels", ignoreCase = true)) {
                val map = parseRelationshipsXml(data)
                relsMap[name] = map
                val base = name.substringAfterLast('/').removeSuffix(".rels")
                relsMap[base] = map
            }
        }

        // 6. Pre-parse slideLayouts to inherit exact coordinates and placeholder styling
        val layoutCache = mutableMapOf<String, SlideLayoutInfo>()
        for ((name, data) in entryMap) {
            if (name.contains("ppt/slidelayouts/slidelayout", ignoreCase = true) && name.endsWith(".xml", ignoreCase = true)) {
                val layoutInfo = parseSlideLayoutXml(data, slideWidth, slideHeight, theme)
                layoutCache[name] = layoutInfo
                layoutCache[name.removePrefix("ppt/")] = layoutInfo
                layoutCache[name.substringAfterLast('/')] = layoutInfo
            }
        }

        // 7. Extract Speaker Notes (notesSlide*.xml)
        for ((name, data) in entryMap) {
            if (name.contains("notesslide", ignoreCase = true) && name.endsWith(".xml", ignoreCase = true)) {
                val noteText = parseNotesXml(data)
                if (noteText.isNotEmpty()) {
                    notesMap[name] = noteText
                    notesMap[name.removePrefix("ppt/")] = noteText
                    notesMap[name.substringAfterLast('/')] = noteText
                }
            }
        }

        // 8. Determine slide entries in true presentation order
        val slideKeys = mutableListOf<String>()
        if (orderedSlideRelIds.isNotEmpty()) {
            for (relId in orderedSlideRelIds) {
                val target = presRelsMap[relId]
                if (target != null) {
                    val targetNorm = target.trimStart('/').let {
                        if (it.startsWith("slides/")) "ppt/$it"
                        else if (!it.startsWith("ppt/")) "ppt/$it"
                        else it
                    }
                    val foundKey = if (entryMap.containsKey(targetNorm)) targetNorm
                    else if (entryMap.containsKey(targetNorm.lowercase())) targetNorm.lowercase()
                    else entryMap.keys.firstOrNull { it.endsWith(target.substringAfterLast('/'), ignoreCase = true) }

                    if (foundKey != null && !slideKeys.contains(foundKey)) {
                        slideKeys.add(foundKey)
                    }
                }
            }
        }

        // Fallback: If ordered list is empty, scan all slide XML files and sort them numerically
        if (slideKeys.isEmpty()) {
            val candidateSlides = entryMap.keys.filter { key ->
                val n = key.lowercase()
                n.contains("ppt/slides/slide") && n.endsWith(".xml") && !n.contains("_rels")
            }.distinctBy { it.substringAfterLast('/') }.sortedBy { key ->
                val numStr = key.filter { it.isDigit() }
                numStr.toIntOrNull() ?: 0
            }
            slideKeys.addAll(candidateSlides)
        }

        val presAspectRatio = if (slideHeight > 0) slideWidth.toFloat() / slideHeight.toFloat() else 16f / 9f

        // 9. Parse each slide XML into rich visual elements
        val slidesList = mutableListOf<PptxSlide>()
        for ((index, key) in slideKeys.withIndex()) {
            val slideNum = index + 1
            val slideBase = key.substringAfterLast('/')
            val relsKey = "ppt/slides/_rels/$slideBase.rels"

            val slideRels = relsMap[relsKey]
                ?: relsMap[slideBase]
                ?: relsMap["slide$slideNum.xml"]
                ?: emptyMap()

            // Resolve slide layout reference
            val layoutTarget = slideRels.values.find { it.contains("slideLayout", ignoreCase = true) }
            val slideLayout = if (layoutTarget != null) {
                layoutCache[layoutTarget]
                    ?: layoutCache[layoutTarget.removePrefix("ppt/")]
                    ?: layoutCache[layoutTarget.substringAfterLast('/')]
            } else null

            // Resolve speaker notes reference
            val noteTarget = slideRels.values.find { it.contains("notesSlide", ignoreCase = true) }
            val slideNotes = (if (noteTarget != null) {
                notesMap[noteTarget] ?: notesMap[noteTarget.substringAfterLast('/')]
            } else null)
                ?: notesMap["ppt/notesSlides/notesSlide$slideNum.xml"]
                ?: notesMap["notesSlide$slideNum.xml"]

            val slideBytes = entryMap[key] ?: continue
            val slide = parseSlideXml(
                stream = ByteArrayInputStream(slideBytes),
                slideNum = slideNum,
                slideWidth = slideWidth,
                slideHeight = slideHeight,
                aspectRatio = presAspectRatio,
                mediaMap = mediaMap,
                relsMap = slideRels,
                theme = theme,
                layoutInfo = slideLayout,
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

    private fun defaultTheme(): ThemeInfo {
        val colors = mapOf(
            "dk1" to Color(0xFF0F172A),
            "lt1" to Color(0xFFFFFFFF),
            "dk2" to Color(0xFF334155),
            "lt2" to Color(0xFFF8FAFC),
            "accent1" to Color(0xFF2563EB),
            "accent2" to Color(0xFFDC2626),
            "accent3" to Color(0xFF16A34A),
            "accent4" to Color(0xFFD97706),
            "accent5" to Color(0xFF7C3AED),
            "accent6" to Color(0xFF0D9488),
            "hlink" to Color(0xFF1D4ED8),
            "folHlink" to Color(0xFF6D28D9)
        )
        return ThemeInfo(colors, majorFont = "Arial", minorFont = "Calibri")
    }

    private fun parseThemeXml(xmlBytes: ByteArray): ThemeInfo {
        val colors = mutableMapOf<String, Color>()
        var majorFont = "Arial"
        var minorFont = "Calibri"

        try {
            val parser = createPullParser(ByteArrayInputStream(xmlBytes))
            var eventType = parser.eventType
            var inClrScheme = false
            var currentClrTag = ""
            var inMajorFont = false
            var inMinorFont = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tag = parser.name ?: ""
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when {
                            isTag(tag, "clrScheme") -> inClrScheme = true
                            inClrScheme && (tag in listOf("dk1", "lt1", "dk2", "lt2", "accent1", "accent2", "accent3", "accent4", "accent5", "accent6", "hlink", "folHlink")) -> {
                                currentClrTag = tag
                            }
                            inClrScheme && currentClrTag.isNotEmpty() && isTag(tag, "srgbClr") -> {
                                val hex = parser.getAttributeValue(null, "val")
                                parseHexColor(hex)?.let { colors[currentClrTag] = it }
                            }
                            inClrScheme && currentClrTag.isNotEmpty() && isTag(tag, "sysClr") -> {
                                val hex = parser.getAttributeValue(null, "lastClr")
                                parseHexColor(hex)?.let { colors[currentClrTag] = it }
                            }
                            isTag(tag, "majorFont") -> inMajorFont = true
                            isTag(tag, "minorFont") -> inMinorFont = true
                            (inMajorFont || inMinorFont) && isTag(tag, "latin") -> {
                                val typeface = parser.getAttributeValue(null, "typeface")
                                if (!typeface.isNullOrBlank()) {
                                    if (inMajorFont) majorFont = typeface
                                    if (inMinorFont) minorFont = typeface
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when {
                            isTag(tag, "clrScheme") -> inClrScheme = false
                            tag == currentClrTag -> currentClrTag = ""
                            isTag(tag, "majorFont") -> inMajorFont = false
                            isTag(tag, "minorFont") -> inMinorFont = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback standard colors if missing in theme
        val defaults = defaultTheme().colorScheme
        for ((k, v) in defaults) {
            if (!colors.containsKey(k)) colors[k] = v
        }

        return ThemeInfo(colors, majorFont, minorFont)
    }

    private fun parseSlideLayoutXml(
        xmlBytes: ByteArray,
        slideWidth: Long,
        slideHeight: Long,
        theme: ThemeInfo
    ): SlideLayoutInfo {
        val placeholders = mutableMapOf<String, PlaceholderLayout>()
        var backgroundFill: Color? = null
        val backgroundShapes = mutableListOf<VisualElement>()

        try {
            val parser = createPullParser(ByteArrayInputStream(xmlBytes))
            var eventType = parser.eventType
            var inShape = false
            var inSpPr = false
            var inLn = false
            var inTxBody = false
            var inDefRPr = false
            var inBg = false

            var curPhType: String? = null
            var curPhIdx: String? = null
            var curOffX = 0L; var curOffY = 0L; var curExtX = 0L; var curExtY = 0L
            var curFillColor: Color? = null
            var curBorderColor: Color? = null
            var curBorderWidth = 0f
            var curPreset: String? = null
            var curDefFont: String? = null
            var curDefSizeSp: Float? = null
            var curDefBold: Boolean? = null
            var curDefItalic: Boolean? = null
            var curDefColor: Color? = null
            var curDefAlign: TextAlign? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tag = parser.name ?: ""
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when {
                            isTag(tag, "bg") || isTag(tag, "bgPr") -> inBg = true
                            isTag(tag, "sp") -> {
                                inShape = true
                                curPhType = null; curPhIdx = null
                                curOffX = 0L; curOffY = 0L; curExtX = 0L; curExtY = 0L
                                curFillColor = null; curBorderColor = null; curBorderWidth = 0f
                                curPreset = null; curDefFont = null; curDefSizeSp = null
                                curDefBold = null; curDefItalic = null; curDefColor = null; curDefAlign = null
                            }
                            isTag(tag, "spPr") -> inSpPr = true
                            isTag(tag, "ln") -> {
                                inLn = true
                                val w = parser.getAttributeValue(null, "w")?.toFloatOrNull()
                                if (w != null) curBorderWidth = (w / 12700f).coerceIn(0.5f, 10f)
                            }
                            isTag(tag, "txBody") -> inTxBody = true
                            isTag(tag, "defRPr") -> {
                                inDefRPr = true
                                val sz = parser.getAttributeValue(null, "sz")?.toFloatOrNull()
                                if (sz != null) curDefSizeSp = (sz / 100f).coerceIn(8f, 72f)
                                if (parser.getAttributeValue(null, "b") == "1") curDefBold = true
                                if (parser.getAttributeValue(null, "i") == "1") curDefItalic = true
                            }
                            isTag(tag, "pPr") -> {
                                val algn = parser.getAttributeValue(null, "algn")
                                curDefAlign = when (algn) {
                                    "ctr" -> TextAlign.Center
                                    "r" -> TextAlign.End
                                    "just" -> TextAlign.Justify
                                    else -> TextAlign.Start
                                }
                            }
                            isTag(tag, "ph") -> {
                                curPhType = parser.getAttributeValue(null, "type")?.lowercase()
                                curPhIdx = parser.getAttributeValue(null, "idx")
                            }
                            isTag(tag, "off") -> {
                                curOffX = parser.getAttributeValue(null, "x")?.toLongOrNull() ?: 0L
                                curOffY = parser.getAttributeValue(null, "y")?.toLongOrNull() ?: 0L
                            }
                            isTag(tag, "ext") -> {
                                val cx = parser.getAttributeValue(null, "cx")?.toLongOrNull() ?: 0L
                                val cy = parser.getAttributeValue(null, "cy")?.toLongOrNull() ?: 0L
                                if (cx > 0L && cy > 0L) {
                                    curExtX = cx; curExtY = cy
                                }
                            }
                            isTag(tag, "prstGeom") -> {
                                curPreset = parser.getAttributeValue(null, "prst")
                            }
                            isTag(tag, "srgbClr") -> {
                                val hex = parser.getAttributeValue(null, "val")
                                parseHexColor(hex)?.let { c ->
                                    when {
                                        inLn -> curBorderColor = c
                                        inDefRPr -> curDefColor = c
                                        inBg -> backgroundFill = c
                                        inSpPr -> if (curFillColor == null) curFillColor = c
                                    }
                                }
                            }
                            isTag(tag, "schemeClr") -> {
                                val scheme = parser.getAttributeValue(null, "val") ?: ""
                                resolveDrawingSchemeColor(scheme, theme.colorScheme)?.let { c ->
                                    when {
                                        inLn -> curBorderColor = c
                                        inDefRPr -> curDefColor = c
                                        inBg -> backgroundFill = c
                                        inSpPr -> if (curFillColor == null) curFillColor = c
                                    }
                                }
                            }
                            isTag(tag, "latin") -> {
                                val tf = parser.getAttributeValue(null, "typeface")
                                val resolved = when {
                                    tf?.startsWith("+mj") == true -> theme.majorFont
                                    tf?.startsWith("+mn") == true -> theme.minorFont
                                    else -> tf
                                }
                                if (inDefRPr) curDefFont = resolved
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when {
                            isTag(tag, "bg") || isTag(tag, "bgPr") -> inBg = false
                            isTag(tag, "spPr") -> inSpPr = false
                            isTag(tag, "ln") -> inLn = false
                            isTag(tag, "txBody") -> inTxBody = false
                            isTag(tag, "defRPr") -> inDefRPr = false
                            isTag(tag, "sp") -> {
                                inShape = false
                                val bounds = if (curExtX > 0L && curExtY > 0L) {
                                    calculateRawBounds(curOffX, curOffY, curExtX, curExtY, slideWidth, slideHeight)
                                } else null

                                val layout = PlaceholderLayout(
                                    bounds = bounds,
                                    fillColor = curFillColor,
                                    borderColor = curBorderColor,
                                    borderWidthDp = curBorderWidth,
                                    preset = curPreset,
                                    defFont = curDefFont,
                                    defSizeSp = curDefSizeSp,
                                    defBold = curDefBold,
                                    defItalic = curDefItalic,
                                    defColor = curDefColor,
                                    defAlign = curDefAlign
                                )

                                if (curPhType != null) {
                                    placeholders[curPhType!!] = layout
                                    if (curPhIdx != null) placeholders["${curPhType!!}:$curPhIdx"] = layout
                                }
                                if (curPhIdx != null) {
                                    placeholders[curPhIdx!!] = layout
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return SlideLayoutInfo(placeholders, backgroundFill, backgroundShapes)
    }

    private fun parsePresentationInfo(xmlBytes: ByteArray): Pair<Pair<Long, Long>, List<String>> {
        var w = 9144000L
        var h = 5143500L
        val relIds = mutableListOf<String>()

        try {
            val parser = createPullParser(ByteArrayInputStream(xmlBytes))
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val tag = parser.name ?: ""
                    if (isTag(tag, "sldSz")) {
                        val cx = parser.getAttributeValue(null, "cx")?.toLongOrNull()
                        val cy = parser.getAttributeValue(null, "cy")?.toLongOrNull()
                        if (cx != null && cx > 0L) w = cx
                        if (cy != null && cy > 0L) h = cy
                    } else if (isTag(tag, "sldId")) {
                        var rId = parser.getAttributeValue(null, "r:id")
                            ?: parser.getAttributeValue(null, "id")
                        if (rId == null) {
                            for (i in 0 until parser.attributeCount) {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Pair(Pair(w, h), relIds)
    }

    private fun parseRelationshipsXml(xmlBytes: ByteArray): MutableMap<String, String> {
        val rels = mutableMapOf<String, String>()
        try {
            val parser = createPullParser(ByteArrayInputStream(xmlBytes))
            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && isTag(parser.name ?: "", "Relationship")) {
                    val id = parser.getAttributeValue(null, "Id")
                    val target = parser.getAttributeValue(null, "Target")
                    if (id != null && target != null) {
                        val cleanTarget = if (target.startsWith("../")) {
                            "ppt/" + target.removePrefix("../")
                        } else if (target.startsWith("ppt/")) {
                            target
                        } else {
                            "ppt/$target"
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
            val parser = createPullParser(ByteArrayInputStream(xmlBytes))
            var eventType = parser.eventType
            val sb = StringBuilder()
            var currentTag = ""
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> currentTag = parser.name ?: ""
                    XmlPullParser.TEXT -> {
                        if (isTag(currentTag, "t")) {
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
        theme: ThemeInfo,
        layoutInfo: SlideLayoutInfo?,
        notes: String? = null
    ): PptxSlide {
        val visualElements = mutableListOf<VisualElement>()
        val plainTextParagraphs = mutableListOf<String>()
        var slideBgColor: Color? = layoutInfo?.backgroundFill

        // Group transform stack for nested <p:grpSp>
        val groupTransformStack = ArrayDeque<GroupTransform>()

        try {
            val parser = createPullParser(stream)

            var currentTag = ""
            var inShape = false
            var inSpPr = false
            var inLn = false
            var inTxBody = false
            var inPic = false
            var inGraphicFrame = false
            var inTable = false
            var inBg = false
            var inPPr = false
            var inDefRPr = false
            var inRPr = false
            var shapeCounter = 0

            var curOffX = 0L; var curOffY = 0L; var curExtX = 0L; var curExtY = 0L
            var curFillColor: Color? = null
            var curBorderColor: Color? = null
            var curBorderWidth = 0f
            var curIsRounded = false
            var curIsEllipse = false
            var curShapePreset: String? = null
            var curPlaceholderType: String? = null
            var curPlaceholderIdx: String? = null
            var curBlipEmbed: String? = null

            // Color transform attributes
            var curLumMod = 1.0f
            var curLumOff = 0.0f
            var curTint = 0.0f
            var curShade = 0.0f

            // Paragraph styling defaults
            var paraDefSize: Float? = null
            var paraDefBold = false
            var paraDefItalic = false
            var paraDefUnderline = false
            var paraDefStrike = false
            var paraDefFont: String? = null
            var paraDefColor: Color? = null
            var curBulletChar: String? = null
            var curIndentLvl = 0
            var curParaAlign = TextAlign.Start

            val curParagraphs = mutableListOf<SlideParagraph>()
            val curRuns = mutableListOf<SlideTextRun>()
            val currentRunText = StringBuilder()
            var curRunSize = 14f
            var curRunBold = false
            var curRunItalic = false
            var curRunUnderline = false
            var curRunStrike = false
            var curRunFont: String? = null
            var curRunColor: Color? = null

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
                            isTag(tagName, "bg") || isTag(tagName, "bgPr") -> inBg = true

                            // Group Shape start
                            isTag(tagName, "grpSp") -> {
                                groupTransformStack.push(
                                    GroupTransform(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L)
                                )
                            }

                            isTag(tagName, "sp") -> {
                                inShape = true
                                inSpPr = false; inLn = false; inTxBody = false; inRPr = false; inDefRPr = false
                                curOffX = 0L; curOffY = 0L; curExtX = 0L; curExtY = 0L
                                curFillColor = null; curBorderColor = null; curBorderWidth = 0f
                                curIsRounded = false; curIsEllipse = false
                                curShapePreset = null
                                curPlaceholderType = null
                                curPlaceholderIdx = null
                                curParagraphs.clear()
                            }

                            isTag(tagName, "spPr") -> inSpPr = true

                            isTag(tagName, "ln") -> {
                                inLn = true
                                val w = parser.getAttributeValue(null, "w")?.toFloatOrNull()
                                if (w != null) {
                                    curBorderWidth = (w / 12700f).coerceIn(0.5f, 10f)
                                }
                            }

                            isTag(tagName, "txBody") -> inTxBody = true

                            isTag(tagName, "noFill") -> {
                                if (inLn) curBorderColor = null
                                else if (inSpPr) curFillColor = Color.Transparent
                            }

                            isTag(tagName, "pic") -> {
                                inPic = true
                                curOffX = 0L; curOffY = 0L; curExtX = 0L; curExtY = 0L
                                curBlipEmbed = null
                            }

                            isTag(tagName, "graphicFrame") -> {
                                inGraphicFrame = true
                                curOffX = 0L; curOffY = 0L; curExtX = 0L; curExtY = 0L
                                curTableRows.clear()
                            }

                            isTag(tagName, "tbl") -> {
                                inTable = true
                                curTableRows.clear()
                            }

                            isTag(tagName, "tr") -> curTableRow = mutableListOf()

                            isTag(tagName, "tc") -> curCellText.clear()

                            isTag(tagName, "ph") -> {
                                val phType = parser.getAttributeValue(null, "type")
                                val phIdx = parser.getAttributeValue(null, "idx")
                                curPlaceholderType = phType?.lowercase()
                                curPlaceholderIdx = phIdx
                            }

                            // Coordinate transforms for shapes & groups
                            isTag(tagName, "off") -> {
                                val x = parser.getAttributeValue(null, "x")?.toLongOrNull() ?: 0L
                                val y = parser.getAttributeValue(null, "y")?.toLongOrNull() ?: 0L
                                curOffX = x; curOffY = y
                            }

                            isTag(tagName, "ext") -> {
                                val cx = parser.getAttributeValue(null, "cx")?.toLongOrNull() ?: 0L
                                val cy = parser.getAttributeValue(null, "cy")?.toLongOrNull() ?: 0L
                                if (cx > 0L && cy > 0L) {
                                    curExtX = cx; curExtY = cy
                                }
                            }

                            isTag(tagName, "chOff") -> {
                                if (!groupTransformStack.isEmpty()) {
                                    val top = groupTransformStack.pop()
                                    val cx = parser.getAttributeValue(null, "x")?.toLongOrNull() ?: 0L
                                    val cy = parser.getAttributeValue(null, "y")?.toLongOrNull() ?: 0L
                                    groupTransformStack.push(top.copy(chOffX = cx, chOffY = cy))
                                }
                            }

                            isTag(tagName, "chExt") -> {
                                if (!groupTransformStack.isEmpty()) {
                                    val top = groupTransformStack.pop()
                                    val cx = parser.getAttributeValue(null, "cx")?.toLongOrNull() ?: 0L
                                    val cy = parser.getAttributeValue(null, "cy")?.toLongOrNull() ?: 0L
                                    groupTransformStack.push(top.copy(chExtX = cx, chExtY = cy))
                                }
                            }

                            isTag(tagName, "prstGeom") -> {
                                val prst = parser.getAttributeValue(null, "prst") ?: ""
                                curShapePreset = prst
                                if (prst.contains("round", ignoreCase = true)) {
                                    curIsRounded = true
                                } else if (prst == "ellipse" || prst == "circle") {
                                    curIsEllipse = true
                                }
                            }

                            // Color modifiers
                            isTag(tagName, "lumMod") -> {
                                curLumMod = (parser.getAttributeValue(null, "val")?.toFloatOrNull() ?: 100000f) / 100000f
                            }
                            isTag(tagName, "lumOff") -> {
                                curLumOff = (parser.getAttributeValue(null, "val")?.toFloatOrNull() ?: 0f) / 100000f
                            }
                            isTag(tagName, "tint") -> {
                                curTint = (parser.getAttributeValue(null, "val")?.toFloatOrNull() ?: 0f) / 100000f
                            }
                            isTag(tagName, "shade") -> {
                                curShade = (parser.getAttributeValue(null, "val")?.toFloatOrNull() ?: 0f) / 100000f
                            }

                            isTag(tagName, "srgbClr") -> {
                                val hex = parser.getAttributeValue(null, "val")
                                val baseColor = parseHexColor(hex)
                                val color = baseColor?.let { applyColorTransforms(it, curLumMod, curLumOff, curTint, curShade) }
                                if (color != null) {
                                    when {
                                        inLn -> curBorderColor = color
                                        inRPr -> curRunColor = color
                                        inDefRPr -> paraDefColor = color
                                        inSpPr -> if (curFillColor == null) curFillColor = color
                                        inBg -> slideBgColor = color
                                        inTxBody -> if (curRunColor == null) curRunColor = color
                                        else -> if (inShape && curFillColor == null) curFillColor = color
                                    }
                                }
                                curLumMod = 1f; curLumOff = 0f; curTint = 0f; curShade = 0f
                            }

                            isTag(tagName, "schemeClr") -> {
                                val scheme = parser.getAttributeValue(null, "val") ?: ""
                                val baseColor = resolveDrawingSchemeColor(scheme, theme.colorScheme)
                                val color = baseColor?.let { applyColorTransforms(it, curLumMod, curLumOff, curTint, curShade) }
                                if (color != null) {
                                    when {
                                        inLn -> curBorderColor = color
                                        inRPr -> curRunColor = color
                                        inDefRPr -> paraDefColor = color
                                        inSpPr -> if (curFillColor == null) curFillColor = color
                                        inBg -> slideBgColor = color
                                        inTxBody -> if (curRunColor == null) curRunColor = color
                                        else -> if (inShape && curFillColor == null) curFillColor = color
                                    }
                                }
                                curLumMod = 1f; curLumOff = 0f; curTint = 0f; curShade = 0f
                            }

                            isTag(tagName, "blip") -> {
                                var embed: String? = null
                                for (i in 0 until parser.attributeCount) {
                                    if (parser.getAttributeName(i).contains("embed", ignoreCase = true)) {
                                        embed = parser.getAttributeValue(i)
                                        break
                                    }
                                }
                                if (embed != null) curBlipEmbed = embed
                            }

                            isTag(tagName, "p") -> {
                                curRuns.clear()
                                currentRunText.clear()
                                curParaAlign = TextAlign.Start
                                inPPr = false
                                inDefRPr = false
                                paraDefSize = null
                                paraDefBold = false
                                paraDefItalic = false
                                paraDefUnderline = false
                                paraDefStrike = false
                                paraDefFont = null
                                paraDefColor = null
                                curBulletChar = null
                                curIndentLvl = 0
                                curRunSize = when (curPlaceholderType) {
                                    "title", "ctrtitle" -> 28f
                                    "subtitle" -> 20f
                                    "body", "obj" -> 15f
                                    "dt", "ftr", "sldnum" -> 10f
                                    else -> 14f
                                }
                                curRunBold = curPlaceholderType in listOf("title", "ctrtitle")
                                curRunItalic = false
                                curRunUnderline = false
                                curRunStrike = false
                                curRunFont = if (curPlaceholderType in listOf("title", "ctrtitle")) theme.majorFont else theme.minorFont
                                curRunColor = null
                            }

                            isTag(tagName, "pPr") -> {
                                inPPr = true
                                val algn = parser.getAttributeValue(null, "algn")
                                curParaAlign = when (algn) {
                                    "ctr" -> TextAlign.Center
                                    "r" -> TextAlign.End
                                    "just" -> TextAlign.Justify
                                    else -> TextAlign.Start
                                }
                                val lvl = parser.getAttributeValue(null, "lvl")?.toIntOrNull()
                                if (lvl != null) curIndentLvl = lvl
                            }

                            isTag(tagName, "buChar") -> {
                                curBulletChar = parser.getAttributeValue(null, "char") ?: "•"
                            }

                            isTag(tagName, "buAutoNum") -> {
                                curBulletChar = "1."
                            }

                            isTag(tagName, "buNone") -> {
                                curBulletChar = null
                            }

                            isTag(tagName, "defRPr") -> {
                                inDefRPr = true
                                val sz = parser.getAttributeValue(null, "sz")?.toFloatOrNull()
                                if (sz != null) {
                                    paraDefSize = (sz / 100f).coerceIn(8f, 72f)
                                }
                                if (parser.getAttributeValue(null, "b") == "1") paraDefBold = true
                                if (parser.getAttributeValue(null, "i") == "1") paraDefItalic = true
                                val u = parser.getAttributeValue(null, "u")
                                if (u != null && u != "none") paraDefUnderline = true
                                val strike = parser.getAttributeValue(null, "strike")
                                if (strike != null && strike != "noStrike") paraDefStrike = true
                            }

                            isTag(tagName, "r") || isTag(tagName, "fld") -> {
                                currentRunText.clear()
                                val baseSize = paraDefSize ?: when (curPlaceholderType) {
                                    "title", "ctrtitle" -> 28f
                                    "subtitle" -> 20f
                                    "body", "obj" -> 15f
                                    "dt", "ftr", "sldnum" -> 10f
                                    else -> 14f
                                }
                                curRunSize = baseSize
                                curRunBold = paraDefBold || (curPlaceholderType in listOf("title", "ctrtitle"))
                                curRunItalic = paraDefItalic
                                curRunUnderline = paraDefUnderline
                                curRunStrike = paraDefStrike
                                curRunFont = paraDefFont ?: if (curPlaceholderType in listOf("title", "ctrtitle")) theme.majorFont else theme.minorFont
                                curRunColor = paraDefColor
                            }

                            isTag(tagName, "rPr") -> {
                                inRPr = true
                                val sz = parser.getAttributeValue(null, "sz")?.toFloatOrNull()
                                if (sz != null) {
                                    curRunSize = (sz / 100f).coerceIn(8f, 72f)
                                }
                                val b = parser.getAttributeValue(null, "b")
                                if (b != null) curRunBold = b == "1"
                                val i = parser.getAttributeValue(null, "i")
                                if (i != null) curRunItalic = i == "1"
                                val u = parser.getAttributeValue(null, "u")
                                if (u != null) curRunUnderline = u != "none"
                                val strike = parser.getAttributeValue(null, "strike")
                                if (strike != null) curRunStrike = strike != "noStrike"
                            }

                            isTag(tagName, "latin") -> {
                                val tf = parser.getAttributeValue(null, "typeface")
                                val resolved = when {
                                    tf?.startsWith("+mj") == true -> theme.majorFont
                                    tf?.startsWith("+mn") == true -> theme.minorFont
                                    else -> tf
                                }
                                if (inDefRPr) {
                                    paraDefFont = resolved
                                } else {
                                    curRunFont = resolved
                                }
                            }

                            isTag(tagName, "br") -> {
                                currentRunText.append("\n")
                            }
                        }
                    }

                    XmlPullParser.TEXT -> {
                        if (isTag(currentTag, "t")) {
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
                            isTag(tagName, "bg") || isTag(tagName, "bgPr") -> inBg = false
                            isTag(tagName, "spPr") -> inSpPr = false
                            isTag(tagName, "ln") -> inLn = false
                            isTag(tagName, "txBody") -> inTxBody = false
                            isTag(tagName, "rPr") -> inRPr = false
                            isTag(tagName, "pPr") -> inPPr = false
                            isTag(tagName, "defRPr") -> inDefRPr = false

                            // Group Shape end
                            isTag(tagName, "grpSp") -> {
                                if (!groupTransformStack.isEmpty()) groupTransformStack.pop()
                            }

                            isTag(tagName, "r") || isTag(tagName, "fld") -> {
                                val text = currentRunText.toString()
                                if (text.isNotEmpty()) {
                                    curRuns.add(
                                        SlideTextRun(
                                            text = text,
                                            fontSizeSp = curRunSize,
                                            isBold = curRunBold,
                                            isItalic = curRunItalic,
                                            isUnderline = curRunUnderline,
                                            isStrikeThrough = curRunStrike,
                                            fontFamily = curRunFont,
                                            color = curRunColor
                                        )
                                    )
                                    currentRunText.clear()
                                }
                            }

                            isTag(tagName, "p") -> {
                                if (currentRunText.isNotEmpty()) {
                                    curRuns.add(
                                        SlideTextRun(
                                            text = currentRunText.toString(),
                                            fontSizeSp = curRunSize,
                                            isBold = curRunBold,
                                            isItalic = curRunItalic,
                                            isUnderline = curRunUnderline,
                                            isStrikeThrough = curRunStrike,
                                            fontFamily = curRunFont,
                                            color = curRunColor
                                        )
                                    )
                                    currentRunText.clear()
                                }
                                if (curRuns.isNotEmpty()) {
                                    val isBullet = curBulletChar != null || curIndentLvl > 0 || (curPlaceholderType in listOf("body", "obj"))
                                    val para = SlideParagraph(
                                        runs = curRuns.toList(),
                                        alignment = curParaAlign,
                                        isBullet = isBullet,
                                        bulletChar = curBulletChar,
                                        indentLevel = curIndentLvl
                                    )
                                    curParagraphs.add(para)
                                    val plain = para.plainText.trim()
                                    if (plain.isNotEmpty()) {
                                        plainTextParagraphs.add(plain)
                                    }
                                }
                            }

                            isTag(tagName, "tc") -> {
                                curTableRow.add(curCellText.toString().trim())
                            }

                            isTag(tagName, "tr") -> {
                                if (curTableRow.isNotEmpty()) {
                                    curTableRows.add(curTableRow.toList().toMutableList())
                                }
                            }

                            isTag(tagName, "tbl") -> {
                                inTable = false
                                if (curTableRows.isNotEmpty()) {
                                    val bounds = resolveElementBounds(
                                        curOffX, curOffY, curExtX, curExtY,
                                        slideWidth, slideHeight,
                                        curPlaceholderType, curPlaceholderIdx,
                                        layoutInfo, groupTransformStack, shapeCounter++
                                    )
                                    visualElements.add(
                                        VisualElement(
                                            type = VisualElementType.TABLE,
                                            bounds = bounds,
                                            tableRows = curTableRows.map { it.toList() }
                                        )
                                    )
                                }
                            }

                            isTag(tagName, "graphicFrame") -> {
                                inGraphicFrame = false
                            }

                            isTag(tagName, "sp") -> {
                                inShape = false
                                inSpPr = false; inLn = false; inTxBody = false; inRPr = false; inDefRPr = false

                                if (curParagraphs.isNotEmpty() || curFillColor != null || curBorderColor != null || curShapePreset != null || curBlipEmbed != null) {
                                    val bounds = resolveElementBounds(
                                        curOffX, curOffY, curExtX, curExtY,
                                        slideWidth, slideHeight,
                                        curPlaceholderType, curPlaceholderIdx,
                                        layoutInfo, groupTransformStack, shapeCounter++
                                    )

                                    val embedId = curBlipEmbed
                                    val shapeImageBytes = if (embedId != null) {
                                        val targetPath = relsMap[embedId]
                                        if (targetPath != null) {
                                            mediaMap[targetPath]
                                                ?: mediaMap[targetPath.substringAfterLast('/')]
                                                ?: mediaMap["ppt/" + targetPath.removePrefix("../")]
                                        } else null
                                    } else null

                                    if (shapeImageBytes != null && shapeImageBytes.isNotEmpty()) {
                                        visualElements.add(
                                            VisualElement(
                                                type = VisualElementType.IMAGE,
                                                bounds = bounds,
                                                imageBytes = shapeImageBytes
                                            )
                                        )
                                    }

                                    if (curParagraphs.isNotEmpty() || (curFillColor != null && shapeImageBytes == null) || curBorderColor != null || (curShapePreset != null && shapeImageBytes == null)) {
                                        // Inherit layout styling if slide shape did not specify explicit colors
                                        val layoutPh = findPlaceholder(layoutInfo, curPlaceholderType, curPlaceholderIdx)
                                        val effectiveFill = curFillColor ?: layoutPh?.fillColor
                                        val effectiveBorder = curBorderColor ?: layoutPh?.borderColor
                                        val effectiveBorderWidth = if (curBorderWidth > 0f) curBorderWidth else (layoutPh?.borderWidthDp ?: 0f)
                                        val effectivePreset = curShapePreset ?: layoutPh?.preset

                                        val type = if (effectiveFill != null || effectiveBorder != null || effectivePreset != null) {
                                            VisualElementType.SHAPE
                                        } else {
                                            VisualElementType.TEXT_BOX
                                        }

                                        visualElements.add(
                                            VisualElement(
                                                type = type,
                                                bounds = bounds,
                                                backgroundColor = if (shapeImageBytes != null) null else effectiveFill,
                                                borderColor = effectiveBorder,
                                                borderWidthDp = effectiveBorderWidth,
                                                isRounded = curIsRounded || effectivePreset?.contains("round", ignoreCase = true) == true,
                                                isEllipse = curIsEllipse || effectivePreset == "ellipse" || effectivePreset == "circle",
                                                shapePreset = effectivePreset,
                                                paragraphs = curParagraphs.toList()
                                            )
                                        )
                                    }
                                }
                            }

                            isTag(tagName, "pic") -> {
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
                                        val bounds = resolveElementBounds(
                                            curOffX, curOffY, curExtX, curExtY,
                                            slideWidth, slideHeight,
                                            curPlaceholderType, curPlaceholderIdx,
                                            layoutInfo, groupTransformStack, shapeCounter++
                                        )
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
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val title = plainTextParagraphs.firstOrNull() ?: "Slide $slideNum"
        val content = if (plainTextParagraphs.size > 1) plainTextParagraphs.drop(1) else emptyList()

        val finalVisualElements = if (visualElements.isEmpty() && plainTextParagraphs.isNotEmpty()) {
            val autoVisuals = mutableListOf<VisualElement>()
            autoVisuals.add(
                VisualElement(
                    type = VisualElementType.TEXT_BOX,
                    bounds = SlideBounds(0.06f, 0.08f, 0.88f, 0.18f),
                    paragraphs = listOf(
                        SlideParagraph(
                            runs = listOf(SlideTextRun(text = title, fontSizeSp = 24f, isBold = true))
                        )
                    )
                )
            )
            if (content.isNotEmpty()) {
                autoVisuals.add(
                    VisualElement(
                        type = VisualElementType.TEXT_BOX,
                        bounds = SlideBounds(0.06f, 0.28f, 0.88f, 0.64f),
                        paragraphs = content.map { line ->
                            SlideParagraph(
                                runs = listOf(SlideTextRun(text = line, fontSizeSp = 15f)),
                                isBullet = true
                            )
                        }
                    )
                )
            }
            autoVisuals
        } else {
            visualElements
        }

        return PptxSlide(
            slideNumber = slideNum,
            title = title,
            content = content,
            visualElements = finalVisualElements,
            backgroundColor = slideBgColor,
            aspectRatio = aspectRatio,
            notes = notes
        )
    }

    private fun findPlaceholder(
        layoutInfo: SlideLayoutInfo?,
        type: String?,
        idx: String?
    ): PlaceholderLayout? {
        if (layoutInfo == null) return null
        if (type != null && idx != null) {
            layoutInfo.placeholders["$type:$idx"]?.let { return it }
        }
        if (type != null) {
            layoutInfo.placeholders[type]?.let { return it }
        }
        if (idx != null) {
            layoutInfo.placeholders[idx]?.let { return it }
        }
        return null
    }

    private fun resolveElementBounds(
        offX: Long,
        offY: Long,
        extX: Long,
        extY: Long,
        slideWidth: Long,
        slideHeight: Long,
        phType: String?,
        phIdx: String?,
        layoutInfo: SlideLayoutInfo?,
        groupTransformStack: ArrayDeque<GroupTransform>,
        shapeIndex: Int
    ): SlideBounds {
        // 1. If explicit coordinates are present on the shape:
        if (extX > 0L && extY > 0L) {
            var curX = offX
            var curY = offY
            var curW = extX
            var curH = extY

            // Apply group transforms from top of stack downward
            for (t in groupTransformStack) {
                if (t.chExtX > 0L && t.chExtY > 0L) {
                    val scaleX = t.extX.toDouble() / t.chExtX.toDouble()
                    val scaleY = t.extY.toDouble() / t.chExtY.toDouble()
                    curX = t.offX + ((curX - t.chOffX) * scaleX).toLong()
                    curY = t.offY + ((curY - t.chOffY) * scaleY).toLong()
                    curW = (curW * scaleX).toLong()
                    curH = (curH * scaleY).toLong()
                }
            }

            return calculateRawBounds(curX, curY, curW, curH, slideWidth, slideHeight)
        }

        // 2. Check layout placeholder inheritance (resolves comparison 2-column slides, titles, subtitles)
        val inheritedPh = findPlaceholder(layoutInfo, phType, phIdx)
        if (inheritedPh?.bounds != null) {
            return inheritedPh.bounds
        }

        // 3. Fallback coordinates based on semantic placeholder role
        return when (phType) {
            "title", "ctrtitle" -> SlideBounds(left = 0.06f, top = 0.08f, width = 0.88f, height = 0.18f)
            "subtitle" -> SlideBounds(left = 0.08f, top = 0.28f, width = 0.84f, height = 0.18f)
            "body", "obj" -> {
                if (phIdx == "2" || phIdx == "3") {
                    // Right column on a two-content slide
                    SlideBounds(left = 0.52f, top = 0.30f, width = 0.42f, height = 0.60f)
                } else if (phIdx == "1") {
                    // Left column on a two-content slide
                    SlideBounds(left = 0.06f, top = 0.30f, width = 0.42f, height = 0.60f)
                } else {
                    SlideBounds(left = 0.06f, top = 0.30f, width = 0.88f, height = 0.60f)
                }
            }
            "dt" -> SlideBounds(left = 0.06f, top = 0.92f, width = 0.25f, height = 0.05f)
            "ftr" -> SlideBounds(left = 0.35f, top = 0.92f, width = 0.30f, height = 0.05f)
            "sldnum" -> SlideBounds(left = 0.80f, top = 0.92f, width = 0.14f, height = 0.05f)
            else -> {
                // Arrange unpositioned shapes in a safe vertical cascade without stacking
                val topOffset = (0.08f + (shapeIndex * 0.18f)).coerceIn(0.08f, 0.74f)
                SlideBounds(left = 0.06f, top = topOffset, width = 0.88f, height = 0.16f)
            }
        }
    }

    private fun calculateRawBounds(
        offX: Long,
        offY: Long,
        extX: Long,
        extY: Long,
        slideWidth: Long,
        slideHeight: Long
    ): SlideBounds {
        val w = if (slideWidth > 0) slideWidth.toFloat() else 9144000f
        val h = if (slideHeight > 0) slideHeight.toFloat() else 5143500f
        val left = (offX.toFloat() / w).coerceIn(0f, 0.99f)
        val top = (offY.toFloat() / h).coerceIn(0f, 0.99f)
        val maxW = (1f - left).coerceAtLeast(0.01f)
        val maxH = (1f - top).coerceAtLeast(0.01f)
        val width = (extX.toFloat() / w).coerceIn(0.001f, maxW)
        val height = (extY.toFloat() / h).coerceIn(0.001f, maxH)
        return SlideBounds(left = left, top = top, width = width, height = height)
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

    private fun resolveDrawingSchemeColor(scheme: String, themeColors: Map<String, Color>): Color? {
        val key = scheme.lowercase().trim()
        val mappedKey = when (key) {
            "tx1" -> "dk1"
            "tx2" -> "dk2"
            "bg1" -> "lt1"
            "bg2" -> "lt2"
            else -> key
        }
        return themeColors[mappedKey] ?: themeColors[key]
    }

    private fun applyColorTransforms(
        baseColor: Color,
        lumMod: Float,
        lumOff: Float,
        tint: Float,
        shade: Float
    ): Color {
        var r = baseColor.red
        var g = baseColor.green
        var b = baseColor.blue

        if (tint > 0f) {
            r += (1f - r) * tint
            g += (1f - g) * tint
            b += (1f - b) * tint
        }

        if (shade > 0f) {
            r *= (1f - shade)
            g *= (1f - shade)
            b *= (1f - shade)
        }

        if (lumMod != 1.0f || lumOff != 0.0f) {
            r = (r * lumMod + lumOff).coerceIn(0f, 1f)
            g = (g * lumMod + lumOff).coerceIn(0f, 1f)
            b = (b * lumMod + lumOff).coerceIn(0f, 1f)
        }

        return Color(red = r.coerceIn(0f, 1f), green = g.coerceIn(0f, 1f), blue = b.coerceIn(0f, 1f), alpha = baseColor.alpha)
    }

    /* =========================================================================
     * SECTION 2: LEGACY BINARY POWERPOINT (.ppt / OLE2) PARSER
     * ========================================================================= */

    /**
     * Pure Kotlin Compound File Binary Format (CFBF / OLE2) stream extractor and
     * PowerPoint Document record parser for legacy .ppt files.
     */
    private fun parseBinaryPpt(bytes: ByteArray): PptxPresentation {
        try {
            val ole2 = Ole2File(bytes)
            val pptDocStream = ole2.getStream("PowerPoint Document")
            val picturesStream = ole2.getStream("Pictures")

            if (pptDocStream != null && pptDocStream.isNotEmpty()) {
                val pres = parsePptDocStream(pptDocStream, picturesStream)
                if (pres.slides.isNotEmpty()) return pres
            }
        } catch (_: Exception) {}

        // Fallback: Resilient raw atom scanner over bytes
        return parseBinaryPptFallback(bytes)
    }

    /**
     * Parses the assembled PowerPoint Document stream directly.
     */
    private fun parsePptDocStream(docBytes: ByteArray, picturesBytes: ByteArray?): PptxPresentation {
        val slides = mutableListOf<PptxSlide>()
        var slideCounter = 1

        // Extract embedded pictures from Pictures stream (JPEG & PNG signatures)
        val extractedPictures = mutableListOf<ByteArray>()
        if (picturesBytes != null && picturesBytes.size > 32) {
            extractPicturesFromStream(picturesBytes, extractedPictures)
        }

        var i = 0
        val total = docBytes.size
        var currentTitle = ""
        val currentContent = mutableListOf<String>()

        fun flushSlide() {
            if (currentTitle.isNotBlank() || currentContent.isNotEmpty()) {
                val t = if (currentTitle.isNotBlank()) currentTitle else "Slide $slideCounter"
                val visuals = mutableListOf<VisualElement>()

                // Title shape
                visuals.add(
                    VisualElement(
                        type = VisualElementType.TEXT_BOX,
                        bounds = SlideBounds(0.06f, 0.08f, 0.88f, 0.18f),
                        paragraphs = listOf(
                            SlideParagraph(
                                runs = listOf(SlideTextRun(text = t, fontSizeSp = 24f, isBold = true))
                            )
                        )
                    )
                )

                // Associate extracted picture if available
                val picIndex = slideCounter - 1
                val hasPic = picIndex < extractedPictures.size
                val bodyWidth = if (hasPic) 0.46f else 0.88f

                // Body content / bullet points
                if (currentContent.isNotEmpty()) {
                    visuals.add(
                        VisualElement(
                            type = VisualElementType.TEXT_BOX,
                            bounds = SlideBounds(0.06f, 0.28f, bodyWidth, 0.64f),
                            paragraphs = currentContent.map { line ->
                                SlideParagraph(
                                    runs = listOf(SlideTextRun(text = line, fontSizeSp = 15f)),
                                    isBullet = true
                                )
                            }
                        )
                    )
                }

                if (hasPic) {
                    val picBytes = extractedPictures[picIndex]
                    visuals.add(
                        VisualElement(
                            type = VisualElementType.IMAGE,
                            bounds = SlideBounds(0.54f, 0.28f, 0.40f, 0.60f),
                            imageBytes = picBytes
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

        while (i <= total - 8) {
            val recType = (docBytes[i + 2].toInt() and 0xFF) or ((docBytes[i + 3].toInt() and 0xFF) shl 8)
            val recLen = (docBytes[i + 4].toLong() and 0xFF) or
                ((docBytes[i + 5].toLong() and 0xFF) shl 8) or
                ((docBytes[i + 6].toLong() and 0xFF) shl 16) or
                ((docBytes[i + 7].toLong() and 0xFF) shl 24)

            if (recLen < 0 || recLen > 2_000_000 || i + 8 + recLen > total) {
                i += 2
                continue
            }

            when (recType) {
                0x03EE, 0x03F3 -> { // RT_Slide container or RT_SlidePersistAtom
                    flushSlide()
                }

                0x0F9F -> { // RT_TextBytesAtom (Latin-1 / ANSI)
                    if (recLen > 0) {
                        val text = String(docBytes, i + 8, recLen.toInt(), Charsets.ISO_8859_1).trim()
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
                        val text = String(docBytes, i + 8, recLen.toInt(), Charsets.UTF_16LE).trim()
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

        return if (slides.isNotEmpty()) {
            PptxPresentation(slides = slides, aspectRatio = 4f / 3f)
        } else {
            parseBinaryPptFallback(docBytes)
        }
    }

    private fun extractPicturesFromStream(bytes: ByteArray, output: MutableList<ByteArray>) {
        var idx = 0
        val len = bytes.size
        while (idx < len - 4) {
            // Check JPEG SOI (FF D8 FF)
            if (bytes[idx] == 0xFF.toByte() && bytes[idx + 1] == 0xD8.toByte() && bytes[idx + 2] == 0xFF.toByte()) {
                var end = idx + 3
                while (end < len - 1) {
                    if (bytes[end] == 0xFF.toByte() && bytes[end + 1] == 0xD9.toByte()) { // EOI
                        val img = bytes.copyOfRange(idx, end + 2)
                        if (img.size in 1024..5_000_000) {
                            output.add(img)
                        }
                        idx = end + 2
                        break
                    }
                    end++
                }
            }
            // Check PNG magic (89 50 4E 47)
            else if (bytes[idx] == 0x89.toByte() && bytes[idx + 1] == 0x50.toByte() && bytes[idx + 2] == 0x4E.toByte() && bytes[idx + 3] == 0x47.toByte()) {
                var end = idx + 4
                while (end < len - 8) {
                    // Search for IEND chunk (49 45 4E 44)
                    if (bytes[end] == 0x49.toByte() && bytes[end + 1] == 0x45.toByte() && bytes[end + 2] == 0x4E.toByte() && bytes[end + 3] == 0x44.toByte()) {
                        val img = bytes.copyOfRange(idx, end + 8)
                        if (img.size in 1024..5_000_000) {
                            output.add(img)
                        }
                        idx = end + 8
                        break
                    }
                    end++
                }
            }
            idx++
        }
    }

    private fun parseBinaryPptFallback(bytes: ByteArray): PptxPresentation {
        val extracted = extractReadableStrings(bytes)
        if (extracted.isEmpty()) {
            return emptyPresentation()
        }

        val slides = mutableListOf<PptxSlide>()
        val chunks = extracted.chunked(6)
        for ((idx, chunk) in chunks.withIndex()) {
            val t = chunk.first()
            val c = if (chunk.size > 1) chunk.drop(1) else emptyList()
            val visuals = mutableListOf<VisualElement>()

            visuals.add(
                VisualElement(
                    type = VisualElementType.TEXT_BOX,
                    bounds = SlideBounds(0.06f, 0.08f, 0.88f, 0.18f),
                    paragraphs = listOf(
                        SlideParagraph(
                            runs = listOf(SlideTextRun(text = t, fontSizeSp = 22f, isBold = true))
                        )
                    )
                )
            )

            if (c.isNotEmpty()) {
                visuals.add(
                    VisualElement(
                        type = VisualElementType.TEXT_BOX,
                        bounds = SlideBounds(0.06f, 0.30f, 0.88f, 0.60f),
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

        return PptxPresentation(slides = slides, aspectRatio = 4f / 3f)
    }

    private fun extractReadableStrings(bytes: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        val seen = mutableSetOf<String>()

        fun isMetadataOrGarbage(s: String): Boolean {
            if (s.length < 3 || !s.any { it.isLetter() }) return true
            val lower = s.lowercase()
            return lower.contains("root entry") ||
                lower.contains("current user") ||
                lower.contains("powerpoint document") ||
                lower.contains("summaryinformation") ||
                lower.contains("compobj") ||
                lower.startsWith("<?xml") ||
                lower.startsWith("[content_") ||
                lower.contains("schemas.openxmlformats") ||
                lower.contains("vnd.openxmlformats") ||
                lower.contains("microsoft") ||
                lower.contains("font name")
        }

        fun addCandidate(text: String) {
            val lines = text.split("\r\n", "\r", "\n")
            for (rawLine in lines) {
                val trimmed = rawLine.trim()
                if (!isMetadataOrGarbage(trimmed) && seen.add(trimmed.lowercase())) {
                    strings.add(trimmed)
                }
            }
        }

        // 1. Scan for UTF-16LE character sequences (predominant in binary .ppt files)
        var i = 0
        val sbUtf16 = StringBuilder()
        while (i < bytes.size - 1) {
            val b0 = bytes[i].toInt() and 0xFF
            val b1 = bytes[i + 1].toInt() and 0xFF
            val ch = (b1 shl 8) or b0
            if (b1 == 0 && (b0 in 32..126 || b0 == 10 || b0 == 13)) {
                sbUtf16.append(b0.toChar())
                i += 2
            } else if (ch in 0x00A0..0x024F || ch in 0x0400..0x04FF || ch in 0x0600..0x06FF || ch in 0x0900..0x097F) {
                sbUtf16.append(ch.toChar())
                i += 2
            } else {
                if (sbUtf16.length >= 3) {
                    addCandidate(sbUtf16.toString())
                }
                sbUtf16.clear()
                i += 2
            }
        }
        if (sbUtf16.length >= 3) {
            addCandidate(sbUtf16.toString())
        }

        // 2. Scan for 1-byte ASCII / Latin-1 sequences
        val sbAscii = StringBuilder()
        for (b in bytes) {
            val c = b.toInt().toChar()
            if (c in ' '..'~' || c == '\n') {
                sbAscii.append(c)
            } else {
                if (sbAscii.length >= 4) {
                    addCandidate(sbAscii.toString())
                }
                sbAscii.clear()
            }
        }
        if (sbAscii.length >= 4) {
            addCandidate(sbAscii.toString())
        }

        return strings.take(80)
    }

    /**
     * Pure Kotlin Compound File Binary Format (CFBF / OLE2) parser.
     */
    private class Ole2File(private val data: ByteArray) {
        val sectorSize: Int
        private val fat: IntArray
        private val dirEntries = mutableListOf<DirEntry>()

        data class DirEntry(
            val name: String,
            val type: Int,
            val startSector: Int,
            val size: Long
        )

        init {
            require(data.size >= 512) { "File too small for OLE2" }
            val sectorShift = getShortLE(data, 30).toInt()
            sectorSize = 1 shl (if (sectorShift in 9..12) sectorShift else 9)

            val numFatSectors = getIntLE(data, 44)
            val firstDirSector = getIntLE(data, 48)

            // Read first 109 DIFAT entries
            val fatSectorLocations = mutableListOf<Int>()
            for (i in 0 until 109) {
                val off = 76 + (i * 4)
                if (off + 4 <= 512) {
                    val sec = getIntLE(data, off)
                    if (sec >= 0) fatSectorLocations.add(sec)
                }
            }

            // Assemble FAT table
            val fatIntCount = numFatSectors * (sectorSize / 4)
            fat = IntArray(maxOf(fatIntCount, 1024))
            var fatIndex = 0
            for (fatSec in fatSectorLocations) {
                val secOffset = (fatSec + 1) * sectorSize
                if (secOffset + sectorSize <= data.size) {
                    for (j in 0 until (sectorSize / 4)) {
                        if (fatIndex < fat.size) {
                            fat[fatIndex++] = getIntLE(data, secOffset + (j * 4))
                        }
                    }
                }
            }

            // Read Directory Entries
            val dirBytes = readSectorChain(firstDirSector, 100_000L)
            var dirOffset = 0
            while (dirOffset + 128 <= dirBytes.size) {
                val nameLen = getShortLE(dirBytes, dirOffset + 64).toInt()
                val type = dirBytes[dirOffset + 66].toInt() and 0xFF
                val startSec = getIntLE(dirBytes, dirOffset + 116)
                val streamSize = getLongLE(dirBytes, dirOffset + 120)

                val name = if (nameLen > 2) {
                    String(dirBytes, dirOffset, minOf(nameLen - 2, 64), Charsets.UTF_16LE).trim()
                } else ""

                if (name.isNotEmpty()) {
                    dirEntries.add(DirEntry(name, type, startSec, streamSize))
                }
                dirOffset += 128
            }
        }

        fun getStream(name: String): ByteArray? {
            val entry = dirEntries.find { it.name.equals(name, ignoreCase = true) } ?: return null
            if (entry.startSector < 0 || entry.size <= 0) return null
            return readSectorChain(entry.startSector, entry.size)
        }

        private fun readSectorChain(startSector: Int, maxSize: Long): ByteArray {
            val out = java.io.ByteArrayOutputStream()
            var curSector = startSector
            var remaining = maxSize

            while (curSector >= 0 && curSector < fat.size && remaining > 0) {
                val offset = (curSector + 1) * sectorSize
                if (offset >= data.size) break
                val toRead = minOf(sectorSize.toLong(), remaining).toInt()
                if (offset + toRead <= data.size) {
                    out.write(data, offset, toRead)
                    remaining -= toRead
                } else break

                val next = fat[curSector]
                if (next < 0 || next == curSector) break
                curSector = next
            }
            return out.toByteArray()
        }

        companion object {
            fun getShortLE(b: ByteArray, offset: Int): Short {
                if (offset + 1 >= b.size) return 0
                return ((b[offset].toInt() and 0xFF) or ((b[offset + 1].toInt() and 0xFF) shl 8)).toShort()
            }

            fun getIntLE(b: ByteArray, offset: Int): Int {
                if (offset + 3 >= b.size) return 0
                return (b[offset].toInt() and 0xFF) or
                    ((b[offset + 1].toInt() and 0xFF) shl 8) or
                    ((b[offset + 2].toInt() and 0xFF) shl 16) or
                    ((b[offset + 3].toInt() and 0xFF) shl 24)
            }

            fun getLongLE(b: ByteArray, offset: Int): Long {
                if (offset + 7 >= b.size) return 0L
                val low = getIntLE(b, offset).toLong() and 0xFFFFFFFFL
                val high = getIntLE(b, offset + 4).toLong() and 0xFFFFFFFFL
                return low or (high shl 32)
            }
        }
    }

    companion object {
        fun isTag(tagName: String, name: String): Boolean {
            val colonIdx = tagName.indexOf(':')
            val local = if (colonIdx >= 0) tagName.substring(colonIdx + 1) else tagName
            return local.equals(name, ignoreCase = true)
        }

        fun createPullParser(inputStream: InputStream): XmlPullParser {
            return try {
                android.util.Xml.newPullParser().apply {
                    setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                    setInput(inputStream, "UTF-8")
                }
            } catch (_: Throwable) {
                XmlPullParserFactory.newInstance().apply {
                    isNamespaceAware = false
                }.newPullParser().apply {
                    setInput(inputStream, "UTF-8")
                }
            }
        }
    }
}
