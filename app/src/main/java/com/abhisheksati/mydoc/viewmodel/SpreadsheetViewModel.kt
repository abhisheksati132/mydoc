package com.abhisheksati.mydoc.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.rendering.engine.spreadsheet.SheetData
import com.abhisheksati.mydoc.rendering.engine.spreadsheet.SpreadsheetModel
import com.abhisheksati.mydoc.rendering.engine.spreadsheet.SpreadsheetParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SpreadsheetUiState(
    val model: SpreadsheetModel = SpreadsheetModel(),
    val selectedSheetIndex: Int = 0,
    val selectedCell: Pair<Int, Int>? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val currentSheet: SheetData?
        get() = model.sheets.getOrNull(selectedSheetIndex)
}

class SpreadsheetViewModel(
    private val context: Context,
    private val documentFile: DocumentFile,
    private val parser: SpreadsheetParser = SpreadsheetParser()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpreadsheetUiState())
    val uiState: StateFlow<SpreadsheetUiState> = _uiState.asStateFlow()

    init {
        loadSpreadsheet()
    }

    private fun loadSpreadsheet() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val uri = Uri.parse(documentFile.uri)
                val inputStream = com.abhisheksati.mydoc.data.local.DocumentStreamResolver.openStream(context, uri, documentFile.displayName)
                    ?: throw IllegalStateException("Could not open input stream")

                val model = inputStream.use { parser.parse(it, documentFile.extension) }
                val app = context.applicationContext as? com.abhisheksati.mydoc.MyDocApp
                val savedState = app?.documentRepository?.getViewState(documentFile.uri)
                val initialSheet = savedState?.lastPage?.coerceIn(0, maxOf(0, model.sheets.size - 1)) ?: 0

                _uiState.update {
                    it.copy(
                        model = model,
                        selectedSheetIndex = initialSheet,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to parse spreadsheet"
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
                app?.documentRepository?.updatePageProgress(documentFile.uri, index, _uiState.value.model.sheets.size)
                app?.documentRepository?.saveViewState(
                    com.abhisheksati.mydoc.domain.model.ViewState(
                        uri = documentFile.uri,
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
            return SpreadsheetViewModel(context, documentFile) as T
        }
    }
}
