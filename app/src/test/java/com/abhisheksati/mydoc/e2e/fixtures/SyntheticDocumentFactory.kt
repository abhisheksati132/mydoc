package com.abhisheksati.mydoc.e2e.fixtures

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Deterministic synthetic document generator for E2E opaque-box testing.
 * Produces valid, boundary, and corrupted PDF, DOCX, PPTX, XLSX, CSV, and ZIP archives.
 */
object SyntheticDocumentFactory {

    /**
     * Generates a valid standard PDF with embedded text streams.
     */
    fun createValidPdf(
        target: File,
        text: String = "Hello MyDoc Opaque Box Test",
        pageCount: Int = 1
    ): File {
        target.parentFile?.mkdirs()
        val safeText = text.replace("(", "").replace(")", "").replace("\\", "")
        val pdfContent = buildString {
            appendLine("%PDF-1.4")
            appendLine("1 0 obj")
            appendLine("<< /Type /Catalog /Pages 2 0 R >>")
            appendLine("endobj")
            appendLine("2 0 obj")
            val kids = (1..pageCount).joinToString(" ") { "${it * 2 + 1} 0 R" }
            appendLine("<< /Type /Pages /Kids [$kids] /Count $pageCount >>")
            appendLine("endobj")

            var objId = 3
            for (p in 1..pageCount) {
                val pageObjId = objId
                val contentObjId = objId + 1
                objId += 2

                appendLine("$pageObjId 0 obj")
                appendLine("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents $contentObjId 0 R /Resources << /Font << /F1 ${pageCount * 2 + 3} 0 R >> >> >>")
                appendLine("endobj")

                val streamData = "BT /F1 12 Tf 50 700 Td ($safeText Page $p) Tj ET"
                appendLine("$contentObjId 0 obj")
                appendLine("<< /Length ${streamData.length} >>")
                appendLine("stream")
                appendLine(streamData)
                appendLine("endstream")
                appendLine("endobj")
            }

            val fontObjId = pageCount * 2 + 3
            appendLine("$fontObjId 0 obj")
            appendLine("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>")
            appendLine("endobj")
            appendLine("xref")
            appendLine("0 ${fontObjId + 1}")
            appendLine("0000000000 65535 f ")
            for (i in 1..fontObjId) {
                appendLine(String.format("%010d 00000 n ", i * 60))
            }
            appendLine("trailer")
            appendLine("<< /Size ${fontObjId + 1} /Root 1 0 R >>")
            appendLine("startxref")
            appendLine("500")
            appendLine("%%EOF")
        }
        target.writeText(pdfContent)
        return target
    }

    /**
     * Generates a scanned/raster-only PDF that contains no searchable text streams.
     */
    fun createScannedImagePdf(target: File): File {
        target.parentFile?.mkdirs()
        val pdfContent = """
            %PDF-1.4
            1 0 obj
            << /Type /Catalog /Pages 2 0 R >>
            endobj
            2 0 obj
            << /Type /Pages /Kids [3 0 R] /Count 1 >>
            endobj
            3 0 obj
            << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >>
            endobj
            4 0 obj
            << /Length 30 >>
            stream
            q 10 0 0 10 50 50 cm /Im1 Do Q
            endstream
            endobj
            xref
            0 5
            0000000000 65535 f 
            0000000009 00000 n 
            0000000058 00000 n 
            0000000115 00000 n 
            0000000204 00000 n 
            trailer
            << /Size 5 /Root 1 0 R >>
            startxref
            280
            %%EOF
        """.trimIndent()
        target.writeText(pdfContent)
        return target
    }

    /**
     * Generates a corrupted PDF with broken header and missing xref.
     */
    fun createCorruptedPdf(target: File): File {
        target.parentFile?.mkdirs()
        target.writeBytes(ByteArray(256) { (it % 256).toByte() })
        return target
    }

    /**
     * Generates a 0-page or empty PDF shell.
     */
    fun createZeroPagePdf(target: File): File {
        target.parentFile?.mkdirs()
        val content = """
            %PDF-1.4
            1 0 obj
            << /Type /Catalog /Pages 2 0 R >>
            endobj
            2 0 obj
            << /Type /Pages /Kids [] /Count 0 >>
            endobj
            xref
            0 3
            0000000000 65535 f 
            0000000009 00000 n 
            0000000058 00000 n 
            trailer
            << /Size 3 /Root 1 0 R >>
            startxref
            120
            %%EOF
        """.trimIndent()
        target.writeText(content)
        return target
    }

    /**
     * Generates a valid standard DOCX archive.
     */
    fun createDocx(
        target: File,
        paragraphs: List<String>,
        headings: List<String> = emptyList()
    ): File {
        target.parentFile?.mkdirs()
        val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                <Default Extension="xml" ContentType="application/xml"/>
                <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
            </Types>
        """.trimIndent()

        val relsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
            </Relationships>
        """.trimIndent()

        val docXml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>""")
            for (h in headings) {
                append("<w:p><w:pPr><w:pStyle w:val=\"Heading1\"/></w:pPr><w:r><w:t>$h</w:t></w:r></w:p>")
            }
            for (p in paragraphs) {
                append("<w:p><w:r><w:t>$p</w:t></w:r></w:p>")
            }
            append("""</w:body></w:document>""")
        }

        createZip(
            target,
            mapOf(
                "[Content_Types].xml" to contentTypesXml.toByteArray(Charsets.UTF_8),
                "_rels/.rels" to relsXml.toByteArray(Charsets.UTF_8),
                "word/document.xml" to docXml.toByteArray(Charsets.UTF_8)
            )
        )
        return target
    }

    /**
     * Generates a valid standard PPTX archive.
     */
    fun createPptx(target: File, slideTitles: List<String>): File {
        target.parentFile?.mkdirs()
        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                <Default Extension="xml" ContentType="application/xml"/>
                <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
                ${slideTitles.mapIndexed { i, _ -> "<Override PartName=\"/ppt/slides/slide${i + 1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slide+xml\"/>" }.joinToString("\n")}
            </Types>
        """.trimIndent()

        val rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
            </Relationships>
        """.trimIndent()

        val presXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <p:presentation xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
                <p:sldIdLst>
                    ${slideTitles.mapIndexed { i, _ -> "<p:sldId id=\"${256 + i}\" r:id=\"rId${i + 1}\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"/>" }.joinToString("\n")}
                </p:sldIdLst>
            </p:presentation>
        """.trimIndent()

        val entries = mutableMapOf<String, ByteArray>(
            "[Content_Types].xml" to contentTypes.toByteArray(Charsets.UTF_8),
            "_rels/.rels" to rels.toByteArray(Charsets.UTF_8),
            "ppt/presentation.xml" to presXml.toByteArray(Charsets.UTF_8)
        )

        slideTitles.forEachIndexed { idx, title ->
            val slideNum = idx + 1
            val slideXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
                    <p:cSld>
                        <p:spTree>
                            <p:sp>
                                <p:txBody>
                                    <a:p><a:r><a:t>$title</a:t></a:r></a:p>
                                </p:txBody>
                            </p:sp>
                        </p:spTree>
                    </p:cSld>
                </p:sld>
            """.trimIndent()
            entries["ppt/slides/slide$slideNum.xml"] = slideXml.toByteArray(Charsets.UTF_8)
        }

        createZip(target, entries)
        return target
    }

    /**
     * Generates a valid standard XLSX archive with cell values and optional formulas.
     */
    fun createXlsx(
        target: File,
        rows: List<List<String>>,
        formulas: Map<Pair<Int, Int>, String> = emptyMap()
    ): File {
        target.parentFile?.mkdirs()
        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                <Default Extension="xml" ContentType="application/xml"/>
                <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
            </Types>
        """.trimIndent()

        val rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
            </Relationships>
        """.trimIndent()

        val workbookRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
            </Relationships>
        """.trimIndent()

        val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                <sheets><sheet name="Sheet1" sheetId="1" r:id="rId1" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"/></sheets>
            </workbook>
        """.trimIndent()

        val sheetXml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
            rows.forEachIndexed { rIdx, row ->
                val rNum = rIdx + 1
                append("<row r=\"$rNum\">")
                row.forEachIndexed { cIdx, cellValue ->
                    val colLetter = ('A'.code + cIdx).toChar()
                    val cellRef = "$colLetter$rNum"
                    val formula = formulas[Pair(rIdx, cIdx)]
                    if (formula != null) {
                        append("<c r=\"$cellRef\"><f>$formula</f><v>$cellValue</v></c>")
                    } else {
                        val isNum = cellValue.toDoubleOrNull() != null
                        if (isNum) {
                            append("<c r=\"$cellRef\"><v>$cellValue</v></c>")
                        } else {
                            append("<c r=\"$cellRef\" t=\"inlineStr\"><is><t>$cellValue</t></is></c>")
                        }
                    }
                }
                append("</row>")
            }
            append("""</sheetData></worksheet>""")
        }

        createZip(
            target,
            mapOf(
                "[Content_Types].xml" to contentTypes.toByteArray(Charsets.UTF_8),
                "_rels/.rels" to rels.toByteArray(Charsets.UTF_8),
                "xl/workbook.xml" to workbookXml.toByteArray(Charsets.UTF_8),
                "xl/_rels/workbook.xml.rels" to workbookRels.toByteArray(Charsets.UTF_8),
                "xl/worksheets/sheet1.xml" to sheetXml.toByteArray(Charsets.UTF_8)
            )
        )
        return target
    }

    /**
     * Generates an RFC 4180 compliant CSV document.
     */
    fun createCsv(target: File, rows: List<List<String>>, delimiter: String = ","): File {
        target.parentFile?.mkdirs()
        val text = rows.joinToString("\r\n") { row ->
            row.joinToString(delimiter) { field ->
                if (field.contains(delimiter) || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
                    "\"" + field.replace("\"", "\"\"") + "\""
                } else {
                    field
                }
            }
        }
        target.writeText(text, Charsets.UTF_8)
        return target
    }

    /**
     * Creates a ZIP file containing arbitrary entries.
     */
    fun createZip(target: File, entries: Map<String, ByteArray>): File {
        target.parentFile?.mkdirs()
        FileOutputStream(target).use { fos ->
            ZipOutputStream(fos).use { zos ->
                for ((name, data) in entries) {
                    val entry = ZipEntry(name)
                    zos.putNextEntry(entry)
                    zos.write(data)
                    zos.closeEntry()
                }
            }
        }
        return target
    }

    /**
     * Creates a hostile ZIP Slip archive containing directory traversal paths.
     */
    fun createZipSlipArchive(target: File, slipTargetName: String = "../../evil_override.txt"): File {
        return createZip(
            target,
            mapOf(
                "safe_file.txt" to "Safe content".toByteArray(Charsets.UTF_8),
                slipTargetName to "Malicious Traversal Content".toByteArray(Charsets.UTF_8)
            )
        )
    }

    /**
     * Creates a high-ratio Zip Bomb archive where small compressed payload expands to huge zeroes.
     */
    fun createZipBomb(target: File, uncompressedSizeBytes: Long = 10L * 1024 * 1024): File {
        target.parentFile?.mkdirs()
        FileOutputStream(target).use { fos ->
            ZipOutputStream(fos).use { zos ->
                val entry = ZipEntry("bomb.bin")
                zos.putNextEntry(entry)
                val buffer = ByteArray(64 * 1024) // 64 KB of zeroes
                var written = 0L
                while (written < uncompressedSizeBytes) {
                    val toWrite = minOf(buffer.size.toLong(), uncompressedSizeBytes - written).toInt()
                    zos.write(buffer, 0, toWrite)
                    written += toWrite
                }
                zos.closeEntry()
            }
        }
        return target
    }
}
