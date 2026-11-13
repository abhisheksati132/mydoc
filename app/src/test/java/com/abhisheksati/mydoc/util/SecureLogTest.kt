package com.abhisheksati.mydoc.util

import com.abhisheksati.mydoc.BuildConfig
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Batch 6: document names and paths were going to logcat, where a bug report or a connected debugger
 * can read them back. The message a release build emits must not contain them — and the private text
 * must not even be built, so nothing can leak through a lazily-shaped bug.
 */
class SecureLogTest {

    @After
    fun restoreBuildDefault() {
        SecureLog.verbose = BuildConfig.DEBUG
    }

    @Test
    fun aReleaseBuildLogsOnlyTheSummary() {
        SecureLog.verbose = false
        val message = SecureLog.messageFor("could not open document") { "Income Tax Return 2026.pdf" }
        assertEquals("could not open document", message)
        assertFalse("the file name reached the release message", message.contains("Income Tax"))
    }

    @Test
    fun aReleaseBuildNeverEvenEvaluatesThePrivateDetail() {
        SecureLog.verbose = false
        var evaluated = 0
        SecureLog.messageFor("could not read document") {
            evaluated++
            "/storage/emulated/0/Documents/tax-return.pdf"
        }
        assertEquals("detail was built in a release build", 0, evaluated)
    }

    @Test
    fun aDebugBuildStillCarriesTheDetailForTheDeveloper() {
        SecureLog.verbose = true
        val message = SecureLog.messageFor("could not open document") { "report.pdf" }
        assertTrue("debug message lost the name: $message", message.contains("report.pdf"))
    }

    @Test
    fun anEmptyDetailDoesNotLeaveTrailingWhitespace() {
        SecureLog.verbose = true
        assertEquals("no PDF produced", SecureLog.messageFor("no PDF produced") { "   " })
    }

    @Test
    fun aReleaseBuildDoesNotForwardThrowableMessages() {
        SecureLog.verbose = false
        val secret = java.io.IOException("/storage/emulated/0/Documents/tax-return.pdf")
        val (message, forwarded) = SecureLog.payloadFor("could not open document", { "private" }, secret)
        assertNull("release forwarded a throwable carrying a private path", forwarded)
        assertFalse("the throwable message reached the release log", message.contains("tax-return"))
        assertTrue("the exception type should remain diagnosable", message.contains("IOException"))
    }

    @Test
    fun aDebugBuildForwardsTheThrowable() {
        SecureLog.verbose = true
        val secret = java.io.IOException("detail")
        val (_, forwarded) = SecureLog.payloadFor("summary", { "" }, secret)
        assertSame(secret, forwarded)
    }
}
