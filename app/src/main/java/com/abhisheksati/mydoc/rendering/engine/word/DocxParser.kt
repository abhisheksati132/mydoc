package com.abhisheksati.mydoc.rendering.engine.word

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
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
    data class Heading(val text: String, val level: Int) : DocxBlock()
    data class Paragraph(val runs: List<DocxRun>) : DocxBlock() {
        val fullText: String get() = runs.joinToString("") { it.text }
    }
    data class Bullet(val runs: List<DocxRun>) : DocxBlock()
    data class Table(val rows: List<List<String>>) : DocxBlock()
}

data class DocxDocument(
    val blocks: List<DocxBlock>
)

class DocxParser {

    suspend fun parse(inputStream: InputStream): DocxDocument = withContext(Dispatchers.IO) {
        val bytes = inputStream.readBytes()
        if (bytes.isEmpty()) {
            return@withContext DocxDocument(listOf(DocxBlock.Paragraph(listOf(DocxRun("Document is empty.")))))
        }

        val isZip = bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()
        val blocks = mutableListOf<DocxBlock>()

        if (isZip) {
            val entryMap = mutableMapOf<String, ByteArray>()
            try {
                val zis = ZipInputStream(ByteArrayInputStream(bytes))
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val norm = entry.name.replace('\\', '/').trimStart('/').lowercase()
                        if (norm == "word/document.xml" || norm == "content.xml") {
                            entryMap[norm] = zis.readBytes()
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            } catch (_: Throwable) {}

            // 1. DOCX: word/document.xml
            val docXml = entryMap["word/document.xml"]
            if (docXml != null) {
                try {
                    val parser = Xml.newPullParser().apply {
                        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                        setInput(ByteArrayInputStream(docXml), "UTF-8")
                    }
                    blocks.addAll(parseDocumentXml(parser))
                } catch (_: Throwable) {}
            }

            // 2. ODT: content.xml
            if (blocks.isEmpty()) {
                val contentXml = entryMap["content.xml"]
                if (contentXml != null) {
                    blocks.addAll(parseOdtXml(contentXml))
                }
            }
        }

        // 3. Fallback for binary .doc (OLE2) or corrupted document streams
        if (blocks.isEmpty()) {
            blocks.addAll(parseRawTextFallback(bytes))
        }

        if (blocks.isEmpty()) {
            blocks.add(DocxBlock.Paragraph(listOf(DocxRun("Document is empty."))))
        }

        DocxDocument(blocks)
    }

    private fun parseOdtXml(xmlBytes: ByteArray): List<DocxBlock> {
        val blocks = mutableListOf<DocxBlock>()
        try {
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(ByteArrayInputStream(xmlBytes), "UTF-8")
            }
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
        } catch (_: Throwable) {}
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

        for ((idx, str) in validStrings.withIndex()) {
            if (idx == 0 && str.length < 60) {
                blocks.add(DocxBlock.Heading(str, 1))
            } else {
                blocks.add(DocxBlock.Paragraph(listOf(DocxRun(str))))
            }
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
}
