package com.abhisheksati.mydoc.data.local

import com.abhisheksati.mydoc.domain.model.ClipboardState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

class ExplorerClipboard {
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
    fun beginPaste(): ClipboardState? = if (pasting.compareAndSet(false, true)) {
        value.value.also { if (it == null) pasting.set(false) }
    } else null
    fun endPaste(success: Boolean) { if (success) { value.value = null; changes.value++ }; pasting.set(false) }
}
