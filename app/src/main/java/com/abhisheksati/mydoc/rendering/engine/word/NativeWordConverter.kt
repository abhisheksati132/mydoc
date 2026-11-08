package com.abhisheksati.mydoc.rendering.engine.word

import android.content.Context
import android.net.Uri
import com.abhisheksati.mydoc.rendering.engine.MuPdfRenderer
import com.abhisheksati.mydoc.domain.model.LoadFailure
import com.abhisheksati.mydoc.rendering.engine.OfficeConversion
import com.abhisheksati.mydoc.rendering.engine.OfficeSource
import com.abhisheksati.mydoc.rendering.engine.OfficeSourceReader
import com.abhisheksati.mydoc.rendering.engine.asConversionFailure
import com.abhisheksati.mydoc.util.DocumentIdentity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal object NativeWordConverter {
    private val conversions = Mutex()

    suspend fun convert(context: Context, uri: Uri, displayName: String, password: String? = null): OfficeConversion = conversions.withLock {
        withContext(Dispatchers.IO) {
            if (password != null && password.length > 1024) return@withContext locked()
            val source = OfficeSourceReader.read(context, uri, displayName)
            val bytes = when (source) {
                is OfficeSource.Ready -> source.bytes
                is OfficeSource.Failed -> return@withContext source.asConversionFailure()
            }
            ensureActive()
            val directory = File(context.cacheDir, "converted_docs").apply { mkdirs() }
            val target = File(directory, "word_native_${NativeWordCore.VERSION}_r3_${DocumentIdentity.ofBytes(bytes)}.pdf")
            if (target.isFile && target.length() > 16L && validPdf(target, null))
                return@withContext OfficeConversion.Success(target)
            val jobs = File(context.cacheDir, "word_engine_jobs").apply { mkdirs() }
            val job = Files.createTempDirectory(jobs.toPath(), "word-").toFile()
            try {
                val extension = displayName.substringAfterLast('.', "docx").lowercase(java.util.Locale.ROOT)
                    .takeIf { it in setOf("docx", "dotx", "doc", "dot", "odt", "ott", "fodt", "rtf") } ?: "docx"
                val input = File(job, "input.$extension").apply { writeBytes(bytes) }
                val output = File(job, "output.pdf")
                val result = withTimeout(60_000) {
                    var reply = WordEngineConnection.convert(context.applicationContext, input, output, password)
                    if (reply.busy || (!reply.success && reply.processId < 0)) {
                        delay(200)
                        reply = WordEngineConnection.convert(context.applicationContext, input, output, password)
                    }
                    reply
                }
                ensureActive()
                if (result.passwordRequired) return@withContext locked()
                if (!result.success || result.processId == android.os.Process.myPid()) return@withContext failure()
                if (!validPdf(output, result.renderPassword)) return@withContext failure()
                if (result.renderPassword != null) {
                    val temporary = File.createTempFile("word_private_", ".pdf", directory)
                    try {
                        Files.move(output.toPath(), temporary.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                        OfficeConversion.Success(temporary, renderPassword = result.renderPassword, deleteWhenClosed = true)
                    } catch (error: Throwable) { temporary.delete(); throw error }
                } else {
                    Files.move(output.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                    OfficeConversion.Success(target)
                }
            } catch (_: TimeoutCancellationException) {
                OfficeConversion.Failure(LoadFailure.CONVERSION_FAILED, "This Word document took too long to render.")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                failure()
            } finally {
                // Only this call's freshly created private job directory is removed.
                check(job.canonicalFile.parentFile == jobs.canonicalFile)
                job.deleteRecursively()
            }
        }
    }

    private fun validPdf(file: File, password: String?): Boolean = runCatching {
        val renderer = MuPdfRenderer(filePath = file.absolutePath, mimeType = "application/pdf")
        try {
            if (renderer.isPasswordProtected) {
                if (password == null || !renderer.authenticatePassword(password)) return@runCatching false
            }
            renderer.pageCount in 1..4000
        } finally { renderer.close() }
    }.getOrDefault(false)

    private fun locked() = OfficeConversion.Failure(LoadFailure.PASSWORD_PROTECTED,
        "Enter the password to open this Word document.")

    private fun failure() = OfficeConversion.Failure(LoadFailure.CONVERSION_FAILED,
        "MyDoc could not render the original Word layout. A simplified reading view may still be available.")
}
