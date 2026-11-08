package com.abhisheksati.mydoc.rendering.engine.word

import android.app.Service
import android.content.Intent
import android.os.*
import com.abhisheksati.mydoc.rendering.engine.OfficeSourceReader
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** One native conversion at a time, outside the reader process. The caller owns the binding. */
class WordEngineService : Service() {
    private val generation = generations.incrementAndGet()
    private val messenger = Messenger(Handler(Looper.getMainLooper()) { request ->
        if (request.sendingUid != android.os.Process.myUid()) return@Handler true
        val id = request.data.getString("id") ?: return@Handler true
        if (request.what == CANCEL && activeId == id) {
            android.os.Process.killProcess(android.os.Process.myPid())
        } else if (request.what == CONVERT) {
            val reply = request.replyTo ?: return@Handler true
            if (!busy.compareAndSet(false, true)) {
                respond(reply, id, false, busy = true)
                return@Handler true
            }
            activeId = id
            val input = request.data.getString("input").orEmpty()
            val output = request.data.getString("output").orEmpty()
            val password = request.data.getString("password")
            worker.execute {
                var success = false
                var passwordRequired = false
                var renderPassword: String? = null
                try {
                    val root = File(cacheDir, "word_engine_jobs").canonicalFile
                    val source = File(input).canonicalFile
                    val target = File(output).canonicalFile
                    require(source.path.startsWith(root.path + File.separator))
                    require(target.parentFile == source.parentFile && target.name == "output.pdf")
                    require(com.abhisheksati.mydoc.rendering.engine.nativeOfficeExtension(source.name) != null)
                    require(source.isFile && source.length() in 1..OfficeSourceReader.MAX_SOURCE_BYTES)
                    WordEngineGuard.installAndVerify()
                    renderPassword = NativeWordCore.convert(this, source, target, password)
                    success = true
                } catch (_: WordPasswordRequired) {
                    passwordRequired = true
                } catch (error: Throwable) {
                    com.abhisheksati.mydoc.util.SecureLog.warn("WordEngine", "Office conversion failed") {
                        "${error.javaClass.simpleName}: ${error.message}"
                    }
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
