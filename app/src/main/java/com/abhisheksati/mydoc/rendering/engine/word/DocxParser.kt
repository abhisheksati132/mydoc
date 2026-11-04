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
    val isUnderline: Boolean = false
)

sealed class DocxBlock {
    data class Heading(val text: String, val level: Int = 1) : DocxBlock()
    data class Paragraph(val runs: List<DocxRun>) : DocxBlock() {
        val fullText: String get() = runs.joinToString("") { it.text }
    }
    data class Bullet(val runs: List<DocxRun>) : DocxBlock()
    data class Table(val rows: List<List<String>>) : DocxBlock()
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
                            val norm = entry.name.replace('\\', '/').trimStart('/').lowercase()
                            if (norm == "word/document.xml" || norm == "content.xml") {
                                val data = readBounded(zis, 25 * 1024 * 1024)
                                totalExtractedBytes += data.size
                                if (totalExtractedBytes > 100L * 1024 * 1024) {
                                    throw SecurityException("Aggregate decompressed content exceeds 100 MB limit")
                                }
                                entryMap[norm] = data
                            }
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

            entryMap["word/document.xml"]?.let { blocks.addAll(parseDocumentXml(createPullParser(it))) }
            if (blocks.isEmpty()) entryMap["content.xml"]?.let { blocks.addAll(parseOdtXml(it)) }

            if (blocks.isEmpty()) {
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

        // Legacy binary .doc: text recovery only. The flag is what makes the reader say so
        // instead of presenting a scrape as a formatted document.
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
                                        blocks.add(DocxBlock.Bullet(runs))
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
                                    blocks.add(DocxBlock.Table(currentTableRows.map { it.toList() }))
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

    private fun parseDocumentXml(parser: XmlPullParser): List<DocxBlock> {
        val blocks = mutableListOf<DocxBlock>()
        var eventType = parser.eventType

        var inParagraph = false
        var inRun = false
        var inTable = false
        var inTableRow = false
        var inTableCell = false

        var isBold = false
        var isItalic = false
        var isUnderline = false
        var headingLevel = 0
        var isBullet = false

        val currentRuns = mutableListOf<DocxRun>()
        val currentTableRows = mutableListOf<MutableList<String>>()
        val currentTableCellText = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name ?: ""

            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (name) {
                        "w:p" -> {
                            inParagraph = true
                            currentRuns.clear()
                            headingLevel = 0
                            isBullet = false
                        }
                        "w:pStyle" -> {
                            val styleVal = parser.getAttributeValue(null, "w:val") ?: ""
                            headingLevel = when {
                                styleVal.contains("Heading1", ignoreCase = true) || styleVal.contains("Title", ignoreCase = true) -> 1
                                styleVal.contains("Heading2", ignoreCase = true) -> 2
                                styleVal.contains("Heading3", ignoreCase = true) -> 3
                                else -> 0
                            }
                            // Word puts the numbering on the paragraph style most of the time, so a
                            // list is usually recognisable only by its style name.
                            if (styleVal.contains("ListBullet", ignoreCase = true) ||
                                styleVal.contains("ListParagraph", ignoreCase = true)
                            ) {
                                isBullet = true
                            }
                        }
                        "w:numPr" -> {
                            isBullet = true
                        }
                        "w:r" -> {
                            inRun = true
                            isBold = false
                            isItalic = false
                            isUnderline = false
                        }
                        "w:b" -> isBold = true
                        "w:i" -> isItalic = true
                        "w:u" -> isUnderline = true
                        "w:t" -> {
                            val text = parser.nextText()
                            if (text.isNotEmpty()) {
                                if (inTableCell) {
                                    currentTableCellText.append(text)
                                } else {
                                    currentRuns.add(
                                        DocxRun(
                                            text = text,
                                            isBold = isBold,
                                            isItalic = isItalic,
                                            isUnderline = isUnderline
                                        )
                                    )
                                }
                            }
                        }
                        "w:tbl" -> {
                            inTable = true
                            currentTableRows.clear()
                        }
                        "w:tr" -> {
                            inTableRow = true
                            currentTableRows.add(mutableListOf())
                        }
                        "w:tc" -> {
                            inTableCell = true
                            currentTableCellText.clear()
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    when (name) {
                        "w:p" -> {
                            if (!inTableCell) {
                                val fullText = currentRuns.joinToString("") { it.text }.trim()
                                if (fullText.isNotEmpty()) {
                                    if (headingLevel > 0) {
                                        blocks.add(DocxBlock.Heading(fullText, headingLevel))
                                    } else if (isBullet) {
                                        blocks.add(DocxBlock.Bullet(currentRuns.toList()))
                                    } else {
                                        blocks.add(DocxBlock.Paragraph(currentRuns.toList()))
                                    }
                                }
                            }
                            inParagraph = false
                        }
                        "w:r" -> inRun = false
                        "w:tc" -> {
                            val cellText = currentTableCellText.toString().trim()
                            currentTableRows.lastOrNull()?.add(cellText)
                            inTableCell = false
                        }
                        "w:tr" -> inTableRow = false
                        "w:tbl" -> {
                            if (currentTableRows.isNotEmpty()) {
                                blocks.add(DocxBlock.Table(currentTableRows.map { it.toList() }))
                            }
                            inTable = false
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return blocks
    }

    private fun readBounded(input: java.io.InputStream, maxBytes: Int): ByteArray {
        val baos = java.io.ByteArrayOutputStream(minOf(maxBytes, 64 * 1024))
        val buf = ByteArray(16384)
        var total = 0
        var n: Int
        while (input.read(buf).also { n = it } != -1) {
            val toWrite = minOf(n, maxBytes - total)
            baos.write(buf, 0, toWrite)
            total += toWrite
            if (total >= maxBytes) break
        }
        return baos.toByteArray()
    }
}
