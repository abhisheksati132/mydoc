package com.abhisheksati.mydoc.tools.model

import android.net.Uri
import java.io.File

/**
 * Result of executing a PDF tool.
 * Captures before/after sizes and pages for verification.
 */
data class ToolExecutionResult(
    val success: Boolean,
    val tool: PdfTool,
    val inputSizeBytes: Long = 0L,
    val outputSizeBytes: Long = 0L,
    val inputPages: Int = 0,
    val outputPages: Int = 0,
    val outputUri: Uri? = null,
    val outputFile: File? = null,
    val warningNote: String? = null,
    val errorMessage: String? = null,
    val durationMs: Long = 0L
) {
    val savingsBytes: Long get() = (inputSizeBytes - outputSizeBytes).coerceAtLeast(0L)
    val savingsPercent: Float get() = if (inputSizeBytes > 0) {
        ((inputSizeBytes - outputSizeBytes).toFloat() / inputSizeBytes.toFloat()) * 100f
    } else 0f
}

fun formatByteSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(java.util.Locale.US, "%.1f GB", gb)
        mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(java.util.Locale.US, "%.1f KB", kb)
        else -> "$bytes B"
    }
}

/**
 * Real-time progress update for background processing.
 */
data class ToolProgress(
    val currentStep: Int = 0,
    val totalSteps: Int = 1,
    val percent: Int = 0,
    val message: String = "",
    val isIndeterminate: Boolean = false
)

enum class CompressionLevel(val label: String, val description: String) {
    LIGHT("Light", "Removes unused objects and flattens metadata."),
    BALANCED("Balanced", "Recommended. Optimizes image streams (150 DPI) with minimal visual loss."),
    STRONG("Strong", "Maximum compression (100 DPI). Best for email attachments.")
}

enum class PageSizeFormat(val label: String, val widthPt: Float, val heightPt: Float) {
    A4("A4 (210 × 297 mm)", 595.28f, 841.89f),
    LETTER("Letter (8.5 × 11 in)", 612f, 792f),
    FIT_IMAGE("Fit to Image", 0f, 0f)
}

enum class PageMargin(val label: String, val marginPt: Float) {
    NONE("None", 0f),
    SMALL("Small (8 pt)", 8f),
    MEDIUM("Medium (24 pt)", 24f),
    LARGE("Large (36 pt)", 36f)
}

enum class PageNumberPosition(val label: String) {
    BOTTOM_CENTER("Bottom Center"),
    BOTTOM_RIGHT("Bottom Right"),
    BOTTOM_LEFT("Bottom Left"),
    TOP_CENTER("Top Center"),
    TOP_RIGHT("Top Right")
}

enum class PageNumberFormat(val label: String) {
    PAGE_X_OF_Y("Page {n} of {total}"),
    X_OF_Y("{n} / {total}"),
    ONLY_X("{n}")
}

enum class WorkflowPresetType(val title: String, val description: String, val stepsSummary: String) {
    DOCUMENT_CLEANUP(
        "Document Cleanup",
        "Rebuilds structure, purges tracking metadata, and cleans unused PDF objects.",
        "Repair → Strip Metadata → Compress (Balanced)"
    ),
    CONFIDENTIAL_SHARE(
        "Confidential Share",
        "Applies diagonal confidentiality watermark, sanitizes metadata, and encrypts with AES-256.",
        "Watermark → Strip Metadata → Protect (AES-256)"
    ),
    ARCHIVE_PREPARE(
        "Archive Ready",
        "Strips metadata and converts document into PDF/A-style format for long-term storage.",
        "Strip Metadata → PDF/A Conversion"
    )
}

/**
 * Universal options configuration passed to tool execution.
 */
data class ToolOptions(
    // Organize
    val pageRange: String = "",
    val splitEveryNPages: Int = 1,
    val rotateDegrees: Int = 90,
    val cropMarginPercent: Float = 0.1f,
    val reorderedPageIndices: List<Int> = emptyList(),

    // Optimize
    val compressionLevel: CompressionLevel = CompressionLevel.BALANCED,

    // Convert
    val pageSizeFormat: PageSizeFormat = PageSizeFormat.A4,
    val pageMargin: PageMargin = PageMargin.NONE,
    val exportDpi: Int = 150,
    val exportFormatJpg: Boolean = true,
    val exportAsLongImage: Boolean = false,

    // Edit
    val pageNumberPosition: PageNumberPosition = PageNumberPosition.BOTTOM_CENTER,
    val pageNumberFormat: PageNumberFormat = PageNumberFormat.PAGE_X_OF_Y,
    val pageNumberStart: Int = 1,
    val watermarkText: String = "CONFIDENTIAL",
    val watermarkOpacity: Float = 0.25f,
    val watermarkRotation: Float = -45f,
    val flattenForm: Boolean = true,
    val formFieldValues: Map<String, String> = emptyMap(),
    val signatureBitmapFile: File? = null,
    val signaturePage: Int = 0,
    val signatureXPercent: Float = 0.5f,
    val signatureYPercent: Float = 0.8f,
    val signatureScale: Float = 0.3f,

    // Security
    val userPassword: String = "",
    val ownerPassword: String = "",
    val allowPrinting: Boolean = true,
    val allowCopying: Boolean = true,
    val redactQuery: String = "",

    // Automation
    val workflowPreset: WorkflowPresetType = WorkflowPresetType.DOCUMENT_CLEANUP
)
