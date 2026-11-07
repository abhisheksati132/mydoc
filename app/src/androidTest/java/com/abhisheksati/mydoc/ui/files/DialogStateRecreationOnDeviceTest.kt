package com.abhisheksati.mydoc.ui.files

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import org.junit.Rule
import org.junit.Test
import java.io.File

class DialogStateRecreationOnDeviceTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun newFolderDialogKeepsTypedNameAcrossRecreation() {
        DialogHostActivity.content = { NewFolderDialog({}, {}) }
        ActivityScenario.launch(DialogHostActivity::class.java).use { scenario ->
            compose.onNode(hasSetTextAction()).performTextInput("L5check")
            scenario.recreate()
            compose.onNode(hasSetTextAction()).assertTextContains("L5check")
        }
    }

    @Test fun renameDialogKeepsEditedNameAcrossRecreation() {
        val file = File("/sdcard/notes.txt")
        DialogHostActivity.content = { RenameDialog(file, {}, {}) }
        ActivityScenario.launch(DialogHostActivity::class.java).use { scenario ->
            compose.onNode(hasSetTextAction()).performTextClearance()
            compose.onNode(hasSetTextAction()).performTextInput("renamed.txt")
            scenario.recreate()
            compose.onNode(hasSetTextAction()).assertTextContains("renamed.txt")
        }
    }

    @Test fun explorerSearchDialogKeepsFiltersAcrossRecreation() {
        val directory = File("/sdcard")
        DialogHostActivity.content = { ExplorerSearchDialog(directory, null, {}, {}) }
        ActivityScenario.launch(DialogHostActivity::class.java).use { scenario ->
            compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("report")
            scenario.recreate()
            compose.onAllNodes(hasSetTextAction()).onFirst().assertTextContains("report")
        }
    }

    @Test fun saveSearchDialogKeepsLabelAcrossRecreation() {
        DialogHostActivity.content = { SaveExplorerSearchDialog({}, { true }) }
        ActivityScenario.launch(DialogHostActivity::class.java).use { scenario ->
            compose.onNode(hasSetTextAction()).performTextInput("Weekly")
            scenario.recreate()
            compose.onNode(hasSetTextAction()).assertTextContains("Weekly")
        }
    }
}