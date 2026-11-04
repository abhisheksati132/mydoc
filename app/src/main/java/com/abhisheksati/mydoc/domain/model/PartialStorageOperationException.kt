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
    cause: Throwable? = null
) : Exception("$completed of ${completed + failed} items completed", cause)
