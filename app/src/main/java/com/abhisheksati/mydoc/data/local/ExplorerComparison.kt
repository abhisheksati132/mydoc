package com.abhisheksati.mydoc.data.local

import com.abhisheksati.mydoc.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.security.MessageDigest
import java.util.ArrayDeque

/** A bounded, read-only comparison. An incomplete read must never become a claimed match. */
class ExplorerComparison(
    private val maxEntries: Int = 5_000,
    private val maxReadBytes: Long = 512L * 1024 * 1024,
    private val afterScan: () -> Unit = {}
) {
    private data class Root(val original: File, val canonical: File)
    private data class Entry(val file: File, val root: Root, val directory: Boolean, val size: Long, val modified: Long)
    private data class Scan(val entries: Map<String, Entry>, val incomplete: Boolean, val root: Root, val modified: Long)
    private class ReadLimit : java.io.IOException()
    private class Budget(var remaining: Long)

    suspend fun compare(left: File, right: File): FolderComparison = withContext(Dispatchers.IO) {
        require(left.isDirectory && right.isDirectory)
        require(!Files.isSymbolicLink(left.toPath()) && !Files.isSymbolicLink(right.toPath()))
        require(left.canonicalFile != right.canonicalFile)
        val leftScan = scan(left)
        val rightScan = scan(right)
        afterScan()
        val a = validateScan(leftScan)
        val b = validateScan(rightScan)
        var incomplete = a.incomplete || b.incomplete
        val budget = Budget(maxReadBytes.coerceAtLeast(0))
        val results = mutableListOf<ComparisonEntry>()
        for (path in (a.entries.keys + b.entries.keys).sorted()) {
            currentCoroutineContext().ensureActive()
            val one = a.entries[path]
            val two = b.entries[path]
            if (one?.directory == true && two?.directory == true) continue
            val status = when {
                one != null && !unchanged(one) || two != null && !unchanged(two) -> ComparisonStatus.UNKNOWN
                one == null -> if (a.incomplete) ComparisonStatus.UNKNOWN else ComparisonStatus.RIGHT_ONLY
                two == null -> if (b.incomplete) ComparisonStatus.UNKNOWN else ComparisonStatus.LEFT_ONLY
                one.directory != two.directory -> ComparisonStatus.CHANGED
                one.size != two.size -> ComparisonStatus.CHANGED
                one.size > budget.remaining / 2 -> ComparisonStatus.UNKNOWN
                else -> try {
                    val first = digest(one, budget)
                    val second = digest(two, budget)
                    check(unchanged(one) && unchanged(two))
                    if (first.contentEquals(second)) ComparisonStatus.MATCHING else ComparisonStatus.CHANGED
                } catch (e: CancellationException) { throw e }
                catch (_: java.io.IOException) { ComparisonStatus.UNKNOWN }
                catch (_: SecurityException) { ComparisonStatus.UNKNOWN }
                catch (_: IllegalStateException) { ComparisonStatus.UNKNOWN }
            }
            if (status == ComparisonStatus.UNKNOWN) incomplete = true
            results += ComparisonEntry(path, status, one?.directory == true || two?.directory == true)
        }
        FolderComparison(results, incomplete)
    }

    private suspend fun scan(root: File): Scan {
        val location = Root(root, root.canonicalFile)
        val modified = location.canonical.lastModified()
        val queue = ArrayDeque<File>().apply { add(location.canonical) }
        val entries = linkedMapOf<String, Entry>()
        var visited = 0
        var incomplete = false
        scan@ while (queue.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val directory = queue.removeFirst()
            if (!safeLocation(directory, location)) { incomplete = true; continue }
            val children = try { directory.listFiles() } catch (_: SecurityException) { null }
            if (children == null) { incomplete = true; continue }
            for (file in children) {
                currentCoroutineContext().ensureActive()
                if (++visited > maxEntries) { incomplete = true; break@scan }
                try {
                    if (!safeLocation(file, location) || !file.canRead()) { incomplete = true; continue }
                    val directoryEntry = file.isDirectory
                    if (!directoryEntry && !file.isFile) { incomplete = true; continue }
                    entries[file.relativeTo(location.canonical).invariantSeparatorsPath] = Entry(file, location, directoryEntry, file.length(), file.lastModified())
                    if (directoryEntry) queue.add(file)
                } catch (_: java.io.IOException) { incomplete = true }
                catch (_: SecurityException) { incomplete = true }
            }
        }
        return Scan(entries, incomplete, location, modified)
    }

    private fun validateScan(scan: Scan): Scan = scan.copy(incomplete = scan.incomplete ||
        !safeLocation(scan.root.canonical, scan.root) || !scan.root.canonical.isDirectory ||
        scan.root.canonical.lastModified() != scan.modified || scan.entries.values.any { it.directory && !unchanged(it) })

    private fun safeLocation(file: File, root: Root): Boolean = try {
        !Files.isSymbolicLink(file.toPath()) && root.original.canonicalFile == root.canonical &&
            file.canonicalFile.toPath() == file.toPath().toAbsolutePath().normalize() &&
            file.toPath().startsWith(root.canonical.toPath())
    } catch (_: java.io.IOException) { false }
    catch (_: SecurityException) { false }

    private fun unchanged(entry: Entry): Boolean = safeLocation(entry.file, entry.root) &&
        (if (entry.directory) entry.file.isDirectory else entry.file.isFile) &&
        entry.file.length() == entry.size && entry.file.lastModified() == entry.modified

    private suspend fun digest(entry: Entry, budget: Budget): ByteArray {
        check(unchanged(entry))
        val digest = MessageDigest.getInstance("SHA-256")
        var read = 0L
        Files.newInputStream(entry.file.toPath(), LinkOption.NOFOLLOW_LINKS).buffered().use { stream ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                currentCoroutineContext().ensureActive()
                check(safeLocation(entry.file, entry.root))
                val count = stream.read(buffer)
                if (count == -1) break
                if (count > budget.remaining) throw ReadLimit()
                budget.remaining -= count
                read += count
                digest.update(buffer, 0, count)
            }
        }
        check(read == entry.size && unchanged(entry))
        return digest.digest()
    }
}
