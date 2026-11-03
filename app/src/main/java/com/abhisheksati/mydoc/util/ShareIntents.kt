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
        val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
        val copy = File(sharedDir, file.name)
        file.copyTo(copy, overwrite = true)
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

fun openFile(context: Context, uri: Uri, mimeType: String, title: String = "Open with"): Boolean {
    val safeUri = uri.toShareableUri(context) ?: return false
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(safeUri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return runCatching { context.startActivity(Intent.createChooser(intent, title)) }.isSuccess
}
