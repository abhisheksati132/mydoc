package com.abhisheksati.mydoc.ui.viewer

import com.abhisheksati.mydoc.domain.model.DocumentFormats
import com.abhisheksati.mydoc.domain.model.RenderFidelity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The disclosure is the reader's only signal that what is on screen is not what the author made,
 * so these tests hold the two directions it can fail: staying silent when it should speak, and
 * letting a declared capability cover an engine's runtime fallback.
 */
class FidelityDisclosureTest {

    @Test
    fun `formats rendered as authored disclose nothing`() {
        assertNull(fidelityDisclosureFor("pdf"))
        assertNull(fidelityDisclosureFor("txt"))
        assertNull(fidelityDisclosureFor("csv"))
    }

    @Test
    fun `an extension the registry never heard of makes no promise`() {
        assertNull(DocumentFormats.forExtension("zip"))
        assertNull(fidelityDisclosureFor("zip"))
        assertNull(fidelityDisclosureFor("xlsb"))
    }

    @Test
    fun `recovered and text-only formats say so in their own words`() {
        // .xls is decoded as BIFF8 now, so it promises a grid and names what it still drops.
        val xls = fidelityDisclosureFor("xls")!!
        assertTrue(xls.headline.contains("Recovered", ignoreCase = true).not())
        assertTrue(xls.notes.any { it.contains("97-2003") })
        assertTrue(xls.notes.any { it.contains("cached", ignoreCase = true) })
        assertTrue(fidelityDisclosureFor("ppt")!!.headline.contains("Text only", ignoreCase = true))
    }

    @Test
    fun `a runtime scrape downgrades the declared level`() {
        val declared = DocumentFormats.forExtension("docx")!!.fidelity
        assertEquals(RenderFidelity.STRUCTURED_REFLOW, declared)

        val scraped = fidelityDisclosure(
            descriptor = DocumentFormats.forExtension("docx"),
            observedFidelity = RenderFidelity.RECOVERED_SCRAPE
        )!!
        assertTrue(scraped.headline.contains("Recovered", ignoreCase = true))
    }

    @Test
    fun `an observed level can never upgrade what the reader is told`() {
        // A .doc scraped into the reflow viewer is still text-only; claiming more would lie.
        val doc = fidelityDisclosure(
            descriptor = DocumentFormats.forExtension("doc"),
            observedFidelity = RenderFidelity.STRUCTURED_REFLOW
        )!!
        assertTrue(doc.headline.contains("Text only", ignoreCase = true))
    }

    @Test
    fun `a faithful layout with known gaps still names the gaps`() {
        val pptx = fidelityDisclosureFor("pptx")
        assertNotNull(pptx)
        // FIXED_LAYOUT has no headline of its own, so the first limitation carries it.
        assertEquals(DocumentFormats.forExtension("pptx")!!.limitations.first(), pptx!!.headline)
    }

    @Test
    fun `runtime notes ride along with the format limitations`() {
        val sheet = fidelityDisclosure(
            descriptor = DocumentFormats.forExtension("xlsx"),
            extraNotes = listOf("This sheet hit our read limit — later rows are not shown")
        )!!
        assertTrue(sheet.notes.first().contains("read limit"))
        assertTrue(sheet.notes.any { it.contains("recalculated") })
    }
}
