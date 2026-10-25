package com.abhisheksati.mydoc.viewmodel

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

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakeStorageRepository
    private lateinit var tempDir: File
    private lateinit var viewModel: FileManagerViewModel

    @Before
    fun setUp() {
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
                    title = "Internal",
                    path = tempDir.absolutePath,
                    totalBytes = 100_000_000L,
                    freeBytes = 50_000_000L,
                    isPrimary = true
                )
            ),
            filesInDir = testFiles.map { StorageItem(file = it) }
        )

        viewModel = FileManagerViewModel(
            storageRepository = fakeRepo,
            baseDir = tempDir
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        tempDir.deleteRecursively()
    }

    @Test
    fun initialState_loadsVolumesAndDirectory() = runTest(testDispatcher) {
        advanceUntilIdle()
        val state = viewModel.uiState.value

        assertEquals(tempDir.absolutePath, state.currentDirectory.absolutePath)
        assertEquals(1, state.storageVolumes.size)
        assertEquals("Internal", state.storageVolumes[0].title)
        assertFalse(state.isLoading)
        assertEquals(4, state.items.size)
        assertEquals(4, state.filteredItems.size)
    }

    @Test
    fun searchQuery_filtersItemsAccurately() = runTest(testDispatcher) {
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
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showHiddenFiles)

        viewModel.toggleHiddenFiles()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showHiddenFiles)
    }

    @Test
    fun startBrowsingStorage_and_exitToHub_transitionsCorrectly() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isBrowsingStorage)

        viewModel.startBrowsingStorage(tempDir)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isBrowsingStorage)

        viewModel.exitToHub()
        assertFalse(viewModel.uiState.value.isBrowsingStorage)
        assertFalse(viewModel.uiState.value.isSelectionMode)
    }

    private class FakeStorageRepository(
        val volumes: List<StorageVolume>,
        val filesInDir: List<StorageItem>
    ) : StorageRepository {
        override fun getStorageVolumes(): List<StorageVolume> = volumes

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

        override suspend fun copy(sources: List<File>, targetDir: File, onProgress: (Int, Int) -> Unit): Result<Int> =
            Result.success(sources.size)

        override suspend fun move(sources: List<File>, targetDir: File, onProgress: (Int, Int) -> Unit): Result<Int> =
            Result.success(sources.size)

        override suspend fun getFolderProperties(directory: File): FolderProperties =
            FolderProperties(
                file = directory,
                totalSizeBytes = 1024L,
                totalFilesCount = 4,
                totalFoldersCount = 0,
                isReadable = true,
                isWritable = true
            )

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
