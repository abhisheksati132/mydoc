package com.abhisheksati.mydoc.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** General file installation is deliberately separate from the stricter MyDoc updater. */
object LocalApkInstaller {
    const val MIME = "application/vnd.android.package-archive"

    suspend fun stage(context: Context, source: Uri, prefix: String = "package"): Uri = withContext(Dispatchers.IO) {
        require(prefix in setOf("package", "office-engine"))
        val directory = File(context.cacheDir, "shared/installers").apply { mkdirs() }
        directory.listFiles()?.filter { it.isFile && it.lastModified() < System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000 }
            ?.forEach { it.delete() }
        // Never overwrite an APK that Android's installer may still be reading.
        val target = File(directory, "$prefix-${UUID.randomUUID()}.apk")
        try {
            val input = if (source.scheme == "file") File(requireNotNull(source.path)).inputStream()
                else requireNotNull(context.contentResolver.openInputStream(source))
            input.use { from -> target.outputStream().use { to -> from.copyTo(to) } }
            require(target.length() > 0) { "The APK is empty" }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
        } catch (error: Throwable) {
            target.delete()
            throw error
        }
    }

    /** Call off the UI thread once Android reports that the engine is installed. */
    fun clearOfficeEngineStaging(context: Context) {
        val directory = File(context.cacheDir, "shared/installers").canonicalFile
        directory.listFiles()?.filter { it.isFile && it.name.startsWith("office-engine-") && it.extension == "apk" }
            ?.forEach { file -> if (file.canonicalFile.parentFile == directory) file.delete() }
    }

    fun open(context: Context, uri: Uri) {
        context.startActivity(intentFor(uri))
    }

    fun intentFor(uri: Uri): Intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, MIME)
            clipData = ClipData.newRawUri("APK", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
}
