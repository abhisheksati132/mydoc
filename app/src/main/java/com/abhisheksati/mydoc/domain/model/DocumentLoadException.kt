package com.abhisheksati.mydoc.domain.model

/**
 * A document could not be presented, and why.
 *
 * Parsers throw this instead of returning placeholder content. The distinction matters: before it
 * existed, an unreadable file produced a blank sheet, a fabricated slide or the raw markup of the
 * container, and the viewer had no way to tell that apart from a genuinely empty document — so it
 * reported success. Callers map [failure] onto the copy in DocumentProblemScreen.
 */
class DocumentLoadException(
    val failure: LoadFailure,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
