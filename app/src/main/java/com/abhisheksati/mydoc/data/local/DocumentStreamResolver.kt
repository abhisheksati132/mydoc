package com.abhisheksati.mydoc.data.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.database.Cursor
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import com.abhisheksati.mydoc.util.DocumentIdentity

data class ResolvedDocument(
    val file: File,
    val newResolvedUri: Uri? = null,
    val source: String,
    val isFromOfflineCache: Boolean = (source == "saved_docs_key")
)

object DocumentStreamResolver {

    /**
     * Resolves and copies any document URI into a target cache file using resilient multi-tier fallback:
     * 1. Direct ContentResolver or File stream
     * 2. Persistent internal offline cache (`filesDir/saved_docs`)
     * 3. MediaStore ID resolution from MediaDocumentsProvider URIs
     * 4. Primary storage relative path resolution
     *
     * Note: Arbitrary filename fallback in MediaStore/Downloads is strictly prohibited to prevent serving wrong documents.
     */
    fun resolveToCacheFile(
        context: Context,
        uri: Uri?,
        displayName: String?,
        targetCacheFile: File
    ): Result<ResolvedDocument> {
        if (uri == null) {
            return Result.failure(java.io.FileNotFoundException("Unable to access or locate file stream for null Uri"))
        }
        val streamResult = openStreamWithFallback(context, uri)
            ?: return Result.failure(
                // The bytes were never opened, so nothing here says anything about the file's
                // health. Calling this "damaged" sends people to a converter for a permission
                // problem; the audit's own principle is that a failure names its real cause.
                com.abhisheksati.mydoc.domain.model.DocumentLoadException(
                    com.abhisheksati.mydoc.domain.model.LoadFailure.PERMISSION_EXPIRED,
                    "MyDoc could not open this file. It was never read, so it is not known to be damaged."
                )
            )

        return runCatching {
            streamResult.stream.use { input ->
                targetCacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (targetCacheFile.length() == 0L) {
                targetCacheFile.delete()
                throw java.io.IOException("File could not be loaded or is empty (0 bytes)")
            }

            // Immediately cache into saved_docs as permanent backup if not already present,
            // and overwrite cached copy when a fresh live stream is obtained from the original external source
            try {
                val savedDocsDir = File(context.filesDir, "saved_docs").apply { mkdirs() }
                val backupFile = File(savedDocsDir, DocumentIdentity.savedDocsName(uri.toString(), displayName))
                if (streamResult.source != "saved_docs_key") {
                    targetCacheFile.copyTo(backupFile, overwrite = true)
                    backupFile.setLastModified(System.currentTimeMillis())
                } else {
                    backupFile.setLastModified(System.currentTimeMillis())
                }
                trimSavedDocsCache(savedDocsDir)
            } catch (_: Exception) {}

            ResolvedDocument(
                file = targetCacheFile,
                newResolvedUri = streamResult.newUri,
                source = streamResult.source
            )
        }
    }

    /**
     * Size and modification time of the document behind [uri]. Conversion caches key on this so a
     * replaced or edited file can never be served the previous file's conversion.
     */
    fun sourceStamp(context: Context, uri: Uri?): Pair<Long, Long> {
        if (uri == null) return 0L to 0L
        if (uri.scheme == "file") {
            val file = uri.path?.let(::File)
            if (file != null && file.isFile) return file.length() to file.lastModified()
        }
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@runCatching 0L to 0L
                readLongColumn(cursor, OpenableColumns.SIZE) to
                    readLongColumn(cursor, DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            } ?: (0L to 0L)
        }.getOrDefault(0L to 0L)
    }

    private fun readLongColumn(cursor: Cursor, column: String): Long {
        val index = cursor.getColumnIndex(column)
        return if (index < 0 || cursor.isNull(index)) 0L else cursor.getLong(index)
    }

    fun openStream(
        context: Context,
        uri: Uri?
    ): InputStream? {
        if (uri == null) return null
        return openStreamWithFallback(context, uri)?.stream
    }

    private data class StreamResult(
        val stream: InputStream,
        val newUri: Uri?,
        val source: String
    )

    private fun openStreamWithFallback(
        context: Context,
        uri: Uri
    ): StreamResult? {
        val uriString = uri.toString()

        // Tier 1: Direct File or ContentResolver
        try {
            if (uri.scheme == "file" || uri.scheme.isNullOrEmpty()) {
                val filePath = uri.path ?: uriString
                val file = File(filePath)
                if (file.exists() && file.canRead() && file.length() > 0L) {
                    return StreamResult(FileInputStream(file), null, "direct_file")
                }
            } else if (uri.scheme == "content") {
                val cr = runCatching { context.contentResolver }.getOrNull()
                val stream = runCatching { cr?.openInputStream(uri) }.getOrNull()
                if (stream != null) {
                    return StreamResult(stream, null, "content_resolver")
                }
                val pfd = runCatching { cr?.openFileDescriptor(uri, "r") }.getOrNull()
                if (pfd != null) {
                    return StreamResult(ParcelFileDescriptor.AutoCloseInputStream(pfd), null, "pfd")
                }
            }
        } catch (e: Throwable) {
            // Permission denial, revoked grant, or missing file
        }

        // Tier 2: Internal persistent cache in saved_docs, matched on the URI digest only
        try {
            val savedDocsDir = File(context.filesDir, "saved_docs")
            if (savedDocsDir.exists() && savedDocsDir.isDirectory) {
                val files = savedDocsDir.listFiles() ?: emptyArray()

                // Only a copy whose name carries this exact URI identity counts; a similar
                // display name never matches.
                val byKey = files.firstOrNull {
                    !OfflineCopyPolicy.isMetaFile(it.name) && DocumentIdentity.isSavedCopyOf(it, uriString)
                }
                if (byKey != null) {
                    byKey.setLastModified(System.currentTimeMillis())
                    return StreamResult(FileInputStream(byKey), null, "saved_docs_key")
                }
            }
        } catch (e: Throwable) {}

        // Tier 3: Parse MediaStore ID from MediaDocumentsProvider
        // e.g. content://com.android.providers.media.documents/document/document%3A9637 or document:9637
        try {
            if (uriString.contains("document%3A") || uriString.contains("document:")) {
                val idStr = uriString.substringAfterLast("%3A").substringAfterLast(":")
                val id = idStr.toLongOrNull()
                if (id != null) {
                    val cr = runCatching { context.contentResolver }.getOrNull()
                    if (cr != null) {
                        // Try MediaStore.Files
                        val mediaUri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)
                        try {
                            val stream = cr.openInputStream(mediaUri)
                            if (stream != null) {
                                return StreamResult(stream, mediaUri, "mediastore_files_id")
                            }
                        } catch (e: Throwable) {}

                        // Try MediaStore.Downloads (API 29+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            try {
                                val dlUri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                                val stream = cr.openInputStream(dlUri)
                                if (stream != null) {
                                    return StreamResult(stream, dlUri, "mediastore_downloads_id")
                                }
                            } catch (e: Throwable) {}
                        }

                        // Query MediaStore for direct file path column _data
                        try {
                            val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
                            cr.query(
                                MediaStore.Files.getContentUri("external"),
                                projection,
                                "${MediaStore.Files.FileColumns._ID} = ?",
                                arrayOf(id.toString()),
                                null
                            )?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    val dataIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                                    if (dataIdx != -1) {
                                        val dataPath = cursor.getString(dataIdx)
                                        if (!dataPath.isNullOrBlank()) {
                                            val f = File(dataPath)
                                            if (f.exists() && f.canRead() && f.length() > 0L) {
                                                return StreamResult(FileInputStream(f), Uri.fromFile(f), "mediastore_data_path")
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (e: Throwable) {}
                    }
                }
            }
        } catch (e: Throwable) {}

        // Tier 4: Parse primary external storage relative path
        // e.g. content://com.android.externalstorage.documents/document/primary%3ADownload%2Freport-1.pdf
        try {
            if (uriString.contains("primary%3A") || uriString.contains("primary:")) {
                val encodedPath = if (uriString.contains("primary%3A")) {
                    uriString.substringAfter("primary%3A")
                } else {
                    uriString.substringAfter("primary:")
                }
                val relPath = Uri.decode(encodedPath)
                val roots = listOfNotNull(
                    Environment.getExternalStorageDirectory()?.takeIf { it.isDirectory },
                    File("/storage/emulated/0").takeIf { it.isDirectory }
                )
                for (root in roots) {
                    // The relative part arrives from an untrusted content URI, so it is resolved and
                    // checked for containment instead of being appended: a document id of
                    // "primary:../../..<path>" used to reach any readable file on the device.
                    val resolved = externalStorageChild(root, relPath) ?: continue
                    if (resolved.exists() && resolved.canRead() && resolved.length() > 0L) {
                        return StreamResult(FileInputStream(resolved), Uri.fromFile(resolved), "external_storage_path")
                    }
                }
            }
        } catch (e: Throwable) {}

        return null
    }

    /**
     * [relativePath] resolved inside [root], or null when it is absolute, empty, or escapes the root
     * through `..` or a symlink. Canonicalising before the comparison is what makes `a/../b` safe.
     */
    internal fun externalStorageChild(root: File, relativePath: String): File? {
        if (relativePath.isBlank() || relativePath.startsWith("/") || relativePath.startsWith("\\")) {
            return null
        }
        val canonicalRoot = runCatching { root.canonicalFile }.getOrNull() ?: return null
        val candidate = runCatching { File(canonicalRoot, relativePath).canonicalFile }.getOrNull()
            ?: return null
        if (candidate == canonicalRoot) return null
        // Canonical paths carry no trailing separator except for a filesystem root, so the boundary
        // character has to be added before the comparison — otherwise `/a` would contain `/ab/x`.
        val rootPath = canonicalRoot.path
        val prefix = if (rootPath.endsWith(File.separator)) rootPath else rootPath + File.separator
        return candidate.takeIf { it.path.startsWith(prefix) }
    }

    const val MAX_SAVED_DOCS_COUNT = 50
    const val MAX_SAVED_DOCS_BYTES = 100L * 1024 * 1024 // 100 MB

    fun trimSavedDocsCache(savedDocsDir: File) {
        // The sidecars are bookkeeping, not cached documents: they must not occupy a slot in the
        // count or the byte budget, and they go away with the copy they describe.
        val all = savedDocsDir.listFiles()?.filter { it.isFile } ?: return
        val files = all.filterNot { OfflineCopyPolicy.isMetaFile(it.name) }
        var totalBytes = files.sumOf { it.length() }
        var count = files.size
        if (count <= MAX_SAVED_DOCS_COUNT && totalBytes <= MAX_SAVED_DOCS_BYTES) return

        val sorted = files.sortedBy { it.lastModified() }
        for (f in sorted) {
            if (count > MAX_SAVED_DOCS_COUNT || totalBytes > MAX_SAVED_DOCS_BYTES) {
                val len = f.length()
                if (f.delete()) {
                    java.io.File(f.parentFile, OfflineCopyPolicy.metaFileFor(f.name)).delete()
                    totalBytes -= len
                    count--
                }
            } else {
                break
            }
        }
    }
}
