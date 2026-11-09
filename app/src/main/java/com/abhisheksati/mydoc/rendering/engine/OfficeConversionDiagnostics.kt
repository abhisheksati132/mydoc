package com.abhisheksati.mydoc.rendering.engine

import com.abhisheksati.mydoc.domain.model.LoadFailure

/** Last failure in this process. File names, paths and document contents stay out of diagnostics. */
internal object OfficeConversionDiagnostics {
    @Volatile
    var lastFailure: LoadFailure? = null
}
