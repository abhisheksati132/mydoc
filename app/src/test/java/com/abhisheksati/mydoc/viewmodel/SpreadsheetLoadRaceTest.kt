package com.abhisheksati.mydoc.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.rendering.engine.spreadsheet.SheetData
import com.abhisheksati.mydoc.rendering.engine.spreadsheet.SpreadsheetModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpreadsheetLoadRaceTest {
    @Test fun obsoleteSuccessfulParseCannotReplaceTheNewWorkbook() = checkRace(false)
    @Test fun obsoleteFailedParseCannotReplaceTheNewWorkbookWithAnError() = checkRace(true)

    private fun checkRace(failOld: Boolean) = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val oldFinished = CompletableDeferred<Unit>()
        val app = object : Application() { override fun getApplicationContext(): Context = this }
        val old = DocumentFile("old", "old.xlsx", 10, "application/octet-stream", 0)
        val fresh = old.copy(uri = "fresh", displayName = "fresh.xlsx")
        var starts = 0
        val vm = SpreadsheetViewModel(app, old, ioDispatcher = dispatcher, readModel = { document ->
            starts++
            if (document.uri == "old") withContext(NonCancellable) {
                oldFinished.await() // Model a native/parser operation which ignores cancellation.
                if (failOld) error("obsolete parse failed")
            }
            SpreadsheetModel(listOf(SheetData(document.uri)))
        })
        try {
            runCurrent()
            vm.open(old) // The screen effect must not duplicate the constructor's pending load.
            runCurrent()
            assertEquals(1, starts)
            vm.open(fresh)
            runCurrent()
            assertEquals("fresh", vm.uiState.value.model.sheets.single().name)
            oldFinished.complete(Unit)
            advanceUntilIdle()
            assertEquals("fresh", vm.uiState.value.model.sheets.single().name)
            assertFalse(vm.uiState.value.isLoading)
            assertNull(vm.uiState.value.errorMessage)
            assertNull(vm.uiState.value.failure)
        } finally {
            oldFinished.complete(Unit)
            vm.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }
}
