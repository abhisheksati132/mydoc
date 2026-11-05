package com.abhisheksati.mydoc.util

import android.util.Log
import com.abhisheksati.mydoc.BuildConfig

/**
 * Document names are user data. A release build must not write them to logcat, where anything with
 * log access — a bug report, a connected debugger, a rooted device — can read them back.
 *
 * Callers therefore pass two things: a constant [summary] that is safe to publish in any build, and
 * a [detail] lambda holding the name, path or other private context. In a release build the lambda
 * is never invoked, so the private text is not merely filtered on the way out; it is never produced.
 */
object SecureLog {

    /**
     * Seam for tests. [BuildConfig.DEBUG] is a compile-time constant, so a release build folds this
     * to false and R8 drops the whole detail branch; a unit test flips it to exercise both paths.
     */
    @Volatile
    internal var verbose: Boolean = BuildConfig.DEBUG

    fun info(tag: String, summary: String, detail: () -> String = { "" }) {
        emit(Log.INFO, tag, summary, detail, null)
    }

    fun warn(tag: String, summary: String, detail: () -> String = { "" }) {
        emit(Log.WARN, tag, summary, detail, null)
    }

    fun warn(tag: String, summary: String, throwable: Throwable, detail: () -> String = { "" }) {
        emit(Log.WARN, tag, summary, detail, throwable)
    }

    fun error(tag: String, summary: String, detail: () -> String = { "" }) {
        emit(Log.ERROR, tag, summary, detail, null)
    }

    fun error(tag: String, summary: String, throwable: Throwable, detail: () -> String = { "" }) {
        emit(Log.ERROR, tag, summary, detail, throwable)
    }

    private fun emit(level: Int, tag: String, summary: String, detail: () -> String, throwable: Throwable?) {
        val message = messageFor(summary, detail)
        when (level) {
            Log.INFO -> if (throwable == null) Log.i(tag, message) else Log.i(tag, message, throwable)
            Log.WARN -> if (throwable == null) Log.w(tag, message) else Log.w(tag, message, throwable)
            else -> if (throwable == null) Log.e(tag, message) else Log.e(tag, message, throwable)
        }
    }

    /** Split out so a test can assert on exactly the text that would be handed to logcat. */
    internal fun messageFor(summary: String, detail: () -> String): String {
        if (!verbose) return summary
        val extra = detail()
        return if (extra.isBlank()) summary else "$summary $extra"
    }
}
