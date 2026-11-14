package com.abhisheksati.mydoc.rendering.engine.word
import com.abhisheksati.mydoc.rendering.engine.EngineXml

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import com.abhisheksati.mydoc.domain.model.DocumentLoadException
import com.abhisheksati.mydoc.domain.model.LoadFailure
import kotlinx.coroutines.ensureActive
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class DocxRun(
    val text: String,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    /** Where a `w:hyperlink` pointed. Null for ordinary text. */
    val linkTarget: String? = null
)

/**
 * A table cell. It holds blocks rather than a string because a cell can contain several paragraphs
 * and can contain another table: with a string cell, an inner `w:tbl` used to clear the outer
 * table's rows and both tables lost content.
 */
data class DocxCell(val blocks: List<DocxBlock>) {
    /** Everything the cell says, for renderers that only draw text. */
    val text: String
        get() = blocks.mapNotNull { if (it is DocxBlock.Image) "[Image] " + it.description else DocxBlock.plainTextOf(it) }.joinToString("\n").trim()
}

sealed class DocxBlock {
    data class Heading(val text: String, val level: Int = 1, val layout: DocxParagraphLayout = DocxParagraphLayout(),
        val isContinuation: Boolean = false) : DocxBlock()
    data class Paragraph(val runs: List<DocxRun>, val layout: DocxParagraphLayout = DocxParagraphLayout()) : DocxBlock() {
        val fullText: String get() = runs.joinToString("") { it.text }
    }

    /**
     * A list item at any depth. [marker] is what Word would print before the text - "bullet" becomes
     * "\u2022", a decimal level becomes "1." - and [ordered] says whether the marker came from a
     * numbering definition or from a bullet character. The old model could only express bullets, so
     * every numbered list in a .docx was rendered as one.
     */
    data class ListItem(
        val runs: List<DocxRun>,
        val marker: String = "\u2022",
        val level: Int = 0,
        val ordered: Boolean = false,
        val layout: DocxParagraphLayout = DocxParagraphLayout()
    ) : DocxBlock() {
        val fullText: String get() = runs.joinToString("") { it.text }
    }

    data object PageBreak : DocxBlock()

    class Image(
        val bytes: ByteArray?,
        val widthPoints: Float,
        val heightPoints: Float,
        val description: String,
        val floating: Boolean = false,
        val layout: DocxParagraphLayout = DocxParagraphLayout()
    ) : DocxBlock()

    data class Table(val rows: List<List<DocxCell>>) : DocxBlock()

    companion object {
        fun plainTextOf(block: DocxBlock): String? = when (block) {
            is Image, PageBreak -> null
            is Heading -> block.text
            is Paragraph -> block.fullText.takeIf { it.isNotEmpty() }
            is ListItem -> block.fullText.takeIf { it.isNotEmpty() }
            is Table -> block.rows.joinToString("\n") { row -> row.joinToString(" | ") { it.text } }
        }
    }
}

data class DocxDocument(
    val blocks: List<DocxBlock>,
    val usedTextFallback: Boolean = false
)

class DocxParser {

    private fun createPullParser(xmlBytes: ByteArray): XmlPullParser {
        return EngineXml.newPullParser(ByteArrayInputStream(xmlBytes))
    }

    private fun readBoundedInputStream(inputStream: InputStream, maxBytes: Int = 50 * 1024 * 1024): ByteArray {
        val buffer = ByteArray(16384)
        val baos = java.io.ByteArrayOutputStream(minOf(maxBytes, 64 * 1024))
        var totalRead = 0
        var n: Int
        while (inputStream.read(buffer).also { n = it } != -1) {
            totalRead += n
            if (totalRead > maxBytes) {
                throw java.io.IOException("File size exceeds safety limit of ${maxBytes / (1024 * 1024)} MB.")
            }
            baos.write(buffer, 0, n)
        }
        return baos.toByteArray()
    }

    suspend fun parse(inputStream: InputStream): DocxDocument = withContext(Dispatchers.IO) {
        val bytes = try {
            readBoundedInputStream(inputStream, 50 * 1024 * 1024)
        } catch (e: Exception) {
            throw DocumentLoadException(
                LoadFailure.EXCEEDS_LIMITS,
                "This document is larger than the 50 MB limit MyDoc reads.",
                e
            )
        }
        if (bytes.isEmpty()) {
            throw DocumentLoadException(LoadFailure.EMPTY_DOCUMENT, "This file is empty (0 bytes).")
        }
        ensureActive()
        com.abhisheksati.mydoc.rendering.engine.rejectEncryptedOfficeSource(bytes)

        val isZip = bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()
        val isOle2 = bytes.size >= 8 &&
            bytes[0] == 0xD0.toByte() && bytes[1] == 0xCF.toByte() &&
            bytes[2] == 0x11.toByte() && bytes[3] == 0xE0.toByte()
        val blocks = mutableListOf<DocxBlock>()

        if (isZip) {
            val entryMap = mutableMapOf<String, ByteArray>()
            try {
                ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    var entryCount = 0
                    var totalExtractedBytes = 0L
                    while (entry != null) {
                        entryCount++
                        if (entryCount > 10_000) throw SecurityException("Entry count exceeds limit")
                        if (!entry.isDirectory) {
                            val norm = entry.name.replace('\\', '/').trimStart('/').lowercase(java.util.Locale.ROOT)
                            // document.xml alone cannot describe a Word file: hyperlink targets live
                            // in word/_rels/document.xml.rels, list definitions in word/numbering.xml,
                            // heading levels in word/styles.xml, and footnote text in word/footnotes.xml.
                            val wanted = norm in READER_PARTS || norm.startsWith("word/media/")
                            // Drain every entry through the budget, not only the ones we keep: a skipped
                            // bomb entry left to closeEntry() would decompress unbounded off the books.
                            val output = if (wanted) java.io.ByteArrayOutputStream(16384) else null
                            var entryBytes = 0
                            val buf = ByteArray(16384)
                            while (true) {
                                val read = zis.read(buf)
                                if (read < 0) break
                                entryBytes += read
                                totalExtractedBytes += read
                                if (entryBytes > 25 * 1024 * 1024 || totalExtractedBytes > 100L * 1024 * 1024) {
                                    throw SecurityException("Aggregate decompressed content exceeds 100 MB limit")
                                }
                                output?.write(buf, 0, read)
                            }
                            if (output != null) entryMap[norm] = output.toByteArray()
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            } catch (e: SecurityException) {
                throw DocumentLoadException(
                    LoadFailure.EXCEEDS_LIMITS,
                    "This document exceeds the safety limits MyDoc applies: ${e.message}",
                    e
                )
            } catch (e: Throwable) {
                throw DocumentLoadException(
                    LoadFailure.CORRUPT_DOCUMENT,
                    "The document package could not be read. It is truncated or damaged.",
                    e
                )
            }

            entryMap["word/document.xml"]?.let { doc ->
                val ctx = DocxContext(
                    links = readHyperlinkTargets(entryMap["word/_rels/document.xml.rels"]),
                    images = readImageTargets(entryMap["word/_rels/document.xml.rels"], entryMap),
                    numbering = readNumberingDefinitions(entryMap["word/numbering.xml"]),
                    styles = readParagraphStyles(entryMap["word/styles.xml"]),
                    footnotes = readNotes(entryMap["word/footnotes.xml"], "w:footnote"),
                    endnotes = readNotes(entryMap["word/endnotes.xml"], "w:endnote")
                )
                blocks.addAll(parseDocumentXml(createPullParser(doc), ctx))
                blocks.addAll(noteBlocks(ctx))
            }
            if (blocks.isEmpty()) entryMap["content.xml"]?.let { blocks.addAll(parseOdtXml(it)) }

            if (blocks.isEmpty() || blocks.all { it is DocxBlock.Paragraph && it.fullText.isEmpty() }) {
                throw DocumentLoadException(
                    LoadFailure.EMPTY_DOCUMENT,
                    "This document contains no readable content."
                )
            }
            return@withContext DocxDocument(blocks)
        }

        // Flat ODF (.fodt) is the ODF document XML with no zip around it. It previously fell to
        // the byte scanner, which printed <text:p> tags as paragraphs.
        if (!isOle2 && looksLikeXml(bytes)) {
            blocks.addAll(parseOdtXml(bytes))
            if (blocks.isEmpty()) {
                throw DocumentLoadException(LoadFailure.CORRUPT_DOCUMENT, "This flat ODF document has no readable body.")
            }
            return@withContext DocxDocument(blocks)
        }

        // RTF has a real reader now; it used to be byte-scraped and the control words were
        // rendered as document text.
        if (!isOle2 && RtfReader.looksLikeRtf(bytes)) {
            blocks.addAll(RtfReader.read(bytes))
            if (blocks.isEmpty()) {
                throw DocumentLoadException(LoadFailure.EMPTY_DOCUMENT, "This RTF document has no readable text.")
            }
            return@withContext DocxDocument(blocks)
        }

        // Some tools export plain text under a .doc name. Reading it is right; pretending it
        // was parsed as a formatted document is not, so it is recovered and flagged.
        if (!isOle2 && looksLikePlainText(bytes)) {
            blocks.addAll(
                String(bytes, Charsets.UTF_8).lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .map { DocxBlock.Paragraph(listOf(DocxRun(it))) }
            )
            if (blocks.isNotEmpty()) {
                return@withContext DocxDocument(blocks, usedTextFallback = true)
            }
        }

        if (!isOle2) {
            throw DocumentLoadException(
                LoadFailure.UNSUPPORTED_FORMAT,
                "MyDoc has no reader for this document container."
            )
        }

        // A real Word 97-2003 file says where its text is: the FIB points at a piece table that
        // lists the runs and how each is encoded. That is read first, and the byte scan below is
        // only reached when the container is not a Word document at all.
        Word97Text.read(bytes)?.let { text ->
            text.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
                blocks.add(DocxBlock.Paragraph(listOf(DocxRun(line))))
            }
            if (blocks.isNotEmpty()) return@withContext DocxDocument(blocks)
        }

        // Last resort for a damaged binary document: text recovery only, and the flag is what makes
        // the reader say so instead of presenting a scrape as a formatted document.
        blocks.addAll(parseRawTextFallback(bytes))
        if (blocks.isEmpty()) {
            throw DocumentLoadException(
                LoadFailure.CORRUPT_DOCUMENT,
                "No readable text could be recovered from this legacy document."
            )
        }
        DocxDocument(blocks, usedTextFallback = true)
    }

    /** Mostly printable bytes with recognizable line structure, i.e. text mislabelled as .doc. */
    private fun looksLikePlainText(bytes: ByteArray): Boolean {
        val sample = bytes.copyOfRange(0, minOf(bytes.size, 4096))
        if (sample.isEmpty()) return false
        var printable = 0
        for (b in sample) {
            val v = b.toInt() and 0xFF
            if (v in 9..13 || v == 32 || v in 33..126 || v >= 160) printable++
        }
        return printable.toDouble() / sample.size > 0.92
    }

    private fun looksLikeXml(bytes: ByteArray): Boolean {
        var i = 0
        while (i < bytes.size && i < 64 && (bytes[i] == 0x20.toByte() || bytes[i] == 0x09.toByte() ||
                bytes[i] == 0x0A.toByte() || bytes[i] == 0x0D.toByte())) i++
        return i < bytes.size && bytes[i] == 0x3C.toByte()
    }

    private fun parseOdtXml(xmlBytes: ByteArray): List<DocxBlock> {
        val blocks = mutableListOf<DocxBlock>()
        // A failed ODT parse used to return whatever partial blocks it had collected, and the
        // caller reported that as a successfully opened document.
        try {
            val parser = createPullParser(xmlBytes)
            var eventType = parser.eventType

            var inH = false
            var inP = false
            var inList = false
            var inTable = false
            var inTableCell = false
            var isBold = false
            var isItalic = false
            var isUnderline = false
            var headingLevel = 1

            val currentRuns = mutableListOf<DocxRun>()
            val currentTableRows = mutableListOf<MutableList<String>>()
            val currentTableRow = mutableListOf<String>()
            val currentTableCellText = StringBuilder()
            val currentRunText = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val rawName = parser.name ?: ""
                val colonIdx = rawName.indexOf(':')
                val name = if (colonIdx >= 0) rawName.substring(colonIdx + 1) else rawName

                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when {
                            name.equals("h", ignoreCase = true) -> {
                                inH = true
                                currentRuns.clear()
                                currentRunText.clear()
                                val lvlStr = parser.getAttributeValue(null, "text:outline-level") ?: parser.getAttributeValue(null, "outline-level")
                                headingLevel = lvlStr?.toIntOrNull() ?: 1
                                isBold = true
                            }
                            name.equals("p", ignoreCase = true) -> {
                                inP = true
                                currentRuns.clear()
                                currentRunText.clear()
                                isBold = false
                                isItalic = false
                                isUnderline = false
                            }
                            name.equals("list", ignoreCase = true) -> {
                                inList = true
                            }
                            name.equals("table", ignoreCase = true) -> {
                                inTable = true
                                currentTableRows.clear()
                            }
                            name.equals("table-row", ignoreCase = true) -> {
                                currentTableRow.clear()
                            }
                            name.equals("table-cell", ignoreCase = true) -> {
                                inTableCell = true
                                currentTableCellText.clear()
                            }
                            name.equals("span", ignoreCase = true) -> {
                                val style = parser.getAttributeValue(null, "text:style-name") ?: parser.getAttributeValue(null, "style-name") ?: ""
                                if (style.contains("bold", ignoreCase = true)) isBold = true
                                if (style.contains("italic", ignoreCase = true)) isItalic = true
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text ?: ""
                        if (text.isNotEmpty()) {
                            currentRunText.append(text)
                            if (inTableCell) {
                                currentTableCellText.append(text)
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when {
                            name.equals("h", ignoreCase = true) -> {
                                val text = currentRunText.toString().trim()
                                if (text.isNotEmpty()) {
                                    blocks.add(DocxBlock.Heading(text, headingLevel))
                                }
                                currentRunText.clear()
                                inH = false
                            }
                            name.equals("p", ignoreCase = true) -> {
                                val text = currentRunText.toString().trim()
                                if (text.isNotEmpty() && !inTable) {
                                    val runs = listOf(DocxRun(text = text, isBold = isBold, isItalic = isItalic, isUnderline = isUnderline))
                                    if (inList) {
                                        blocks.add(DocxBlock.ListItem(runs, "•", 0, false))
                                    } else {
                                        blocks.add(DocxBlock.Paragraph(runs))
                                    }
                                }
                                currentRunText.clear()
                                inP = false
                            }
                            name.equals("list", ignoreCase = true) -> {
                                inList = false
                            }
                            name.equals("table-cell", ignoreCase = true) -> {
                                currentTableRow.add(currentTableCellText.toString().trim())
                                currentTableCellText.clear()
                                inTableCell = false
                            }
                            name.equals("table-row", ignoreCase = true) -> {
                                if (currentTableRow.isNotEmpty()) {
                                    currentTableRows.add(currentTableRow.toMutableList())
                                }
                            }
                            name.equals("table", ignoreCase = true) -> {
                                if (currentTableRows.isNotEmpty()) {
                                    blocks.add(DocxBlock.Table(currentTableRows.map { row ->
                            row.map { text -> DocxCell(listOf(DocxBlock.Paragraph(listOf(DocxRun(text))))) }
                        }))
                                }
                                inTable = false
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Throwable) {
            throw DocumentLoadException(
                LoadFailure.CORRUPT_DOCUMENT,
                "The OpenDocument content could not be parsed.",
                e
            )
        }
        return blocks
    }

    private fun parseRawTextFallback(bytes: ByteArray): List<DocxBlock> {
        val blocks = mutableListOf<DocxBlock>()
        val strings = mutableListOf<String>()
        val seen = mutableSetOf<String>()

        fun addCandidate(text: String) {
            val lines = text.split("\r\n", "\r", "\n")
            for (rawLine in lines) {
                val trimmed = rawLine.trim()
                if (trimmed.length >= 3 && trimmed.any { it.isLetter() } && seen.add(trimmed.lowercase())) {
                    strings.add(trimmed)
                }
            }
        }

        // Scan UTF-16LE
        var i = 0
        val sbUtf16 = StringBuilder()
        while (i < bytes.size - 1) {
            val b0 = bytes[i].toInt() and 0xFF
            val b1 = bytes[i + 1].toInt() and 0xFF
            val ch = (b1 shl 8) or b0
            if (b1 == 0 && (b0 in 32..126 || b0 == 10 || b0 == 13)) {
                sbUtf16.append(b0.toChar())
                i += 2
            } else if (ch in 0x00A0..0x04FF || ch in 0x0600..0x097F) {
                sbUtf16.append(ch.toChar())
                i += 2
            } else {
                if (sbUtf16.length >= 4) addCandidate(sbUtf16.toString())
                sbUtf16.clear()
                i += 2
            }
        }
        if (sbUtf16.length >= 4) addCandidate(sbUtf16.toString())

        // Scan ASCII / Latin-1
        val sbAscii = StringBuilder()
        for (b in bytes) {
            val c = b.toInt().toChar()
            if (c in ' '..'~' || c == '\n') {
                sbAscii.append(c)
            } else {
                if (sbAscii.length >= 4) addCandidate(sbAscii.toString())
                sbAscii.clear()
            }
        }
        if (sbAscii.length >= 4) addCandidate(sbAscii.toString())

        val noiseSet = setOf(
            "root entry", "worddocument", "1table", "0table", "data", "summaryinformation",
            "documentsummaryinformation", "compobj"
        )
        val validStrings = strings.filter { s ->
            val l = s.lowercase()
            noiseSet.none { l.contains(it) } && !l.startsWith("<?xml")
        }

        // Every token is a paragraph. Promoting the first short string to a heading invented
        // document structure that the binary format never promised.
        for (str in validStrings) {
            blocks.add(DocxBlock.Paragraph(listOf(DocxRun(str))))
        }

        return blocks
    }

    /**
     * What a paragraph's own XML cannot tell you. Hyperlink targets live in
     * `word/_rels/document.xml.rels`, list definitions in `word/numbering.xml`, a heading level is
     * only reliable through `word/styles.xml`, because `w:pStyle` carries a style *id* whose spelling
     * a localized template is free to change, and footnote text is kept out of the body altogether in
     * `word/footnotes.xml`.
     */
    internal class DocxContext(
        val links: Map<String, String> = emptyMap(),
        val images: Map<String, ByteArray> = emptyMap(),
        val numbering: Map<Int, AbstractNumbering> = emptyMap(),
        val styles: Map<String, ParagraphStyle> = emptyMap(),
        val footnotes: Map<Int, List<DocxRun>> = emptyMap(),
        val endnotes: Map<Int, List<DocxRun>> = emptyMap(),
        val cited: NoteCitations = NoteCitations()
    )

    /**
     * Which notes the body points at, in the order they are first cited. Word shows that position as
     * the superscript number, so the same number is used for the reference and for the note text.
     */
    internal class NoteCitations {
        private val footnoteIds = mutableListOf<Int>()
        private val endnoteIds = mutableListOf<Int>()

        fun footnoteNumber(id: Int): Int {
            if (id !in footnoteIds) footnoteIds.add(id)
            return footnoteIds.indexOf(id) + 1
        }

        fun endnoteNumber(id: Int): Int {
            if (id !in endnoteIds) endnoteIds.add(id)
            return endnoteIds.indexOf(id) + 1
        }

        val citedFootnotes: List<Int> get() = footnoteIds
        val citedEndnotes: List<Int> get() = endnoteIds
    }

    internal data class NumberLevel(val format: String, val text: String, val start: Int)
    internal data class AbstractNumbering(val levels: Map<Int, NumberLevel>)
    internal data class ParagraphStyle(
        val name: String,
        val outlineLevel: Int?,
        val basedOn: String?,
        val numId: Int? = null,
        val ilvl: Int = 0,
        val layout: DocxParagraphLayout = DocxParagraphLayout()
    )

    private companion object {
        /** Word nests tables inside cells; past this depth the content is scraped, not parsed. */
        const val MAX_TABLE_DEPTH = 8

        /**
         * The parts the readers below interpret. Everything else in the package stays compressed:
         * styles beyond the paragraph definitions and unrelated binary parts.
         */
        val READER_PARTS = setOf(
            "word/document.xml",
            "word/_rels/document.xml.rels",
            "word/numbering.xml",
            "word/styles.xml",
            "word/footnotes.xml",
            "word/endnotes.xml",
            "content.xml"
        )
    }

    /** Resolve only internal image relationships. Never open a linked URL or filesystem path. */
    private fun readImageTargets(xml: ByteArray?, entries: Map<String, ByteArray>): Map<String, ByteArray> {
        if (xml == null) return emptyMap()
        val result = mutableMapOf<String, ByteArray>()
        val parser = createPullParser(xml)
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "Relationship") {
                val id = parser.getAttributeValue(null, "Id")
                val type = parser.getAttributeValue(null, "Type").orEmpty()
                val target = parser.getAttributeValue(null, "Target").orEmpty()
                val external = parser.getAttributeValue(null, "TargetMode")
                if (id != null && type.endsWith("/image") && external != "External" &&
                    !target.contains(':') && !target.contains('\\')) {
                    val parts = mutableListOf<String>()
                    val path = if (target.startsWith('/')) target.drop(1) else "word/$target"
                    var valid = true
                    for (part in path.split('/')) when (part) {
                        "", "." -> Unit
                        ".." -> if (parts.isEmpty()) valid = false else parts.removeAt(parts.lastIndex)
                        else -> parts.add(part)
                    }
                    val key = parts.joinToString("/").lowercase(java.util.Locale.ROOT)
                    if (valid && key.startsWith("word/media/")) entries[key]?.let { result[id] = it }
                }
            }
            parser.next()
        }
        return result
    }

    /** Pictures become blocks at their text anchor; wrapping/effects are not yet reproduced. */
    private fun readDrawing(parser: XmlPullParser, ctx: DocxContext, layout: DocxParagraphLayout): DocxBlock.Image? {
        var id: String? = null
        var width = 0f
        var height = 0f
        var description = ""
        var floating = false
        var hasPicture = false
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) when (parser.name) {
                "wp:anchor" -> floating = true
                "wp:extent" -> {
                    width = (parser.getAttributeValue(null, "cx")?.toDoubleOrNull()?.div(12700.0) ?: 0.0).toFloat()
                    height = (parser.getAttributeValue(null, "cy")?.toDoubleOrNull()?.div(12700.0) ?: 0.0).toFloat()
                }
                "wp:docPr" -> description = parser.getAttributeValue(null, "descr")
                    ?: parser.getAttributeValue(null, "name").orEmpty()
                "a:blip" -> { hasPicture = true; id = parser.getAttributeValue(null, "r:embed") }
            }
            if (parser.eventType == XmlPullParser.END_TAG && parser.name == "w:drawing") break
            parser.next()
        }
        if (!hasPicture) return null
        return DocxBlock.Image(ctx.images[id], width.takeIf { it.isFinite() && it > 0 } ?: 0f,
            height.takeIf { it.isFinite() && it > 0 } ?: 0f, description, floating, layout)
    }

    private fun attrInt(parser: XmlPullParser, name: String, absent: Int): Int =
        parser.getAttributeValue(null, name)?.toIntOrNull() ?: absent

    /** `<w:b w:val="0"/>` and `w:val="false"` switch a property *off*; the old reader set it on. */
    private fun attrOn(parser: XmlPullParser, absent: Boolean = true): Boolean {
        val value = parser.getAttributeValue(null, "w:val")?.lowercase() ?: return absent
        return value != "false" && value != "0" && value != "none" && value != "off"
    }

    private fun readHyperlinkTargets(xml: ByteArray?): Map<String, String> {
        if (xml == null) return emptyMap()
        val targets = HashMap<String, String>()
        val parser = EngineXml.newPullParser(ByteArrayInputStream(xml))
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "Relationship") {
                val id = parser.getAttributeValue(null, "Id")
                val target = parser.getAttributeValue(null, "Target")
                val mode = parser.getAttributeValue(null, "TargetMode")
                if (id != null && !target.isNullOrBlank() && (mode == null || mode == "External")) {
                    targets[id] = target
                }
            }
            parser.next()
        }
        return targets
    }

    /**
     * Footnote and endnote text, keyed by the id the body cites it with. Word writes two notes that
     * are page furniture rather than content - a `w:type="separator"` and a continuation separator -
     * and those carry only the reference mark itself, so they are skipped instead of read.
     */
    private fun readNotes(xml: ByteArray?, noteTag: String): Map<Int, List<DocxRun>> {
        if (xml == null) return emptyMap()
        val notes = HashMap<Int, List<DocxRun>>()
        val parser = EngineXml.newPullParser(ByteArrayInputStream(xml))
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == noteTag) {
                val id = attrInt(parser, "w:id", -1)
                if (parser.getAttributeValue(null, "w:type") == null && id >= 0) {
                    readNoteText(parser, noteTag).takeIf { it.isNotEmpty() }?.let { notes[id] = it }
                } else {
                    skipElement(parser)
                }
            }
            parser.next()
        }
        return notes
    }

    /** Called on the note's START_TAG; returns on its END_TAG with the runs the note is made of. */
    private fun readNoteText(parser: XmlPullParser, noteTag: String): List<DocxRun> {
        val runs = mutableListOf<DocxRun>()
        var collecting = false
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> if (parser.name == "w:t") collecting = true
                XmlPullParser.END_TAG -> when (parser.name) {
                    "w:t" -> collecting = false
                    noteTag -> return runs
                }

                XmlPullParser.TEXT -> if (collecting) {
                    val text = parser.text
                    if (!text.isNullOrEmpty()) runs.add(DocxRun(text))
                }
            }
            parser.next()
        }
        return runs
    }

    /**
     * Reflow cannot put a note at the bottom of the page Word chose, because the pages are ours. The
     * text is still the reader's, so it is collected at the end of the document under the number the
     * body shows.
     */
    private fun noteBlocks(ctx: DocxContext): List<DocxBlock> {
        fun series(ids: List<Int>, notes: Map<Int, List<DocxRun>>): List<DocxBlock> =
            ids.mapIndexedNotNull { index, id ->
                notes[id]?.takeIf { it.isNotEmpty() }
                    ?.let { DocxBlock.ListItem(runs = it, marker = "[${index + 1}]") }
            }
        return series(ctx.cited.citedFootnotes, ctx.footnotes) +
            series(ctx.cited.citedEndnotes, ctx.endnotes)
    }

    private fun readNumberingDefinitions(xml: ByteArray?): Map<Int, AbstractNumbering> {
        if (xml == null) return emptyMap()
        val levels = HashMap<Int, MutableMap<Int, NumberLevel>>()
        val numToAbstract = HashMap<Int, Int>()
        val parser = EngineXml.newPullParser(ByteArrayInputStream(xml))
        var abstractId = -1
        var levelIndex = -1
        var format = "decimal"
        var levelText = ""
        var startAt = 1
        var numId = -1
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "w:abstractNum" -> {
                        abstractId = attrInt(parser, "w:abstractNumId", -1)
                        if (abstractId >= 0) levels.getOrPut(abstractId) { HashMap() }
                    }

                    "w:lvl" -> {
                        levelIndex = attrInt(parser, "w:ilvl", -1)
                        format = "decimal"; levelText = ""; startAt = 1
                    }

                    "w:numFmt" -> format = parser.getAttributeValue(null, "w:val") ?: "decimal"
                    "w:lvlText" -> levelText = parser.getAttributeValue(null, "w:val") ?: ""
                    "w:start" -> startAt = attrInt(parser, "w:val", 1)
                    "w:num" -> numId = attrInt(parser, "w:numId", -1)
                    "w:abstractNumId" -> if (numId >= 0) numToAbstract[numId] = attrInt(parser, "w:val", -1)
                }

                XmlPullParser.END_TAG -> when (parser.name) {
                    "w:lvl" -> if (abstractId >= 0 && levelIndex >= 0) {
                        levels[abstractId]?.put(levelIndex, NumberLevel(format, levelText, startAt))
                    }

                    "w:abstractNum" -> abstractId = -1
                    "w:num" -> numId = -1
                }
            }
            parser.next()
        }
        return numToAbstract.mapNotNull { (id, abs) ->
            levels[abs]?.let { id to AbstractNumbering(it) }
        }.toMap()
    }

    private fun readParagraphStyles(xml: ByteArray?): Map<String, ParagraphStyle> {
        if (xml == null) return emptyMap()
        val styles = HashMap<String, ParagraphStyle>()
        val numIds = HashMap<String, Int>()
        val parser = EngineXml.newPullParser(ByteArrayInputStream(xml))
        var styleId: String? = null
        var name = ""
        var basedOn: String? = null
        var outline: Int? = null
        var numId: Int? = null
        var ilvl = 0
        var layout = DocxParagraphLayout()
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "w:style" -> {
                        styleId = parser.getAttributeValue(null, "w:styleId")
                        name = ""; basedOn = null; outline = null; numId = null; ilvl = 0; layout = DocxParagraphLayout()
                    }

                    "w:jc", "w:spacing", "w:ind", "w:pageBreakBefore" -> layout = readLayoutProperty(parser, layout)
                    "w:name" -> name = parser.getAttributeValue(null, "w:val") ?: ""
                    "w:basedOn" -> basedOn = parser.getAttributeValue(null, "w:val")
                    "w:outlineLvl" -> outline = attrInt(parser, "w:val", -1).takeIf { it >= 0 }?.plus(1)
                    "w:ilvl" -> ilvl = attrInt(parser, "w:val", 0)
                    "w:numId" -> numId = attrInt(parser, "w:val", -1).takeIf { it >= 0 }
                }

                XmlPullParser.END_TAG -> if (parser.name == "w:style" && styleId != null) {
                    styles[styleId] = ParagraphStyle(name, outline, basedOn, numId, ilvl, layout)
                    if (numId != null) numIds[styleId] = numId
                    styleId = null
                }
            }
            parser.next()
        }
        return styles
    }

    private fun readLayoutProperty(parser: XmlPullParser, layout: DocxParagraphLayout): DocxParagraphLayout {
        fun points(name: String, negative: Boolean = false): Float? =
            parser.getAttributeValue(null, name)?.toIntOrNull()?.let {
                (it / 20f).coerceIn(if (negative) -1440f else 0f, 1440f)
            }
        return when (parser.name) {
            "w:jc" -> layout.copy(alignment = when (parser.getAttributeValue(null, "w:val")) {
                "left" -> DocxAlignment.LEFT
                "right" -> DocxAlignment.RIGHT
                "center" -> DocxAlignment.CENTER
                "both" -> DocxAlignment.JUSTIFY
                "start" -> DocxAlignment.START
                "end" -> DocxAlignment.END
                else -> layout.alignment
            })
            "w:spacing" -> layout.copy(before = points("w:before") ?: layout.before,
                after = points("w:after") ?: layout.after)
            "w:ind" -> layout.copy(left = points("w:left", true) ?: layout.left,
                right = points("w:right", true) ?: layout.right)
            "w:pageBreakBefore" -> layout.copy(pageBreakBefore = attrOn(parser))
            else -> layout
        }
    }

    private fun DocxContext.styleLayout(styleId: String?): DocxParagraphLayout {
        var current = styleId
        var resolved = DocxParagraphLayout()
        val visited = mutableSetOf<String>()
        while (current != null && visited.size < 12 && visited.add(current)) {
            val style = styles[current] ?: break
            resolved = resolved.over(style.layout)
            current = style.basedOn
        }
        return resolved
    }

    private fun DocxContext.styleOutlineAndName(styleId: String?): Pair<Int?, String> {
        var current = styleId
        var hops = 0
        var outline: Int? = null
        var name = ""
        while (current != null && hops++ < 12) {
            val style = styles[current] ?: break
            if (outline == null) outline = style.outlineLevel
            if (name.isEmpty()) name = style.name
            if (outline != null && name.isNotEmpty()) break
            current = style.basedOn
        }
        return outline to name
    }

    private fun DocxContext.styleNumId(styleId: String?): Pair<Int?, Int> {
        var current = styleId
        var hops = 0
        while (current != null && hops++ < 12) {
            val style = styles[current] ?: break
            if (style.numId != null) return style.numId to style.ilvl
            current = style.basedOn
        }
        return null to 0
    }

    /**
     * Recursive-descent reader. The previous version was a flat loop over one shared state, so an
     * inner `w:tbl` cleared the outer table's rows and a hyperlink, a numbered level or a nested
     * paragraph inside a cell could not be represented at all.
     */
    private fun parseDocumentXml(parser: XmlPullParser, ctx: DocxContext = DocxContext()): List<DocxBlock> {
        val blocks = mutableListOf<DocxBlock>()
        val counters = HashMap<Int, IntArray>()
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "w:p" -> blocks.addAll(readParagraph(parser, ctx, counters))
                    "w:tbl" -> readTable(parser, ctx, counters, 0)?.let { blocks.add(it) }
                }
            }
            parser.next()
        }
        return blocks
    }

    /**
     * Consumes the current element and everything under it, leaving the parser on its END_TAG.
     * Anything the readers below do not interpret is dropped this way, so a skipped child can never
     * be mistaken for the close of the element being read - the reason the previous version's
     * hand-rolled depth counters walked a cell open to the end of the document.
     */
    private fun skipElement(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) return
        val name = parser.name
        var depth = 0
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == name) depth++
            else if (parser.eventType == XmlPullParser.END_TAG && parser.name == name) {
                depth--
                if (depth == 0) return
            }
            parser.next()
        }
    }

    /** Called on `w:p`'s START_TAG; returns on its END_TAG. */
    private fun readParagraph(parser: XmlPullParser, ctx: DocxContext, counters: MutableMap<Int, IntArray>): List<DocxBlock> {
        val result = mutableListOf<DocxBlock>()
        var layout = DocxParagraphLayout()
        var styleId: String? = null
        var directOutline: Int? = null
        var numId: Int? = null
        var ilvl: Int? = null
        val runs = mutableListOf<DocxRun>()
        val links = mutableListOf<String?>()
        var bold = false
        var italic = false
        var underline = false
        var collectingText = false
        var firstTextBlock: DocxBlock? = null

        fun flushText() {
            if (runs.any { it.text.isNotEmpty() }) {
                // An image splits one source paragraph, not one list item into two numbers.
                val continuation = layout.over(ctx.styleLayout(styleId)).copy(before = 0f, pageBreakBefore = false)
                when (val first = firstTextBlock) {
                    null -> paragraphBlock(runs.toList(), styleId, directOutline, numId, ilvl, ctx, counters, layout)?.let {
                        firstTextBlock = it
                        result.add(it)
                    }
                    is DocxBlock.Heading -> result.add(DocxBlock.Heading(runs.joinToString("") { it.text }, first.level,
                        continuation, isContinuation = true))
                    else -> result.add(DocxBlock.Paragraph(runs.toList(), continuation))
                }
            }
            runs.clear()
        }
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "w:drawing" -> {
                        flushText()
                        readDrawing(parser, ctx, layout.over(ctx.styleLayout(styleId)).let {
                            if (result.isEmpty()) it else it.copy(before = 0f, pageBreakBefore = false)
                        })?.let(result::add)
                    }
                    "w:jc", "w:spacing", "w:ind", "w:pageBreakBefore" -> layout = readLayoutProperty(parser, layout)
                    "w:pStyle" -> styleId = parser.getAttributeValue(null, "w:val")
                    "w:outlineLvl" -> directOutline =
                        attrInt(parser, "w:val", -1).takeIf { it >= 0 }?.plus(1)
                    "w:ilvl" -> ilvl = attrInt(parser, "w:val", 0)
                    "w:numId" -> attrInt(parser, "w:val", -1).let { if (it >= 0) numId = it }
                    "w:hyperlink" -> links.add(
                        parser.getAttributeValue(null, "r:id")?.let { ctx.links[it] }
                            ?: parser.getAttributeValue(null, "w:anchor")?.let { "#$it" }
                    )

                    "w:r" -> {
                        bold = false; italic = false; underline = false
                    }

                    "w:b", "w:bCs" -> bold = bold || attrOn(parser)
                    "w:i", "w:iCs" -> italic = italic || attrOn(parser)
                    "w:u" -> underline = (parser.getAttributeValue(null, "w:val") ?: "none") != "none"
                    "w:br" -> if (parser.getAttributeValue(null, "w:type") == "page") {
                        flushText()
                        result.add(DocxBlock.PageBreak)
                    } else runs.add(DocxRun("\n", bold, italic, underline, links.lastOrNull()))
                    "w:cr" -> runs.add(DocxRun("\n", bold, italic, underline, links.lastOrNull()))
                    "w:tab" -> runs.add(DocxRun("\t", bold, italic, underline, links.lastOrNull()))
                    "w:footnoteReference" -> citeNote(parser, ctx, runs, footnote = true)
                    "w:endnoteReference" -> citeNote(parser, ctx, runs, footnote = false)
                    "w:t" -> collectingText = true
                }

                XmlPullParser.TEXT -> if (collectingText) {
                    val text = parser.text
                    if (!text.isNullOrEmpty()) {
                        runs.add(DocxRun(text, bold, italic, underline, links.lastOrNull()))
                    }
                }

                XmlPullParser.END_TAG -> when (parser.name) {
                    "w:t" -> collectingText = false
                    "w:hyperlink" -> if (links.isNotEmpty()) links.removeAt(links.size - 1)
                    "w:p" -> {
                        flushText()
                        if (result.isEmpty()) result.add(DocxBlock.Paragraph(emptyList(), layout.over(ctx.styleLayout(styleId))))
                        return result
                    }
                }
            }
            parser.next()
        }
        flushText()
        return result
    }

    /**
     * The body keeps the number Word would print as a superscript; the note's own text is read from
     * its part and appended by [noteBlocks].
     */
    private fun citeNote(parser: XmlPullParser, ctx: DocxContext, runs: MutableList<DocxRun>, footnote: Boolean) {
        val id = attrInt(parser, "w:id", -1)
        val text = if (footnote) ctx.footnotes[id] else ctx.endnotes[id]
        // A reference with no note behind it would be a number the reader could never look up.
        if (text.isNullOrEmpty()) return
        val number = if (footnote) ctx.cited.footnoteNumber(id) else ctx.cited.endnoteNumber(id)
        runs.add(DocxRun("[$number]"))
    }

    private fun paragraphBlock(
        runs: List<DocxRun>,
        styleId: String?,
        directOutline: Int?,
        numId: Int?,
        ilvl: Int?,
        ctx: DocxContext,
        counters: MutableMap<Int, IntArray>,
        layout: DocxParagraphLayout = DocxParagraphLayout()
    ): DocxBlock? {
        if (runs.none { it.text.isNotEmpty() }) return null
        val resolvedLayout = layout.over(ctx.styleLayout(styleId))
        val (styleOutline, styleName) = ctx.styleOutlineAndName(styleId)
        // A package without styles.xml still names the style it used, so the id stays a last resort;
        // outlineLvl, direct or inherited, is the reliable route.
        val level = directOutline ?: styleOutline ?: headingLevelFromName(styleName)
            ?: headingLevelFromName(styleId ?: "") ?: 0
        if (level > 0) return DocxBlock.Heading(runs.joinToString("") { it.text }, level, resolvedLayout)

        val inherited = ctx.styleNumId(styleId)
        val effectiveNumId = numId ?: inherited.first
        val styleHint = styleName + " " + (styleId ?: "")
        val isList = effectiveNumId != null || numId != null || ilvl != null ||
            styleHint.contains("List", ignoreCase = true)
        if (!isList) return DocxBlock.Paragraph(runs, resolvedLayout)
        return listBlock(runs, ctx, counters, effectiveNumId, ilvl ?: inherited.second, styleHint).copy(layout = resolvedLayout)
    }

    /** Called on `w:tbl`'s START_TAG; returns on its END_TAG. Cells recurse, so nesting is kept. */
    private fun readTable(
        parser: XmlPullParser,
        ctx: DocxContext,
        counters: MutableMap<Int, IntArray>,
        depth: Int
    ): DocxBlock? {
        val rows = mutableListOf<List<DocxCell>>()
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "w:tr" -> readRow(parser, ctx, counters, depth)?.let { rows.add(it) }
                    // Only rows and cells are read here. Matching this element's own name would make
                    // the reader recurse into the table it was called on and wrap it in a phantom row,
                    // which is what happened first: a nested table belongs in a cell, handled by
                    // readCell, never directly in the table body.
                    "w:tblPr", "w:tblGrid" -> skipElement(parser)
                }

                XmlPullParser.END_TAG -> if (parser.name == "w:tbl") {
                    return rows.filter { row -> row.any { cell -> cell.blocks.isNotEmpty() } }
                        .takeIf { it.isNotEmpty() }
                        ?.let { DocxBlock.Table(it) }
                }
            }
            parser.next()
        }
        return null
    }

    /** Called on `w:tr`'s START_TAG; returns on its END_TAG. */
    private fun readRow(
        parser: XmlPullParser,
        ctx: DocxContext,
        counters: MutableMap<Int, IntArray>,
        depth: Int
    ): List<DocxCell>? {
        val cells = mutableListOf<DocxCell>()
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "w:tc" -> cells.add(readCell(parser, ctx, counters, depth))
                    "w:trPr" -> skipElement(parser)
                }

                XmlPullParser.END_TAG -> if (parser.name == "w:tr") return cells.ifEmpty { null }
            }
            parser.next()
        }
        return cells.ifEmpty { null }
    }

    /** Called on `w:tc`'s START_TAG; returns on its END_TAG with everything the cell contains. */
    private fun readCell(
        parser: XmlPullParser,
        ctx: DocxContext,
        counters: MutableMap<Int, IntArray>,
        depth: Int
    ): DocxCell {
        val blocks = mutableListOf<DocxBlock>()
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "w:p" -> if (depth < MAX_TABLE_DEPTH) {
                        blocks.addAll(readParagraph(parser, ctx, counters))
                    } else {
                        skipElement(parser)
                    }

                    "w:tbl" -> if (depth + 1 < MAX_TABLE_DEPTH) {
                        readTable(parser, ctx, counters, depth + 1)?.let { blocks.add(it) }
                    } else {
                        skipElement(parser)
                    }

                    "w:tcPr" -> skipElement(parser)
                }

                XmlPullParser.END_TAG -> if (parser.name == "w:tc") {
                    return DocxCell(blocks.toList())
                }
            }
            parser.next()
        }
        return DocxCell(blocks.toList())
    }

    private fun headingLevelFromName(name: String): Int? {
        val match = Regex("^(?:heading|Heading)\\s*([1-9])").find(name.trim())
        if (match != null) return match.groupValues[1].toInt()
        val trimmed = name.trim().lowercase()
        return if (trimmed == "title") 1 else null
    }

    private fun listBlock(
        runs: List<DocxRun>,
        ctx: DocxContext,
        counters: MutableMap<Int, IntArray>,
        numId: Int?,
        ilvl: Int,
        styleHint: String
    ): DocxBlock.ListItem {
        val numbering = numId?.let { ctx.numbering[it] }
        val level = numbering?.levels?.get(ilvl)
        if (numbering != null && level != null) {
            val counter = counters.getOrPut(numId) { IntArray(9) { 0 } }
            val slot = ilvl.coerceIn(0, 8)
            counter[slot] = if (counter[slot] == 0) level.start else counter[slot] + 1
            for (deeper in (slot + 1)..8) counter[deeper] = 0
            val ordered = level.format != "bullet" && level.format != "none"
            return DocxBlock.ListItem(
                runs = runs,
                marker = markerFor(level, numbering, counter),
                level = ilvl.coerceIn(0, 8),
                ordered = ordered
            )
        }
        val bulletAsDigit = styleHint.lowercase().contains("listnumber")
        return DocxBlock.ListItem(
            runs = runs,
            marker = if (bulletAsDigit) "1." else "•",
            level = ilvl,
            ordered = bulletAsDigit
        )
    }

    private fun markerFor(level: NumberLevel, numbering: AbstractNumbering, counter: IntArray): String {
        val template = level.text.ifEmpty {
            return if (level.format == "bullet" || level.format == "none") "•" else "%1."
        }
        var text = template
        for (index in 0..8) {
            val value = counter[index]
            if (value == 0) continue
            val fmt = numbering.levels[index]?.format ?: level.format
            text = text.replace("%${index + 1}", formatNumber(value, fmt))
        }
        // A bullet level's glyph usually comes from Symbol or Wingdings and sits in the private-use
        // area - F0B7 is Symbol's bullet. That is a font instruction, not a character: drawn with the
        // document's own typeface it is an empty box, so an undrawable marker falls back to a real one.
        val trimmed = text.trim()
        return if (trimmed.isEmpty() || trimmed.any { it.code in 0xE000..0xF8FF }) "•" else trimmed
    }

    private fun formatNumber(value: Int, format: String): String = when (format) {
        "bullet", "none" -> "•"
        "lowerLetter" -> ('a' + ((value - 1) % 26)).toString()
        "upperLetter" -> ('A' + ((value - 1) % 26)).toString()
        "lowerRoman" -> roman(value).lowercase()
        "upperRoman" -> roman(value)
        else -> value.toString()
    }

    private fun roman(number: Int): String {
        if (number <= 0) return number.toString()
        val pairs = arrayOf(
            1000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC",
            50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I"
        )
        var rest = number
        val out = StringBuilder()
        for ((value, symbol) in pairs) {
            while (rest >= value) {
                out.append(symbol); rest -= value
            }
        }
        return out.toString()
    }
}
