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
     * Defaults to [BuildConfig.DEBUG], so a release build logs the summary only. It is a mutable field
     * rather than a constant so a test can drive both paths — which also means R8 does not delete the
     * detail branch from the release binary. The guarantee is behavioural, not structural: with this
     * false, [messageFor] never invokes the detail lambda, so no document name is ever put in a
     * message handed to logcat.
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
        val (message, loggedThrowable) = payloadFor(summary, detail, throwable)
        when (level) {
            Log.INFO -> if (loggedThrowable == null) Log.i(tag, message) else Log.i(tag, message, loggedThrowable)
            Log.WARN -> if (loggedThrowable == null) Log.w(tag, message) else Log.w(tag, message, loggedThrowable)
            else -> if (loggedThrowable == null) Log.e(tag, message) else Log.e(tag, message, loggedThrowable)
        }
    }

    /** Split out so a test can assert on exactly the text that would be handed to logcat. */
    internal fun messageFor(summary: String, detail: () -> String): String {
        if (!verbose) return summary
        val extra = detail()
        return if (extra.isBlank()) summary else "$summary $extra"
    }

    /** The exact message/throwable pair handed to logcat. Release keeps only the exception type:
     * throwable messages and stack traces carry paths and URIs. */
    internal fun payloadFor(summary: String, detail: () -> String, throwable: Throwable?): Pair<String, Throwable?> {
        val message = messageFor(summary, detail)
        if (verbose) return message to throwable
        val safe = if (throwable == null) message else "$message [${throwable.javaClass.simpleName}]"
        return safe to null
    }
}
