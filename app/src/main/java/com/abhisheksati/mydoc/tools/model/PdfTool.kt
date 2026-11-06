package com.abhisheksati.mydoc.tools.model

import com.abhisheksati.mydoc.R
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.Grading
import androidx.compose.material.icons.automirrored.outlined.MergeType
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.FormatColorText
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Highlight
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 6 primary task groups for PDF tools:
 * 1. Organize (Merge, Split, Remove, Reorder, Extract, Rotate, Crop)
 * 2. Edit & Sign (Annotate, Page Numbers, Watermark, Sign)
 * 3. Compress (Compress PDF, Repair PDF)
 * 4. Convert (Image/Text to PDF, PDF to Images/Text/Office, Extract Assets)
 * 5. Secure (Protect, Unlock, Redact, Strip Metadata)
 * 6. Scan (Scan to PDF)
 */
enum class ToolGroup(val title: String) {
    ORGANIZE("Organize"),
    EDIT_SIGN("Edit & Sign"),
    COMPRESS("Compress"),
    CONVERT("Convert"),
    SECURE("Secure"),
    SCAN("Scan");

    companion object {
        // Backward-compatibility aliases
        val OPTIMIZE = COMPRESS
        val CONVERT_TO = CONVERT
        val CONVERT_FROM = CONVERT
        val EDIT = EDIT_SIGN
        val SECURITY = SECURE
    }
}

enum class PdfTool(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val group: ToolGroup,
    val icon: ImageVector,
    val defaultSuffix: String,
    val outputExtension: String = "pdf",
    val outputMimeType: String = "application/pdf",
    val requiresMultipleInputFiles: Boolean = false,
    val acceptsNonPdfInput: Boolean = false
) {
    // 1. Organize (7 tools)
    MERGE(
        id = "merge",
        titleRes = R.string.tool_merge_title,
        descriptionRes = R.string.tool_merge_desc,
        group = ToolGroup.ORGANIZE,
        icon = Icons.AutoMirrored.Outlined.MergeType,
        defaultSuffix = "merged",
        requiresMultipleInputFiles = true
    ),
    SPLIT(
        id = "split",
        titleRes = R.string.tool_split_title,
        descriptionRes = R.string.tool_split_desc,
        group = ToolGroup.ORGANIZE,
        icon = Icons.AutoMirrored.Outlined.CallSplit,
        defaultSuffix = "split"
    ),
    REMOVE_PAGES(
        id = "remove_pages",
        titleRes = R.string.tool_remove_pages_title,
        descriptionRes = R.string.tool_remove_pages_desc,
        group = ToolGroup.ORGANIZE,
        icon = Icons.Outlined.Delete,
        defaultSuffix = "trimmed"
    ),
    REORDER_PAGES(
        id = "reorder_pages",
        titleRes = R.string.tool_reorder_pages_title,
        descriptionRes = R.string.tool_reorder_pages_desc,
        group = ToolGroup.ORGANIZE,
        icon = Icons.Outlined.GridOn,
        defaultSuffix = "organized"
    ),
    EXTRACT_PAGES(
        id = "extract_pages",
        titleRes = R.string.tool_extract_pages_title,
        descriptionRes = R.string.tool_extract_pages_desc,
        group = ToolGroup.ORGANIZE,
        icon = Icons.Outlined.Extension,
        defaultSuffix = "extracted"
    ),
    ROTATE(
        id = "rotate",
        titleRes = R.string.tool_rotate_title,
        descriptionRes = R.string.tool_rotate_desc,
        group = ToolGroup.ORGANIZE,
        icon = Icons.AutoMirrored.Outlined.RotateRight,
        defaultSuffix = "rotated"
    ),
    CROP(
        id = "crop",
        titleRes = R.string.tool_crop_title,
        descriptionRes = R.string.tool_crop_desc,
        group = ToolGroup.ORGANIZE,
        icon = Icons.Outlined.Crop,
        defaultSuffix = "cropped"
    ),

    // 2. Compress (2 tools)
    COMPRESS(
        id = "compress",
        titleRes = R.string.tool_compress_title,
        descriptionRes = R.string.tool_compress_desc,
        group = ToolGroup.COMPRESS,
        icon = Icons.Outlined.Compress,
        defaultSuffix = "compressed"
    ),
    REPAIR(
        id = "repair",
        titleRes = R.string.tool_repair_title,
        descriptionRes = R.string.tool_repair_desc,
        group = ToolGroup.COMPRESS,
        icon = Icons.Outlined.Build,
        defaultSuffix = "repaired"
    ),

    // 3. Convert (6 tools: Convert To + Convert From)
    IMAGE_TO_PDF(
        id = "image_to_pdf",
        titleRes = R.string.tool_image_to_pdf_title,
        descriptionRes = R.string.tool_image_to_pdf_desc,
        group = ToolGroup.CONVERT,
        icon = Icons.Outlined.Image,
        defaultSuffix = "from_image",
        acceptsNonPdfInput = true
    ),
    TEXT_TO_PDF(
        id = "text_to_pdf",
        titleRes = R.string.tool_text_to_pdf_title,
        descriptionRes = R.string.tool_text_to_pdf_desc,
        group = ToolGroup.CONVERT,
        icon = Icons.Outlined.TextFields,
        defaultSuffix = "from_text",
        acceptsNonPdfInput = true
    ),
    PDF_TO_IMAGES(
        id = "pdf_to_images",
        titleRes = R.string.tool_pdf_to_images_title,
        descriptionRes = R.string.tool_pdf_to_images_desc,
        group = ToolGroup.CONVERT,
        icon = Icons.Outlined.Image,
        defaultSuffix = "images",
        outputExtension = "zip",
        outputMimeType = "application/zip"
    ),
    PDF_TO_TEXT_MD(
        id = "pdf_to_text_md",
        titleRes = R.string.tool_pdf_to_text_md_title,
        descriptionRes = R.string.tool_pdf_to_text_md_desc,
        group = ToolGroup.CONVERT,
        icon = Icons.Outlined.FormatColorText,
        defaultSuffix = "text",
        outputExtension = "md",
        outputMimeType = "text/markdown"
    ),
    PDF_TO_OFFICE(
        id = "pdf_to_office",
        titleRes = R.string.tool_pdf_to_office_title,
        descriptionRes = R.string.tool_pdf_to_office_desc,
        group = ToolGroup.CONVERT,
        icon = Icons.Outlined.TableChart,
        defaultSuffix = "office",
        outputExtension = "docx",
        outputMimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    ),
    EXTRACT_ASSETS(
        id = "extract_assets",
        titleRes = R.string.tool_extract_assets_title,
        descriptionRes = R.string.tool_extract_assets_desc,
        group = ToolGroup.CONVERT,
        icon = Icons.Outlined.FolderZip,
        defaultSuffix = "assets",
        outputExtension = "zip",
        outputMimeType = "application/zip"
    ),

    // 4. Edit & Sign (4 tools)
    ANNOTATE(
        id = "annotate",
        titleRes = R.string.tool_annotate_title,
        descriptionRes = R.string.tool_annotate_desc,
        group = ToolGroup.EDIT_SIGN,
        icon = Icons.Outlined.Highlight,
        defaultSuffix = "annotated"
    ),
    ADD_PAGE_NUMBERS(
        id = "add_page_numbers",
        titleRes = R.string.tool_add_page_numbers_title,
        descriptionRes = R.string.tool_add_page_numbers_desc,
        group = ToolGroup.EDIT_SIGN,
        icon = Icons.Outlined.FormatListNumbered,
        defaultSuffix = "numbered"
    ),
    ADD_WATERMARK(
        id = "add_watermark",
        titleRes = R.string.tool_add_watermark_title,
        descriptionRes = R.string.tool_add_watermark_desc,
        group = ToolGroup.EDIT_SIGN,
        icon = Icons.Outlined.WaterDrop,
        defaultSuffix = "watermarked"
    ),
    SIGN_PDF(
        id = "sign_pdf",
        titleRes = R.string.tool_sign_pdf_title,
        descriptionRes = R.string.tool_sign_pdf_desc,
        group = ToolGroup.EDIT_SIGN,
        icon = Icons.Outlined.Draw,
        defaultSuffix = "signed"
    ),

    // 5. Secure (4 tools)
    PROTECT(
        id = "protect",
        titleRes = R.string.tool_protect_title,
        descriptionRes = R.string.tool_protect_desc,
        group = ToolGroup.SECURE,
        icon = Icons.Outlined.Lock,
        defaultSuffix = "protected"
    ),
    UNLOCK(
        id = "unlock",
        titleRes = R.string.tool_unlock_title,
        descriptionRes = R.string.tool_unlock_desc,
        group = ToolGroup.SECURE,
        icon = Icons.Outlined.LockOpen,
        defaultSuffix = "unlocked"
    ),
    REDACT(
        id = "redact",
        titleRes = R.string.tool_redact_title,
        descriptionRes = R.string.tool_redact_desc,
        group = ToolGroup.SECURE,
        icon = Icons.Outlined.VisibilityOff,
        defaultSuffix = "redacted"
    ),
    STRIP_METADATA(
        id = "strip_metadata",
        titleRes = R.string.tool_strip_metadata_title,
        descriptionRes = R.string.tool_strip_metadata_desc,
        group = ToolGroup.SECURE,
        icon = Icons.AutoMirrored.Outlined.Grading,
        defaultSuffix = "cleaned"
    ),

    // 6. Scan (1 tool)
    SCAN_TO_PDF(
        id = "scan_to_pdf",
        titleRes = R.string.tool_scan_to_pdf_title,
        descriptionRes = R.string.tool_scan_to_pdf_desc,
        group = ToolGroup.SCAN,
        icon = Icons.Outlined.CameraAlt,
        defaultSuffix = "scanned",
        acceptsNonPdfInput = true
    );

    /** A tool may take several files even when one is enough (Image to PDF appends each). */
    val acceptsMultipleInputFiles: Boolean
        get() = requiresMultipleInputFiles || this == IMAGE_TO_PDF
}
