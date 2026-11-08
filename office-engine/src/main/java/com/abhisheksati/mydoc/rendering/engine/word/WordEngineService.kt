package com.abhisheksati.mydoc.rendering.engine.word

import android.app.Service
import android.content.Intent
import android.os.*

import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** One native conversion at a time, outside the reader process. The caller owns the binding. */
class WordEngineService : Service() {
    private val generation = generations.incrementAndGet()
    private val messenger = Messenger(Handler(Looper.getMainLooper()) { request ->
        if (packageManager.checkSignatures(request.sendingUid, android.os.Process.myUid()) != android.content.pm.PackageManager.SIGNATURE_MATCH) return@Handler true
        val id = request.data.getString("id") ?: return@Handler true
        if (request.what == CANCEL && activeId == id) {
            android.os.Process.killProcess(android.os.Process.myPid())
        } else if (request.what == CONVERT) {
            val reply = request.replyTo ?: return@Handler true
            @Suppress("DEPRECATION")
            val input = request.data.getParcelable<ParcelFileDescriptor>("input")
            @Suppress("DEPRECATION")
            val output = request.data.getParcelable<ParcelFileDescriptor>("output")
            if (!busy.compareAndSet(false, true)) {
                runCatching { input?.close() }
                runCatching { output?.close() }
                respond(reply, id, false, busy = true)
                return@Handler true
            }
            activeId = id
            val extension = request.data.getString("extension").orEmpty()
            val password = request.data.getString("password")
            worker.execute {
                var success = false
                var passwordRequired = false
                var renderPassword: String? = null
                var job: File? = null
                try {
                    val root = File(cacheDir, "word_engine_jobs").canonicalFile
                    require(root.mkdirs() || root.isDirectory)
                    require(extension in setOf("docx","dotx","doc","dot","odt","ott","fodt","rtf",
                        "pptx","pptm","ppsx","potx","ppt","pps","pot","odp","otp","fodp",
                        "xlsx","xlsm","xls","ods","ots","fods"))
                    require(input != null && output != null)
                    require(password == null || password.length <= 1024)
                    job = java.nio.file.Files.createTempDirectory(root.toPath(), "job-").toFile()
                    val source = File(job, "input.$extension")
                    val target = File(job, "output.pdf")
                    ParcelFileDescriptor.AutoCloseInputStream(input).use { from ->
                        source.outputStream().use { to ->
                            val buffer = ByteArray(64 * 1024)
                            var total = 0L
                            while (true) {
                                val count = from.read(buffer)
                                if (count < 0) break
                                total += count
                                require(total <= 64L * 1024 * 1024)
                                to.write(buffer, 0, count)
                            }
                            require(total > 0)
                        }
                    }
                    WordEngineGuard.installAndVerify()
                    renderPassword = NativeWordCore.convert(this, source, target, password)
                    require(target.length() in 1..(256L * 1024 * 1024))
                    ParcelFileDescriptor.AutoCloseOutputStream(output).use { to ->
                        target.inputStream().use { it.copyTo(to) }
                    }
                    success = true
                } catch (_: WordPasswordRequired) {
                    passwordRequired = true
                } catch (error: Throwable) {
                    android.util.Log.w("OfficeEngine", "Office conversion failed: ${error.javaClass.simpleName}")
                }
                runCatching { input?.close() }
                runCatching { output?.close() }
                job?.let { directory ->
                    check(directory.canonicalFile.parentFile == File(cacheDir, "word_engine_jobs").canonicalFile)
                    directory.deleteRecursively()
                }
                activeId = null
                busy.set(false)
                respond(reply, id, success, passwordRequired, renderPassword = renderPassword)
            }
        }
        true
    })

    override fun onCreate() {
        super.onCreate()
        check(isWordEngineProcess())
        // A killed conversion may leave a private source copy. Reclaim it before accepting work.
        val jobs = File(cacheDir, "word_engine_jobs").canonicalFile
        check(jobs.parentFile == cacheDir.canonicalFile)
        jobs.deleteRecursively()
    }

    override fun onBind(intent: Intent): IBinder = messenger.binder

    private fun respond(reply: Messenger, id: String, success: Boolean,
        passwordRequired: Boolean = false, busy: Boolean = false, renderPassword: String? = null) {
        val result = Message.obtain(null, RESULT).apply {
            data = Bundle().apply {
                putString("id", id)
                putBoolean("success", success)
                putBoolean("passwordRequired", passwordRequired)
                putBoolean("busy", busy)
                putString("renderPassword", renderPassword)
                putInt("pid", android.os.Process.myPid())
            }
        }
        runCatching { reply.send(result) }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Allow a queued document to rebind. A new service generation cancels this idle exit.
        Handler(Looper.getMainLooper()).postDelayed({
            if (generations.get() == generation && !busy.get() && isWordEngineProcess())
                android.os.Process.killProcess(android.os.Process.myPid())
        }, 500)
    }

    companion object {
        private val generations = AtomicInteger()
        private val worker = Executors.newSingleThreadExecutor { Thread(it, "WordEngine") }
        private val busy = AtomicBoolean(false)
        @Volatile private var activeId: String? = null
        internal const val CONVERT = 1
        internal const val CANCEL = 2
        internal const val RESULT = 3
    }
}
