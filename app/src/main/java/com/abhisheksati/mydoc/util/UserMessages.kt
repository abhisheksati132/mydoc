package com.abhisheksati.mydoc.util

import android.util.Log
import androidx.annotation.StringRes
import com.abhisheksati.mydoc.R
import java.io.FileNotFoundException

/**
 * The one place an exception is turned into a sentence a reader can act on.
 *
 * Exception strings are written for developers: they name internal classes, quote file paths the
 * reader never entered, and change with the device locale. Showing one puts jargon and sometimes
 * someone else's path on screen, so the raw cause goes to the log and a fixed, honest sentence
 * goes to the UI. The sentence is returned as [UiText] because a ViewModel has no configuration to
 * resolve a resource against; the UI resolves it at the boundary.
 */
object UserMessages {
    val READ_FAILED: UiText = UiText.Res(R.string.user_read_failed)
    val PERMISSION_EXPIRED: UiText = UiText.Res(R.string.user_permission_expired)
    val FILE_MISSING: UiText = UiText.Res(R.string.user_file_missing)
    val WRITE_FAILED: UiText = UiText.Res(R.string.user_write_failed)

    /** Message for a failed read, with the real cause logged rather than displayed. */
    fun readFailure(tag: String, cause: Throwable?): UiText {
        Log.w(tag, "Read failed", cause)
        val text = cause?.message.orEmpty()
        return when {
            // DocumentLoadException messages are authored copy naming a real cause, not stack noise.
            cause is com.abhisheksati.mydoc.domain.model.DocumentLoadException -> UiText.Raw(text)
            cause is SecurityException || text.contains("Permission Denial", ignoreCase = true) ->
                PERMISSION_EXPIRED
            cause is FileNotFoundException || text.contains("ENOENT", ignoreCase = true) -> FILE_MISSING
            else -> READ_FAILED
        }
    }

    /** True when a read failure is really a lost permission, which the UI can offer to re-pick. */
    fun isPermissionFailure(cause: Throwable?): Boolean =
        cause is SecurityException ||
            cause?.message?.contains("Permission Denial", ignoreCase = true) == true

    /**
     * Message for a failed file action (delete, move, compress). The verb stays with the caller;
     * this adds only the reasons a reader can act on, translated out of errno strings, and logs
     * anything else instead of putting it on screen.
     */
    fun actionFailed(@StringRes actionRes: Int, cause: Throwable?, tag: String = "Files"): UiText {
        Log.w(tag, "action failed (res $actionRes)", cause)
        val action = UiText.Res(actionRes)
        if (cause is com.abhisheksati.mydoc.domain.model.PartialStorageOperationException) {
            // Some items did land. Saying only "failed" would send people back to retry work
            // that is already done, and hide that half a batch is now sitting in the folder.
            val total = cause.completed + cause.failed
            val header = UiText.Res(
                R.string.user_partial_header,
                listOf(action, cause.completed, total, cause.failed)
            )
            if (cause.retainedSources.isEmpty()) return header
            // The copies were kept because part of the original was already removed. Saying only
            // "could not be completed" would send people to delete the duplicate by hand.
            val retained = UiText.Res(
                if (cause.retainedSources.size > 1) R.string.user_partial_retained_many
                else R.string.user_partial_retained_one,
                listOf(cause.retainedSources.size)
            )
            return UiText.Joined(listOf(header, retained))
        }
        val text = cause?.message.orEmpty()
        val reasonRes = when {
            cause is SecurityException || text.contains("EACCES") ||
                text.contains("Permission denied", ignoreCase = true) -> R.string.user_reason_no_permission
            text.contains("ENOENT") -> R.string.user_reason_missing
            text.contains("ENOSPC") -> R.string.user_reason_no_space
            text.contains("EROFS") || text.contains("read-only", ignoreCase = true) -> R.string.user_reason_read_only
            text.contains("EXDEV") -> R.string.user_reason_cross_volume
            else -> null
        }
        return if (reasonRes == null) {
            UiText.Res(R.string.user_action_failed, listOf(action))
        } else {
            UiText.Res(R.string.user_action_failed_reason, listOf(action, UiText.Res(reasonRes)))
        }
    }
}
