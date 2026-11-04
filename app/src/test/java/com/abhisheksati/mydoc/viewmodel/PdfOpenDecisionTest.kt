package com.abhisheksati.mydoc.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * H-4 was "a password-protected PDF is reported as damaged". The message is produced by one
 * decision, and the decision is wrong when page counting is consulted before protection is
 * established: an engine that cannot open a locked file reports zero pages, which is what a broken
 * file reports. These tests pin the order.
 */
class PdfOpenDecisionTest {

    @Test
    fun anEngineThatCanTakeAPasswordAsksForIt() {
        assertEquals(
            PdfOpenOutcome.ASK_PASSWORD,
            PdfOpenDecision.choose(
                rendererOpened = true,
                passwordProtected = true,
                canAskForPassword = true,
                pageCount = 0,
                encryptedOnDisk = false
            )
        )
    }

    @Test
    fun anEngineThatCannotTakeAPasswordSaysSoRatherThanCallingTheFileDamaged() {
        // This is the original defect: the OS renderer opens nothing, the page count is zero and
        // the reader was told the document was damaged.
        assertEquals(
            PdfOpenOutcome.PASSWORD_UNAVAILABLE,
            PdfOpenDecision.choose(
                rendererOpened = true,
                passwordProtected = false,
                canAskForPassword = false,
                pageCount = 0,
                encryptedOnDisk = true
            )
        )
    }

    @Test
    fun noRendererAtAllOnAnEncryptedFileIsStillNotCorruption() {
        assertEquals(
            PdfOpenOutcome.PASSWORD_UNAVAILABLE,
            PdfOpenDecision.choose(
                rendererOpened = false,
                passwordProtected = false,
                canAskForPassword = false,
                pageCount = 0,
                encryptedOnDisk = true
            )
        )
    }

    @Test
    fun anUnencryptedFileThatWillNotOpenIsReportedAsDamaged() {
        assertEquals(
            PdfOpenOutcome.DAMAGED,
            PdfOpenDecision.choose(
                rendererOpened = true,
                passwordProtected = false,
                canAskForPassword = true,
                pageCount = 0,
                encryptedOnDisk = false
            )
        )
        assertEquals(
            PdfOpenOutcome.DAMAGED,
            PdfOpenDecision.choose(
                rendererOpened = false,
                passwordProtected = false,
                canAskForPassword = false,
                pageCount = 0,
                encryptedOnDisk = false
            )
        )
    }

    @Test
    fun aHealthyDocumentRendersAndProtectionNeverOverridesAPasswordAlreadyGiven() {
        assertEquals(
            PdfOpenOutcome.RENDER,
            PdfOpenDecision.choose(
                rendererOpened = true,
                passwordProtected = false,
                canAskForPassword = true,
                pageCount = 12,
                encryptedOnDisk = false
            )
        )
    }

    @Test
    fun protectionIsCheckedBeforeThePageCount() {
        // MuPDF reports the real page count of a locked file before authentication, so a
        // non-zero count must not be taken as "already readable".
        assertEquals(
            PdfOpenOutcome.ASK_PASSWORD,
            PdfOpenDecision.choose(
                rendererOpened = true,
                passwordProtected = true,
                canAskForPassword = true,
                pageCount = 1,
                encryptedOnDisk = false
            )
        )
    }
}
