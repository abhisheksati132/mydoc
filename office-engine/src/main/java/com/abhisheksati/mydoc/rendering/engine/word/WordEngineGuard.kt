package com.abhisheksati.mydoc.rendering.engine.word

import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import java.io.File

internal fun isWordEngineProcess(): Boolean = runCatching {
    File("/proc/self/cmdline").inputStream().use { source ->
        val bytes = ByteArray(512)
        val length = source.read(bytes)
        length > 0 && String(bytes, 0, length, Charsets.UTF_8).substringBefore('\u0000').endsWith(":word_engine")
    }
}.getOrDefault(false)

internal object WordEngineGuard {
    private var installed = false
    init { System.loadLibrary("wordguard") }
    private external fun blockNetwork(): Boolean

    @Synchronized fun installAndVerify() {
        check(isWordEngineProcess()) { "The Word guard must never run in the reader process" }
        if (installed) return
        check(blockNetwork()) { "The Word worker could not restrict network access" }
        val blocked = try {
            Os.close(Os.socket(OsConstants.AF_INET, OsConstants.SOCK_STREAM, 0))
            false
        } catch (error: ErrnoException) { error.errno == OsConstants.EPERM }
        check(blocked) { "The Word worker network restriction did not take effect" }
        installed = true
    }
}
