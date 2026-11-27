package com.abhisheksati.mydoc.archive

import com.abhisheksati.mydoc.data.archive.ArchiveListingParser
import com.abhisheksati.mydoc.domain.archive.ArchivePolicy
import org.junit.Assert.*
import org.junit.Test

class ArchiveListingParserTest {
    private fun listing(vararg entries: String) = "7-Zip fixture\nPath = fixture.7z\nType = 7z\n----------\n" + entries.joinToString("\n\n") + "\n"
    private fun file(path: String, bytes: Long = 3) = "Path = $path\nSize = $bytes\nAttributes = A -rw-r--r--\nCRC = 352441C2\nEncrypted = +"

    @Test fun metadataFieldFloodIsRejectedBeforeGrowingAnUnboundedMap() {
        val fields = (1..129).joinToString("\n") { "Custom$it = value" }
        assertThrows(IllegalArgumentException::class.java) { ArchiveListingParser().parse(listing(file("a") + "\n" + fields)) }
    }

    @Test fun gnuTarDotPrefixesNormalizeWithoutLosingExactEngineSelectors() {
        val root = "Path = .\nFolder = +\nSize = 0"
        val result = ArchiveListingParser().parse(listing(root, file("./folder/report.txt")))
        assertEquals(listOf("folder/report.txt"), result.files.map { it.path })
        assertEquals("./folder/report.txt", result.files.single().selector)
        assertThrows(IllegalArgumentException::class.java) { ArchiveListingParser().parse(listing(file("./../outside"))) }
    }

    @Test fun preservesFileOrderCreatesParentsAndReadsIntegrityMetadata() {
        val result = ArchiveListingParser().parse(listing(file("folder/a.txt"), file("b.txt")))
        assertEquals("7z", result.type)
        assertTrue(result.members.first { it.path == "folder" }.directory)
        assertEquals(listOf("folder/a.txt", "b.txt"), result.members.filterNot { it.directory }.map { it.path })
        assertEquals(6L, result.totalBytes)
        assertTrue(result.encrypted)
        assertEquals(0x352441C2L, result.members.last().crc32)
    }

    @Test fun rejectsPathsThatCanEscapeOrConfuseSelectors() {
        for (path in listOf("../outside", "/absolute", "C:/drive", "a/../b", "a//b", "a\u0000b", "a\nb")) {
            assertThrows("Unsafe path: $path", IllegalArgumentException::class.java) { ArchiveListingParser().parse(listing(file(path))) }
        }
    }

    @Test fun rejectsDuplicatesFileFolderCollisionsAndLinks() {
        for (text in listOf(listing(file("a"), file("a")), listing(file("a"), file("a/b")),
            listing(file("a") + "\nSymbolic Link = /outside"), listing(file("a") + "\nHard Link = target"))) {
            assertThrows(IllegalArgumentException::class.java) { ArchiveListingParser().parse(text) }
        }
    }

    @Test fun rejectsOverflowAndEnforcesExpandedByteAndEntryBudgets() {
        assertThrows(IllegalArgumentException::class.java) {
            ArchiveListingParser(ArchivePolicy(maxExpandedBytes = 5)).parse(listing(file("a"), file("b")))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ArchiveListingParser(ArchivePolicy(maxEntries = 1)).parse(listing(file("a"), file("b")))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ArchiveListingParser().parse(listing(file("a", Long.MAX_VALUE)))
        }
    }

    @Test fun rejectsRepeatedMetadataAndUnexpectedRecordLines() {
        assertThrows(IllegalArgumentException::class.java) { ArchiveListingParser().parse(listing(file("a") + "\nSize = 0")) }
        assertThrows(IllegalArgumentException::class.java) { ArchiveListingParser().parse(listing(file("a") + "\nforged line")) }
    }

    @Test fun explicitDirectoryCanFollowAnImpliedParent() {
        val result = ArchiveListingParser().parse(listing(file("folder/a.txt"), "Path = folder\nSize = 0\nAttributes = D drwxr-xr-x"))
        assertEquals(2, result.members.size)
        assertEquals(3L, result.totalBytes)
    }
}
