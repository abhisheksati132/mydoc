package com.abhisheksati.mydoc.tools.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * A stamp icon is only assigned for recognized standard texts. The old mapping fell through to
 * "Draft" for REJECTED and every custom stamp, misrepresenting the annotation.
 */
class StampIconMappingTest {

    @Test
    fun unknownText_doesNotYieldADraftIcon() {
        assertNull(standardStampIcon("REVIEWED"))
        assertNull(standardStampIcon("Quarterly report"))
        assertNull(standardStampIcon(""))
        assertNotEquals("Draft", standardStampIcon("REVIEWED"))
    }

    @Test
    fun rejected_mapsToTheClosestStandardIcon_notDraft() {
        assertEquals("NotApproved", standardStampIcon("REJECTED"))
        assertNotEquals("Draft", standardStampIcon("REJECTED"))
    }

    @Test
    fun recognizedTexts_mapToTheirStandardIcons() {
        assertEquals("Draft", standardStampIcon("DRAFT"))
        assertEquals("Confidential", standardStampIcon("CONFIDENTIAL"))
        assertEquals("Approved", standardStampIcon("APPROVED"))
        assertEquals("ForComment", standardStampIcon("FOR COMMENT"))
        assertEquals("ForComment", standardStampIcon("FORCOMMENT"))
        assertEquals("ForPublicRelease", standardStampIcon("FOR PUBLIC RELEASE"))
        assertEquals("ForPublicRelease", standardStampIcon("FORPUBLICRELEASE"))
        assertEquals("NotApproved", standardStampIcon("NOT APPROVED"))
        assertEquals("NotApproved", standardStampIcon("NOTAPPROVED"))
        assertEquals("TopSecret", standardStampIcon("TOP SECRET"))
        assertEquals("TopSecret", standardStampIcon("TOPSECRET"))
    }

    @Test
    fun mapping_isCaseInsensitive() {
        assertEquals("Draft", standardStampIcon("draft"))
        assertEquals("NotApproved", standardStampIcon("rejected"))
        assertNull(standardStampIcon("my custom stamp"))
    }
}