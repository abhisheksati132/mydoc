package com.abhisheksati.mydoc.viewmodel

/** What the viewer should do with a document it has just tried to open. */
enum class PdfOpenOutcome {
    /** Show the pages. */
    RENDER,

    /** The chosen engine can take a password; ask for one. */
    ASK_PASSWORD,

    /** The document is protected and no engine here can unlock it. */
    PASSWORD_UNAVAILABLE,

    /** Nothing to show and no reason to blame the file. */
    EMPTY_DOCUMENT,

    /** The file itself could not be rendered. */
    DAMAGED
}

/**
 * The one decision that decides whether a reader sees a password prompt or "this document is
 * damaged", written down because getting it wrong makes an encrypted file look broken.
 *
 * Protection has to be established before page counting is trusted: an engine that cannot open a
 * locked file reports zero pages, which is the same number a broken file reports.
 */
object PdfOpenDecision {

    fun choose(
        rendererOpened: Boolean,
        passwordProtected: Boolean,
        canAskForPassword: Boolean,
        pageCount: Int,
        encryptedOnDisk: Boolean
    ): PdfOpenOutcome = when {
        !rendererOpened ->
            if (encryptedOnDisk) PdfOpenOutcome.PASSWORD_UNAVAILABLE else PdfOpenOutcome.DAMAGED

        passwordProtected ->
            if (canAskForPassword) PdfOpenOutcome.ASK_PASSWORD else PdfOpenOutcome.PASSWORD_UNAVAILABLE

        pageCount > 0 -> PdfOpenOutcome.RENDER

        // Opened, not flagged as protected, and nothing in it. An encryption dictionary in the
        // trailer still means the engine refused a locked file rather than an empty one.
        encryptedOnDisk -> PdfOpenOutcome.PASSWORD_UNAVAILABLE

        else -> PdfOpenOutcome.DAMAGED
    }
}
