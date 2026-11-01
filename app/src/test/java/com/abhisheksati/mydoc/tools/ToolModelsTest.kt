package com.abhisheksati.mydoc.tools

import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.tools.model.ToolExecutionResult
import com.abhisheksati.mydoc.tools.model.formatByteSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolModelsTest {

    @Test
    fun formatByteSize_zeroAndNegative_returnsZeroB() {
        assertEquals("0 B", formatByteSize(0L))
        assertEquals("0 B", formatByteSize(-1024L))
    }

    @Test
    fun formatByteSize_bytes_formatsProperly() {
        assertEquals("500 B", formatByteSize(500L))
        assertEquals("1023 B", formatByteSize(1023L))
    }

    @Test
    fun formatByteSize_kilobytes_formatsProperly() {
        assertEquals("1.0 KB", formatByteSize(1024L))
        assertEquals("1.5 KB", formatByteSize(1536L))
    }

    @Test
    fun formatByteSize_megabytes_formatsProperly() {
        assertEquals("1.0 MB", formatByteSize(1024L * 1024L))
        assertEquals("5.2 MB", formatByteSize((5.2 * 1024L * 1024L).toLong()))
    }

    @Test
    fun formatByteSize_gigabytes_formatsProperly() {
        assertEquals("1.0 GB", formatByteSize(1024L * 1024L * 1024L))
        assertEquals("2.5 GB", formatByteSize((2.5 * 1024L * 1024L * 1024L).toLong()))
    }

    @Test
    fun toolExecutionResult_savingsCalculations_handlesPositiveSavings() {
        val result = ToolExecutionResult(
            success = true,
            tool = PdfTool.COMPRESS,
            inputSizeBytes = 1000L,
            outputSizeBytes = 400L
        )
        assertEquals(600L, result.savingsBytes)
        assertEquals(60.0f, result.savingsPercent, 0.001f)
    }

    @Test
    fun toolExecutionResult_savingsCalculations_handlesNegativeSavingsGracefully() {
        val result = ToolExecutionResult(
            success = true,
            tool = PdfTool.COMPRESS,
            inputSizeBytes = 500L,
            outputSizeBytes = 600L
        )
        assertEquals(0L, result.savingsBytes)
        // savingsPercent will be negative when output is larger than input
        assertTrue(result.savingsPercent < 0f)
    }

    @Test
    fun toolExecutionResult_savingsCalculations_handlesZeroInputSize() {
        val result = ToolExecutionResult(
            success = true,
            tool = PdfTool.IMAGE_TO_PDF,
            inputSizeBytes = 0L,
            outputSizeBytes = 1000L
        )
        assertEquals(0L, result.savingsBytes)
        assertEquals(0f, result.savingsPercent, 0.001f)
    }
}
