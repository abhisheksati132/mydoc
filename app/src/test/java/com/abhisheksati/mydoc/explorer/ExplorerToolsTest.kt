package com.abhisheksati.mydoc.explorer

import com.abhisheksati.mydoc.data.local.*
import com.abhisheksati.mydoc.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class ExplorerToolsTest {
    @get:Rule val temp = TemporaryFolder()
    private fun zip(vararg names: String): File = temp.newFile().also { file ->
        ZipOutputStream(file.outputStream()).use { stream -> names.forEach { name ->
            stream.putNextEntry(ZipEntry(name)); stream.write("content".toByteArray()); stream.closeEntry()
        } }
    }

    @Test fun archiveInfersFoldersWithoutExtracting() = runBlocking {
        val file = zip("docs/report.txt", "photo.jpg")
        val index = ExplorerArchive().read(file)
        assertEquals(listOf("docs", "photo.jpg"), index.children("").map { it.name })
        assertEquals("report.txt", index.children("docs/").single().name)
        assertEquals(7L, index.children("docs/").single().bytes)
        assertFalse(File(temp.root, "docs").exists())
    }

    @Test fun archiveRejectsTraversalAndAmbiguousTrees() = runBlocking {
        for (file in listOf(zip("../escape.txt"), zip("/absolute.txt"), zip("a", "a/b.txt"))) {
            try { ExplorerArchive().read(file); fail("Unsafe archive accepted") } catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun archiveHonorsEntryLimit() = runBlocking {
        try { ExplorerArchive(maxEntries = 1).read(zip("a", "b")); fail("Limit ignored") }
        catch (_: IllegalArgumentException) { }
    }

    @Test fun insightsAggregateFoldersAndLargestFiles() = runBlocking {
        val root = temp.newFolder()
        root.resolve("docs").mkdir()
        root.resolve("docs/a").writeText("12345")
        root.resolve(".hidden").writeText("12")
        val result = ExplorerFolderInsights().scan(root)
        assertEquals(7L, result.totalBytes)
        assertEquals(2, result.fileCount)
        assertEquals(5L, result.folders.single().bytes)
        assertEquals("a", result.largestFiles.first().file.name)
        assertFalse(result.incomplete)
    }

    @Test fun insightsCapsTraversalAndSkipsSymlinks() = runBlocking {
        val root = temp.newFolder()
        root.resolve("a").writeText("abc")
        root.resolve("b").writeText("def")
        assertTrue(ExplorerFolderInsights(maxEntries = 1).scan(root).incomplete)
        // Inject the filesystem boundary: creating Windows symlinks requires OS privileges.
        root.resolve("link").writeText("outside")
        val result = ExplorerFolderInsights(isSymbolicLink = { it.name == "link" }).scan(root)
        assertEquals(6L, result.totalBytes)
        assertTrue(result.incomplete)
    }

    @Test fun transfersFinishCurrentItemBeforeCancelAndPreserveRemaining() = runTest {
        val center = ExplorerTransfers()
        val files = listOf(File("a"), File("b"))
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var report: ExplorerTransferReport? = null
        center.start(this, ClipboardState(ClipboardOperation.COPY, files), File("target"), { _, _, _ ->
            entered.complete(Unit); release.await(); Result.success(1)
        }) { report = it }
        entered.await()
        center.cancel()
        assertEquals(TransferStatus.STOPPING, center.state.value.first().status)
        release.complete(Unit)
        advanceUntilIdle()
        assertEquals(TransferStatus.CANCELLED, center.state.value.first().status)
        assertEquals(1, report?.completed)
        assertEquals(listOf(File("b")), report?.remaining)
    }

    @Test fun transfersContinueFailuresAndRejectConcurrentStart() = runTest {
        val center = ExplorerTransfers()
        val gate = CompletableDeferred<Unit>()
        var report: ExplorerTransferReport? = null
        val clip = ClipboardState(ClipboardOperation.CUT, listOf(File("bad"), File("good")))
        assertTrue(center.start(this, clip, File("target"), { _, files, _ ->
            gate.await()
            if (files.single().name == "bad") Result.failure(java.io.IOException()) else Result.success(1)
        }) { report = it })
        assertFalse(center.start(this, clip, File("other"), { _, _, _ -> Result.success(1) }) {})
        gate.complete(Unit); advanceUntilIdle()
        assertEquals(TransferStatus.PARTIAL, center.state.value.first().status)
        assertEquals(1, report?.completed)
        assertEquals(listOf(File("bad")), report?.remaining)
        center.clearFinished()
        assertTrue(center.state.value.isEmpty())
    }
    @Test fun transferErrorsRetainPartialMoveRecoveryReason() = runTest {
        val center = ExplorerTransfers()
        val error = PartialStorageOperationException(0, 1, retainedSources = listOf("source"))
        center.start(this, ClipboardState(ClipboardOperation.CUT, listOf(File("source"))), File("target"),
            { _, _, _ -> Result.failure(error) }) {}
        advanceUntilIdle()
        assertSame(error, center.state.value.first().failures.single().cause)
        assertEquals("source", center.state.value.first().failures.single().name)
    }

    @Test fun storageSpaceGuardGetsActionableMessage() {
        val message = com.abhisheksati.mydoc.util.UserMessages.actionFailed(
            com.abhisheksati.mydoc.R.string.transfers_copy,
            java.io.IOException("Not enough free space on storage for this copy"))
        assertTrue(message is com.abhisheksati.mydoc.util.UiText.Res)
        val reason = (message as com.abhisheksati.mydoc.util.UiText.Res).args.last()
        assertEquals(com.abhisheksati.mydoc.util.UiText.Res(com.abhisheksati.mydoc.R.string.user_reason_no_space), reason)
    }

}
