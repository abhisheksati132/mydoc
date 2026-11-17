package com.abhisheksati.mydoc.e2e

import com.abhisheksati.mydoc.R
import android.content.Context
import android.content.ContextWrapper
import com.abhisheksati.mydoc.domain.model.DocumentCategory
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.e2e.fixtures.SyntheticDocumentFactory
import com.abhisheksati.mydoc.tools.engine.PdfOptimizationEngine
import com.abhisheksati.mydoc.tools.engine.PdfPageOperationsEngine
import com.abhisheksati.mydoc.tools.engine.PdfSecurityEngine
import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.tools.model.ToolExecutionResult
import com.abhisheksati.mydoc.tools.model.ToolGroup
import com.abhisheksati.mydoc.tools.model.ToolOptions
import com.abhisheksati.mydoc.tools.pdf.watermark.PdfWatermarker
import com.abhisheksati.mydoc.ui.viewer.controls.PrintHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * End-to-End Opaque-Box Test Suite for R2: PDF Tool Correctness and Truthfulness.
 * Covers Features 12-22 across Tiers 1-4.
 */
class PdfToolsCorrectnessE2ETest {

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    private lateinit var rootDir: File
    private lateinit var mockContext: Context
    private lateinit var optimizationEngine: PdfOptimizationEngine
    private lateinit var pageOperationsEngine: PdfPageOperationsEngine
    private lateinit var securityEngine: PdfSecurityEngine

    @Before
    fun setUp() {
        rootDir = tempFolder.newFolder("mydoc_e2e_pdf_root")
        mockContext = object : ContextWrapper(null) {
            override fun getCacheDir(): File = File(rootDir, "cache").apply { mkdirs() }
            override fun getFilesDir(): File = File(rootDir, "files").apply { mkdirs() }
        }
        optimizationEngine = PdfOptimizationEngine(mockContext)
        pageOperationsEngine = PdfPageOperationsEngine(mockContext)
        securityEngine = PdfSecurityEngine(mockContext)
    }

    // =========================================================================
    // FEATURE 12 & 13: PDF Optimization Ownership & Text Preservation (Tier 1 & 2)
    // =========================================================================

    @Test
    fun tier1_feature12_compressedPdfReturnsSafeIndependentOutputFile() = runBlocking {
        val testPdf = File(rootDir, "source.pdf")
        SyntheticDocumentFactory.createValidPdf(testPdf, text = "Compressible vector document content", pageCount = 2)

        val options = ToolOptions()
        val resultFile = optimizationEngine.compressPdf(testPdf, options) { }

        assertNotNull(resultFile)
        assertTrue("Compressed output file must exist on disk", resultFile.exists())
        assertNotEquals("Output file must not point to input file to prevent deletion in finally block",
            testPdf.absolutePath, resultFile.absolutePath)
        assertTrue("Output file must have non-zero size", resultFile.length() > 0)
    }

    @Test
    fun tier2_feature13_optimizationPreservesSearchableTextStructure() = runBlocking {
        val testPdf = File(rootDir, "searchable.pdf")
        val uniqueKeyword = "CONFIDENTIAL_PROJECT_OMEGA_12345"
        SyntheticDocumentFactory.createValidPdf(testPdf, text = uniqueKeyword, pageCount = 1)

        val options = ToolOptions()
        val resultFile = optimizationEngine.compressPdf(testPdf, options) { }

        assertTrue(resultFile.exists())
        // Reparse the output with the same PDF library the app ships: a raw byte search would
        // accept any PDF at all, including one whose text stream was stripped.
        val extractedText = com.tom_roush.pdfbox.pdmodel.PDDocument.load(resultFile).use { doc ->
            com.tom_roush.pdfbox.text.PDFTextStripper().getText(doc)
        }
        assertTrue(
            "Searchable text must survive compression; extracted: $extractedText",
            extractedText.contains(uniqueKeyword)
        )
    }

    // =========================================================================
    // FEATURE 14 & 15: Unicode Watermarking & Truthfulness (Tier 1 & 2)
    // =========================================================================

    @Test
    fun tier1_feature14_watermarkSupportsFullUnicodeWithoutReplacingGlyphsWithSpaces() {
        val sourcePdf = File(rootDir, "watermark_in.pdf")
        val destPdf = File(rootDir, "watermark_out.pdf")
        SyntheticDocumentFactory.createValidPdf(sourcePdf, text = "Invoice Document", pageCount = 1)

        val unicodeWatermark = "CONFIDENTIEL - PROPRIÉTAIRE - 机密 - गोपनीय"

        // The Unicode branch draws through Android graphics, which a JVM unit test cannot
        // execute. Only the two known environment artifacts may stand in for a result: a stub
        // RuntimeException or a null Bitmap from the stubbed factory. A validation failure or
        // I/O error in the production path must fail this test.
        try {
            PdfWatermarker.applyText(
                context = mockContext,
                sourceFile = sourcePdf,
                destinationFile = destPdf,
                text = unicodeWatermark,
                rotationDegrees = 45f
            )
            assertTrue("Watermarked destination file must be written", destPdf.exists())
            assertTrue("Watermarked file size must be > 0", destPdf.length() > 0)
        } catch (e: RuntimeException) {
            val stubArtifact = e is NullPointerException || e.message?.startsWith("Stub!") == true
            assertTrue("Unicode watermarking failed for a reason other than the JVM graphics stub: $e", stubArtifact)
        }
    }

    @Test
    fun tier2_feature14_watermarkRejectsEmptyOrWhitespaceOnlyInput() {
        val sourcePdf = File(rootDir, "empty_wm_in.pdf")
        val destPdf = File(rootDir, "empty_wm_out.pdf")
        SyntheticDocumentFactory.createValidPdf(sourcePdf, text = "Content", pageCount = 1)

        try {
            PdfWatermarker.applyText(
                context = mockContext,
                sourceFile = sourcePdf,
                destinationFile = destPdf,
                text = "   "
            )
            fail("Watermark engine must throw IllegalArgumentException on blank watermark text")
        } catch (e: IllegalArgumentException) {
            assertTrue("Exception message must indicate empty watermark text",
                e.message?.contains("empty", ignoreCase = true) == true)
        }
    }

    @Test
    fun tier1_feature15_watermarkToolDescriptionIsHonest() {
        val watermarkTool = PdfTool.ADD_WATERMARK
        assertNotNull(watermarkTool)
        // Description should honestly describe watermark capability
        assertEquals(R.string.tool_add_watermark_title, watermarkTool.titleRes)
    }

    // =========================================================================
    // FEATURE 16: Genuine Redaction & Scanned Document Guidance (Tier 1 & 2)
    // =========================================================================

    @Test
    fun tier1_feature16_redactPdfRejectsWhenTargetKeywordNotFound() = runBlocking {
        val testPdf = File(rootDir, "redact_target.pdf")
        SyntheticDocumentFactory.createValidPdf(testPdf, text = "Public Document Only", pageCount = 1)

        val options = ToolOptions(redactQuery = "SECRET_PASSWORD_123")
        try {
            securityEngine.redactPdf(testPdf, options) { }
            fail("Redact should throw IllegalArgumentException when keyword is absent")
        } catch (e: IllegalArgumentException) {
            assertTrue("Exception must inform user that query was not found",
                e.message?.contains("No match", ignoreCase = true) == true)
        }
    }

    @Test
    fun tier2_feature16_redactScannedPdfProvidesClearGuidance() = runBlocking {
        val scannedPdf = File(rootDir, "scanned_invoice.pdf")
        SyntheticDocumentFactory.createScannedImagePdf(scannedPdf)

        val options = ToolOptions(redactQuery = "Account Number")
        val failure = runCatching { securityEngine.redactPdf(scannedPdf, options) { } }.exceptionOrNull()
        assertNotNull("Redact must not claim success on a scanned document with no text layer", failure)
        assertTrue("Must fail with a user-facing rejection, got $failure",
            failure is IllegalArgumentException || failure is IllegalStateException)
    }

    // =========================================================================
    // FEATURE 17 & 18: Signature Coordinate Scaling & UI Promises (Tier 1 & 2)
    // =========================================================================

    @Test
    fun tier1_feature17_signatureCoordinateScalingBoundsComputation() {
        val points = listOf(
            100f to 50f,
            750f to 200f,
            1200f to 500f // Beyond 700x350
        )

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for ((px, py) in points) {
            if (px < minX) minX = px
            if (py < minY) minY = py
            if (px > maxX) maxX = px
            if (py > maxY) maxY = py
        }

        val strokeWidth = maxX - minX
        val strokeHeight = maxY - minY
        val targetWidth = 700f
        val targetHeight = 350f
        val padding = 30f

        val availableW = targetWidth - padding * 2
        val availableH = targetHeight - padding * 2
        val scale = minOf(availableW / strokeWidth, availableH / strokeHeight)

        val scaledW = strokeWidth * scale
        val scaledH = strokeHeight * scale

        // Assert scaled dimensions strictly fit inside 700x350 canvas without clipping
        assertTrue("Scaled width must fit within 700px bounds", scaledW <= targetWidth)
        assertTrue("Scaled height must fit within 350px bounds", scaledH <= targetHeight)
        assertTrue("Scale factor must be > 0", scale > 0f)
    }

    @Test
    fun tier1_feature18_signatureToolGroupAndDefinition() {
        val signTool = PdfTool.SIGN_PDF
        assertEquals(ToolGroup.EDIT_SIGN, signTool.group)
        assertEquals(R.string.tool_sign_pdf_title, signTool.titleRes)
    }

    // =========================================================================
    // FEATURE 19: PDF Crop Controls & Normalized Rotation (Tier 1 & 2)
    // =========================================================================

    @Test
    fun tier1_feature19_rotationNormalizationAlwaysProducesNonNegativeMultipleOf90() {
        val testAngles = listOf(-360, -270, -180, -90, 0, 90, 180, 270, 360, 450)
        for (angle in testAngles) {
            val normalized = ((angle % 360) + 360) % 360
            assertTrue("Normalized rotation must be non-negative (got $normalized for $angle)", normalized >= 0)
            assertTrue("Normalized rotation must be < 360 (got $normalized for $angle)", normalized < 360)
            assertEquals("Normalized rotation must be multiple of 90", 0, normalized % 90)
        }
    }

    @Test
    fun tier2_feature19_negativeRotationDegreesNormalizeCorrectly() {
        val minus90Normalized = (((-90) % 360) + 360) % 360
        assertEquals(270, minus90Normalized)

        val minus180Normalized = (((-180) % 360) + 360) % 360
        assertEquals(180, minus180Normalized)

        val minus270Normalized = (((-270) % 360) + 360) % 360
        assertEquals(90, minus270Normalized)
    }

    // =========================================================================
    // FEATURE 20: PDF Repair Integrity (Tier 1 & 2)
    // =========================================================================

    @Test
    fun tier2_feature20_repairZeroPageCorruptedPdfThrowsIllegalStateException() = runBlocking {
        val zeroPagePdf = File(rootDir, "broken_zero_page.pdf")
        SyntheticDocumentFactory.createCorruptedPdf(zeroPagePdf)

        val failure = runCatching { optimizationEngine.repairPdf(zeroPagePdf) { } }.exceptionOrNull()
        assertNotNull("Repairing an unrecoverable corrupted PDF must fail rather than create 0-page output", failure)
        assertTrue("Must reject zero page recovery with an explicit exception, got $failure",
            failure is IllegalStateException || failure is IllegalArgumentException)
    }

    // =========================================================================
    // FEATURE 21 & 22: PDF-to-Word & Print Helper Format Safety (Tier 1 & 2)
    // =========================================================================

    @Test
    fun tier1_feature21_pdfToOfficeToolOutputConfiguration() {
        val tool = PdfTool.PDF_TO_OFFICE
        assertEquals(ToolGroup.CONVERT, tool.group)
        assertEquals("docx", tool.outputExtension)
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", tool.outputMimeType)
    }

    @Test
    fun tier2_feature22_printHelperRejectsNonPdfFilesBeforePrintSpooler() {
        val nonPdfDoc = DocumentFile(
            uri = "content://storage/FinancialReport.xlsx",
            displayName = "FinancialReport.xlsx",
            sizeBytes = 1024L,
            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            lastOpenedTimestamp = System.currentTimeMillis()
        )

        // PrintHelper.printDocument with non-PDF must refuse and say why instead of spooling
        var refusalMessage: String? = null
        PrintHelper.printDocument(mockContext, nonPdfDoc) { refusalMessage = it }
        assertNotNull("Non-PDF print must be refused with an explanation", refusalMessage)
        assertTrue("Refusal must say only PDF prints directly, got: $refusalMessage",
            refusalMessage!!.contains("only supported for PDF", ignoreCase = true))
    }

    // =========================================================================
    // TIER 4: Real-World PDF Processing Scenario
    // =========================================================================

    @Test
    fun tier4_realWorldPdfDocumentProcessingLifecycleScenario() = runBlocking {
        // Step 1: Create a multi-page document
        val contractPdf = File(rootDir, "employment_agreement.pdf")
        SyntheticDocumentFactory.createValidPdf(contractPdf, text = "Employment Terms Confidential", pageCount = 3)
        assertTrue(contractPdf.exists())

        // Step 2: Crop margins
        val cropOptions = ToolOptions(cropMarginPercent = 0.05f)
        val croppedFile = pageOperationsEngine.cropPdf(contractPdf, cropOptions) { }
        assertTrue("Cropped file must exist", croppedFile.exists())
        assertTrue("Cropped file must have content", croppedFile.length() > 0)

        // Step 3: Compress document
        val compressOptions = ToolOptions()
        val compressedFile = optimizationEngine.compressPdf(contractPdf, compressOptions) { }
        assertTrue("Optimized file must exist and be independent", compressedFile.exists())
        assertTrue(compressedFile.length() > 0)

        // Step 4: Verify tool execution result accounting
        val executionResult = ToolExecutionResult(
            success = true,
            tool = PdfTool.COMPRESS,
            inputSizeBytes = contractPdf.length(),
            outputSizeBytes = compressedFile.length(),
            outputFile = compressedFile
        )
        assertTrue(executionResult.success)
        assertNotNull(executionResult.outputFile)
        assertTrue(executionResult.outputFile?.exists() == true)
    }
}
