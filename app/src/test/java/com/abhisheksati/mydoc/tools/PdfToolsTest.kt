package com.abhisheksati.mydoc.tools

import com.abhisheksati.mydoc.tools.model.CompressionLevel
import com.abhisheksati.mydoc.tools.model.PageMargin
import com.abhisheksati.mydoc.tools.model.PageNumberFormat
import com.abhisheksati.mydoc.tools.model.PageNumberPosition
import com.abhisheksati.mydoc.tools.model.PageSizeFormat
import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.tools.model.ToolExecutionResult
import com.abhisheksati.mydoc.tools.model.ToolGroup
import com.abhisheksati.mydoc.tools.model.ToolOptions
import com.abhisheksati.mydoc.tools.model.WorkflowPresetType
import com.abhisheksati.mydoc.tools.model.formatByteSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfToolsTest {

    @Test
    fun verifyAll30ToolsPresentAcross8Groups() {
        val tools = PdfTool.entries
        assertEquals(30, tools.size)

        val groups = tools.groupBy { it.group }
        assertEquals(8, groups.size)

        // 1. Organize (7 tools)
        assertEquals(7, groups[ToolGroup.ORGANIZE]?.size)
        assertTrue(tools.contains(PdfTool.MERGE))
        assertTrue(tools.contains(PdfTool.SPLIT))
        assertTrue(tools.contains(PdfTool.REMOVE_PAGES))
        assertTrue(tools.contains(PdfTool.REORDER_PAGES))
        assertTrue(tools.contains(PdfTool.EXTRACT_PAGES))
        assertTrue(tools.contains(PdfTool.ROTATE))
        assertTrue(tools.contains(PdfTool.CROP))

        // 2. Optimize (2 tools)
        assertEquals(2, groups[ToolGroup.OPTIMIZE]?.size)
        assertTrue(tools.contains(PdfTool.COMPRESS))
        assertTrue(tools.contains(PdfTool.REPAIR))

        // 3. Convert To (3 tools)
        assertEquals(3, groups[ToolGroup.CONVERT_TO]?.size)
        assertTrue(tools.contains(PdfTool.IMAGE_TO_PDF))
        assertTrue(tools.contains(PdfTool.OFFICE_TO_PDF))
        assertTrue(tools.contains(PdfTool.TEXT_TO_PDF))

        // 4. Convert From (5 tools)
        assertEquals(5, groups[ToolGroup.CONVERT_FROM]?.size)
        assertTrue(tools.contains(PdfTool.PDF_TO_IMAGES))
        assertTrue(tools.contains(PdfTool.PDF_TO_TEXT_MD))
        assertTrue(tools.contains(PdfTool.PDF_TO_OFFICE))
        assertTrue(tools.contains(PdfTool.PDF_TO_PDFA))
        assertTrue(tools.contains(PdfTool.EXTRACT_ASSETS))

        // 5. Edit (6 tools)
        assertEquals(6, groups[ToolGroup.EDIT]?.size)
        assertTrue(tools.contains(PdfTool.ANNOTATE))
        assertTrue(tools.contains(PdfTool.ADD_PAGE_NUMBERS))
        assertTrue(tools.contains(PdfTool.ADD_WATERMARK))
        assertTrue(tools.contains(PdfTool.FILL_FORM))
        assertTrue(tools.contains(PdfTool.SIGN_PDF))
        assertTrue(tools.contains(PdfTool.COMPARE_PDF))

        // 6. Security (4 tools)
        assertEquals(4, groups[ToolGroup.SECURITY]?.size)
        assertTrue(tools.contains(PdfTool.PROTECT))
        assertTrue(tools.contains(PdfTool.UNLOCK))
        assertTrue(tools.contains(PdfTool.REDACT))
        assertTrue(tools.contains(PdfTool.STRIP_METADATA))

        // 7. Scan & OCR (2 tools)
        assertEquals(2, groups[ToolGroup.SCAN]?.size)
        assertTrue(tools.contains(PdfTool.SCAN_TO_PDF))
        assertTrue(tools.contains(PdfTool.OCR_PDF))

        // 8. Automation (1 tool)
        assertEquals(1, groups[ToolGroup.AUTOMATION]?.size)
        assertTrue(tools.contains(PdfTool.WORKFLOW))
    }

    @Test
    fun toolExecutionResult_calculatesSavingsCorrectly() {
        val result = ToolExecutionResult(
            success = true,
            tool = PdfTool.COMPRESS,
            inputSizeBytes = 2_000_000L,
            outputSizeBytes = 1_000_000L,
            inputPages = 10,
            outputPages = 10
        )

        assertEquals(1_000_000L, result.savingsBytes)
        assertEquals(50f, result.savingsPercent, 0.01f)
    }

    @Test
    fun toolExecutionResult_handlesZeroInputGracefully() {
        val result = ToolExecutionResult(
            success = true,
            tool = PdfTool.MERGE,
            inputSizeBytes = 0L,
            outputSizeBytes = 0L
        )

        assertEquals(0L, result.savingsBytes)
        assertEquals(0f, result.savingsPercent, 0.01f)
    }

    @Test
    fun formatByteSize_formatsUnitsAccurately() {
        assertEquals("0 B", formatByteSize(0L))
        assertEquals("500 B", formatByteSize(500L))
        assertEquals("1.0 KB", formatByteSize(1024L))
        assertEquals("2.5 MB", formatByteSize((2.5 * 1024 * 1024).toLong()))
        assertEquals("1.5 GB", formatByteSize((1.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun toolOptions_defaultsAreValid() {
        val options = ToolOptions()
        assertEquals(CompressionLevel.BALANCED, options.compressionLevel)
        assertEquals(PageSizeFormat.A4, options.pageSizeFormat)
        assertEquals(PageMargin.NONE, options.pageMargin)
        assertEquals(PageNumberPosition.BOTTOM_CENTER, options.pageNumberPosition)
        assertEquals(PageNumberFormat.PAGE_X_OF_Y, options.pageNumberFormat)
        assertEquals(1, options.pageNumberStart)
        assertEquals("CONFIDENTIAL", options.watermarkText)
        assertEquals(-45f, options.watermarkRotation, 0.01f)
        assertEquals(WorkflowPresetType.DOCUMENT_CLEANUP, options.workflowPreset)
    }
}
