package com.abhisheksati.mydoc.discovery

import org.junit.Assert.*
import org.junit.Test

class DiscoveryRulesTest {
    @Test fun ftsOperatorsAreAlwaysQuotedAsWords() {
        assertEquals("\"budget*\" AND \"OR*\" AND \"secret*\"",contentMatchQuery("budget OR \"secret\"*"))
        assertEquals("",contentMatchQuery("\"* () - +"))
    }
    @Test fun unicodeQueriesAndTagIdentityArePreserved() {
        assertEquals("\"café*\" AND \"समाचार*\"",contentMatchQuery("café समाचार"))
        assertEquals(listOf("work","receipts"),normalizedTags(" Work, receipts,WORK, ,"))
    }
    @Test fun requestsAreBounded() {
        assertTrue(contentMatchQuery((1..100).joinToString(" "){"token$it"}).split(" AND ").size<=12)
        assertEquals(20,normalizedTags((1..100).joinToString(","){"tag$it"}).size)
        assertEquals(40,normalizedTags("a".repeat(100)).single().length)
    }
}
