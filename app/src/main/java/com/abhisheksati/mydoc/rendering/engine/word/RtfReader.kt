package com.abhisheksati.mydoc.rendering.engine.word

import java.nio.charset.Charset

/**
 * A minimal but real RTF reader.
 *
 * RTF files previously reached the byte scanner, which surfaced `\ansi\deff0` and `{\*\...}` as
 * document paragraphs. This walks the control stream: group state for bold, italic and underline
 * is saved and restored, `\'hh` decodes in the declared code page, `\uN` resolves while skipping
 * the `uc` count of fallback characters that follows, `\bin` payloads are dropped by length, and
 * destination groups (font and color tables, pictures, headers, footers) are ignored wholesale.
 *
 * It produces paragraphs, not headings: without resolving the stylesheet, RTF carries no reliable
 * heading semantics, and inventing them would be the same fabrication the byte scanner did.
 */
object RtfReader {

    private val ignoredDestinations = setOf(
        "fonttbl", "colortbl", "stylesheet", "info", "header", "footer", "pict",
        "generator", "listtable", "listoverride", "object", "themedata",
        "colorschememapping", "latentstyles", "datastore", "xmlnstbl", "mmathPr"
    )

    private const val HEX_ESCAPE = "#hex"
    private const val LITERAL = "#literal"

    fun looksLikeRtf(bytes: ByteArray): Boolean {
        if (bytes.size < 6) return false
        val head = String(bytes, 0, minOf(bytes.size, 64), Charsets.ISO_8859_1).trimStart()
        return head.startsWith("{\\rtf")
    }

    fun read(bytes: ByteArray): List<DocxBlock> {
        val text = String(bytes, Charsets.ISO_8859_1)
        val blocks = mutableListOf<DocxBlock>()
        val paragraph = StringBuilder()
        val runs = mutableListOf<DocxRun>()

        var bold = false
        var italic = false
        var underline = false
        var codePage = Charset.forName("windows-1252")
        var unicodeFallbackCount = 1
        var pendingUnicodeSkip = 0
        var skipDepth = -1

        data class Frame(val bold: Boolean, val italic: Boolean, val underline: Boolean)
        val stack = ArrayDeque<Frame>()

        fun skipping() = skipDepth >= 0

        fun flushRun() {
            if (paragraph.isEmpty()) return
            runs += DocxRun(paragraph.toString(), bold, italic, underline)
            paragraph.setLength(0)
        }

        fun endParagraph() {
            flushRun()
            if (runs.isNotEmpty()) {
                blocks += DocxBlock.Paragraph(runs.toList())
                runs.clear()
            }
        }

        fun setStyle(block: () -> Unit) {
            flushRun()
            block()
        }

        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                ch == '{' -> {
                    stack.addLast(Frame(bold, italic, underline))
                    i++
                }

                ch == '}' -> {
                    if (stack.isNotEmpty()) {
                        val frame = stack.removeLast()
                        flushRun()
                        bold = frame.bold
                        italic = frame.italic
                        underline = frame.underline
                    }
                    if (stack.size < skipDepth) skipDepth = -1
                    i++
                }

                ch == '\\' -> {
                    i = readControl(text, i + 1) { word, argument, digitArg ->
                        if (skipping()) {
                            // Still needed: \bin must consume its payload even inside a skipped group.
                            if (word == "bin") i += digitArg.coerceAtLeast(0)
                        } else {
                            when {
                                word == HEX_ESCAPE ->
                                    paragraph.append(String(byteArrayOf(digitArg.toByte()), codePage))
                                word == LITERAL -> when (argument) {
                                    "~" -> paragraph.append(' ')
                                    "-" -> Unit
                                    else -> paragraph.append(argument)
                                }
                                word == "par" || word == "line" || word == "sect" -> endParagraph()
                                word == "tab" -> paragraph.append('\t')
                                word == "uc" -> unicodeFallbackCount = digitArg.coerceAtLeast(0)
                                word == "u" -> {
                                    flushRun()
                                    paragraph.append((if (digitArg < 0) digitArg + 65536 else digitArg).toChar())
                                    pendingUnicodeSkip = unicodeFallbackCount
                                }
                                word == "ansicpg" -> runCatching { codePage = Charset.forName("windows-$digitArg") }
                                // A toggle control with no parameter means ON; only `0` (and
                                // the same for i/ul) turns it off. Reading the missing parameter as
                                // zero dropped every bold, italic and underline in the document.
                                word == "b" -> setStyle { bold = argument.isEmpty() || digitArg != 0 }
                                word == "i" -> setStyle { italic = argument.isEmpty() || digitArg != 0 }
                                word == "ul" -> setStyle { underline = argument.isEmpty() || digitArg != 0 }
                                word == "ulnone" -> setStyle { underline = false }
                                word == "plain" -> setStyle {
                                    bold = false; italic = false; underline = false
                                }
                                word in ignoredDestinations -> skipDepth = stack.size
                            }
                        }
                    }
                }

                ch == '\r' || ch == '\n' -> i++

                else -> {
                    if (pendingUnicodeSkip > 0 && !skipping()) {
                        pendingUnicodeSkip--
                    } else if (!skipping()) {
                        paragraph.append(ch)
                    }
                    i++
                }
            }
        }
        endParagraph()
        return blocks.filter { it !is DocxBlock.Paragraph || it.fullText.isNotBlank() }
    }

    /**
     * Consumes the control word starting at [start] and returns the index to continue from.
     * [onControl] receives the word, its literal argument and its numeric argument.
     */
    private inline fun readControl(
        text: String,
        start: Int,
        onControl: (word: String, argument: String, digitArg: Int) -> Unit
    ): Int {
        if (start >= text.length) return start + 1

        val first = text[start]
        if (!first.isLetter()) {
            if (first == '\'' && start + 2 < text.length) {
                val hex = text.substring(start + 1, start + 3)
                hex.toIntOrNull(16)?.let { byte -> onControl(HEX_ESCAPE, "", byte) }
                return start + 3
            }
            onControl(LITERAL, first.toString(), 0)
            return start + 1
        }

        var i = start
        while (i < text.length && text[i].isLetter()) i++
        val word = text.substring(start, i)

        var sign = 1
        if (i < text.length && text[i] == '-') {
            sign = -1
            i++
        }
        val digitsStart = i
        while (i < text.length && text[i].isDigit()) i++
        val digits = text.substring(digitsStart, i)
        val number = digits.toIntOrNull() ?: 0

        // A single space after a control word is part of the delimiter, not content.
        if (i < text.length && text[i] == ' ') i++

        onControl(word, digits, sign * number)
        return i
    }
}
