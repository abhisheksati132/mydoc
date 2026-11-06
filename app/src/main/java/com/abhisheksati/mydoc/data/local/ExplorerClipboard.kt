package com.abhisheksati.mydoc.data.local

import com.abhisheksati.mydoc.domain.model.ClipboardState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import com.abhisheksati.mydoc.domain.repository.StorageRepository
import com.abhisheksati.mydoc.domain.model.ClipboardOperation
import kotlinx.coroutines.CoroutineScope
import java.io.File

class ExplorerClipboard {
    val transfers = ExplorerTransfers()
    private var owner: CoroutineScope? = null
    private var repository: StorageRepository? = null
    /** The root browser owns transfers; closing an individual tab must not cancel a move. */
    fun bind(scope: CoroutineScope, storage: StorageRepository) {
        if (owner == null) { owner = scope; repository = storage }
    }
    fun paste(target: File): Boolean {
        val scope = owner ?: return false
        val storage = repository ?: return false
        val clip = beginPaste() ?: return false
        val started = transfers.start(scope, clip, target, { operation, items, destination ->
            if (operation == ClipboardOperation.COPY) storage.copy(items, destination)
            else storage.move(items, destination)
        }) { report ->
            value.value = if (report.remaining.isEmpty()) null else clip.copy(items = report.remaining)
            changes.value++
            pasting.set(false)
        }
        if (!started) pasting.set(false)
        return started
    }
    private val value = MutableStateFlow<ClipboardState?>(null)
    val state = value.asStateFlow()
    private val changes = MutableStateFlow(0)
    val revision = changes.asStateFlow()
    private val pasting = AtomicBoolean(false)
    fun set(clipboard: ClipboardState?): Boolean {
        if (pasting.get()) return false
        value.value = clipboard
        return true
    }
    private fun beginPaste(): ClipboardState? = if (pasting.compareAndSet(false, true)) {
        value.value.also { if (it == null) pasting.set(false) }
    } else null
}
