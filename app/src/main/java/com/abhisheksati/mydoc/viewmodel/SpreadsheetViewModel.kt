package com.abhisheksati.mydoc.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.util.UserMessages
import com.abhisheksati.mydoc.rendering.engine.spreadsheet.SheetData
import com.abhisheksati.mydoc.rendering.engine.spreadsheet.SpreadsheetModel
import com.abhisheksati.mydoc.rendering.engine.spreadsheet.SpreadsheetParser
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class SpreadsheetUiState(
    val model: SpreadsheetModel = SpreadsheetModel(),
    val selectedSheetIndex: Int = 0,
    val selectedCell: Pair<Int, Int>? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    /** Why loading stopped, so the screen can name the cause instead of showing an empty grid. */
    val failure: com.abhisheksati.mydoc.domain.model.LoadFailure? = null
) {
    val currentSheet: SheetData?
        get() = model.sheets.getOrNull(selectedSheetIndex)
}

class SpreadsheetViewModel(
    // Application, not Context: this outlives the Activity that creates it.
    private val context: Application,
    private val initialDocument: DocumentFile,
    private val parser: SpreadsheetParser = SpreadsheetParser()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpreadsheetUiState())
    val uiState: StateFlow<SpreadsheetUiState> = _uiState.asStateFlow()

    private var activeDocument: DocumentFile = initialDocument
    private var parsedFor: String? = null

    /**
     * Shows another spreadsheet in this session and lets go of the parsed model of the previous
     * one. Keying the model to a fresh view model per document used to mean every workbook that
     * was ever opened stayed in memory until the Activity's store was cleared.
     */
    fun open(document: DocumentFile) {
        if (document.uri == activeDocument.uri && parsedFor == document.uri) return
        activeDocument = document
        parsedFor = null
        _uiState.value = SpreadsheetUiState()
        loadSpreadsheet()
    }

    init {
        loadSpreadsheet()
    }

    private fun loadSpreadsheet() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, failure = null) }
            try {
                val uri = Uri.parse(activeDocument.uri)
                val inputStream = com.abhisheksati.mydoc.data.local.DocumentStreamResolver.openStream(context, uri)
                    ?: throw IllegalStateException("Could not open input stream")

                val model = inputStream.use { parser.parse(it, activeDocument.extension) }
                val app = context.applicationContext as? com.abhisheksati.mydoc.MyDocApp
                val savedState = app?.documentRepository?.getViewState(activeDocument.uri)
                parsedFor = activeDocument.uri
                val initialSheet = savedState?.lastPage?.coerceIn(0, maxOf(0, model.sheets.size - 1)) ?: 0

                _uiState.update {
                    it.copy(
                        model = model,
                        selectedSheetIndex = initialSheet,
                        isLoading = false
                    )
                }
            } catch (e: com.abhisheksati.mydoc.domain.model.DocumentLoadException) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message, failure = e.failure)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = UserMessages.readFailure("SpreadsheetViewModel", e),
                        failure = com.abhisheksati.mydoc.domain.model.LoadFailure.CORRUPT_DOCUMENT
                    )
                }
            }
        }
    }

    fun selectSheet(index: Int) {
        if (index in _uiState.value.model.sheets.indices) {
            _uiState.update { it.copy(selectedSheetIndex = index, selectedCell = null) }
            viewModelScope.launch {
                val app = context.applicationContext as? com.abhisheksati.mydoc.MyDocApp
                app?.documentRepository?.updatePageProgress(activeDocument.uri, index, _uiState.value.model.sheets.size)
                app?.documentRepository?.saveViewState(
                    com.abhisheksati.mydoc.domain.model.ViewState(
                        uri = activeDocument.uri,
                        lastPage = index
                    )
                )
            }
        }
    }

    fun selectCell(row: Int, col: Int) {
        _uiState.update { it.copy(selectedCell = Pair(row, col)) }
    }

    fun clearSelectedCell() {
        _uiState.update { it.copy(selectedCell = null) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    class Factory(
        private val context: Context,
        private val documentFile: DocumentFile
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SpreadsheetViewModel::class.java)) {
                return SpreadsheetViewModel(context.applicationContext as Application, documentFile) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ")
        }
    }
}
