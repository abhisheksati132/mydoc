package com.abhisheksati.mydoc.data.local

import com.abhisheksati.mydoc.domain.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/** Cancellation is an item-boundary request, never a mid-move coroutine cancellation. */
class ExplorerTransfers {
    private val history = MutableStateFlow<List<ExplorerTransfer>>(emptyList())
    val state = history.asStateFlow()
    private val busy = AtomicBoolean(false)
    private val stop = AtomicBoolean(false)
    private val ids = AtomicLong(0)

    fun start(
        scope: CoroutineScope,
        clipboard: ClipboardState,
        target: File,
        perform: suspend (ClipboardOperation, List<File>, File) -> Result<Int>,
        finished: (ExplorerTransferReport) -> Unit
    ): Boolean {
        if (!scope.isActive || clipboard.items.isEmpty() || !busy.compareAndSet(false, true)) return false
        stop.set(false)
        val id = ids.incrementAndGet()
        history.update { (listOf(ExplorerTransfer(id, clipboard.operation, target, clipboard.items.size)) + it).take(20) }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            val remaining = clipboard.items.toMutableList()
            var completed = 0
            var failed = 0
            val failures = mutableListOf<TransferFailure>()
            try {
                for (file in clipboard.items) {
                    if (stop.get()) break
                    update(id) { it.copy(currentName = file.name) }
                    val result = try { perform(clipboard.operation, listOf(file), target) }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { Result.failure(e) }
                    if (result.getOrNull() == 1) { completed++; remaining.remove(file) }
                    else { failed++; if (failures.size < 20) failures += TransferFailure(file.name, result.exceptionOrNull()) }
                    update(id) { it.copy(completed = completed, failed = failed, failures = failures.toList()) }
                }
                val status = when {
                    remaining.isEmpty() -> TransferStatus.COMPLETED
                    stop.get() -> TransferStatus.CANCELLED
                    completed > 0 -> TransferStatus.PARTIAL
                    else -> TransferStatus.FAILED
                }
                update(id) { it.copy(status = status, currentName = "") }
            } catch (e: CancellationException) {
                update(id) { it.copy(status = TransferStatus.CANCELLED, currentName = "") }
                throw e
            } finally {
                busy.set(false)
                finished(ExplorerTransferReport(completed, remaining.toList()))
            }
        }
        return true
    }

    fun cancel() { if (busy.get()) { stop.set(true); history.update { list -> list.map { if (it.running) it.copy(status = TransferStatus.STOPPING) else it } } } }
    fun clearFinished() { history.update { it.filter(ExplorerTransfer::running) } }
    private fun update(id: Long, change: (ExplorerTransfer) -> ExplorerTransfer) { history.update { list -> list.map { if (it.id == id) change(it) else it } } }
}
