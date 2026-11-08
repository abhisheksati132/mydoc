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
                    if (message.what == WordEngineService.RESULT && message.data.getString("id") == id && continuation.isActive) {
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
                            service.send(Message.obtain(null, WordEngineService.CONVERT).apply {
                                replyTo = replies
                                data = Bundle().apply {
                                    putString("id", id)
                                    putString("input", source.absolutePath)
                                    putString("output", output.absolutePath)
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
                    runCatching { remote.get()?.send(Message.obtain(null, WordEngineService.CANCEL).apply {
                        data = Bundle().apply { putString("id", id) }
                    }) }
                    unbind()
                }
                try {
                    bound.set(context.bindService(Intent(context, WordEngineService::class.java), connection, Context.BIND_AUTO_CREATE))
                    if (!bound.get()) fail()
                    if (finished.get()) unbind()
                } catch (_: Exception) { fail() }
            }
        } finally {
            finished.set(true)
            unbind()
        }
    }
}
