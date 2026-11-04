package com.abhisheksati.mydoc.data.repository

import java.io.File

/**
 * The two filesystem primitives every destructive storage operation is built from.
 *
 * Both are injected because the guarantees that matter — "never throw away the only complete copy"
 * and "never delete a record for content that is still on disk" — depend on what happens when a
 * rename or a delete *fails partway*. Whether an operating system allows a particular delete is
 * not something a test can rely on, and the CI runner is not the machine the code is written on;
 * whether the code survives one is.
 */
interface EntryMutator {

    /** Moves [src] to [dest] in one step. False means the caller must fall back to copy + delete. */
    fun rename(src: File, dest: File): Boolean = runCatching { src.renameTo(dest) }.getOrDefault(false)

    /** Removes one entry and reports whether it is actually gone. */
    fun delete(entry: File): Boolean =
        runCatching { entry.delete() }.getOrDefault(false) || !entry.exists()
}

object DefaultEntryMutator : EntryMutator
