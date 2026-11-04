package com.abhisheksati.mydoc.tools.engine

/**
 * The /P permissions value written into a PDF encryption dictionary.
 *
 * Bit numbers follow ISO 32000-1 Table 25 and are counted from the low end of the 32-bit word,
 * so `PRINT` is bit position 3 with the value 4. Reserved bits 1-2 must stay zero, which is why
 * the unrestricted base is -4 (0xFFFFFFFC) and not -1.
 *
 * Build the value by starting unrestricted and clearing what the user denied. Starting from a
 * restricted base and OR-ing what was allowed silently leaves the denied bits set.
 */
object PdfPermissions {

    /** Bit 3 — print the document. */
    const val PRINT = 4

    /** Bit 4 — modify the document. */
    const val MODIFY = 8

    /** Bit 5 — extract text and graphics. */
    const val COPY = 16

    /** Bit 6 — add or change annotations and form fields. */
    const val ANNOTATE = 32

    /** Bit 9 — fill in existing form fields. */
    const val FILL_FORMS = 256

    /** Bit 10 — extract text for accessibility. */
    const val ACCESSIBILITY = 512

    /** Bit 11 — insert, rotate or delete pages and create bookmarks. */
    const val ASSEMBLE = 1024

    /** Bit 12 — print at full resolution. */
    const val PRINT_HIGH_QUALITY = 2048

    /** Everything allowed except the two reserved bits. */
    const val UNRESTRICTED = -4

    /**
     * Denying printing means denying both print bits. A reader honouring PDF 1.7 will let a
     * viewer print a low-resolution copy while bit 3 is clear but bit 12 is set, so bit 12 has
     * to be cleared with bit 3. Note that an AES-256 file is ExtensionLevel 6, where PDF 2.0
     * ignores bits 10 and 12: clearing them is correct and necessary for older readers, but is
     * not what makes the restriction hold on a modern one.
     */
    private val PRINT_BITS = PRINT or PRINT_HIGH_QUALITY

    /** Same reasoning for extraction: bit 5 is the operative flag, bit 10 its accessibility twin. */
    private val COPY_BITS = COPY or ACCESSIBILITY

    fun mask(allowPrinting: Boolean, allowCopying: Boolean): Int {
        var permissions = UNRESTRICTED
        if (!allowPrinting) permissions = permissions and PRINT_BITS.inv()
        if (!allowCopying) permissions = permissions and COPY_BITS.inv()
        return permissions
    }

    fun allows(permissions: Int, flag: Int): Boolean = permissions and flag == flag
}
