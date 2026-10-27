package com.abhisheksati.mydoc.tools.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.Grading
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.MergeType
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Difference
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Filter
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.FormatColorText
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Highlight
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Transform
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector

enum class ToolGroup(val title: String) {
    ORGANIZE("Organize"),
    OPTIMIZE("Optimize"),
    CONVERT_TO("Convert to PDF"),
    CONVERT_FROM("Convert from PDF"),
    EDIT("Edit"),
    SECURITY("Security"),
    SCAN("Scan & OCR")
}

enum class PdfTool(
    val id: String,
    val title: String,
    val description: String,
    val group: ToolGroup,
    val icon: ImageVector,
    val defaultSuffix: String,
    val outputExtension: String = "pdf",
    val outputMimeType: String = "application/pdf",
    val requiresMultipleInputFiles: Boolean = false,
    val acceptsNonPdfInput: Boolean = false
) {
    // 1. Organize
    MERGE(
        id = "merge",
        title = "Merge PDF",
        description = "Combine multiple PDFs into a single file",
        group = ToolGroup.ORGANIZE,
        icon = Icons.AutoMirrored.Outlined.MergeType,
        defaultSuffix = "merged",
        requiresMultipleInputFiles = true
    ),
    SPLIT(
        id = "split",
        title = "Split PDF",
        description = "Divide a document into separate files",
        group = ToolGroup.ORGANIZE,
        icon = Icons.AutoMirrored.Outlined.CallSplit,
        defaultSuffix = "split"
    ),
    REMOVE_PAGES(
        id = "remove_pages",
        title = "Remove pages",
        description = "Delete unwanted pages from PDF",
        group = ToolGroup.ORGANIZE,
        icon = Icons.Outlined.Delete,
        defaultSuffix = "trimmed"
    ),
    REORDER_PAGES(
        id = "reorder_pages",
        title = "Reorder pages",
        description = "Rearrange or rotate pages visually",
        group = ToolGroup.ORGANIZE,
        icon = Icons.Outlined.GridOn,
        defaultSuffix = "organized"
    ),
    EXTRACT_PAGES(
        id = "extract_pages",
        title = "Extract pages",
        description = "Save selected pages as a new file",
        group = ToolGroup.ORGANIZE,
        icon = Icons.Outlined.Extension,
        defaultSuffix = "extracted"
    ),
    ROTATE(
        id = "rotate",
        title = "Rotate PDF",
        description = "Rotate pages 90° or 180°",
        group = ToolGroup.ORGANIZE,
        icon = Icons.AutoMirrored.Outlined.RotateRight,
        defaultSuffix = "rotated"
    ),
    CROP(
        id = "crop",
        title = "Crop PDF",
        description = "Trim page margins",
        group = ToolGroup.ORGANIZE,
        icon = Icons.Outlined.Crop,
        defaultSuffix = "cropped"
    ),

    // 2. Optimize
    COMPRESS(
        id = "compress",
        title = "Compress PDF",
        description = "Reduce file size",
        group = ToolGroup.OPTIMIZE,
        icon = Icons.Outlined.Compress,
        defaultSuffix = "compressed"
    ),
    REPAIR(
        id = "repair",
        title = "Repair PDF",
        description = "Fix damaged PDF structure",
        group = ToolGroup.OPTIMIZE,
        icon = Icons.Outlined.Build,
        defaultSuffix = "repaired"
    ),

    // 3. Convert (to PDF)
    IMAGE_TO_PDF(
        id = "image_to_pdf",
        title = "Image to PDF",
        description = "Convert photos into a PDF",
        group = ToolGroup.CONVERT_TO,
        icon = Icons.Outlined.Image,
        defaultSuffix = "from_image",
        acceptsNonPdfInput = true
    ),
    OFFICE_TO_PDF(
        id = "office_to_pdf",
        title = "Office to PDF",
        description = "Convert Word, Excel, or slides to PDF",
        group = ToolGroup.CONVERT_TO,
        icon = Icons.Outlined.Description,
        defaultSuffix = "converted",
        acceptsNonPdfInput = true
    ),
    TEXT_TO_PDF(
        id = "text_to_pdf",
        title = "Text / CSV to PDF",
        description = "Convert plain text or CSV into PDF",
        group = ToolGroup.CONVERT_TO,
        icon = Icons.Outlined.TextFields,
        defaultSuffix = "from_text",
        acceptsNonPdfInput = true
    ),

    // 4. Convert (from PDF)
    PDF_TO_IMAGES(
        id = "pdf_to_images",
        title = "PDF to JPG / PNG",
        description = "Export pages as JPG or PNG images",
        group = ToolGroup.CONVERT_FROM,
        icon = Icons.Outlined.Image,
        defaultSuffix = "images",
        outputExtension = "zip",
        outputMimeType = "application/zip"
    ),
    PDF_TO_TEXT_MD(
        id = "pdf_to_text_md",
        title = "PDF to Text / Markdown",
        description = "Extract text and markdown",
        group = ToolGroup.CONVERT_FROM,
        icon = Icons.Outlined.FormatColorText,
        defaultSuffix = "text",
        outputExtension = "md",
        outputMimeType = "text/markdown"
    ),
    PDF_TO_OFFICE(
        id = "pdf_to_office",
        title = "PDF to Word / Excel",
        description = "Export to Word, Excel, or PowerPoint",
        group = ToolGroup.CONVERT_FROM,
        icon = Icons.Outlined.TableChart,
        defaultSuffix = "office",
        outputExtension = "docx",
        outputMimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    ),
    PDF_TO_PDFA(
        id = "pdf_to_pdfa",
        title = "PDF to PDF/A",
        description = "Convert for long-term archiving",
        group = ToolGroup.CONVERT_FROM,
        icon = Icons.Outlined.Shield,
        defaultSuffix = "pdfa"
    ),
    EXTRACT_ASSETS(
        id = "extract_assets",
        title = "Extract images & text",
        description = "Save all embedded images and text",
        group = ToolGroup.CONVERT_FROM,
        icon = Icons.Outlined.FolderZip,
        defaultSuffix = "assets",
        outputExtension = "zip",
        outputMimeType = "application/zip"
    ),

    // 5. Edit
    ANNOTATE(
        id = "annotate",
        title = "Annotate PDF",
        description = "Add highlights, drawings, and notes",
        group = ToolGroup.EDIT,
        icon = Icons.Outlined.Highlight,
        defaultSuffix = "annotated"
    ),
    ADD_PAGE_NUMBERS(
        id = "add_page_numbers",
        title = "Add page numbers",
        description = "Number pages at chosen positions",
        group = ToolGroup.EDIT,
        icon = Icons.Outlined.FormatListNumbered,
        defaultSuffix = "numbered"
    ),
    ADD_WATERMARK(
        id = "add_watermark",
        title = "Add watermark",
        description = "Add text or image watermark",
        group = ToolGroup.EDIT,
        icon = Icons.Outlined.WaterDrop,
        defaultSuffix = "watermarked"
    ),
    FILL_FORM(
        id = "fill_form",
        title = "Fill forms",
        description = "Fill and save PDF form fields",
        group = ToolGroup.EDIT,
        icon = Icons.Outlined.Edit,
        defaultSuffix = "filled"
    ),
    SIGN_PDF(
        id = "sign_pdf",
        title = "Sign PDF",
        description = "Add your saved signature",
        group = ToolGroup.EDIT,
        icon = Icons.Outlined.Draw,
        defaultSuffix = "signed"
    ),
    COMPARE_PDF(
        id = "compare_pdf",
        title = "Compare PDF",
        description = "Compare differences between two PDFs",
        group = ToolGroup.EDIT,
        icon = Icons.AutoMirrored.Outlined.CompareArrows,
        defaultSuffix = "diff",
        requiresMultipleInputFiles = true
    ),

    // 6. Security
    PROTECT(
        id = "protect",
        title = "Protect PDF",
        description = "Set password to lock document",
        group = ToolGroup.SECURITY,
        icon = Icons.Outlined.Lock,
        defaultSuffix = "protected"
    ),
    UNLOCK(
        id = "unlock",
        title = "Unlock PDF",
        description = "Remove document password",
        group = ToolGroup.SECURITY,
        icon = Icons.Outlined.LockOpen,
        defaultSuffix = "unlocked"
    ),
    REDACT(
        id = "redact",
        title = "Redact PDF",
        description = "Black out sensitive information",
        group = ToolGroup.SECURITY,
        icon = Icons.Outlined.VisibilityOff,
        defaultSuffix = "redacted"
    ),
    STRIP_METADATA(
        id = "strip_metadata",
        title = "Strip metadata",
        description = "Remove author and tracking details",
        group = ToolGroup.SECURITY,
        icon = Icons.AutoMirrored.Outlined.Grading,
        defaultSuffix = "cleaned"
    ),

    // 7. Scan & OCR
    SCAN_TO_PDF(
        id = "scan_to_pdf",
        title = "Scan to PDF",
        description = "Scan paper documents with camera",
        group = ToolGroup.SCAN,
        icon = Icons.Outlined.CameraAlt,
        defaultSuffix = "scanned",
        acceptsNonPdfInput = true
    ),
    OCR_PDF(
        id = "ocr_pdf",
        title = "OCR PDF",
        description = "Make scanned text searchable",
        group = ToolGroup.SCAN,
        icon = Icons.Outlined.Transform,
        defaultSuffix = "ocr"
    )
}
