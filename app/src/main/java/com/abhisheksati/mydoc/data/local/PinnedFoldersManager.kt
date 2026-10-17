package com.abhisheksati.mydoc.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class PinnedFoldersManager(context: Context) {

    private val prefs = context.getSharedPreferences("pinned_folders_prefs", Context.MODE_PRIVATE)
    private val key = "pinned_folder_paths"

    private val _pinnedFolders = MutableStateFlow<List<File>>(loadPinned())
    val pinnedFolders: StateFlow<List<File>> = _pinnedFolders.asStateFlow()

    private fun loadPinned(): List<File> {
        val set = prefs.getStringSet(key, emptySet()) ?: emptySet()
        return set.map { File(it) }.filter { it.exists() && it.isDirectory }.sortedBy { it.name.lowercase() }
    }

    fun isPinned(dir: File): Boolean {
        return _pinnedFolders.value.any { it.absolutePath == dir.absolutePath }
    }

    fun togglePin(dir: File) {
        val currentSet = prefs.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (currentSet.contains(dir.absolutePath)) {
            currentSet.remove(dir.absolutePath)
        } else {
            currentSet.add(dir.absolutePath)
        }
        prefs.edit().putStringSet(key, currentSet).apply()
        _pinnedFolders.value = currentSet.map { File(it) }.filter { it.exists() && it.isDirectory }.sortedBy { it.name.lowercase() }
    }
}
