package com.abhisheksati.mydoc.tools.ui.components

import com.abhisheksati.mydoc.tools.engine.MAX_TOOL_INPUT_BYTES
import com.abhisheksati.mydoc.tools.engine.copyStagedInput
import kotlinx.coroutines.CancellationException
import java.io.File
import java.io.InputStream

/** Owns a new editor scratch file until a complete, nonempty import succeeds. */
internal suspend fun stageEditorImage(
    target: File,
    maxBytes: Long = MAX_TOOL_INPUT_BYTES,
    openStream: () -> InputStream?
): File? {
    var complete = false
    try {
        val input = openStream() ?: return null
        input.use {
            target.outputStream().use { output -> copyStagedInput(it, output, maxBytes) }
        }
        if (target.length() == 0L) return null
        complete = true
        return target
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        return null
    } finally {
        if (!complete) target.delete()
    }
}
