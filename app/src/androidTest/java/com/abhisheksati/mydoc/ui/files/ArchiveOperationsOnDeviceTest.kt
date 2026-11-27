package com.abhisheksati.mydoc.ui.files

import androidx.test.platform.app.InstrumentationRegistry
import com.abhisheksati.mydoc.data.archive.NativeArchiveEngine
import com.abhisheksati.mydoc.data.archive.ArchiveNativeFiles
import com.abhisheksati.mydoc.domain.archive.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.withTimeout
import com.abhisheksati.mydoc.data.archive.ArchiveProcessControl
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ArchiveOperationsOnDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun gnuStyleTarRootAndDotPrefixesExtractSelectedContent() = runBlocking {
        val root = Files.createTempDirectory(context.cacheDir.toPath(), "archive-dot-path-").toFile()
        try {
            val archive = File(root, "dot-prefix.tar")
            InstrumentationRegistry.getInstrumentation().context.assets.open("archive-reference/dot-prefix.tar").use { input -> archive.outputStream().use { input.copyTo(it) } }
            val engine = NativeArchiveEngine(context)
            assertEquals("folder/report.txt", engine.list(archive).files.single().path)
            val output = engine.extract(archive, root, setOf("folder/report.txt"))
            assertEquals("GNU-style path", File(output, "folder/report.txt").readText())
        } finally { root.deleteRecursively() }
    }

    @Test fun mergingSkipsOrRenamesConflictsWithoutOverwritingUserFiles() = runBlocking {
        val root = Files.createTempDirectory(context.cacheDir.toPath(), "archive-merge-").toFile()
        try {
            val source = File(root, "report.txt").apply { writeText("Incoming bytes") }
            val engine = NativeArchiveEngine(context)
            val archive = engine.create(listOf(source), root, "reference.zip", ArchiveCreateOptions()).first()
            for (conflict in listOf(ArchiveConflictPolicy.SKIP_EXISTING, ArchiveConflictPolicy.KEEP_BOTH)) {
                val destination = File(root, conflict.name).apply { mkdir() }
                val existing = File(destination, "report.txt").apply { writeText("Existing user bytes") }
                engine.extract(archive, destination, conflict = conflict)
                assertEquals("Existing user bytes", existing.readText())
                if (conflict == ArchiveConflictPolicy.KEEP_BOTH) assertEquals("Incoming bytes", File(destination, "report (1).txt").readText())
                else assertEquals(listOf("report.txt"), destination.listFiles()!!.map { it.name })
            }
        } finally { root.deleteRecursively() }
    }

    @Test fun deletingEveryMemberCreatesAValidEmptyCopy() = runBlocking {
        val root = Files.createTempDirectory(context.cacheDir.toPath(), "archive-empty-").toFile()
        try {
            val source = File(root, "single.txt").apply { writeText("Original stays intact") }
            val engine = NativeArchiveEngine(context)
            for (format in listOf(ArchiveFormat.ZIP, ArchiveFormat.SEVEN_ZIP)) {
                val original = engine.create(listOf(source), root, "reference.${format.extension}", ArchiveCreateOptions(format = format)).first()
                val copy = engine.editCopy(original, root, remove = setOf("single.txt")).first()
                assertTrue(engine.list(copy).members.isEmpty())
                engine.test(copy)
                assertEquals(1, engine.list(original).files.size)
            }
        } finally { root.deleteRecursively() }
    }

    @Test fun corruptArchiveWithPasswordInItsNameDoesNotAskForCredentials() = runBlocking {
        val root = Files.createTempDirectory(context.cacheDir.toPath(), "archive-errors-").toFile()
        try {
            for (name in listOf("password-encrypted.7z", "wrong password?.7z", "encrypted archive.7z")) {
                val file = File(root, name).apply { writeText("Not an archive") }
                val failure = runCatching { NativeArchiveEngine(context).list(file) }.exceptionOrNull()
                assertEquals(name, ArchiveProblem.CORRUPT, (failure as ArchiveFailure).problem)
            }
        } finally { root.deleteRecursively() }
    }

    @Test fun multipartRarReferenceOpensFromLaterPartsAndDetectsGaps() = runBlocking {
        val root = Files.createTempDirectory(context.cacheDir.toPath(), "archive-rar-parts-").toFile()
        try {
            val parts = (1..8).map { number ->
                val name = "test_read_format_rar5_multiarchive.part${number.toString().padStart(2, '0')}.rar"
                File(root, name).apply {
                    InstrumentationRegistry.getInstrumentation().context.assets.open("archive-reference/$name").use { input -> outputStream().use { input.copyTo(it) } }
                }
            }
            val engine = NativeArchiveEngine(context)
            val listing = engine.list(parts[3])
            assertEquals(2, listing.files.size)
            val selected = listing.files.first()
            val output = engine.extract(parts[3], root, setOf(selected.path))
            assertEquals(selected.bytes, File(output, selected.path).length())
            engine.test(parts.first())
            assertTrue(parts[4].delete())
            val error = runCatching { engine.list(parts.first()) }.exceptionOrNull()
            assertEquals(ArchiveProblem.MISSING_VOLUMES, (error as ArchiveFailure).problem)
        } finally { root.deleteRecursively() }
    }

    @Test fun nativeOutputRefusesReplacedParentsAndExistingFiles() {
        val root = Files.createTempDirectory(context.cacheDir.toPath(), "archive-path-").toFile().canonicalFile
        try {
            val outside = File(root, "outside").apply { mkdir() }
            val directory = File(outside, "nested").apply { mkdir() }
            val link = File(root, "link")
            Files.createSymbolicLink(link.toPath(), outside.toPath())
            assertThrows(java.io.IOException::class.java) { ArchiveNativeFiles.output(File(link, "nested/escape.txt")) }
            assertFalse(File(directory, "escape.txt").exists())
            val existing = File(root, "existing.txt").apply { writeText("Keep original") }
            assertThrows(java.io.IOException::class.java) { ArchiveNativeFiles.output(existing) }
            assertEquals("Keep original", existing.readText())
        } finally { root.deleteRecursively() }
    }

    @Test fun independentRar5ReferenceExtractsAndRar4LinksAreRejected() = runBlocking {
        val root = Files.createTempDirectory(context.cacheDir.toPath(), "archive-rar-").toFile()
        try {
            fun fixture(name: String): File = File(root, name).apply {
                InstrumentationRegistry.getInstrumentation().context.assets.open("archive-reference/$name").use { input -> outputStream().use { input.copyTo(it) } }
            }
            val engine = NativeArchiveEngine(context)
            val rar5 = fixture("rar5.rar")
            val listing = engine.list(rar5)
            assertEquals("helloworld.txt", listing.files.single().path)
            assertEquals("hello libarchive test suite!\n", File(engine.extract(rar5, root), "helloworld.txt").readText())
            engine.test(rar5)
            val rar4 = fixture("rar4.rar")
            assertTrue(runCatching { engine.extract(rar4, root) }.isFailure)
            assertFalse(File(root, "testlink").exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun archivesPublishOnEmulatedExternalStorage() = runBlocking {
        val root = Files.createTempDirectory(requireNotNull(context.getExternalFilesDir(null)).toPath(), "archive-external-").toFile()
        try {
            val source = File(root, "source.txt").apply { writeText("External storage") }
            val engine = NativeArchiveEngine(context)
            val archive = engine.create(listOf(source), root, "external.zip", ArchiveCreateOptions()).first()
            assertEquals(source.readText(), File(engine.extract(archive, root), source.name).readText())
        } finally { root.deleteRecursively() }
    }

    @Test fun editedCopyRenamesRemovesAndAddsWithoutChangingOriginal() = runBlocking {
        val root = Files.createTempDirectory(context.cacheDir.toPath(), "archive-edit-").toFile()
        try {
            val first = File(root, "first.txt").apply { writeText("Keep this content") }
            val removed = File(root, "remove.txt").apply { writeText("Remove from copy") }
            val added = File(root, "added.txt").apply { writeText("New content") }
            val engine = NativeArchiveEngine(context)
            for (format in listOf(ArchiveFormat.ZIP, ArchiveFormat.SEVEN_ZIP)) {
                val archive = engine.create(listOf(first, removed), root, "original.${format.extension}", ArchiveCreateOptions(format = format)).first()
                val originalBytes = archive.readBytes()
                val edited = engine.editCopy(archive, root, setOf("remove.txt"), mapOf("first.txt" to "renamed.txt"), listOf(added)).first()
                assertEquals(setOf("renamed.txt", "added.txt"), engine.list(edited).files.map { it.path }.toSet())
                val output = engine.extract(edited, root)
                assertEquals(first.readText(), File(output, "renamed.txt").readText())
                assertEquals(added.readText(), File(output, "added.txt").readText())
                assertArrayEquals(originalBytes, archive.readBytes())
            }
        } finally { root.deleteRecursively() }
    }

    @Test fun encrypted7zRoundTripAndWrongPasswordRecovery() = runBlocking {
        val root = Files.createTempDirectory(context.cacheDir.toPath(), "archive-test-").toFile()
        val password = "fixture-pass-Δ".toCharArray()
        try {
            val source = File(root, "reference.txt").apply { writeText("Archive round trip ✓") }
            val engine = NativeArchiveEngine(context)
            val archive = engine.create(listOf(source), root, "protected.7z",
                ArchiveCreateOptions(format = ArchiveFormat.SEVEN_ZIP, encryptNames = true), password).first()
            assertTrue(engine.list(archive, password).encrypted)
            val wrong = runCatching { engine.list(archive, "incorrect".toCharArray()) }.exceptionOrNull()
            assertEquals(ArchiveProblem.WRONG_PASSWORD, (wrong as ArchiveFailure).problem)
            val missing = runCatching { engine.list(archive) }.exceptionOrNull()
            assertEquals(ArchiveProblem.PASSWORD_REQUIRED, (missing as ArchiveFailure).problem)
            val extracted = engine.extract(archive, root, setOf("reference.txt"), password)
            assertEquals(source.readText(), File(extracted, "reference.txt").readText())
            engine.test(archive, password)
            assertEquals("Archive round trip ✓", source.readText())
        } finally { password.fill('\u0000'); root.deleteRecursively() }
    }

    @Test fun zipRoundTripPreservesNestedSelectionEmptyFilesAndDirectories() = runBlocking {
        val root = Files.createTempDirectory(context.cacheDir.toPath(), "archive-test-").toFile()
        try {
            val source = File(root, "input").apply { mkdirs() }
            File(source, "nested").mkdir()
            File(source, "empty-folder").mkdir()
            File(source, "nested/日本語.txt").writeText("UTF-8 content")
            File(source, "nested/empty.txt").writeBytes(byteArrayOf())
            File(source, "not-selected.txt").writeText("Keep out")
            val engine = NativeArchiveEngine(context)
            val archive = engine.create(listOf(source), root, "nested.zip", ArchiveCreateOptions()).first()
            val output = engine.extract(archive, root, setOf("input/nested", "input/empty-folder"))
            assertEquals("UTF-8 content", File(output, "input/nested/日本語.txt").readText())
            assertEquals(0, File(output, "input/nested/empty.txt").length())
            assertTrue(File(output, "input/empty-folder").isDirectory)
            assertFalse(File(output, "input/not-selected.txt").exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun splitVolumesReopenAndMissingPartsAreExplained() = runBlocking {
        val root = Files.createTempDirectory(context.cacheDir.toPath(), "archive-test-").toFile()
        try {
            val bytes = ByteArray(3 * 1024 * 1024).also { java.util.Random(43).nextBytes(it) }
            val source = File(root, "large.bin").apply { writeBytes(bytes) }
            val engine = NativeArchiveEngine(context)
            val parts = engine.create(listOf(source), root, "parts.7z",
                ArchiveCreateOptions(format = ArchiveFormat.SEVEN_ZIP, splitBytes = 1024 * 1024))
            assertTrue(parts.size >= 3)
            engine.test(parts.first())
            val output = engine.extract(parts.first(), root, setOf("large.bin"))
            assertArrayEquals(bytes, File(output, "large.bin").readBytes())
            assertTrue(parts[1].renameTo(File(parts[1].parentFile, "held-part")))
            val error = runCatching { engine.list(parts.first()) }.exceptionOrNull()
            assertEquals(ArchiveProblem.MISSING_VOLUMES, (error as ArchiveFailure).problem)
        } finally { root.deleteRecursively() }
    }

    @Test fun hostileEntryNeverWritesOutsideTheExtractionWorkspace() = runBlocking {
        val root = Files.createTempDirectory(context.cacheDir.toPath(), "archive-test-").toFile()
        try {
            val archive = File(root, "hostile.zip")
            ZipOutputStream(archive.outputStream()).use {
                it.putNextEntry(ZipEntry("../outside.txt")); it.write(byteArrayOf(1)); it.closeEntry()
            }
            val engine = NativeArchiveEngine(context)
            assertTrue(runCatching { engine.extract(archive, root) }.isFailure)
            assertFalse(File(root.parentFile, "outside.txt").exists())
            assertEquals(listOf("hostile.zip"), root.listFiles()!!.map { it.name })
        } finally { root.deleteRecursively() }
    }

    @Test fun atomicPublicationNeverReplacesAnExistingUserFile() {
        val root = Files.createTempDirectory(context.cacheDir.toPath(), "archive-test-").toFile().canonicalFile
        try {
            val staged = File(root, "staged").apply { writeText("new data") }
            val existing = File(root, "existing").apply { writeText("user data") }
            assertThrows(java.nio.file.FileAlreadyExistsException::class.java) { ArchiveNativeFiles.move(staged, existing) }
            assertEquals("user data", existing.readText())
            assertEquals("new data", staged.readText())
            val published = File(root, "published")
            ArchiveNativeFiles.move(staged, published)
            assertEquals("new data", published.readText())
            assertFalse(staged.exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun aesZipAndTarCreationCanBeVerifiedAndExtracted() = runBlocking {
        val root = Files.createTempDirectory(context.cacheDir.toPath(), "archive-test-").toFile()
        val password = "zip-fixture-pass".toCharArray()
        try {
            val source = File(root, "brief.txt").apply { writeText("Verified content") }
            val engine = NativeArchiveEngine(context)
            for (format in listOf(ArchiveFormat.ZIP, ArchiveFormat.TAR, ArchiveFormat.TAR_GZIP, ArchiveFormat.TAR_BZIP2, ArchiveFormat.TAR_XZ)) {
                val secret = password.takeIf { format == ArchiveFormat.ZIP }
                val archive = engine.create(listOf(source), root, "reference.${format.extension}", ArchiveCreateOptions(format = format), secret).first()
                if (format == ArchiveFormat.ZIP) {
                    val missing = runCatching { engine.extract(archive, root) }.exceptionOrNull()
                    assertEquals(ArchiveProblem.PASSWORD_REQUIRED, (missing as ArchiveFailure).problem)
                    val wrong = runCatching { engine.extract(archive, root, password = "incorrect".toCharArray()) }.exceptionOrNull()
                    assertEquals(ArchiveProblem.WRONG_PASSWORD, (wrong as ArchiveFailure).problem)
                }
                engine.test(archive, secret)
                val output = engine.extract(archive, root, password = secret)
                assertEquals(source.readText(), File(output, source.name).readText())
            }
        } finally { password.fill('\u0000'); root.deleteRecursively() }
    }

    @Test fun windowsPathsAndLiteralWildcardsExtractWithoutPatternExpansion() = runBlocking {
        val root = Files.createTempDirectory(context.cacheDir.toPath(), "archive-windows-").toFile()
        try {
            val archive = File(root, "windows.zip")
            ZipOutputStream(archive.outputStream()).use { zip ->
                for ((name, text) in listOf("windows\\literal[1]*?.txt" to "Literal bytes", "windows\\other.txt" to "Exclude this")) {
                    zip.putNextEntry(ZipEntry(name)); zip.write(text.toByteArray()); zip.closeEntry()
                }
            }
            val engine = NativeArchiveEngine(context)
            val output = engine.extract(archive, root, setOf("windows/literal[1]*?.txt"))
            assertEquals("Literal bytes", File(output, "windows/literal[1]*?.txt").readText())
            assertFalse(File(output, "windows/other.txt").exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun pausedNativeWorkCanResumeAndCancelWithoutPublishingPartialOutput() = runBlocking {
        val root = Files.createTempDirectory(context.cacheDir.toPath(), "archive-test-").toFile()
        try {
            val source = File(root, "large.bin")
            source.writeBytes(ByteArray(32 * 1024 * 1024).also { java.util.Random(71).nextBytes(it) })
            val started = CompletableDeferred<ArchiveProcessControl>()
            val engine = NativeArchiveEngine(context, onControl = { if (it != null && !started.isCompleted) started.complete(it) })
            val job = launch { engine.create(listOf(source), root, "cancelled.7z", ArchiveCreateOptions(format = ArchiveFormat.SEVEN_ZIP)) }
            val control = withTimeout(20_000) { started.await() }
            assertTrue(control.pause())
            assertTrue(control.resume())
            job.cancelAndJoin()
            assertFalse(File(root, "cancelled.7z").exists())
            assertEquals(32L * 1024 * 1024, source.length())
        } finally { root.deleteRecursively() }
    }
}
