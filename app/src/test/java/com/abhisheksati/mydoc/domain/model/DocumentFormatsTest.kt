package com.abhisheksati.mydoc.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The registry is the only place format capability is decided, so these tests are the contract
 * the viewer, the converters, the chips and the thumbnails all rely on.
 */
class DocumentFormatsTest {

    @Test
    fun everyDeclaredFormatHasViewerAndFamily() {
        for (descriptor in DocumentFormats.all) {
            assertTrue(descriptor.extension, descriptor.viewer != ViewerKind.UNSUPPORTED)
            assertTrue(descriptor.extension, descriptor.extension in DocumentFormats.openableExtensions)
        }
    }

    @Test
    fun refusedContainersAreReportedNotScraped() {
        for (ext in listOf("xlsb", "wps", "wpd")) {
            assertEquals(ext, LoadFailure.UNSUPPORTED_FORMAT, DocumentFormats.failureFor(ext))
        }
    }

    @Test
    fun fb2IsOpenedByTheEbookEngineRatherThanRefused() {
        // MuPDF ships an FB2 refiner (fb2_document_handler and the FictionBook stylesheet are in the
        // bundled native library), so refusing the extension was a routing gap, not a capability gap.
        assertEquals(ViewerKind.PDF_PAGES, DocumentFormats.viewerFor("fb2"))
        assertEquals(DocumentFamily.EBOOK, DocumentFormats.familyOf("fb2"))
        assertEquals(ConverterKind.NONE, DocumentFormats.converterFor("fb2"))
        assertNull(DocumentFormats.failureFor("fb2"))
        assertTrue(DocumentFormats.forExtension("fb2")!!.limitations.isNotEmpty())
        assertTrue(DocumentFormats.openableExtensions.contains("fb2"))
        assertEquals(DocumentCategory.EPUB, DocumentFormats.categoryOf("fb2"))
    }

    @Test
    fun refusalBeatsUnknownExtensionRouting() {
        // .xlsb has no descriptor. Routing must consult the refusal list first, otherwise it
        // falls to the PDF renderer and reports a healthy refusal as a damaged file.
        val xlsb = DocumentFormats.forExtension("xlsb")
        val declared = DocumentFormats.failureFor("xlsb")
        assertTrue(xlsb == null && declared != null)
    }

    @Test
    fun paginatedWordFormatsRequireTheirConverter() {
        for (ext in listOf("docx", "dotx", "doc", "dot", "odt", "ott", "fodt", "rtf")) {
            assertEquals(ext, ConverterKind.DOCX_TO_PDF, DocumentFormats.converterFor(ext))
            assertEquals(ext, ViewerKind.CONVERTED_PAGES, DocumentFormats.viewerFor(ext))
        }
    }

    @Test
    fun gridOnlyFormatsAreNeverSentToAConverter() {
        // Delimited text stays interactive; it has no stored print layout to render.
        for (ext in listOf("csv", "tsv")) {
            assertEquals(ext, ConverterKind.NONE, DocumentFormats.converterFor(ext))
            assertEquals(ext, ViewerKind.SPREADSHEET_GRID, DocumentFormats.viewerFor(ext))
        }
    }

    @Test
    fun degradedFidelityAlwaysCarriesADisclosure() {
        for (descriptor in DocumentFormats.all) {
            if (descriptor.fidelity == RenderFidelity.TEXT_ONLY || descriptor.fidelity == RenderFidelity.RECOVERED_SCRAPE) {
                assertTrue(descriptor.extension, descriptor.limitations.isNotEmpty())
            }
        }
    }

    @Test
    fun macrosAreDisclosedForMacroEnabledContainers() {
        val xlsm = DocumentFormats.forExtension("xlsm")!!
        assertTrue(xlsm.limitations.any { it.contains("Macros", ignoreCase = true) })
        val pptm = DocumentFormats.forExtension("pptm")!!
        assertTrue(pptm.limitations.any { it.contains("Macros", ignoreCase = true) })
    }

    @Test
    fun categoryMappingFollowsFamily() {
        assertEquals(DocumentCategory.WORD, DocumentFormats.categoryOf("odt"))
        assertEquals(DocumentCategory.EXCEL, DocumentFormats.categoryOf("fods"))
        assertEquals(DocumentCategory.PPT, DocumentFormats.categoryOf("odp"))
        assertEquals(DocumentCategory.EPUB, DocumentFormats.categoryOf("epub"))
        assertEquals(DocumentCategory.ALL, DocumentFormats.categoryOf("unknownext"))
    }

    @Test
    fun filterLabelsResolveThroughTheRegistry() {
        assertTrue(DocumentFilter.EXCEL.matches("xlsm"))
        assertTrue(DocumentFilter.WORD.matches("fodt"))
        assertTrue(DocumentFilter.PPT.matches("ppsx"))
        assertFalse(DocumentFilter.EXCEL.matches("pdf"))
        assertEquals(DocumentFilter.WORD, DocumentFilter.fromLabel("Word"))
    }

    @Test
    fun thumbnailSetComesFromTheRegistry() {
        assertTrue(DocumentFormats.thumbnailExtensions.contains("docx"))
        assertTrue(DocumentFormats.thumbnailExtensions.contains("xlsx"))
        assertTrue(DocumentFormats.thumbnailExtensions.contains("pdf"))
        assertFalse(DocumentFormats.thumbnailExtensions.contains("csv"))
    }

    @Test
    fun mimeTypesCoverTheOfficeContainersTheManifestClaims() {
        for (ext in listOf("docx", "xlsx", "pptx", "xls", "odt", "csv", "rtf")) {
            assertNotNull(ext, DocumentFormats.mimeTypesFor(ext).firstOrNull())
        }
        assertNull(DocumentFormats.mimeTypesFor("unknownext").firstOrNull())
    }

    private fun assertFalse(condition: Boolean) = org.junit.Assert.assertFalse(condition)
}
