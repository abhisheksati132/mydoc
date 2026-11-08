package com.abhisheksati.mydoc.rendering.engine.word

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume

internal class WordEngineResult(val success: Boolean, val processId: Int = -1,
    val passwordRequired: Boolean = false, val busy: Boolean = false, val renderPassword: String? = null)

internal object WordEngineConnection {
    suspend fun convert(context: Context, source: File, output: File, password: String? = null): WordEngineResult = withTimeout(60_000) {
        check(com.abhisheksati.mydoc.rendering.engine.OfficeEngineManager.isInstalled(context))
        val sourceFd = ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY)
        val outputFd = try { ParcelFileDescriptor.open(output,
            ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE or ParcelFileDescriptor.MODE_WRITE_ONLY)
        } catch (error: Throwable) { sourceFd.close(); throw error }
        val bound = AtomicBoolean(false)
        val finished = AtomicBoolean(false)
        val remote = AtomicReference<Messenger?>()
        val id = UUID.randomUUID().toString()
        lateinit var connection: ServiceConnection
        fun unbind() {
            if (bound.compareAndSet(true, false)) runCatching { context.unbindService(connection) }
        }
        try {
            suspendCancellableCoroutine { continuation ->
                val replies = Messenger(Handler(Looper.getMainLooper()) { message ->
                    if (message.what == WordEngineProtocol.RESULT && message.data.getString("id") == id && continuation.isActive) {
                        continuation.resume(WordEngineResult(message.data.getBoolean("success"), message.data.getInt("pid"),
                            message.data.getBoolean("passwordRequired"), message.data.getBoolean("busy"),
                            message.data.getString("renderPassword")))
                    }
                    true
                })
                fun fail() { if (continuation.isActive) continuation.resume(WordEngineResult(false)) }
                connection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                        if (!continuation.isActive) { unbind(); return }
                        val service = Messenger(binder)
                        remote.set(service)
                        runCatching {
                            service.send(Message.obtain(null, WordEngineProtocol.CONVERT).apply {
                                replyTo = replies
                                data = Bundle().apply {
                                    putString("id", id)
                                    putParcelable("input", sourceFd)
                                    putParcelable("output", outputFd)
                                    putString("extension", source.extension)
                                    putString("password", password)
                                }
                            })
                        }.onFailure { fail() }
                    }
                    override fun onServiceDisconnected(name: ComponentName) = fail()
                    override fun onNullBinding(name: ComponentName) = fail()
                    override fun onBindingDied(name: ComponentName) = fail()
                }
                continuation.invokeOnCancellation {
                    finished.set(true)
                    runCatching { remote.get()?.send(Message.obtain(null, WordEngineProtocol.CANCEL).apply {
                        data = Bundle().apply { putString("id", id) }
                    }) }
                    unbind()
                }
                try {
                    bound.set(context.bindService(Intent().setComponent(com.abhisheksati.mydoc.rendering.engine.OfficeEngineManager.component(context)), connection, Context.BIND_AUTO_CREATE))
                    if (!bound.get()) fail()
                    if (finished.get()) unbind()
                } catch (_: Exception) { fail() }
            }
        } finally {
            finished.set(true)
            unbind()
            sourceFd.close()
            outputFd.close()
        }
    }
}

internal object WordEngineProtocol { const val CONVERT = 1; const val CANCEL = 2; const val RESULT = 3 }
