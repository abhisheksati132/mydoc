package com.abhisheksati.mydoc.tools.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The bug these tests pin: the mask used to start from -64 and OR in what the user allowed.
 * -64 already has bits 9-12 set, so unchecking "Allow printing" left bit 12 (2048, full-resolution
 * print) granted and unchecking "Allow Copying" left bit 10 (512, extraction for accessibility)
 * granted. A denied permission was not denied.
 */
class PdfPermissionsTest {

    private fun bit(permissions: Int, flag: Int) = permissions and flag == flag

    @Test
    fun bothAllowedIsTheUnrestrictedValue() {
        assertEquals(-4, PdfPermissions.mask(allowPrinting = true, allowCopying = true))
    }

    @Test
    fun reservedBitsStayZeroInEveryCombination() {
        listOf(true, false).forEach { print ->
            listOf(true, false).forEach { copy ->
                val mask = PdfPermissions.mask(print, copy)
                assertEquals("bits 1-2 must be 0 for $print/$copy", 0, mask and 3)
            }
        }
    }

    @Test
    fun denyingPrintingClearsBothPrintBits() {
        val mask = PdfPermissions.mask(allowPrinting = false, allowCopying = true)
        assertFalse(bit(mask, PdfPermissions.PRINT))
        // The original defect: full-resolution print stayed granted.
        assertFalse(bit(mask, PdfPermissions.PRINT_HIGH_QUALITY))
        assertTrue(bit(mask, PdfPermissions.COPY))
    }

    @Test
    fun denyingCopyingClearsBothExtractionBits() {
        val mask = PdfPermissions.mask(allowPrinting = true, allowCopying = false)
        assertFalse(bit(mask, PdfPermissions.COPY))
        // The original defect: accessibility extraction stayed granted.
        assertFalse(bit(mask, PdfPermissions.ACCESSIBILITY))
        assertTrue(bit(mask, PdfPermissions.PRINT))
    }

    @Test
    fun denyingEverythingClearsFourBitsAndKeepsTheRest() {
        val mask = PdfPermissions.mask(allowPrinting = false, allowCopying = false)
        assertFalse(bit(mask, PdfPermissions.PRINT))
        assertFalse(bit(mask, PdfPermissions.PRINT_HIGH_QUALITY))
        assertFalse(bit(mask, PdfPermissions.COPY))
        assertFalse(bit(mask, PdfPermissions.ACCESSIBILITY))
        // The two switches on screen say nothing about editing, annotating, filling forms or
        // assembling, so those keep the value the tool has always produced.
        assertTrue(bit(mask, PdfPermissions.MODIFY))
        assertTrue(bit(mask, PdfPermissions.ANNOTATE))
        assertTrue(bit(mask, PdfPermissions.FILL_FORMS))
        assertTrue(bit(mask, PdfPermissions.ASSEMBLE))
    }

    @Test
    fun printingAllowedKeepsCopyingDeniedIndependent() {
        val mask = PdfPermissions.mask(allowPrinting = true, allowCopying = false)
        assertTrue(bit(mask, PdfPermissions.PRINT))
        assertTrue(bit(mask, PdfPermissions.PRINT_HIGH_QUALITY))
        assertFalse(bit(mask, PdfPermissions.COPY))
        assertFalse(bit(mask, PdfPermissions.ACCESSIBILITY))
    }

    @Test
    fun allowsReportsTheSameTruthAsTheBitTest() {
        val mask = PdfPermissions.mask(allowPrinting = false, allowCopying = true)
        assertFalse(PdfPermissions.allows(mask, PdfPermissions.PRINT))
        assertTrue(PdfPermissions.allows(mask, PdfPermissions.COPY))
    }

    @Test
    fun anAllZeroWordIsNotTreatedAsUnrestricted() {
        // Nothing granted and the reserved bits clear: `allows` must say no for every operation.
        listOf(
            PdfPermissions.PRINT, PdfPermissions.MODIFY, PdfPermissions.COPY,
            PdfPermissions.ANNOTATE, PdfPermissions.FILL_FORMS, PdfPermissions.ACCESSIBILITY,
            PdfPermissions.ASSEMBLE, PdfPermissions.PRINT_HIGH_QUALITY
        ).forEach {
            assertFalse(PdfPermissions.allows(0, it))
        }
    }
}
