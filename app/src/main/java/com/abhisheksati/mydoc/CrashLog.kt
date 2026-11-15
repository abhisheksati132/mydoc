package com.abhisheksati.mydoc

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Keeps the most recent uncaught exception on disk so users can share it from
 * Settings. Stays entirely on-device: nothing is uploaded automatically.
 */
object CrashLog {

    private const val DIR_NAME = "crash"
    private const val FILE_NAME = "last-crash.txt"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(appContext, thread, error) }
            previous?.uncaughtException(thread, error)
        }
    }

    fun read(context: Context): String? {
        val file = file(context)
        return if (file.isFile) file.readText() else null
    }

    fun clear(context: Context) {
        file(context).delete()
    }

    private fun write(context: Context, thread: Thread, error: Throwable) {
        val trace = StringWriter().also { writer ->
            PrintWriter(writer).use { printer -> error.printStackTrace(printer) }
        }.toString()
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val report = buildString {
            appendLine("MyDoc ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Time: $stamp")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Thread: ${thread.name}")
            appendLine()
            append(trace)
        }
        val file = file(context)
        file.parentFile?.mkdirs()
        file.writeText(report)
    }

    private fun file(context: Context) =
        File(context.filesDir, DIR_NAME).resolve(FILE_NAME)
}
