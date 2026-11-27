package com.abhisheksati.mydoc.data.archive

import android.content.Context
import android.system.Os
import android.system.OsConstants
import com.abhisheksati.mydoc.domain.archive.ArchiveFailure
import com.abhisheksati.mydoc.domain.archive.ArchiveProblem
import kotlinx.coroutines.*
import java.io.File
import java.io.InputStream
import java.nio.CharBuffer
import java.nio.file.Files
import java.nio.file.LinkOption
import java.util.UUID

/** A live handle only. It is never persisted and never contains credentials. */
class ArchiveProcessControl internal constructor(private val process: Process, private val pid: Int) {
    @Volatile private var paused = false
    @Synchronized fun pause(): Boolean {
        if (!process.isAlive) return false
        try { Os.kill(pid, OsConstants.SIGSTOP) } catch (error: android.system.ErrnoException) {
            if (error.errno == OsConstants.ESRCH) return false else throw error
        }
        paused = true
        return true
    }
    @Synchronized fun resume(): Boolean {
        if (!process.isAlive) return false
        try { if (paused) Os.kill(pid, OsConstants.SIGCONT) } catch (error: android.system.ErrnoException) {
            if (error.errno == OsConstants.ESRCH) return false else throw error
        }
        paused = false
        return true
    }
    @Synchronized internal fun cancel() {
        if (!process.isAlive) return
        runCatching { if (paused) Os.kill(pid, OsConstants.SIGCONT) }
        paused = false
        process.destroy()
        if (process.isAlive) process.destroyForcibly()
    }
}

/** Executes the pinned installed binary directly, never through a shell. */
internal class ArchiveProcessRunner(
    private val context: Context,
    private val onControl: (ArchiveProcessControl?) -> Unit,
    private val onPercent: (Int) -> Unit
) {
    suspend fun text(args: List<String>, password: CharArray?, limit: Int, directory: File? = null): String =
        execute(args, password, directory) { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(32 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(count <= limit - output.size()) { "Archive metadata limit exceeded" }
                output.write(buffer, 0, count)
            }
            output.toString(Charsets.UTF_8.name())
        }

    suspend fun <T> execute(args: List<String>, password: CharArray?, directory: File? = null, read: (InputStream) -> T): T =
        withTimeout(60L * 60 * 1000) {
            withContext(Dispatchers.IO) {
                coroutineScope {
                    require(password == null || password.size in 1..1024 && password.none { it == '\n' || it == '\r' || it == '\u0000' })
                    val executable = File(context.applicationInfo.nativeLibraryDir, "libmydoc7z.so")
                    if (!executable.isFile) throw ArchiveFailure(ArchiveProblem.ENGINE)
                    val processRoot = File(context.cacheDir, "archive-processes")
                    check(processRoot.isDirectory || processRoot.mkdirs())
                    require(!Files.isSymbolicLink(processRoot.toPath()))
                    val pidFile = File(processRoot, "${UUID.randomUUID()}.pid")
                    val process = ProcessBuilder(listOf(executable.absolutePath) + args).apply {
                        directory?.let { directory(it) }
                        environment()["LC_ALL"] = "C.UTF-8"
                        environment()["MYDOC_ARCHIVE_PID_FILE"] = pidFile.absolutePath
                    }.start()
                    var control: ArchiveProcessControl? = null
                    val errors = StringBuilder()
                    val stderr = async {
                        val line = StringBuilder()
                        try { process.errorStream.reader(Charsets.UTF_8).use { reader ->
                            val buffer = CharArray(4096)
                            while (true) {
                                val size = reader.read(buffer)
                                if (size < 0) break
                                for (i in 0 until size) {
                                    val char = buffer[i]
                                    if (errors.length < 64 * 1024) errors.append(char)
                                    if (char == '%') {
                                        Regex("(\\d{1,3})$").find(line)?.groupValues?.get(1)?.toIntOrNull()
                                            ?.takeIf { it in 0..100 }?.let(onPercent)
                                    }
                                    if (char == '\n' || char == '\r') {
                                        line.setLength(0)
                                    } else if (char == '\b') {
                                        if (line.isNotEmpty()) line.setLength(line.length - 1)
                                    } else if (line.length < 8192) line.append(char)
                                }
                            }
                        } } catch (error: java.io.IOException) {
                            currentCoroutineContext().ensureActive()
                            if (process.isAlive) throw error
                        }
                    }
                    val lifetime = launch(start = CoroutineStart.UNDISPATCHED) {
                        try { awaitCancellation() } finally {
                            control?.cancel() ?: run { process.destroy(); if (process.isAlive) process.destroyForcibly() }
                        }
                    }
                    try {
                        val deadline = android.os.SystemClock.uptimeMillis() + 5_000
                        var pid: Int? = null
                        while (android.os.SystemClock.uptimeMillis() < deadline) {
                            currentCoroutineContext().ensureActive()
                            pid = pidFile.takeIf { it.isFile && !Files.isSymbolicLink(it.toPath()) }
                                ?.readText()?.trim()?.toIntOrNull()?.takeIf { it > 0 }
                            if (pid != null || !process.isAlive) break
                            delay(5)
                        }
                        control = ArchiveProcessControl(process, pid ?: throw ArchiveFailure(ArchiveProblem.ENGINE))
                        onControl(control)
                        try { process.outputStream.use { input ->
                            if (password != null) {
                                val encoded = Charsets.UTF_8.encode(CharBuffer.wrap(password))
                                val bytes = ByteArray(encoded.remaining())
                                encoded.get(bytes)
                                try { input.write(bytes); input.write('\n'.code); input.flush() }
                                finally { bytes.fill(0); if (encoded.hasArray()) encoded.array().fill(0) }
                            }
                        } } catch (_: java.io.IOException) {
                            // A metadata-only command may finish without requesting a
                            // password. The exit code below determines success.
                        }
                        val result = try { Result.success(read(process.inputStream)) }
                        catch (error: java.io.IOException) {
                            currentCoroutineContext().ensureActive()
                            Result.failure(error)
                        }
                        val code = process.waitFor()
                        stderr.await()
                        currentCoroutineContext().ensureActive()
                        if (code != 0) throw failure(code, errors.toString() + (result.getOrNull() as? String).orEmpty(), password != null)
                        result.getOrThrow()
                    } finally {
                        withContext(NonCancellable) {
                            lifetime.cancelAndJoin()
                            stderr.cancelAndJoin()
                            runCatching { process.inputStream.close() }
                            runCatching { process.errorStream.close() }
                            runCatching { onControl(null) }
                            Files.deleteIfExists(pidFile.toPath())
                        }
                    }
                }
            }
        }

    private fun failure(code: Int, diagnostic: String, hasPassword: Boolean): ArchiveFailure {
        val text = diagnostic.lowercase(java.util.Locale.ROOT)
        val passwordPrompt = Regex("enter password(?: \\([^\\r\\n]*\\))?:")
        val requestedPassword = text.lineSequence().any { passwordPrompt.matches(it.trim()) }
        val passwordError = Regex("^error: (?:wrong password(?:[ :?]|$)|data error in encrypted file\\.)")
        val rejectedPassword = text.lineSequence().any { passwordError.containsMatchIn(it.trim()) }
        val problem = when {
            requestedPassword || rejectedPassword ->
                if (hasPassword) ArchiveProblem.WRONG_PASSWORD else ArchiveProblem.PASSWORD_REQUIRED
            "missing volume" in text || "cannot find volume" in text -> ArchiveProblem.MISSING_VOLUMES
            "no space" in text || "disk full" in text -> ArchiveProblem.NO_SPACE
            code == 8 || code == 137 || "not enough memory" in text -> ArchiveProblem.MEMORY
            code == 7 || "not implemented" in text || "unsupported" in text -> ArchiveProblem.UNSUPPORTED
            else -> ArchiveProblem.CORRUPT
        }
        return ArchiveFailure(problem)
    }
}
