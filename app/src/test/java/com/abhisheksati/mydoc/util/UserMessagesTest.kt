package com.abhisheksati.mydoc.util

import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.domain.model.DocumentLoadException
import com.abhisheksati.mydoc.domain.model.LoadFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.FileNotFoundException
import java.io.IOException

class UserMessagesTest {

    private fun UiText.texts(): List<String> = when (this) {
        is UiText.Raw -> listOf(text)
        is UiText.Res -> listOf(resId.toString()) + args.map { it.toString() }
        is UiText.Joined -> parts.flatMap { it.texts() }
    }

    @Test
    fun `unknown read failure hides the exception text`() {
        val secret = IOException("java.io.FileInputStream /storage/emulated/0/private/diary.pdf")
        val message = UserMessages.readFailure("Viewer", secret)
        assertEquals(UserMessages.READ_FAILED, message)
        assertFalse(message.texts().any { it.contains("diary.pdf") })
    }

    @Test
    fun `missing file and lost permission keep their actionable text`() {
        assertEquals(
            UserMessages.FILE_MISSING,
            UserMessages.readFailure("Viewer", FileNotFoundException("/private/gone.pdf"))
        )
        assertEquals(
            UserMessages.PERMISSION_EXPIRED,
            UserMessages.readFailure("Viewer", SecurityException("Permission Denial: opening provider"))
        )
    }

    @Test
    fun `authored load failure keeps its own copy`() {
        val typed = DocumentLoadException(
            LoadFailure.CORRUPT_DOCUMENT,
            "This PDF is damaged and cannot be opened."
        )
        assertEquals(
            UiText.Raw("This PDF is damaged and cannot be opened."),
            UserMessages.readFailure("Viewer", typed)
        )
    }

    @Test
    fun `file action failures map errno without exposing the message`() {
        val noSpace = UserMessages.actionFailed(
            R.string.transfers_copy,
            IOException("write failed: ENOSPC (/data/private/x.pdf)")
        ) as UiText.Res
        assertEquals(R.string.user_action_failed_reason, noSpace.resId)
        assertEquals(UiText.Res(R.string.user_reason_no_space), noSpace.args.last())

        val unknown = UserMessages.actionFailed(
            R.string.transfers_copy,
            IOException("boom at /data/private/x.pdf")
        ) as UiText.Res
        assertEquals(R.string.user_action_failed, unknown.resId)
        assertEquals(listOf(UiText.Res(R.string.transfers_copy)), unknown.args)
        assertFalse(unknown.texts().any { it.contains("x.pdf") })
    }
}
