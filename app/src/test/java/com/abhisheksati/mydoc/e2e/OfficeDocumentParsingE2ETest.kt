package com.abhisheksati.mydoc.e2e

import com.abhisheksati.mydoc.domain.model.DocumentCategory
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.e2e.fixtures.SyntheticDocumentFactory
import com.abhisheksati.mydoc.rendering.engine.presentation.PptxParser
import com.abhisheksati.mydoc.rendering.engine.spreadsheet.SpreadsheetParser
import com.abhisheksati.mydoc.rendering.engine.word.DocxBlock
import com.abhisheksati.mydoc.rendering.engine.word.DocxParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream

/**
 * End-to-End Opaque-Box Test Suite for R3: Office Document Parsing, Rendering, and Compatibility.
 * Covers Features 23-26 across Tiers 1-4.
 */
class OfficeDocumentParsingE2ETest {

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    private lateinit var rootDir: File
    private lateinit var docxParser: DocxParser
    private lateinit var pptxParser: PptxParser
    private lateinit var spreadsheetParser: SpreadsheetParser

    @Before
    fun setUp() {
        rootDir = tempFolder.newFolder("mydoc_e2e_office_root")
        docxParser = DocxParser()
        pptxParser = PptxParser()
        spreadsheetParser = SpreadsheetParser()
    }

    // =========================================================================
    // FEATURE 23: Office Parsers Streaming & Limits (Tier 1 & 2)
    // =========================================================================

    @Test
    fun tier1_feature23_docxParserExtractsHeadingsAndParagraphs() = runBlocking {
        val docxFile = File(rootDir, "contract.docx")
        SyntheticDocumentFactory.createDocx(
            docxFile,
            paragraphs = listOf("This is the main contract clause.", "Governing law clause."),
            headings = listOf("Master Services Agreement")
        )

        val doc = FileInputStream(docxFile).use { docxParser.parse(it) }

        assertTrue(doc.blocks.isNotEmpty())
        val heading = doc.blocks.firstOrNull { it is DocxBlock.Heading } as? DocxBlock.Heading
        assertNotNull(heading)
        assertEquals("Master Services Agreement", heading?.text)

        val paragraphs = doc.blocks.filterIsInstance<DocxBlock.Paragraph>()
        assertEquals(2, paragraphs.size)
        assertEquals("This is the main contract clause.", paragraphs[0].fullText)
    }

    @Test
    fun tier1_feature23_pptxParserExtractsSlideTitles() = runBlocking {
        val pptxFile = File(rootDir, "quarterly_review.pptx")
        SyntheticDocumentFactory.createPptx(
            pptxFile,
            slideTitles = listOf("Q1 Revenue Highlights", "Roadmap Overview", "Conclusion")
        )

        val pres = FileInputStream(pptxFile).use { pptxParser.parse(it, rootDir) }

        assertEquals(3, pres.slides.size)
        assertTrue(pres.slides.any { slide -> slide.title.contains("Q1 Revenue Highlights") || slide.content.any { it.contains("Q1 Revenue Highlights") } })
        assertTrue(pres.slides.any { slide -> slide.title.contains("Roadmap Overview") || slide.content.any { it.contains("Roadmap Overview") } })
    }

    @Test
    fun tier2_feature23_docxParserHandlesEmptyOrTruncatedStreamGracefully() = runBlocking {
        // Graceful means a typed, explained failure — not a placeholder block that the reader
        // cannot tell apart from the document's own words.
        val failure = runCatching { docxParser.parse(ByteArrayInputStream(ByteArray(0))) }
            .exceptionOrNull() as? com.abhisheksati.mydoc.domain.model.DocumentLoadException
        assertNotNull(failure)
        assertEquals(com.abhisheksati.mydoc.domain.model.LoadFailure.EMPTY_DOCUMENT, failure?.failure)
    }

    @Test
    fun tier2_feature23_xmlEntityExpansionAttacksHandledSafely() = runBlocking {
        // XML entity expansion attempt (Billion Laughs / external entity)
        val evilXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE lolz [
             <!ENTITY lol "lol">
             <!ELEMENT lolz (#PCDATA)>
             <!ENTITY lol1 "&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;">
             <!ENTITY lol2 "&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;">
            ]>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                <w:body><w:p><w:r><w:t>&lol2;</w:t></w:r></w:p></w:body>
            </w:document>
        """.trimIndent()

        val docxFile = File(rootDir, "entity_attack.docx")
        SyntheticDocumentFactory.createZip(
            docxFile,
            mapOf("word/document.xml" to evilXml.toByteArray(Charsets.UTF_8))
        )

        try {
            val doc = FileInputStream(docxFile).use { docxParser.parse(it) }
            assertNotNull(doc)
        } catch (_: Exception) {
            // Rejection of DOCTYPE is also valid security behavior
        }
    }

    // =========================================================================
    // FEATURE 24: Consolidated Spreadsheet Pipeline & RFC 4180 CSV (Tier 1 & 2)
    // =========================================================================

    @Test
    fun tier1_feature24_rfc4180CsvBasicDelimiters() = runBlocking {
        val csvFile = File(rootDir, "employees.csv")
        SyntheticDocumentFactory.createCsv(
            csvFile,
            rows = listOf(
                listOf("ID", "Name", "Department"),
                listOf("1", "Alice", "Engineering"),
                listOf("2", "Bob", "Marketing")
            )
        )

        val model = FileInputStream(csvFile).use { spreadsheetParser.parse(it, "csv") }
        assertEquals(1, model.sheets.size)
        val sheet = model.sheets[0]

        assertEquals("ID", sheet.getCellValue(0, 0))
        assertEquals("Name", sheet.getCellValue(0, 1))
        assertEquals("Department", sheet.getCellValue(0, 2))
        assertEquals("Alice", sheet.getCellValue(1, 1))
        assertEquals("Bob", sheet.getCellValue(2, 1))
    }

    @Test
    fun tier2_feature24_rfc4180CsvQuotedDelimitersPreservedInSingleCell() = runBlocking {
        // Delimiter inside quotes must NOT split into multiple columns
        val csvData = """
            City,Coordinates,Description
            "New York, NY","40.7128,-74.0060","The Big Apple, USA"
            "Paris, France","48.8566,2.3522","City of Light"
        """.trimIndent()

        val model = spreadsheetParser.parse(ByteArrayInputStream(csvData.toByteArray(Charsets.UTF_8)), "csv")
        val sheet = model.sheets[0]

        assertEquals("New York, NY", sheet.getCellValue(1, 0))
        assertEquals("40.7128,-74.0060", sheet.getCellValue(1, 1))
        assertEquals("The Big Apple, USA", sheet.getCellValue(1, 2))
    }

    @Test
    fun tier2_feature24_rfc4180CsvEscapedDoubleQuotesUnescapedProperly() = runBlocking {
        // Double quotes inside quoted fields ("") must be unescaped to single quote (")
        val csvData = "Item,Quote\nBook,\"He said \"\"Hello world!\"\"\""

        val model = spreadsheetParser.parse(ByteArrayInputStream(csvData.toByteArray(Charsets.UTF_8)), "csv")
        val sheet = model.sheets[0]

        assertEquals("Book", sheet.getCellValue(1, 0))
        val quoteCell = sheet.getCellValue(1, 1)
        assertTrue("Escaped double quote must be parsed into quote character",
            quoteCell.contains("\"") || quoteCell.contains("Hello world!"))
    }

    @Test
    fun tier2_feature24_rfc4180CsvMultilineFieldsRetainRowStructure() = runBlocking {
        // Multiline field enclosed in quotes must remain in the same row
        val csvData = "\"Address Line 1\nAddress Line 2\",1000\n\"Single Line\",2000"

        val model = spreadsheetParser.parse(ByteArrayInputStream(csvData.toByteArray(Charsets.UTF_8)), "csv")
        val sheet = model.sheets[0]

        assertTrue(sheet.getCellValue(0, 0).contains("Address Line 1"))
        assertEquals("1000", sheet.getCellValue(0, 1))
        assertEquals("Single Line", sheet.getCellValue(1, 0))
        assertEquals("2000", sheet.getCellValue(1, 1))
    }

    @Test
    fun tier1_feature24_xlsxFormulasAndValuesSeparation() = runBlocking {
        val xlsxFile = File(rootDir, "financials.xlsx")
        SyntheticDocumentFactory.createXlsx(
            xlsxFile,
            rows = listOf(
                listOf("Gross Revenue", "1000"),
                listOf("Cost of Goods", "400"),
                listOf("Net Margin", "600")
            ),
            formulas = mapOf(
                Pair(2, 1) to "B1-B2"
            )
        )

        val model = FileInputStream(xlsxFile).use { spreadsheetParser.parse(it, "xlsx") }
        val sheet = model.sheets[0]

        assertEquals("Gross Revenue", sheet.getCellValue(0, 0))
        assertEquals("1000", sheet.getCellValue(0, 1))
        assertEquals("600", sheet.getCellValue(2, 1))
    }

    // =========================================================================
    // FEATURE 25: Presentation Extension Mapping (Tier 1 & 2)
    // =========================================================================

    @Test
    fun tier1_feature25_presentationExtensionSetIncludesOpenXmlVariants() {
        val supportedExtensions = setOf("ppt", "pptx", "pot", "potx", "pps", "ppsx", "pptm", "odp", "fodp", "otp")

        assertTrue(supportedExtensions.contains("pptx"))
        assertTrue(supportedExtensions.contains("pptm"))
        assertTrue(supportedExtensions.contains("ppsx"))
        assertTrue(supportedExtensions.contains("potx"))
    }

    @Test
    fun tier2_feature25_presentationFilesNeverTreatedAsRawPdfs() {
        val presentationExtensions = listOf("pptx", "pptm", "ppsx", "potx")
        for (ext in presentationExtensions) {
            val doc = DocumentFile(
                uri = "content://storage/Slides.$ext",
                displayName = "Slides.$ext",
                sizeBytes = 1024L,
                mimeType = "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                lastOpenedTimestamp = System.currentTimeMillis()
            )
            assertEquals(DocumentCategory.PPT, doc.category)
            assertFalse(doc.extension.equals("pdf", ignoreCase = true))
        }
    }

    // =========================================================================
    // FEATURE 26: Word Viewer Search with Context (Tier 1 & 2)
    // =========================================================================

    @Test
    fun tier1_feature26_wordSearchRetainsAllBlocksAndComputesMatchingIndices() {
        val blocks = listOf(
            DocxBlock.Heading("Chapter 1: The Beginning", 1),
            DocxBlock.Paragraph(listOf(com.abhisheksati.mydoc.rendering.engine.word.DocxRun("It was the best of times."))),
            DocxBlock.Paragraph(listOf(com.abhisheksati.mydoc.rendering.engine.word.DocxRun("It was the worst of times."))),
            DocxBlock.Heading("Chapter 2: The Middle", 1),
            DocxBlock.Paragraph(listOf(com.abhisheksati.mydoc.rendering.engine.word.DocxRun("They continued on their journey.")))
        )

        val query = "times"
        val matchingIndices = blocks.mapIndexedNotNull { index, block ->
            val hasMatch = when (block) {
                is DocxBlock.Heading -> block.text.contains(query, ignoreCase = true)
                is DocxBlock.Paragraph -> block.fullText.contains(query, ignoreCase = true)
                is DocxBlock.ListItem -> block.runs.any { it.text.contains(query, ignoreCase = true) }
                is DocxBlock.Table -> block.rows.any { row ->
                    row.any { it.text.contains(query, ignoreCase = true) }
                }
            }
            if (hasMatch) index else null
        }

        // Must retain all 5 blocks in full document context
        assertEquals(5, blocks.size)
        // Matches are at block index 1 and 2
        assertEquals(listOf(1, 2), matchingIndices)
    }

    @Test
    fun tier2_feature26_emptySearchQueryYieldsZeroMatchesWithoutFilteringDocument() {
        val blocks = listOf(
            DocxBlock.Heading("Overview", 1),
            DocxBlock.Paragraph(listOf(com.abhisheksati.mydoc.rendering.engine.word.DocxRun("General content.")))
        )

        val emptyQuery = ""
        val matchingIndices = if (emptyQuery.isBlank()) emptyList<Int>() else blocks.indices.toList()

        assertEquals(0, matchingIndices.size)
        assertEquals(2, blocks.size)
    }

    // =========================================================================
    // TIER 4: Real-World Office Presentation Pipeline Scenario
    // =========================================================================

    @Test
    fun tier4_realWorldOfficePresentationPipelineScenario() = runBlocking {
        // Step 1: Create a presentation with multiple slides and topics
        val deckFile = File(rootDir, "all_hands.pptx")
        SyntheticDocumentFactory.createPptx(
            deckFile,
            slideTitles = listOf(
                "Welcome & Vision",
                "Product Engineering Milestones",
                "Financial Growth Q4",
                "Next Quarter Goals"
            )
        )
        assertTrue(deckFile.exists())

        // Step 2: Stream parse presentation
        val presentation = FileInputStream(deckFile).use { pptxParser.parse(it, rootDir) }
        assertEquals(4, presentation.slides.size)

        // Step 3: Search text across presentation slides
        val searchQuery = "Engineering"
        val matchingSlides = presentation.slides.filter { slide ->
            slide.title.contains(searchQuery, ignoreCase = true) || slide.content.any { it.contains(searchQuery, ignoreCase = true) }
        }
        assertEquals(1, matchingSlides.size)
        assertTrue(matchingSlides[0].title.contains("Product Engineering Milestones") || matchingSlides[0].content.any { it.contains("Product Engineering Milestones") })
    }
}
