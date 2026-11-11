package com.abhisheksati.mydoc.viewmodel

import com.abhisheksati.mydoc.R
import androidx.lifecycle.viewModelScope
import com.abhisheksati.mydoc.domain.model.DocumentTypeFilter
import com.abhisheksati.mydoc.domain.model.FileCategory
import com.abhisheksati.mydoc.domain.model.FileSortField
import com.abhisheksati.mydoc.domain.model.FileSortOrder
import com.abhisheksati.mydoc.domain.model.FolderProperties
import com.abhisheksati.mydoc.domain.model.StorageItem
import com.abhisheksati.mydoc.domain.model.StorageVolume
import com.abhisheksati.mydoc.domain.model.TrashItem
import com.abhisheksati.mydoc.domain.repository.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class FileManagerViewModelTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var fakeRepo: FakeStorageRepository
    private lateinit var tempDir: File
    private val modelOwner = androidx.lifecycle.ViewModelStore()
    private lateinit var viewModel: FileManagerViewModel

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        tempDir = File.createTempFile("fm_test", "dir").apply {
            delete()
            mkdirs()
        }

        val testFiles = listOf(
            File(tempDir, "report.pdf").apply { writeText("Report content") },
            File(tempDir, "photo.jpg").apply { writeText("Image content") },
            File(tempDir, "notes.txt").apply { writeText("Notes content") },
            File(tempDir, ".hidden_config").apply { writeText("Config content") }
        )

        fakeRepo = FakeStorageRepository(
            volumes = listOf(
                StorageVolume(
                    titleRes = R.string.files_title_internal,
                    path = tempDir.absolutePath,
                    totalBytes = 100_000_000L,
                    freeBytes = 50_000_000L,
                    isPrimary = true
                )
            ),
            filesInDir = testFiles.map { StorageItem(file = it) }
        )
    }

    private fun createViewModel(preferences: com.abhisheksati.mydoc.data.local.ExplorerPreferences? = null): FileManagerViewModel {
        val vm = FileManagerViewModel(
            storageRepository = fakeRepo,
            baseDir = tempDir,
            explorerPreferences = preferences ?: com.abhisheksati.mydoc.data.local.ExplorerPreferences(),
            ioDispatcher = testDispatcher
        )
        viewModel = vm
        modelOwner.put("browser", vm)
        return vm
    }

    @After
    fun tearDown() {
        if (::viewModel.isInitialized) {
            val job = viewModel.viewModelScope.coroutineContext[kotlinx.coroutines.Job]
            runTest(testDispatcher) {
                modelOwner.clear()
                job?.join()
            }
        }
        testDispatcher.scheduler.advanceUntilIdle()
        Dispatchers.resetMain()
        tempDir.deleteRecursively()
    }

    @Test fun pendingUpDoesNotOverrideANewerCategory() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        val child = File(tempDir, "child").apply { mkdirs() }
        viewModel.loadDirectory(child, addToHistory = false)
        advanceUntilIdle()
        assertTrue(viewModel.navigateUp())
        viewModel.selectCategory(FileCategory.IMAGES)
        advanceUntilIdle()
        assertEquals(FileCategory.IMAGES, viewModel.uiState.value.activeCategory)
    }

    @Test fun clearingSearchFiltersKeepsCategoryAndRestoresItsFiles() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        viewModel.selectCategory(FileCategory.DOCUMENTS)
        advanceUntilIdle()
        viewModel.setDocTypeFilter(DocumentTypeFilter.PDF)
        viewModel.setSearchQuery("missing")
        assertTrue(viewModel.uiState.value.filteredItems.isEmpty())
        viewModel.clearSearchFilters()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(FileCategory.DOCUMENTS, state.activeCategory)
        assertEquals(DocumentTypeFilter.ALL, state.docTypeFilter)
        assertEquals("", state.searchQuery)
        assertEquals(4, state.filteredItems.size)
    }

    @Test fun clearingAdvancedFiltersReturnsToTheFolderWithoutAHiddenQuery() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        viewModel.searchAdvanced(com.abhisheksati.mydoc.domain.model.ExplorerSearchOptions(name = "missing"))
        advanceUntilIdle()
        viewModel.setSearchQuery("missing")
        viewModel.clearSearchFilters()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(tempDir, state.currentDirectory)
        assertEquals(null, state.advancedSearch)
        assertEquals("", state.searchQuery)
        assertEquals(4, state.filteredItems.size)
    }

    @Test fun copyItemTargetsExactlyTheRequestedFileEvenWhenItWasSelected() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        val target = File(tempDir, "report.pdf")
        viewModel.toggleSelection(target)
        viewModel.toggleSelection(File(tempDir, "notes.txt"))
        viewModel.copyItem(target)
        advanceUntilIdle()
        assertEquals(listOf(target), viewModel.explorerClipboard.state.value?.items)
        assertEquals(com.abhisheksati.mydoc.domain.model.ClipboardOperation.COPY, viewModel.explorerClipboard.state.value?.operation)
        assertTrue(viewModel.uiState.value.selectedFiles.isEmpty())
    }

    @Test fun moveItemDoesNotIncludeAnUnrelatedSelection() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        val target = File(tempDir, "report.pdf")
        viewModel.toggleSelection(File(tempDir, "notes.txt"))
        viewModel.cutItem(target)
        advanceUntilIdle()
        assertEquals(listOf(target), viewModel.explorerClipboard.state.value?.items)
        assertEquals(com.abhisheksati.mydoc.domain.model.ClipboardOperation.CUT, viewModel.explorerClipboard.state.value?.operation)
        assertFalse(viewModel.uiState.value.isSelectionMode)
    }

    @Test fun aBusyClipboardKeepsBothTheTransferAndNewSelectionIntact() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        val original = File(tempDir, "report.pdf")
        val next = File(tempDir, "notes.txt")
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        fakeRepo.copyGate = gate
        try {
            viewModel.copyItem(original)
            viewModel.pasteClipboard()
            advanceUntilIdle()
            viewModel.toggleSelection(next)
            viewModel.copyItem(next)
            assertEquals(listOf(original), viewModel.explorerClipboard.state.value?.items)
            assertEquals(setOf(next), viewModel.uiState.value.selectedFiles)
            assertEquals(com.abhisheksati.mydoc.util.UiText.Res(R.string.transfers_active), viewModel.uiState.value.userMessage)
        } finally {
            gate.complete(Unit)
            advanceUntilIdle()
        }
    }

    @Test fun completedTransferDestinationCanBeOpened() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        val destination = File(tempDir, "destination").apply { mkdirs() }
        viewModel.openTransferDestination(destination)
        advanceUntilIdle()
        assertEquals(destination, viewModel.uiState.value.currentDirectory)
        assertTrue(viewModel.uiState.value.isBrowsingStorage)
    }

    @Test fun missingDestinationKeepsTheCurrentViewAndSelection() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        val selected = File(tempDir, "report.pdf")
        viewModel.toggleSelection(selected)
        viewModel.setSearchQuery("report")
        viewModel.openTransferDestination(File(tempDir, "missing"))
        advanceUntilIdle()
        assertEquals(tempDir, viewModel.uiState.value.currentDirectory)
        assertEquals("report", viewModel.uiState.value.searchQuery)
        assertEquals(setOf(selected), viewModel.uiState.value.selectedFiles)
        assertEquals(com.abhisheksati.mydoc.util.UiText.Res(R.string.transfers_destination_missing), viewModel.uiState.value.userMessage)
    }

    @Test fun newerNavigationWinsOverPendingTransferDestination() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        val destination = File(tempDir, "destination").apply { mkdirs() }
        viewModel.openTransferDestination(destination)
        viewModel.selectCategory(FileCategory.IMAGES)
        advanceUntilIdle()
        assertEquals(FileCategory.IMAGES, viewModel.uiState.value.activeCategory)
    }

    @Test fun returningToTheHubCancelsAPendingDestinationFromACategory() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        viewModel.selectCategory(FileCategory.IMAGES)
        advanceUntilIdle()
        val destination = File(tempDir, "destination").apply { mkdirs() }
        viewModel.openTransferDestination(destination)
        viewModel.selectCategory(null)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isBrowsingStorage)
        assertEquals(null, viewModel.uiState.value.activeCategory)
        assertEquals(tempDir, viewModel.uiState.value.currentDirectory)
    }

    @Test fun newerDestinationRequestReplacesAnOlderOne() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        val first = File(tempDir, "first").apply { mkdirs() }
        val second = File(tempDir, "second").apply { mkdirs() }
        viewModel.openTransferDestination(first)
        viewModel.openTransferDestination(second)
        advanceUntilIdle()
        assertEquals(second, viewModel.uiState.value.currentDirectory)
    }

    @Test fun refreshKeepsActiveCategory() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        viewModel.selectCategory(FileCategory.IMAGES)
        advanceUntilIdle()
        viewModel.refreshCurrentSource()
        advanceUntilIdle()
        assertEquals(FileCategory.IMAGES, viewModel.uiState.value.activeCategory)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test fun refreshRerunsAdvancedSearchWithoutDroppingIt() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        val options = com.abhisheksati.mydoc.domain.model.ExplorerSearchOptions(name = "report")
        viewModel.searchAdvanced(options)
        advanceUntilIdle()
        viewModel.refreshCurrentSource()
        advanceUntilIdle()
        assertEquals(options, viewModel.uiState.value.advancedSearch)
    }

    @Test fun dismissedPropertiesDialogIsNotReopenedByStaleInspection() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        fakeRepo.propertiesGate = gate
        viewModel.inspectItem(tempDir)
        advanceUntilIdle()
        viewModel.dismissPropertiesDialog()
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(null, viewModel.uiState.value.selectedProperties)
    }

    @Test fun staleInspectionDoesNotOverwriteNewerOne() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        fakeRepo.propertiesGate = gate
        viewModel.inspectItem(tempDir)
        advanceUntilIdle()
        fakeRepo.propertiesGate = null
        val file = File(tempDir, "report.pdf")
        viewModel.inspectItem(file)
        advanceUntilIdle()
        assertEquals(file, viewModel.uiState.value.selectedProperties?.file)
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(file, viewModel.uiState.value.selectedProperties?.file)
    }

    @Test fun chipCountsSurviveChipSelection() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        viewModel.selectCategory(FileCategory.DOCUMENTS)
        advanceUntilIdle()
        viewModel.setDocTypeFilter(DocumentTypeFilter.PDF)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(1, state.filteredItems.size)
        val counts = documentTypeCounts(state.items, state.searchQuery)
        assertEquals(4, counts[DocumentTypeFilter.ALL])
        assertEquals(1, counts[DocumentTypeFilter.PDF])
        assertEquals(1, counts[DocumentTypeFilter.TEXT])
    }

    @Test fun leavingSearchClearsItsScopeBeforeCategoryRefresh() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        viewModel.searchAdvanced(com.abhisheksati.mydoc.domain.model.ExplorerSearchOptions(name = "report"))
        viewModel.exitToHub()
        assertEquals(null, viewModel.uiState.value.advancedSearch)
        assertFalse(viewModel.uiState.value.searchIncomplete)
        viewModel.selectCategory(FileCategory.IMAGES)
        advanceUntilIdle()
        assertEquals(null, viewModel.uiState.value.advancedSearch)
        assertEquals(FileCategory.IMAGES, viewModel.uiState.value.activeCategory)
    }

    @Test
    fun initialState_loadsVolumesAndDirectory() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        val state = viewModel.uiState.value

        assertEquals(tempDir.absolutePath, state.currentDirectory.absolutePath)
        assertEquals(1, state.storageVolumes.size)
        assertEquals(R.string.files_title_internal, state.storageVolumes[0].titleRes)
        assertFalse(state.isLoading)
        assertEquals(4, state.items.size)
        assertEquals(4, state.filteredItems.size)
    }

    @Test
    fun selectAll_onlyCoversRowsTheFormatChipShows() = runTest(testDispatcher) {
        // The chip used to filter purely in the composable, so Select All picked up files the
        // reader could not see and a share, cut or trash then acted on them.
        createViewModel()
        advanceUntilIdle()

        viewModel.selectCategory(FileCategory.DOCUMENTS)
        advanceUntilIdle()
        viewModel.setDocTypeFilter(DocumentTypeFilter.PDF)
        advanceUntilIdle()

        val visible = viewModel.uiState.value.filteredItems
        assertTrue("the chip must narrow the list the ViewModel exposes", visible.isNotEmpty())
        assertTrue(
            "only PDFs should remain visible: ${visible.map { it.name }}",
            visible.all { it.file.extension.equals("pdf", ignoreCase = true) }
        )

        viewModel.selectAll()
        advanceUntilIdle()

        assertEquals(visible.map { it.file }.toSet(), viewModel.uiState.value.selectedFiles)
        assertTrue(
            "Select All reached a file the chip was hiding",
            viewModel.uiState.value.selectedFiles.none { !it.extension.equals("pdf", ignoreCase = true) }
        )
    }

    @Test
    fun searchQuery_filtersItemsAccurately() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()

        viewModel.setSearchQuery("report")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("report", state.searchQuery)
        assertEquals(1, state.filteredItems.size)
        assertEquals("report.pdf", state.filteredItems[0].name)

        // Clear search
        viewModel.setSearchQuery("")
        advanceUntilIdle()
        assertEquals(4, viewModel.uiState.value.filteredItems.size)
    }

    @Test
    fun toggleSelection_activatesAndUpdatesSelectionMode() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        val targetFile = File(tempDir, "report.pdf")

        assertFalse(viewModel.uiState.value.isSelectionMode)
        assertTrue(viewModel.uiState.value.selectedFiles.isEmpty())

        viewModel.toggleSelection(targetFile)
        assertTrue(viewModel.uiState.value.isSelectionMode)
        assertEquals(1, viewModel.uiState.value.selectedFiles.size)
        assertTrue(viewModel.uiState.value.selectedFiles.contains(targetFile))

        // Deselect
        viewModel.toggleSelection(targetFile)
        assertFalse(viewModel.uiState.value.isSelectionMode)
        assertTrue(viewModel.uiState.value.selectedFiles.isEmpty())
    }

    @Test
    fun selectAll_selectsAllFilesInCurrentList() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        viewModel.selectAll()

        assertTrue(viewModel.uiState.value.isSelectionMode)
        assertEquals(4, viewModel.uiState.value.selectedFiles.size)

        viewModel.clearSelection()
        assertFalse(viewModel.uiState.value.isSelectionMode)
        assertTrue(viewModel.uiState.value.selectedFiles.isEmpty())
    }

    @Test
    fun sortOrder_togglesSortingFieldAndDirection() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        val initialSort = viewModel.uiState.value.sortOrder

        assertEquals(FileSortField.NAME, initialSort.field)
        assertTrue(initialSort.isAscending)

        // Switch to Size
        viewModel.toggleSort(FileSortField.SIZE)
        advanceUntilIdle()
        assertEquals(FileSortField.SIZE, viewModel.uiState.value.sortOrder.field)

        // Toggle direction (same field toggles ascending/descending)
        viewModel.toggleSort(FileSortField.SIZE)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.sortOrder.isAscending)
    }

    @Test
    fun toggleHiddenFiles_updatesStateAndReloads() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showHiddenFiles)

        viewModel.toggleHiddenFiles()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showHiddenFiles)
    }

    @Test
    fun startBrowsingStorage_and_exitToHub_transitionsCorrectly() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isBrowsingStorage)

        viewModel.startBrowsingStorage(tempDir)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isBrowsingStorage)

        viewModel.exitToHub()
        assertFalse(viewModel.uiState.value.isBrowsingStorage)
        assertFalse(viewModel.uiState.value.isSelectionMode)
    }

    @Test
    fun explorerTabsKeepNavigationSeparateAndShareClipboard() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        val workspace = ExplorerWorkspaceViewModel(viewModel, fakeRepo, null, androidx.lifecycle.SavedStateHandle())
        val owner = androidx.lifecycle.ViewModelStore().apply { put("workspace", workspace) }
        try {
            val newId = workspace.add()!!
            advanceUntilIdle()
            val other = workspace.tabs.value.first { it.id == newId }.browser
            val child = File(tempDir, "child").apply { mkdir() }
            other.startBrowsingStorage(child)
            advanceUntilIdle()
            assertEquals(tempDir, viewModel.uiState.value.currentDirectory)
            assertEquals(child, other.uiState.value.currentDirectory)
            viewModel.toggleSelection(File(tempDir, "report.pdf"))
            viewModel.copySelected()
            advanceUntilIdle()
            assertEquals(listOf(File(tempDir, "report.pdf")), other.uiState.value.clipboardState?.items)
            other.pasteClipboard()
            advanceUntilIdle()
            assertEquals(child, fakeRepo.lastPasteTarget)
            assertEquals(null, viewModel.uiState.value.clipboardState)
            workspace.toggleSplit()
            assertTrue(workspace.secondary.value != null)
            workspace.close(newId)
            assertEquals(1, workspace.tabs.value.size)
        } finally { owner.clear() }
    }

    @Test fun closingTransferTabDoesNotCancelSharedTransfer() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        fakeRepo.copyGate = kotlinx.coroutines.CompletableDeferred()
        val workspace = ExplorerWorkspaceViewModel(viewModel, fakeRepo, null, androidx.lifecycle.SavedStateHandle())
        val owner = androidx.lifecycle.ViewModelStore().apply { put("workspace", workspace) }
        try {
            val id = workspace.add()!!
            advanceUntilIdle()
            val child = workspace.tabs.value.last().browser
            child.toggleSelection(File(tempDir, "report.pdf"))
            child.copySelected()
            child.pasteClipboard()
            assertTrue(viewModel.explorerClipboard.transfers.state.value.first().running)
            workspace.close(id)
            fakeRepo.copyGate!!.complete(Unit)
            advanceUntilIdle()
            assertEquals(com.abhisheksati.mydoc.domain.model.TransferStatus.COMPLETED, viewModel.explorerClipboard.transfers.state.value.first().status)
            assertEquals(null, viewModel.explorerClipboard.state.value)
        } finally { fakeRepo.copyGate!!.complete(Unit); owner.clear() }
    }

    @Test
    fun explorerWorkspaceRestoresLocationsAndLimitsTabs() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()
        val saved = androidx.lifecycle.SavedStateHandle(mapOf("ids" to intArrayOf(0, 3), "active" to 3, "next" to 4, "path3" to tempDir.absolutePath))
        val workspace = ExplorerWorkspaceViewModel(viewModel, fakeRepo, null, saved)
        val owner = androidx.lifecycle.ViewModelStore().apply { put("workspace", workspace) }
        try {
            advanceUntilIdle()
            assertEquals(3, workspace.active.value)
            assertEquals(tempDir, workspace.tabs.value.last().browser.uiState.value.currentDirectory)
            repeat(8) { workspace.add() }
            advanceUntilIdle()
            assertEquals(6, workspace.tabs.value.size)
            assertEquals(null, workspace.add())
        } finally { owner.clear() }
    }

    @Test fun folderViewsRestoreAndUpdateOtherTabs() = runTest(testDispatcher) {
        createViewModel()
        viewModel.startBrowsingStorage(tempDir)
        advanceUntilIdle()
        val workspace = ExplorerWorkspaceViewModel(viewModel, fakeRepo, null, androidx.lifecycle.SavedStateHandle())
        val owner = androidx.lifecycle.ViewModelStore().apply { put("workspace", workspace) }
        try {
            workspace.add(); advanceUntilIdle()
            val other = workspace.tabs.value.last().browser
            viewModel.toggleViewMode()
            viewModel.toggleSort(FileSortField.SIZE)
            advanceUntilIdle()
            assertTrue(other.uiState.value.isGridView)
            assertEquals(FileSortField.SIZE, other.uiState.value.sortOrder.field)
            val child = tempDir.resolve("child").apply { mkdir() }
            viewModel.loadDirectory(child); advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isGridView)
            assertEquals(FileSortField.NAME, viewModel.uiState.value.sortOrder.field)
            viewModel.loadDirectory(tempDir); advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isGridView)
            viewModel.resetFolderView(); advanceUntilIdle()
            assertFalse(other.uiState.value.isGridView)
            assertEquals(FileSortField.NAME, other.uiState.value.sortOrder.field)
        } finally { owner.clear() }
    }

    @Test fun resetClearsUnsavedLayoutAfterStorageWriteRecovers() = runTest(testDispatcher) {
        var failing = true
        val preferences = com.abhisheksati.mydoc.data.local.ExplorerPreferences(write = {
            if (failing) throw java.io.IOException("failed")
        })
        createViewModel(preferences)
        viewModel.startBrowsingStorage(tempDir); advanceUntilIdle()
        viewModel.toggleViewMode(); advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isGridView)
        assertEquals(null, preferences.folderView(tempDir))
        failing = false
        viewModel.resetFolderView(); advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isGridView)
    }

    @Test fun categoriesDoNotInheritFolderOverrides() = runTest(testDispatcher) {
        createViewModel()
        viewModel.startBrowsingStorage(tempDir); advanceUntilIdle()
        viewModel.toggleViewMode(); viewModel.toggleSort(FileSortField.SIZE); advanceUntilIdle()
        viewModel.exitToHub(); viewModel.selectCategory(FileCategory.DOCUMENTS); advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isGridView)
        assertEquals(FileSortField.NAME, viewModel.uiState.value.sortOrder.field)
        viewModel.toggleSort(FileSortField.DATE); advanceUntilIdle()
        viewModel.loadDirectory(tempDir); advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isGridView)
        assertEquals(FileSortField.SIZE, viewModel.uiState.value.sortOrder.field)
        viewModel.selectCategory(FileCategory.DOCUMENTS); advanceUntilIdle()
        assertEquals(FileSortField.DATE, viewModel.uiState.value.sortOrder.field)
    }

    @Test fun imagesStartAsNewestFirstGalleryAndRefreshKeepsUserView() = runTest(testDispatcher) {
        createViewModel()
        viewModel.selectCategory(FileCategory.IMAGES); advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isGridView)
        assertEquals(FileSortField.DATE, viewModel.uiState.value.sortOrder.field)
        assertFalse(viewModel.uiState.value.sortOrder.isAscending)

        viewModel.toggleViewMode()
        viewModel.toggleSort(FileSortField.NAME)
        viewModel.selectCategory(FileCategory.IMAGES); advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isGridView)
        assertEquals(FileSortField.NAME, viewModel.uiState.value.sortOrder.field)
    }

    @Test fun selectingAllInImageFolderUsesOnlyVisibleFiles() = runTest(testDispatcher) {
        val camera = File(tempDir, "Camera").apply { mkdirs() }
        val screenshots = File(tempDir, "Screenshots").apply { mkdirs() }
        val photo = File(camera, "photo.jpg").apply { writeText("Photo") }
        val screenshot = File(screenshots, "screen.png").apply { writeText("Screenshot") }
        fakeRepo = FakeStorageRepository(fakeRepo.volumes, listOf(StorageItem(photo), StorageItem(screenshot)))
        createViewModel()
        viewModel.selectCategory(FileCategory.IMAGES)
        advanceUntilIdle()

        viewModel.setImageFolder(camera.absolutePath)
        viewModel.selectAll()
        assertEquals(listOf(photo), viewModel.uiState.value.filteredItems.map { it.file })
        assertEquals(setOf(photo), viewModel.uiState.value.selectedFiles)

        viewModel.clearSearchFilters()
        assertEquals(setOf(photo, screenshot), viewModel.uiState.value.filteredItems.map { it.file }.toSet())
        assertTrue(viewModel.uiState.value.selectedFiles.isEmpty())
    }

    @Test fun savedSearchWinsAgainstQueuedDirectoryLoad() = runTest(testDispatcher) {
        createViewModel(); advanceUntilIdle()
        val target = tempDir.resolve("saved").apply { mkdir() }
        val preferences = viewModel.explorerPreferences
        assertTrue(preferences.saveSearch("Reports", target, com.abhisheksati.mydoc.domain.model.ExplorerSearchOptions(name = "report")))
        viewModel.loadDirectory(tempDir)
        viewModel.openSavedSearch(preferences.savedSearches.value.single().id)?.join()
        advanceUntilIdle()
        // The IO search may finish later, but its folder and filters are installed atomically.
        assertEquals(target, viewModel.uiState.value.currentDirectory)
        assertEquals("report", viewModel.uiState.value.advancedSearch?.name)
        assertTrue(viewModel.uiState.value.isBrowsingStorage)
    }

    @Test fun missingSavedFolderKeepsCurrentLocation() = runTest(testDispatcher) {
        createViewModel(); advanceUntilIdle()
        val preferences = viewModel.explorerPreferences
        preferences.saveSearch("Missing", tempDir.resolve("missing"), com.abhisheksati.mydoc.domain.model.ExplorerSearchOptions())
        viewModel.openSavedSearch(preferences.savedSearches.value.single().id)?.join()
        advanceUntilIdle()
        assertEquals(tempDir, viewModel.uiState.value.currentDirectory)
        assertEquals(null, viewModel.uiState.value.advancedSearch)
        assertTrue(viewModel.uiState.value.userMessage != null)
    }

    private class FakeStorageRepository(
        val volumes: List<StorageVolume>,
        val filesInDir: List<StorageItem>
    ) : StorageRepository {
        override suspend fun renameBatch(plan: com.abhisheksati.mydoc.domain.model.ExplorerRenamePlan) = com.abhisheksati.mydoc.domain.model.ExplorerRenameResult(false)
        override suspend fun pendingRenameRecovery(): List<String> = emptyList()
        override suspend fun dismissRenameRecovery() = Unit
        var copyGate: kotlinx.coroutines.CompletableDeferred<Unit>? = null
        var propertiesGate: kotlinx.coroutines.CompletableDeferred<Unit>? = null
        var lastPasteTarget: File? = null
        override suspend fun getStorageVolumes(): List<StorageVolume> = volumes

        override suspend fun listDirectory(
            directory: File,
            showHidden: Boolean,
            sortOrder: FileSortOrder
        ): List<StorageItem> = filesInDir

        override suspend fun createDirectory(parent: File, name: String): Result<File> =
            Result.success(File(parent, name))

        override suspend fun createFile(parent: File, name: String): Result<File> =
            Result.success(File(parent, name))

        override suspend fun rename(source: File, newName: String): Result<File> =
            Result.success(File(source.parentFile, newName))

        override suspend fun delete(files: List<File>): Result<Int> = Result.success(files.size)

        override suspend fun copy(sources: List<File>, targetDir: File, onProgress: (Int, Int) -> Unit): Result<Int> {
            lastPasteTarget = targetDir
            copyGate?.await()
            return Result.success(sources.size)
        }

        override suspend fun move(sources: List<File>, targetDir: File, onProgress: (Int, Int) -> Unit): Result<Int> =
            Result.success(sources.size)

        override suspend fun getFolderProperties(directory: File): FolderProperties {
            propertiesGate?.await()
            return FolderProperties(
                file = directory,
                totalSizeBytes = 1024L,
                totalFilesCount = 4,
                totalFoldersCount = 0,
                isReadable = true,
                isWritable = true
            )
        }

        override suspend fun calculateHash(file: File, algorithm: String): String = "mock_hash"

        override suspend fun zip(sources: List<File>, destinationZip: File, onProgress: (Int, Int) -> Unit): Result<File> =
            Result.success(destinationZip)

        override suspend fun unzip(zipFile: File, destinationDir: File, onProgress: (Int) -> Unit): Result<File> =
            Result.success(destinationDir)

        override suspend fun queryCategory(category: FileCategory): List<StorageItem> = filesInDir

        override suspend fun searchFiles(rootDirectory: File, query: String, recursive: Boolean): List<StorageItem> =
            filesInDir.filter { it.name.contains(query, ignoreCase = true) }

        override fun getTrashItems(): Flow<List<TrashItem>> = flowOf(emptyList())

        override suspend fun moveToTrash(files: List<File>): Result<List<TrashItem>> = Result.success(emptyList())

        override suspend fun restoreFromTrash(items: List<TrashItem>): Result<Int> = Result.success(items.size)

        override suspend fun deletePermanently(items: List<TrashItem>): Result<Int> = Result.success(items.size)

        override suspend fun emptyTrash(): Result<Int> = Result.success(0)
    }
}
