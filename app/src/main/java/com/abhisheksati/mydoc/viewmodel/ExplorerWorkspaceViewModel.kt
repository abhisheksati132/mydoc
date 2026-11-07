package com.abhisheksati.mydoc.viewmodel

import androidx.lifecycle.*
import com.abhisheksati.mydoc.data.local.SettingsRepository
import com.abhisheksati.mydoc.domain.repository.StorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class ExplorerTab(val id: Int, val browser: FileManagerViewModel)

class ExplorerWorkspaceViewModel(
    private val root: FileManagerViewModel,
    private val repository: StorageRepository,
    private val settings: SettingsRepository?,
    private val saved: SavedStateHandle
) : ViewModel() {
    private val stores = mutableMapOf<Int, ViewModelStore>()
    private val observers = mutableMapOf<Int, kotlinx.coroutines.Job>()
    private var nextId = saved.get<Int>("next") ?: 1
    private val _tabs = MutableStateFlow<List<ExplorerTab>>(emptyList())
    val tabs = _tabs.asStateFlow()
    private val _active = MutableStateFlow(saved.get<Int>("active") ?: 0)
    val active = _active.asStateFlow()
    private val _secondary = MutableStateFlow(saved.get<Int>("secondary"))
    val secondary = _secondary.asStateFlow()
    private val _focused = MutableStateFlow(_active.value)
    val focused = _focused.asStateFlow()

    fun focus(id: Int) { if (id == _active.value || id == _secondary.value) _focused.value = id }

    init {
        val ids = saved.get<IntArray>("ids")?.toList() ?: listOf(0)
        _tabs.value = ids.map { id ->
            val browser = if (id == 0) root else createBrowser(id)
            saved.get<String>("path$id")?.let { browser.startBrowsingStorage(File(it)) }
            ExplorerTab(id, browser).also(::observe)
        }
        if (_active.value !in ids) _active.value = ids.first()
        if (_secondary.value !in ids) _secondary.value = null
    }

    private fun createBrowser(id: Int): FileManagerViewModel {
        val store = ViewModelStore().also { stores[id] = it }
        val owner = object : ViewModelStoreOwner { override val viewModelStore = store }
        return ViewModelProvider.create(owner, FileManagerViewModel.Factory(repository, settings, root.explorerClipboard, root.explorerPreferences))[FileManagerViewModel::class]
    }

    private fun observe(tab: ExplorerTab) {
        observers[tab.id] = viewModelScope.launch {
            tab.browser.uiState.collect { state ->
                if (state.isBrowsingStorage) saved["path${tab.id}"] = state.currentDirectory.absolutePath
                else saved.remove<String>("path${tab.id}")
            }
        }
    }

    fun select(id: Int) {
        if (_tabs.value.none { it.id == id }) return
        if (_secondary.value == id) {
            _secondary.value = _active.value
            saved["secondary"] = _secondary.value
        }
        _active.value = id
        _focused.value = id
        saved["active"] = id
    }

    fun add(): Int? {
        if (_tabs.value.size >= 6) return null
        val id = nextId++
        val browser = createBrowser(id)
        val directory = _tabs.value.first { it.id == _active.value }.browser.uiState.value.currentDirectory
        browser.startBrowsingStorage(directory)
        val tab = ExplorerTab(id, browser)
        _tabs.value += tab
        observe(tab)
        saved["next"] = nextId
        saved["ids"] = _tabs.value.map { it.id }.toIntArray()
        select(id)
        return id
    }

    fun close(id: Int) {
        val tab = _tabs.value.firstOrNull { it.id == id } ?: return
        if (_tabs.value.size == 1 || tab.browser.uiState.value.activeOperation != null) return
        observers.remove(id)?.cancel()
        stores.remove(id)?.clear()
        _tabs.value = _tabs.value.filterNot { it.id == id }
        saved.remove<String>("path$id")
        if (_secondary.value == id) _secondary.value = null
        if (_active.value == id) { _active.value = _tabs.value.first().id; _secondary.value = null }
        persistSelection()
    }

    fun toggleSplit() {
        if (_secondary.value != null) _secondary.value = null
        else {
            val previous = _active.value
            val other = _tabs.value.firstOrNull { it.id != previous }?.id ?: add()
            _active.value = previous
            _secondary.value = other
        }
        persistSelection()
    }

    private fun persistSelection() {
        if (_focused.value != _active.value && _focused.value != _secondary.value) _focused.value = _active.value
        saved["active"] = _active.value
        saved["secondary"] = _secondary.value
        saved["ids"] = _tabs.value.map { it.id }.toIntArray()
    }

    override fun onCleared() { stores.values.forEach { it.clear() } }
}
