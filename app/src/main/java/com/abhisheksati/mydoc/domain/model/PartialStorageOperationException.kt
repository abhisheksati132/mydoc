package com.abhisheksati.mydoc.domain.model

/**
 * A batch file operation that finished some items and could not finish others.
 *
 * Callers used to see either a bare success count or a single exception from the first item that
 * failed, which stopped the batch and hid how much had already been written. The UI must report
 * the counts in here rather than claiming the whole operation succeeded.
 */
class PartialStorageOperationException(
    val completed: Int,
    val failed: Int,
    cause: Throwable? = null,
    /**
     * Items that ended up in two places: copied to the destination while part of the original
     * could not be removed. Deleting the copy there would destroy the parts already removed from
     * the source, so both remain and the UI has to say so.
     */
    val retainedSources: List<String> = emptyList()
) : Exception("$completed of ${completed + failed} items completed", cause)
