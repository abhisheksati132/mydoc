package com.abhisheksati.mydoc.tools.engine

import com.abhisheksati.mydoc.tools.model.ImagePdfQuality
import com.abhisheksati.mydoc.tools.model.PageOrientation
import com.abhisheksati.mydoc.tools.model.PageSizeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Regression coverage for the image → PDF editor's page options: the page box must follow the
 * chosen size, an explicit orientation must beat the photo's own aspect, AUTO must match the
 * photo as stored, and the decode budget must respect both the page box and the quality tier.
 */
class ImagePdfPageOptionsTest {

    private fun assertDims(expectedWidth: Float, expectedHeight: Float, actual: Pair<Float, Float>?) {
        checkNotNull(actual) { "expected fixed page dimensions, got null (fit to image)" }
        assertEquals(expectedWidth, actual.first, 0.01f)
        assertEquals(expectedHeight, actual.second, 0.01f)
    }

    @Test
    fun autoOrientation_matchesThePhotoAspect() {
        assertDims(PageSizeFormat.A4.widthPt, PageSizeFormat.A4.heightPt,
            pdfPageDimensions(PageSizeFormat.A4, PageOrientation.AUTO, imageIsLandscape = false))
        assertDims(PageSizeFormat.A4.heightPt, PageSizeFormat.A4.widthPt,
            pdfPageDimensions(PageSizeFormat.A4, PageOrientation.AUTO, imageIsLandscape = true))
    }

    @Test
    fun explicitOrientation_overridesThePhotoAspect() {
        assertDims(PageSizeFormat.LETTER.widthPt, PageSizeFormat.LETTER.heightPt,
            pdfPageDimensions(PageSizeFormat.LETTER, PageOrientation.PORTRAIT, imageIsLandscape = true))
        assertDims(PageSizeFormat.LETTER.heightPt, PageSizeFormat.LETTER.widthPt,
            pdfPageDimensions(PageSizeFormat.LETTER, PageOrientation.LANDSCAPE, imageIsLandscape = false))
    }

    @Test
    fun everyFixedFormat_getsItsOwnBox() {
        assertDims(419.53f, 595.28f,
            pdfPageDimensions(PageSizeFormat.A5, PageOrientation.PORTRAIT, imageIsLandscape = false))
        assertDims(612f, 1008f,
            pdfPageDimensions(PageSizeFormat.LEGAL, PageOrientation.PORTRAIT, imageIsLandscape = false))
    }

    @Test
    fun fitToImage_hasNoFixedBox() {
        assertNull(pdfPageDimensions(PageSizeFormat.FIT_IMAGE, PageOrientation.AUTO, imageIsLandscape = true))
    }

    @Test
    fun decodeBudget_isBoundedByThePageAndTheQualityTier() {
        val a4 = pdfPageDimensions(PageSizeFormat.A4, PageOrientation.AUTO, imageIsLandscape = false)

        // A4 at 3x supersample asks for 2525px, so HIGH's 2480 cap and the page box agree closely.
        assertEquals(2480, pdfImageDecodeBudget(a4, ImagePdfQuality.HIGH, compress = false))
        assertEquals(1704, pdfImageDecodeBudget(a4, ImagePdfQuality.BALANCED, compress = false))
        assertEquals(1200, pdfImageDecodeBudget(a4, ImagePdfQuality.COMPACT, compress = false))

        // Fit-to-image has no page box, so the quality cap is the only bound.
        assertEquals(2480, pdfImageDecodeBudget(null, ImagePdfQuality.HIGH, compress = false))

        // Compression shrinks the budget further.
        assertEquals((1704 * 0.65f).toInt(), pdfImageDecodeBudget(a4, ImagePdfQuality.BALANCED, compress = true))

        // A small page box wins over a generous quality tier (LETTER is 792pt tall → 2376px at 3x).
        assertEquals(2376, pdfImageDecodeBudget(
            pdfPageDimensions(PageSizeFormat.LETTER, PageOrientation.PORTRAIT, imageIsLandscape = false),
            ImagePdfQuality.HIGH,
            compress = false
        ))
    }
}
