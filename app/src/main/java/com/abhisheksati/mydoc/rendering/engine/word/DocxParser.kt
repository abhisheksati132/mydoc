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
        val zip = ZipInputStream(ByteArrayInputStream(bytes))
        var entry: ZipEntry? = zip.nextEntry
        val blocks = mutableListOf<DocxBlock>()

        while (entry != null) {
            if (entry.name == "word/document.xml") {
                val parser = Xml.newPullParser().apply {
                    setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                    setInput(zip, "UTF-8")
                }
                blocks.addAll(parseDocumentXml(parser))
                break
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }

        if (blocks.isEmpty()) {
            blocks.add(DocxBlock.Paragraph(listOf(DocxRun("Document is empty."))))
        }

        DocxDocument(blocks)
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
