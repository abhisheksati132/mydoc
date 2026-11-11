package com.abhisheksati.mydoc.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

private fun authority(context: Context) = "${context.packageName}.fileprovider"

/**
 * Returns a URI another app can read. Content URIs pass through; file URIs are exposed
 * through FileProvider, falling back to a copy in cache/shared when the file sits outside
 * the configured provider paths.
 */
fun Uri.toShareableUri(context: Context): Uri? {
    if (scheme == "content") return this
    if (scheme != null && scheme != "file") return null
    val path = path ?: return null
    val file = File(path)
    if (!file.isFile) return null

    runCatching { FileProvider.getUriForFile(context, authority(context), file) }
        .getOrNull()
        ?.let { return it }

    return runCatching {
        // Opportunistic cleanup of stale shared directories older than 24 hours
        val rootShared = File(context.cacheDir, "shared")
        if (rootShared.isDirectory) {
            val oneDayAgo = System.currentTimeMillis() - 86_400_000L
            rootShared.listFiles()?.forEach { dir ->
                if (dir.isDirectory && dir.lastModified() < oneDayAgo) {
                    dir.deleteRecursively()
                }
            }
        }
        // One directory per handoff: a basename-only staging path made two same-named files share a
        // URI, so the recipient got the last copy twice and an earlier grant could read it.
        val sharedDir = File(rootShared, java.util.UUID.randomUUID().toString()).apply { mkdirs() }
        val copy = File(sharedDir, file.name)
        file.copyTo(copy, overwrite = false)
        FileProvider.getUriForFile(context, authority(context), copy)
    }.getOrNull()
}

fun shareFile(context: Context, uri: Uri, mimeType: String, title: String): Boolean {
    val safeUri = uri.toShareableUri(context) ?: return false
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, safeUri)
        clipData = ClipData.newRawUri(null, safeUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return runCatching { context.startActivity(Intent.createChooser(intent, title)) }.isSuccess
}

/**
 * Multiple attachments need ACTION_SEND_MULTIPLE, but receivers only get read grants for the
 * URIs the chooser can see, so every URI goes into the clip data as well as the extra.
 */
fun shareUris(context: Context, uris: List<Uri>, title: String): Boolean {
    val safeUris = uris.mapNotNull { it.toShareableUri(context) }
    if (safeUris.isEmpty()) return false
    val intent = if (safeUris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, safeUris[0])
            clipData = ClipData.newRawUri(null, safeUris[0])
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(safeUris))
            clipData = ClipData("Shared documents", arrayOf("*/*"), ClipData.Item(safeUris[0])).apply {
                safeUris.drop(1).forEach { addItem(ClipData.Item(it)) }
            }
        }
    }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return runCatching { context.startActivity(Intent.createChooser(intent, title)) }.isSuccess
}

fun openFile(context: Context, uri: Uri, mimeType: String, title: String = "Open with"): Boolean {
    val safeUri = uri.toShareableUri(context) ?: return false
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(safeUri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return runCatching { context.startActivity(Intent.createChooser(intent, title)) }.isSuccess
}
