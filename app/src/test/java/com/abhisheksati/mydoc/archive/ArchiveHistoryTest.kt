package com.abhisheksati.mydoc.archive

import com.abhisheksati.mydoc.data.archive.ArchiveJobRecord
import com.abhisheksati.mydoc.data.archive.ArchiveJobStatus
import com.abhisheksati.mydoc.data.archive.retainArchiveHistory
import org.junit.Assert.*
import org.junit.Test

class ArchiveHistoryTest {
    @Test fun oldPausedOrQueuedTasksCannotBeEvictedByFinishedHistory() {
        val active = listOf(ArchiveJobRecord("paused", "Paused", ArchiveJobStatus.PAUSED), ArchiveJobRecord("queued", "Queued"))
        val finished = (1..80).map { ArchiveJobRecord("finished-$it", "Finished", ArchiveJobStatus.COMPLETED) }
        val retained = retainArchiveHistory(finished + active)
        assertEquals(50, retained.size)
        assertEquals(listOf("paused", "queued"), retained.filter { it.active }.map { it.id })
        assertEquals("finished-1", retained.first().id)
        assertFalse(retained.any { it.id == "finished-49" })
    }
}
