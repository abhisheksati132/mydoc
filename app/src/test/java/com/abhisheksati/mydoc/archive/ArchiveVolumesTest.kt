package com.abhisheksati.mydoc.archive

import com.abhisheksati.mydoc.data.archive.ArchiveVolumes
import com.abhisheksati.mydoc.domain.archive.ArchiveFailure
import com.abhisheksati.mydoc.domain.archive.ArchiveProblem
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ArchiveVolumesTest {
    @Test fun openingLaterNumberedAndRarPartsStartsAtTheFirst() {
        val root = Files.createTempDirectory("archive-volumes-").toFile()
        try {
            for (names in listOf(listOf("sample.7z.001", "sample.7z.002", "sample.7z.003"),
                listOf("sample.part01.rar", "sample.part02.rar", "sample.part03.rar"), listOf("old.rar", "old.r00", "old.r01"))) {
                names.forEach { File(root, it).writeText("fixture") }
                assertEquals(names, ArchiveVolumes.ordered(File(root, names.last()), 20).map { it.name })
            }
        } finally { root.deleteRecursively() }
    }

    @Test fun missingMiddleOrFirstPartCannotBeSilentlyIgnored() {
        val root = Files.createTempDirectory("archive-volumes-").toFile()
        try {
            File(root, "sample.part01.rar").writeText("fixture")
            File(root, "sample.part03.rar").writeText("fixture")
            val error = assertThrows(ArchiveFailure::class.java) { ArchiveVolumes.ordered(File(root, "sample.part03.rar"), 20) }
            assertEquals(ArchiveProblem.MISSING_VOLUMES, error.problem)
            File(root, "old.r00").writeText("fixture")
            assertThrows(ArchiveFailure::class.java) { ArchiveVolumes.ordered(File(root, "old.r00"), 20) }
        } finally { root.deleteRecursively() }
    }
}
