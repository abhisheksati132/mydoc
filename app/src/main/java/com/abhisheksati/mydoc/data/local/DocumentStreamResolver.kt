package com.abhisheksati.mydoc.data.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

data class ResolvedDocument(
    val file: File,
    val newResolvedUri: Uri? = null,
    val source: String
)

object DocumentStreamResolver {

    /**
     * Resolves and copies any document URI into a target cache file using resilient multi-tier fallback:
     * 1. Direct ContentResolver or File stream
     * 2. Persistent internal offline cache (`filesDir/saved_docs`)
     * 3. MediaStore ID resolution from MediaDocumentsProvider URIs
     * 4. Primary storage relative path resolution
     * 5. MediaStore DISPLAY_NAME query
     * 6. Standard common storage directories (Downloads, Documents, root)
     */
    fun resolveToCacheFile(
        context: Context,
        uri: Uri,
        displayName: String?,
        targetCacheFile: File
    ): Result<ResolvedDocument> {
        val streamResult = openStreamWithFallback(context, uri, displayName)
            ?: return Result.failure(IllegalStateException("Unable to access or locate file stream for: $uri"))

        return runCatching {
            streamResult.stream.use { input ->
                targetCacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Immediately cache into saved_docs as permanent backup if not already present
            try {
                val savedDocsDir = File(context.filesDir, "saved_docs").apply { mkdirs() }
                val docKey = uri.toString().hashCode().toString(16)
                val safeName = (displayName ?: "doc.pdf").replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val backupFile = File(savedDocsDir, "${docKey}_$safeName")
                if (!backupFile.exists() || backupFile.length() == 0L) {
                    targetCacheFile.copyTo(backupFile, overwrite = true)
                }
            } catch (_: Exception) {}

            ResolvedDocument(
                file = targetCacheFile,
                newResolvedUri = streamResult.newUri,
                source = streamResult.source
            )
        }
    }

    fun openStream(
        context: Context,
        uri: Uri,
        displayName: String? = null
    ): InputStream? {
        return openStreamWithFallback(context, uri, displayName)?.stream
    }

    private data class StreamResult(
        val stream: InputStream,
        val newUri: Uri?,
        val source: String
    )

    private fun openStreamWithFallback(
        context: Context,
        uri: Uri,
        displayName: String?
    ): StreamResult? {
        val uriString = uri.toString()
        val docKey = uriString.hashCode().toString(16)
        val safeName = displayName?.replace(Regex("[^a-zA-Z0-9._-]"), "_")

        // Tier 1: Direct File or ContentResolver
        try {
            if (uri.scheme == "file" || uri.scheme.isNullOrEmpty()) {
                val filePath = uri.path ?: uriString
                val file = File(filePath)
                if (file.exists() && file.canRead() && file.length() > 0L) {
                    return StreamResult(FileInputStream(file), null, "direct_file")
                }
            } else if (uri.scheme == "content") {
                val stream = context.contentResolver.openInputStream(uri)
                if (stream != null) {
                    return StreamResult(stream, null, "content_resolver")
                }
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                if (pfd != null) {
                    return StreamResult(ParcelFileDescriptor.AutoCloseInputStream(pfd), null, "pfd")
                }
            }
        } catch (_: Throwable) {
            // Permission denial, revoked grant, or missing file
        }

        // Tier 2: Internal persistent cache in saved_docs
        try {
            val savedDocsDir = File(context.filesDir, "saved_docs")
            if (savedDocsDir.exists() && savedDocsDir.isDirectory) {
                val files = savedDocsDir.listFiles() ?: emptyArray()

                // Check by docKey prefix
                val byKey = files.firstOrNull { it.name.startsWith(docKey) && it.length() > 0L }
                if (byKey != null) {
                    return StreamResult(FileInputStream(byKey), null, "saved_docs_key")
                }

                // Check by safeName suffix
                if (!safeName.isNullOrBlank()) {
                    val byName = files.firstOrNull {
                        (it.name.endsWith(safeName, ignoreCase = true) || it.name.contains(safeName, ignoreCase = true)) && it.length() > 0L
                    }
                    if (byName != null) {
                        return StreamResult(FileInputStream(byName), null, "saved_docs_name")
                    }
                }
            }
        } catch (_: Throwable) {}

        // Tier 3: Parse MediaStore ID from MediaDocumentsProvider
        // e.g. content://com.android.providers.media.documents/document/document%3A9637 or document:9637
        try {
            if (uriString.contains("document%3A") || uriString.contains("document:")) {
                val idStr = uriString.substringAfterLast("%3A").substringAfterLast(":")
                val id = idStr.toLongOrNull()
                if (id != null) {
                    // Try MediaStore.Files
                    val mediaUri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)
                    try {
                        val stream = context.contentResolver.openInputStream(mediaUri)
                        if (stream != null) {
                            return StreamResult(stream, mediaUri, "mediastore_files_id")
                        }
                    } catch (_: Throwable) {}

                    // Try MediaStore.Downloads (API 29+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        try {
                            val dlUri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                            val stream = context.contentResolver.openInputStream(dlUri)
                            if (stream != null) {
                                return StreamResult(stream, dlUri, "mediastore_downloads_id")
                            }
                        } catch (_: Throwable) {}
                    }

                    // Query MediaStore for direct file path column _data
                    try {
                        val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
                        context.contentResolver.query(
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
                    } catch (_: Throwable) {}
                }
            }
        } catch (_: Throwable) {}

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
                val testPaths = listOf(
                    File("/storage/emulated/0", relPath),
                    File(Environment.getExternalStorageDirectory(), relPath)
                )
                for (f in testPaths) {
                    if (f.exists() && f.canRead() && f.length() > 0L) {
                        return StreamResult(FileInputStream(f), Uri.fromFile(f), "external_storage_path")
                    }
                }
            }
        } catch (_: Throwable) {}

        // Tier 5: MediaStore lookup by display name
        if (!displayName.isNullOrBlank()) {
            try {
                val projection = arrayOf(
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.DATA
                )
                context.contentResolver.query(
                    MediaStore.Files.getContentUri("external"),
                    projection,
                    "${MediaStore.Files.FileColumns.DISPLAY_NAME} = ?",
                    arrayOf(displayName),
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
                        val dataIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)

                        if (dataIdx != -1) {
                            val path = cursor.getString(dataIdx)
                            if (!path.isNullOrBlank()) {
                                val f = File(path)
                                if (f.exists() && f.canRead() && f.length() > 0L) {
                                    return StreamResult(FileInputStream(f), Uri.fromFile(f), "mediastore_name_path")
                                }
                            }
                        }

                        if (idIdx != -1) {
                            val id = cursor.getLong(idIdx)
                            val mUri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)
                            try {
                                val s = context.contentResolver.openInputStream(mUri)
                                if (s != null) {
                                    return StreamResult(s, mUri, "mediastore_name_id")
                                }
                            } catch (_: Throwable) {}
                        }
                    }
                }
            } catch (_: Throwable) {}

            // Tier 6: Common public directories
            try {
                val candidateDirs = listOf(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    File("/storage/emulated/0/Download"),
                    File("/storage/emulated/0/Documents"),
                    File("/storage/emulated/0")
                )
                for (dir in candidateDirs) {
                    if (dir.exists() && dir.isDirectory) {
                        val directFile = File(dir, displayName)
                        if (directFile.exists() && directFile.canRead() && directFile.length() > 0L) {
                            return StreamResult(FileInputStream(directFile), Uri.fromFile(directFile), "candidate_dir_direct")
                        }
                    }
                }
            } catch (_: Throwable) {}
        }

        return null
    }
}
