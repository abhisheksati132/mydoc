package com.abhisheksati.mydoc.domain.model

/**
 * The single source of truth for what MyDoc can actually do with a file.
 *
 * Before this, format knowledge lived in six divergent lists (viewer routing, the conversion
 * switch in ViewerViewModel, DocumentFile.category, DocumentFilter, SpotlightSearch and the
 * thumbnail preview set) that disagreed about which extensions existed and what each one
 * supported. Routing, capability gating and user-facing disclosure all read from here instead.
 *
 * The descriptors describe real behaviour, not aspiration: if no engine can read a container,
 * the format is UNSUPPORTED rather than quietly scraped into a blank document.
 */

enum class DocumentFamily(val label: String) {
    PDF("PDF"),
    WORD("Word"),
    SPREADSHEET("Spreadsheet"),
    PRESENTATION("Presentation"),
    EBOOK("eBook"),
    TEXT("Text"),
    UNKNOWN("Document")
}

enum class ViewerKind {
    /** MuPDF tile renderer — fixed-layout pages. */
    PDF_PAGES,

    /** Paginated output produced by an in-app Office-to-PDF converter. */
    CONVERTED_PAGES,

    SPREADSHEET_GRID,
    PRESENTATION_SLIDES,
    WORD_REFLOW,
    TEXT_VIEWER,

    /** No engine can read this container; the viewer must explain rather than render. */
    UNSUPPORTED
}

enum class ConverterKind {
    NONE,
    DOCX_TO_PDF,
    PPTX_TO_PDF,
    XLSX_TO_PDF
}

enum class ThumbnailKind {
    /** MuPDF can rasterise the file directly. */
    DIRECT_PDF,

    /** First page comes from the same converter the viewer uses. */
    VIA_CONVERTER,
    ICON_ONLY
}

/**
 * How much of the source document's design survives into what is shown. This drives the
 * disclosure banner, so a text scrape can never be presented as a faithful rendering.
 */
enum class RenderFidelity {
    /** Fixed layout, true to the source: PDF and converted Office pages. */
    FIXED_LAYOUT,

    /** Real structure — headings, lists, tables, emphasis — re-laid out for the screen. */
    STRUCTURED_REFLOW,

    /** Text recovered from a container whose layout we cannot read. */
    TEXT_ONLY,

    /** Best-effort byte scrape of an unsupported binary; content may be out of order. */
    RECOVERED_SCRAPE,

    /** Nothing was rendered. */
    NONE
}

/** Fine-grained reasons a file cannot be opened, each with its own user-facing wording. */
enum class LoadFailure {
    UNSUPPORTED_FORMAT,
    CORRUPT_DOCUMENT,
    EMPTY_DOCUMENT,
    PERMISSION_EXPIRED,
    PASSWORD_PROTECTED,
    EXCEEDS_LIMITS,
    CONVERSION_FAILED,
    FEATURE_UNAVAILABLE
}

data class FormatDescriptor(
    val extension: String,
    val family: DocumentFamily,
    val viewer: ViewerKind,
    val converter: ConverterKind,
    val thumbnail: ThumbnailKind,
    val fidelity: RenderFidelity,
    /** Human-readable, shown in the reader's format-info sheet. Keep each one short. */
    val limitations: List<String> = emptyList()
) {
    val isSupported: Boolean get() = viewer != ViewerKind.UNSUPPORTED
}

private const val OFFICE_LAYOUT_LIMIT =
    "Embedded images and page headers/footers are not rendered; footnote text is collected at the end of the document"

private val TEXT_EXTENSIONS = setOf(
    "txt", "md", "log", "json", "xml", "yaml", "yml", "ini", "conf", "sh", "sql",
    "kt", "java", "py", "html", "css", "js", "ts", "c", "cpp", "h", "gradle",
    "properties", "bat"
)

private val descriptors: List<FormatDescriptor> = buildList {
    // ---- Fixed-layout vector documents, rendered natively by MuPDF ----
    add(FormatDescriptor("pdf", DocumentFamily.PDF, ViewerKind.PDF_PAGES, ConverterKind.NONE, ThumbnailKind.DIRECT_PDF, RenderFidelity.FIXED_LAYOUT))
    add(FormatDescriptor("xps", DocumentFamily.EBOOK, ViewerKind.PDF_PAGES, ConverterKind.NONE, ThumbnailKind.DIRECT_PDF, RenderFidelity.FIXED_LAYOUT))
    add(FormatDescriptor("oxps", DocumentFamily.EBOOK, ViewerKind.PDF_PAGES, ConverterKind.NONE, ThumbnailKind.DIRECT_PDF, RenderFidelity.FIXED_LAYOUT))
    add(FormatDescriptor("cbz", DocumentFamily.EBOOK, ViewerKind.PDF_PAGES, ConverterKind.NONE, ThumbnailKind.DIRECT_PDF, RenderFidelity.FIXED_LAYOUT))
    add(
        FormatDescriptor(
            extension = "epub",
            family = DocumentFamily.EBOOK,
            viewer = ViewerKind.PDF_PAGES,
            converter = ConverterKind.NONE,
            thumbnail = ThumbnailKind.ICON_ONLY,
            fidelity = RenderFidelity.STRUCTURED_REFLOW,
            limitations = listOf("Reflowable text is laid out by MuPDF, not by the book's own CSS")
        )
    )

    // FictionBook is a MuPDF format like EPUB: the engine refows it, we do not parse it.
    add(
        FormatDescriptor(
            extension = "fb2",
            family = DocumentFamily.EBOOK,
            viewer = ViewerKind.PDF_PAGES,
            converter = ConverterKind.NONE,
            thumbnail = ThumbnailKind.ICON_ONLY,
            fidelity = RenderFidelity.STRUCTURED_REFLOW,
            limitations = listOf(
                "Reflowable text is laid out by MuPDF, not by the book's own stylesheet",
                "Only the first story is followed: a .fb2 with several binary-linked bodies shows one"
            )
        )
    )

    // ---- Word processing ----
    add(
        FormatDescriptor(
            extension = "docx",
            family = DocumentFamily.WORD,
            viewer = ViewerKind.CONVERTED_PAGES,
            converter = ConverterKind.DOCX_TO_PDF,
            thumbnail = ThumbnailKind.VIA_CONVERTER,
            fidelity = RenderFidelity.STRUCTURED_REFLOW,
            limitations = listOf(
                OFFICE_LAYOUT_LIMIT,
                "Page breaks follow our own re-pagination, so page numbers can differ from Word"
            )
        )
    )
    add(FormatDescriptor("dotx", DocumentFamily.WORD, ViewerKind.CONVERTED_PAGES, ConverterKind.DOCX_TO_PDF, ThumbnailKind.VIA_CONVERTER, RenderFidelity.STRUCTURED_REFLOW, listOf(OFFICE_LAYOUT_LIMIT)))

    // ODT is a zip carrying content.xml, which the same parser already reads.
    add(
        FormatDescriptor(
            extension = "odt",
            family = DocumentFamily.WORD,
            viewer = ViewerKind.CONVERTED_PAGES,
            converter = ConverterKind.DOCX_TO_PDF,
            thumbnail = ThumbnailKind.VIA_CONVERTER,
            fidelity = RenderFidelity.STRUCTURED_REFLOW,
            limitations = listOf("Styles, images and page masters from styles.xml are not applied")
        )
    )
    add(FormatDescriptor("ott", DocumentFamily.WORD, ViewerKind.CONVERTED_PAGES, ConverterKind.DOCX_TO_PDF, ThumbnailKind.VIA_CONVERTER, RenderFidelity.STRUCTURED_REFLOW, listOf("Styles, images and page masters from styles.xml are not applied")))
    add(FormatDescriptor("fodt", DocumentFamily.WORD, ViewerKind.WORD_REFLOW, ConverterKind.NONE, ThumbnailKind.ICON_ONLY, RenderFidelity.STRUCTURED_REFLOW, listOf("Flat ODF: images and styles are not rendered")))

    // Legacy binary Word has no layout engine here; say so instead of inventing one.
    add(
        FormatDescriptor(
            extension = "doc",
            family = DocumentFamily.WORD,
            viewer = ViewerKind.WORD_REFLOW,
            converter = ConverterKind.NONE,
            thumbnail = ThumbnailKind.ICON_ONLY,
            fidelity = RenderFidelity.TEXT_ONLY,
            limitations = listOf(
                "Text only: the characters and paragraph breaks come from the Word 97 piece table, but formatting, table structure and images are not decoded",
                "Headers, footers and footnotes of a binary .doc are not read"
            )
        )
    )
    add(FormatDescriptor("dot", DocumentFamily.WORD, ViewerKind.WORD_REFLOW, ConverterKind.NONE, ThumbnailKind.ICON_ONLY, RenderFidelity.TEXT_ONLY, listOf("Text only: the characters and paragraph breaks come from the Word 97 piece table, but formatting, table structure and images are not decoded")))
    add(
        FormatDescriptor(
            extension = "rtf",
            family = DocumentFamily.WORD,
            viewer = ViewerKind.WORD_REFLOW,
            converter = ConverterKind.NONE,
            thumbnail = ThumbnailKind.ICON_ONLY,
            fidelity = RenderFidelity.STRUCTURED_REFLOW,
            limitations = listOf("Character and paragraph properties are read; tables, images and styles are not")
        )
    )

    // ---- Spreadsheets ----
    add(
        FormatDescriptor(
            extension = "xlsx",
            family = DocumentFamily.SPREADSHEET,
            viewer = ViewerKind.SPREADSHEET_GRID,
            converter = ConverterKind.XLSX_TO_PDF,
            thumbnail = ThumbnailKind.VIA_CONVERTER,
            fidelity = RenderFidelity.STRUCTURED_REFLOW,
            limitations = listOf("Formulas are shown as their stored cached values — nothing is recalculated")
        )
    )
    add(
        FormatDescriptor(
            extension = "xlsm",
            family = DocumentFamily.SPREADSHEET,
            viewer = ViewerKind.SPREADSHEET_GRID,
            converter = ConverterKind.XLSX_TO_PDF,
            thumbnail = ThumbnailKind.VIA_CONVERTER,
            fidelity = RenderFidelity.STRUCTURED_REFLOW,
            limitations = listOf("Macros are never executed", "Formulas are shown as cached values, not recalculated")
        )
    )
    add(
        FormatDescriptor(
            extension = "xls",
            family = DocumentFamily.SPREADSHEET,
            viewer = ViewerKind.SPREADSHEET_GRID,
            converter = ConverterKind.NONE,
            thumbnail = ThumbnailKind.ICON_ONLY,
            fidelity = RenderFidelity.STRUCTURED_REFLOW,
            limitations = listOf(
                "Number formats, styles and charts from the 97-2003 binary are not applied, so dates show as their serial number",
                "Formula cells show the value Excel last cached — nothing is recalculated"
            )
        )
    )
    add(
        FormatDescriptor(
            extension = "ods",
            family = DocumentFamily.SPREADSHEET,
            viewer = ViewerKind.SPREADSHEET_GRID,
            converter = ConverterKind.NONE,
            thumbnail = ThumbnailKind.ICON_ONLY,
            fidelity = RenderFidelity.STRUCTURED_REFLOW,
            limitations = listOf("Cell styles and number formats are not applied")
        )
    )
    add(FormatDescriptor("fods", DocumentFamily.SPREADSHEET, ViewerKind.SPREADSHEET_GRID, ConverterKind.NONE, ThumbnailKind.ICON_ONLY, RenderFidelity.STRUCTURED_REFLOW, listOf("Cell styles and number formats are not applied")))
    add(FormatDescriptor("ots", DocumentFamily.SPREADSHEET, ViewerKind.SPREADSHEET_GRID, ConverterKind.NONE, ThumbnailKind.ICON_ONLY, RenderFidelity.STRUCTURED_REFLOW, listOf("Cell styles and number formats are not applied")))
    add(FormatDescriptor("csv", DocumentFamily.SPREADSHEET, ViewerKind.SPREADSHEET_GRID, ConverterKind.NONE, ThumbnailKind.ICON_ONLY, RenderFidelity.FIXED_LAYOUT))
    add(FormatDescriptor("tsv", DocumentFamily.SPREADSHEET, ViewerKind.SPREADSHEET_GRID, ConverterKind.NONE, ThumbnailKind.ICON_ONLY, RenderFidelity.FIXED_LAYOUT))

    // ---- Presentations ----
    add(
        FormatDescriptor(
            extension = "pptx",
            family = DocumentFamily.PRESENTATION,
            viewer = ViewerKind.CONVERTED_PAGES,
            converter = ConverterKind.PPTX_TO_PDF,
            thumbnail = ThumbnailKind.VIA_CONVERTER,
            fidelity = RenderFidelity.FIXED_LAYOUT,
            limitations = listOf("Charts, SmartArt and embedded objects are not drawn", "Animations and transitions are not played")
        )
    )
    add(FormatDescriptor("pptm", DocumentFamily.PRESENTATION, ViewerKind.CONVERTED_PAGES, ConverterKind.PPTX_TO_PDF, ThumbnailKind.VIA_CONVERTER, RenderFidelity.FIXED_LAYOUT, listOf("Macros are never executed", "Charts and SmartArt are not drawn")))
    add(FormatDescriptor("ppsx", DocumentFamily.PRESENTATION, ViewerKind.CONVERTED_PAGES, ConverterKind.PPTX_TO_PDF, ThumbnailKind.VIA_CONVERTER, RenderFidelity.FIXED_LAYOUT, listOf("Charts and SmartArt are not drawn")))
    add(FormatDescriptor("potx", DocumentFamily.PRESENTATION, ViewerKind.CONVERTED_PAGES, ConverterKind.PPTX_TO_PDF, ThumbnailKind.VIA_CONVERTER, RenderFidelity.FIXED_LAYOUT, listOf("Charts and SmartArt are not drawn")))
    add(
        FormatDescriptor(
            extension = "odp",
            family = DocumentFamily.PRESENTATION,
            viewer = ViewerKind.PRESENTATION_SLIDES,
            converter = ConverterKind.NONE,
            thumbnail = ThumbnailKind.ICON_ONLY,
            fidelity = RenderFidelity.STRUCTURED_REFLOW,
            limitations = listOf("Draw styles and master pages are simplified")
        )
    )
    add(FormatDescriptor("otp", DocumentFamily.PRESENTATION, ViewerKind.PRESENTATION_SLIDES, ConverterKind.NONE, ThumbnailKind.ICON_ONLY, RenderFidelity.STRUCTURED_REFLOW, listOf("Draw styles and master pages are simplified")))
    add(FormatDescriptor("fodp", DocumentFamily.PRESENTATION, ViewerKind.PRESENTATION_SLIDES, ConverterKind.NONE, ThumbnailKind.ICON_ONLY, RenderFidelity.TEXT_ONLY, listOf("Flat ODP: only text frames are recovered")))
    add(
        FormatDescriptor(
            extension = "ppt",
            family = DocumentFamily.PRESENTATION,
            viewer = ViewerKind.PRESENTATION_SLIDES,
            converter = ConverterKind.NONE,
            thumbnail = ThumbnailKind.ICON_ONLY,
            fidelity = RenderFidelity.TEXT_ONLY,
            limitations = listOf("Text only: shape positions, colors and images in binary .ppt are not decoded")
        )
    )
    add(FormatDescriptor("pps", DocumentFamily.PRESENTATION, ViewerKind.PRESENTATION_SLIDES, ConverterKind.NONE, ThumbnailKind.ICON_ONLY, RenderFidelity.TEXT_ONLY, listOf("Text only: shape geometry in binary .pps is not decoded")))
    add(FormatDescriptor("pot", DocumentFamily.PRESENTATION, ViewerKind.PRESENTATION_SLIDES, ConverterKind.NONE, ThumbnailKind.ICON_ONLY, RenderFidelity.TEXT_ONLY, listOf("Text only: shape geometry in binary .pot is not decoded")))

    // ---- Plain text and source ----
    for (ext in TEXT_EXTENSIONS) {
        add(FormatDescriptor(ext, DocumentFamily.TEXT, ViewerKind.TEXT_VIEWER, ConverterKind.NONE, ThumbnailKind.ICON_ONLY, RenderFidelity.FIXED_LAYOUT))
    }
}

private val byExtension: Map<String, FormatDescriptor> =
    descriptors.associateBy { it.extension }

/** Containers we deliberately refuse rather than scrape into nonsense. */
private val explicitlyUnsupported: Map<String, LoadFailure> = mapOf(
    "xlsb" to LoadFailure.UNSUPPORTED_FORMAT,
    "wps" to LoadFailure.UNSUPPORTED_FORMAT,
    "wpd" to LoadFailure.UNSUPPORTED_FORMAT
)

object DocumentFormats {

    val all: List<FormatDescriptor> get() = descriptors

    fun forExtension(extension: String?): FormatDescriptor? =
        extension?.lowercase()?.trimStart('.')?.let { byExtension[it] }

    fun isKnown(extension: String?): Boolean = forExtension(extension) != null

    fun isSupported(extension: String?): Boolean = forExtension(extension)?.isSupported == true

    /** Why a file cannot be opened, when we know it up front. */
    fun failureFor(extension: String?): LoadFailure? =
        extension?.lowercase()?.trimStart('.')?.let { explicitlyUnsupported[it] }

    fun familyOf(extension: String?): DocumentFamily =
        forExtension(extension)?.family ?: DocumentFamily.UNKNOWN

    /** Library chip grouping. Unknown extensions fall to [DocumentCategory.ALL]. */
    fun categoryOf(extension: String?): DocumentCategory = when (familyOf(extension)) {
        DocumentFamily.PDF -> DocumentCategory.PDF
        DocumentFamily.WORD -> DocumentCategory.WORD
        DocumentFamily.SPREADSHEET -> DocumentCategory.EXCEL
        DocumentFamily.PRESENTATION -> DocumentCategory.PPT
        DocumentFamily.TEXT -> DocumentCategory.TEXT
        DocumentFamily.EBOOK -> DocumentCategory.EPUB
        DocumentFamily.UNKNOWN -> DocumentCategory.ALL
    }

    fun viewerFor(extension: String?): ViewerKind? = forExtension(extension)?.viewer

    fun converterFor(extension: String?): ConverterKind =
        forExtension(extension)?.converter ?: ConverterKind.NONE

    fun thumbnailFor(extension: String?): ThumbnailKind =
        forExtension(extension)?.thumbnail ?: ThumbnailKind.ICON_ONLY

    fun fidelityFor(extension: String?): RenderFidelity =
        forExtension(extension)?.fidelity ?: RenderFidelity.NONE

    fun extensionsIn(family: DocumentFamily): Set<String> =
        descriptors.filter { it.family == family && it.isSupported }.map { it.extension }.toSet()

    /** Extensions the reader can open at all — used by pickers and search filters. */
    val openableExtensions: Set<String> = byExtension.keys + explicitlyUnsupported.keys

    val supportedExtensions: Set<String> = byExtension.keys.filter { byExtension.getValue(it).isSupported }.toSet()

    /** Formats whose pages a thumbnail can be produced for. */
    val thumbnailExtensions: Set<String> =
        byExtension.values.filter { it.thumbnail != ThumbnailKind.ICON_ONLY }.map { it.extension }.toSet()

    fun mimeTypesFor(extension: String?): List<String> = when (extension?.lowercase()) {
        "docx" -> listOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        "dotx" -> listOf("application/vnd.openxmlformats-officedocument.wordprocessingml.template")
        "xlsx" -> listOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        "xlsm" -> listOf("application/vnd.ms-excel.sheet.macroEnabled.12")
        "xlsb" -> listOf("application/vnd.ms-excel.sheet.binary.macroEnabled.12")
        "pptx" -> listOf("application/vnd.openxmlformats-officedocument.presentationml.presentation")
        "ppsx" -> listOf("application/vnd.openxmlformats-officedocument.presentationml.slideshow")
        "potx" -> listOf("application/vnd.openxmlformats-officedocument.presentationml.template")
        "pptm" -> listOf("application/vnd.ms-powerpoint.presentation.macroEnabled.12")
        "doc" -> listOf("application/msword")
        "xls" -> listOf("application/vnd.ms-excel")
        "ppt", "pps", "pot" -> listOf("application/vnd.ms-powerpoint")
        "odt" -> listOf("application/vnd.oasis.opendocument.text")
        "ods" -> listOf("application/vnd.oasis.opendocument.spreadsheet")
        "odp" -> listOf("application/vnd.oasis.opendocument.presentation")
        "rtf" -> listOf("application/rtf", "text/rtf")
        "csv" -> listOf("text/csv")
        "tsv" -> listOf("text/tab-separated-values")
        "pdf" -> listOf("application/pdf")
        "epub" -> listOf("application/epub+zip")
        "fb2" -> listOf("application/x-fictionbook+xml", "application/xml")
        else -> emptyList()
    }
}
