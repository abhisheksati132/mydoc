package com.abhisheksati.mydoc.rendering.engine.word

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class WordPdfOptionsTest {
    @Test fun preservesImageQualityAndDocumentStructure() {
        val options = JSONObject(wordPdfOptions(null))
        assertEquals("true", options.getJSONObject("UseLosslessCompression").getString("value"))
        assertEquals("false", options.getJSONObject("ReduceImageResolution").getString("value"))
        assertFalse(options.has("EncryptFile"))
    }

    @Test fun passwordRoundTripsWithoutBecomingALegacyExportOption() {
        val secret = "a,Password=otherPASSWORDEND\"\\日本語😀"
        val encoded = wordPdfOptions(secret)
        assertFalse(encoded.contains(",Password="))
        assertEquals(secret, JSONObject(encoded).getJSONObject("DocumentOpenPassword").getString("value"))
        assertEquals("true", JSONObject(encoded).getJSONObject("EncryptFile").getString("value"))
    }
}
