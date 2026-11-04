package com.abhisheksati.mydoc.util

import android.util.Log
import java.io.FileNotFoundException

/**
 * The one place an exception is turned into a sentence a reader can act on.
 *
 * Exception strings are written for developers: they name internal classes, quote file paths the
 * reader never entered, and change with the device locale. Showing one puts jargon and sometimes
 * someone else's path on screen, so the raw cause goes to the log and a fixed, honest sentence
 * goes to the UI. Use [logCause] when a caller already knows which message it wants.
 */
object UserMessages {
    const val READ_FAILED = "This file could not be read. It may have been moved, deleted, or MyDoc lost permission to open it."
    const val PERMISSION_EXPIRED = "Storage permission has expired for this file. Pick it again to restore access."
    const val FILE_MISSING = "This file is no longer where MyDoc last saw it."
    const val WRITE_FAILED = "MyDoc could not complete that action. Nothing was changed."

    /** Message for a failed read, with the real cause logged rather than displayed. */
    fun readFailure(tag: String, cause: Throwable?): String {
        Log.w(tag, "Read failed", cause)
        val text = cause?.message.orEmpty()
        return when {
            // DocumentLoadException messages are authored copy naming a real cause, not stack noise.
            cause is com.abhisheksati.mydoc.domain.model.DocumentLoadException -> text
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
    fun actionFailed(action: String, cause: Throwable?, tag: String = "Files"): String {
        Log.w(tag, "$action failed", cause)
        if (cause is com.abhisheksati.mydoc.domain.model.PartialStorageOperationException) {
            // Some items did land. Saying only "failed" would send people back to retry work
            // that is already done, and hide that half a batch is now sitting in the folder.
            val total = cause.completed + cause.failed
            val base = "$action completed for ${cause.completed} of $total items — ${cause.failed} could not be completed."
            if (cause.retainedSources.isEmpty()) return base
            // The copies were kept because part of the original was already removed. Saying only
            // "could not be completed" would send people to delete the duplicate by hand.
            val plural = cause.retainedSources.size > 1
            return base + " " + cause.retainedSources.size.toString() +
                (if (plural) " items are also still in their original location, so their copies were kept too."
                 else " item is also still in its original location, so its copy was kept too.")
        }
        val text = cause?.message.orEmpty()
        val reason = when {
            cause is SecurityException || text.contains("EACCES") ||
                text.contains("Permission denied", ignoreCase = true) ->
                "you don't have permission to change it here"
            text.contains("ENOENT") -> "the file is no longer there"
            text.contains("ENOSPC") -> "there is no space left on this storage"
            text.contains("EROFS") || text.contains("read-only", ignoreCase = true) ->
                "this storage is read-only"
            text.contains("EXDEV") -> "it spans volumes MyDoc cannot move between"
            else -> null
        }
        return if (reason == null) "$action failed." else "$action failed — $reason."
    }
}
