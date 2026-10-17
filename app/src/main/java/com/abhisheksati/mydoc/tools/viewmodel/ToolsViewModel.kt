package com.abhisheksati.mydoc.tools.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.abhisheksati.mydoc.tools.engine.PdfToolsEngine
import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.tools.model.ToolExecutionResult
import com.abhisheksati.mydoc.tools.model.ToolOptions
import com.abhisheksati.mydoc.tools.model.ToolProgress
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream

sealed interface ToolExecutionUiState {
    data object Idle : ToolExecutionUiState
    data class Running(val progress: ToolProgress) : ToolExecutionUiState
    data class Success(val result: ToolExecutionResult) : ToolExecutionUiState
    data class Error(val message: String) : ToolExecutionUiState
}

class ToolsViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = PdfToolsEngine(application.applicationContext)

    private val _selectedTool = MutableStateFlow<PdfTool?>(null)
    val selectedTool: StateFlow<PdfTool?> = _selectedTool.asStateFlow()

    private val _inputUris = MutableStateFlow<List<Uri>>(emptyList())
    val inputUris: StateFlow<List<Uri>> = _inputUris.asStateFlow()

    private val _toolOptions = MutableStateFlow(ToolOptions())
    val toolOptions: StateFlow<ToolOptions> = _toolOptions.asStateFlow()

    private val _executionState = MutableStateFlow<ToolExecutionUiState>(ToolExecutionUiState.Idle)
    val executionState: StateFlow<ToolExecutionUiState> = _executionState.asStateFlow()

    private var executionJob: Job? = null

    fun selectTool(tool: PdfTool, preselectedUri: Uri? = null) {
        _selectedTool.value = tool
        _executionState.value = ToolExecutionUiState.Idle
        _toolOptions.value = ToolOptions()
        if (preselectedUri != null) {
            _inputUris.value = listOf(preselectedUri)
        } else {
            _inputUris.value = emptyList()
        }
    }

    fun closeTool() {
        cancelExecution()
        _selectedTool.value = null
        _inputUris.value = emptyList()
        _executionState.value = ToolExecutionUiState.Idle
    }

    fun setInputUris(uris: List<Uri>) {
        _inputUris.value = uris
    }

    fun addInputUri(uri: Uri) {
        if (!_inputUris.value.contains(uri)) {
            _inputUris.value = _inputUris.value + uri
        }
    }

    fun removeInputUri(uri: Uri) {
        _inputUris.value = _inputUris.value.filter { it != uri }
    }

    fun updateOptions(transform: (ToolOptions) -> ToolOptions) {
        _toolOptions.value = transform(_toolOptions.value)
    }

    fun runTool() {
        val tool = _selectedTool.value ?: return
        val uris = _inputUris.value

        executionJob?.cancel()
        _executionState.value = ToolExecutionUiState.Running(ToolProgress(0, 100, 0, "Starting...", true))

        executionJob = viewModelScope.launch {
            try {
                val result = engine.executeTool(
                    tool = tool,
                    inputUris = uris,
                    options = _toolOptions.value,
                    onProgress = { progress ->
                        _executionState.value = ToolExecutionUiState.Running(progress)
                    }
                )

                if (result.success) {
                    _executionState.value = ToolExecutionUiState.Success(result)
                } else {
                    _executionState.value = ToolExecutionUiState.Error(result.errorMessage ?: "Tool execution failed.")
                }
            } catch (e: Exception) {
                _executionState.value = ToolExecutionUiState.Error(e.localizedMessage ?: "Unexpected error occurred.")
            }
        }
    }

    fun cancelExecution() {
        executionJob?.cancel()
        executionJob = null
        if (_executionState.value is ToolExecutionUiState.Running) {
            _executionState.value = ToolExecutionUiState.Idle
        }
    }

    fun saveResultToSaf(context: Context, destinationUri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val state = _executionState.value
        if (state !is ToolExecutionUiState.Success) {
            onError("No completed result to save.")
            return
        }

        val outputFile = state.result.outputFile
        if (outputFile == null || !outputFile.exists()) {
            onError("Output file not found.")
            return
        }

        viewModelScope.launch {
            try {
                context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                    FileInputStream(outputFile).use { input ->
                        input.copyTo(out)
                    }
                } ?: throw IllegalStateException("Could not open output destination.")
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to save file.")
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ToolsViewModel::class.java)) {
                return ToolsViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
