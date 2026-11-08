package com.abhisheksati.mydoc.rendering.engine

import com.abhisheksati.mydoc.domain.model.ConverterKind
import com.abhisheksati.mydoc.domain.model.DocumentFormats
import com.abhisheksati.mydoc.domain.model.ThumbnailKind
import com.abhisheksati.mydoc.domain.model.ViewerKind
import org.junit.Assert.*
import org.junit.Test

class NativeOfficeFormatsTest {
    @Test fun everyPageRouteUsesASuffixTheNativeImporterAccepts() {
        for (format in DocumentFormats.all.filter { it.converter != ConverterKind.NONE }) {
            assertEquals(format.extension, nativeOfficeExtension("Document.${format.extension}"))
            assertEquals(ViewerKind.CONVERTED_PAGES, format.viewer)
            assertEquals(ThumbnailKind.VIA_CONVERTER, format.thumbnail)
        }
    }

    @Test fun legacyAndOpenDocumentFilesUseTheSameEngineAsModernOffice() {
        for (ext in listOf("pptx", "pptm", "ppt", "pps", "pot", "odp", "otp", "fodp")) {
            assertEquals(ext, ConverterKind.PPTX_TO_PDF, DocumentFormats.converterFor(ext))
        }
        for (ext in listOf("xlsx", "xlsm", "xls", "ods", "ots", "fods")) {
            assertEquals(ext, ConverterKind.XLSX_TO_PDF, DocumentFormats.converterFor(ext))
        }
    }

    @Test fun inputNamesCannotSilentlyTurnIntoWordDocuments() {
        assertEquals("pptx", nativeOfficeExtension("Quarterly.PPTX"))
        assertEquals("xls", nativeOfficeExtension("Financials.XLS"))
        for (name in listOf("no-extension", "file.xlsb", "file.csv", "file.pdf", "file.pptx/extra", "file.")) {
            assertNull(name, nativeOfficeExtension(name))
        }
    }
}
