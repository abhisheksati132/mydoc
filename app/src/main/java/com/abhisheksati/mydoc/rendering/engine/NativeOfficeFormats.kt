package com.abhisheksati.mydoc.rendering.engine

import java.util.Locale

/** Explicit import suffixes: never disguise a presentation or workbook as a DOCX. */
internal fun nativeOfficeExtension(displayName: String): String? =
    displayName.substringAfterLast('.', "").lowercase(Locale.ROOT).takeIf {
        it in setOf(
            "docx", "dotx", "doc", "dot", "odt", "ott", "fodt", "rtf",
            "pptx", "pptm", "ppsx", "potx", "ppt", "pps", "pot", "odp", "otp", "fodp",
            "xlsx", "xlsm", "xls", "ods", "ots", "fods"
        )
    }
