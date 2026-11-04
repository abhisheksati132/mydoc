package com.abhisheksati.mydoc.rendering.engine

import com.abhisheksati.mydoc.domain.model.LoadFailure
import java.io.File

/**
 * An office → PDF conversion attempt: either a usable PDF, or the reason there isn't one.
 *
 * Both converters used to return `null` for every failure, so a document that blew the size budget
 * looked identical to one that uses layout features our converter does not implement. The cause is
 * carried with the result now, so the viewer can name what actually happened.
 */
sealed class OfficeConversion {

    val pdfFile: File? get() = (this as? Success)?.pdf

    class Success(val pdf: File) : OfficeConversion()

    class Failure(val failure: LoadFailure, val message: String) : OfficeConversion()
}
