package com.abhisheksati.mydoc.ui.viewer

import android.app.Application
import android.content.Context
import android.graphics.RectF
import android.net.Uri
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.domain.model.ViewState
import com.abhisheksati.mydoc.domain.repository.DocumentRepository
import com.abhisheksati.mydoc.rendering.engine.SearchMatch
import com.abhisheksati.mydoc.viewmodel.ViewerUiState
import com.abhisheksati.mydoc.viewmodel.ViewerViewModel
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
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
class ViewerViewModelTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var tempDir: File
    private lateinit var fakeRepo: FakeDocumentRepository
    private lateinit var docFile: DocumentFile
    private var currentViewModel: ViewerViewModel? = null

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        tempDir = File.createTempFile("vm_test", "dir").apply {
            delete()
            mkdirs()
        }
        fakeRepo = FakeDocumentRepository()
        docFile = DocumentFile(
            uri = "content://test/sample.pdf",
            displayName = "sample.pdf",
            sizeBytes = 2048L,
            mimeType = "application/pdf",
            lastOpenedTimestamp = 1000L
        )
    }

    @After
    fun tearDown() {
        currentViewModel?.let { vm ->
            try {
                val method = ViewModel::class.java.getDeclaredMethod("onCleared")
                method.isAccessible = true
                method.invoke(vm)
            } catch (_: Exception) {}
        }
        testDispatcher.scheduler.advanceUntilIdle()
        Dispatchers.resetMain()
        tempDir.deleteRecursively()
    }

    private fun mockApplication(cacheDir: File): Application {
        return object : Application() {
            override fun getCacheDir(): File = cacheDir
            override fun getApplicationContext(): Context = this
        }
    }

    private fun createViewModel(): Pair<ViewerViewModel, MutableStateFlow<ViewerUiState>> {
        val app = mockApplication(tempDir)
        val vm = ViewerViewModel(app, fakeRepo, docFile)
        currentViewModel = vm
        val field = ViewerViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = field.get(vm) as MutableStateFlow<ViewerUiState>
        return Pair(vm, stateFlow)
    }

    @Test
    fun viewerUiState_hasSensibleDefaults() {
        val state = ViewerUiState()
        assertEquals(0, state.pageCount)
        assertEquals(0, state.currentPage)
        assertEquals(1.0f, state.zoomScale, 0.001f)
        assertEquals(ReadingMode.NORMAL, state.readingMode)
        assertTrue(state.isContinuousScroll)
        assertFalse(state.isDualPageView)
        assertFalse(state.isFitPage)
        assertTrue(state.areControlsVisible)
        assertFalse(state.isSearchOpen)
        assertEquals("", state.searchQuery)
        assertTrue(state.searchMatches.isEmpty())
        assertEquals(-1, state.currentMatchIndex)
    }

    @Test
    fun initialState_hasCorrectDefaults() = runTest(testDispatcher) {
        val (viewModel, _) = createViewModel()
        val state = viewModel.uiState.value

        assertEquals(0, state.currentPage)
        assertFalse(state.isSearchOpen)
        assertEquals("", state.searchQuery)
        assertTrue(state.searchMatches.isEmpty())
        assertEquals(-1, state.currentMatchIndex)
        assertEquals(1.0f, state.zoomScale, 0.001f)
        assertEquals(ReadingMode.NORMAL, state.readingMode)
        assertTrue(state.isContinuousScroll)
        advanceUntilIdle()
    }

    @Test
    fun pageNavigation_incrementsAndDecrementsWithinBounds() = runTest(testDispatcher) {
        val (viewModel, stateFlow) = createViewModel()
        // Simulate a loaded document with 10 pages
        stateFlow.update { it.copy(pageCount = 10, currentPage = 0) }

        viewModel.onPageChanged(1)
        assertEquals(1, viewModel.uiState.value.currentPage)

        viewModel.onPageChanged(5)
        assertEquals(5, viewModel.uiState.value.currentPage)

        viewModel.jumpToPage(9)
        assertEquals(9, viewModel.uiState.value.currentPage)

        // Attempting to go out of bounds should be ignored or clamped
        viewModel.onPageChanged(10) // Out of bounds for 0 until 10
        assertEquals(9, viewModel.uiState.value.currentPage)

        viewModel.jumpToPage(20) // Clamped to max page 9
        assertEquals(9, viewModel.uiState.value.currentPage)

        viewModel.jumpToPage(-5) // Clamped to 0
        assertEquals(0, viewModel.uiState.value.currentPage)
        advanceUntilIdle()
    }

    @Test
    fun searchState_opensUpdatesQueryAndCloses() = runTest(testDispatcher) {
        val (viewModel, _) = createViewModel()

        assertFalse(viewModel.uiState.value.isSearchOpen)
        viewModel.openSearch()
        assertTrue(viewModel.uiState.value.isSearchOpen)

        viewModel.onSearchQueryChanged("antigravity")
        assertEquals("antigravity", viewModel.uiState.value.searchQuery)

        // Reset query on short length (< 2)
        viewModel.onSearchQueryChanged("a")
        assertEquals("a", viewModel.uiState.value.searchQuery)
        assertTrue(viewModel.uiState.value.searchMatches.isEmpty())
        assertEquals(-1, viewModel.uiState.value.currentMatchIndex)

        viewModel.closeSearch()
        assertFalse(viewModel.uiState.value.isSearchOpen)
        assertEquals("", viewModel.uiState.value.searchQuery)
        assertTrue(viewModel.uiState.value.searchMatches.isEmpty())
        assertEquals(-1, viewModel.uiState.value.currentMatchIndex)
        advanceUntilIdle()
    }

    @Test
    fun searchMatches_cycleNextAndPrevious() = runTest(testDispatcher) {
        val (viewModel, stateFlow) = createViewModel()
        val dummyMatches = listOf(
            SearchMatch(pageIndex = 1, bounds = RectF(0f, 0f, 10f, 10f)),
            SearchMatch(pageIndex = 3, bounds = RectF(0f, 0f, 10f, 10f)),
            SearchMatch(pageIndex = 7, bounds = RectF(0f, 0f, 10f, 10f))
        )
        stateFlow.update {
            it.copy(
                pageCount = 10,
                isSearchOpen = true,
                searchMatches = dummyMatches,
                currentMatchIndex = 0,
                currentPage = 1
            )
        }

        viewModel.nextSearchMatch()
        assertEquals(1, viewModel.uiState.value.currentMatchIndex)
        assertEquals(3, viewModel.uiState.value.currentPage)

        viewModel.nextSearchMatch()
        assertEquals(2, viewModel.uiState.value.currentMatchIndex)
        assertEquals(7, viewModel.uiState.value.currentPage)

        // Cycles back to 0
        viewModel.nextSearchMatch()
        assertEquals(0, viewModel.uiState.value.currentMatchIndex)
        assertEquals(1, viewModel.uiState.value.currentPage)

        // Previous cycles to last
        viewModel.previousSearchMatch()
        assertEquals(2, viewModel.uiState.value.currentMatchIndex)
        assertEquals(7, viewModel.uiState.value.currentPage)
        advanceUntilIdle()
    }

    @Test
    fun controlsAndDisplayMode_toggleAccurately() = runTest(testDispatcher) {
        val (viewModel, _) = createViewModel()

        // Zoom
        viewModel.setZoomScale(2.5f)
        assertEquals(2.5f, viewModel.uiState.value.zoomScale, 0.001f)

        // Controls visibility
        assertTrue(viewModel.uiState.value.areControlsVisible)
        viewModel.toggleControls()
        assertFalse(viewModel.uiState.value.areControlsVisible)
        viewModel.setControlsVisible(true)
        assertTrue(viewModel.uiState.value.areControlsVisible)

        // Continuous scroll
        assertTrue(viewModel.uiState.value.isContinuousScroll)
        viewModel.toggleScrollMode()
        assertFalse(viewModel.uiState.value.isContinuousScroll)
        viewModel.setContinuousScroll(true)
        assertTrue(viewModel.uiState.value.isContinuousScroll)

        // Reading mode cycle: NORMAL -> INVERTED_DARK -> SEPIA -> EYE_COMFORT -> NORMAL
        assertEquals(ReadingMode.NORMAL, viewModel.uiState.value.readingMode)
        viewModel.toggleReadingMode()
        assertEquals(ReadingMode.INVERTED_DARK, viewModel.uiState.value.readingMode)
        viewModel.toggleReadingMode()
        assertEquals(ReadingMode.SEPIA, viewModel.uiState.value.readingMode)
        viewModel.toggleReadingMode()
        assertEquals(ReadingMode.EYE_COMFORT, viewModel.uiState.value.readingMode)
        viewModel.toggleReadingMode()
        assertEquals(ReadingMode.NORMAL, viewModel.uiState.value.readingMode)

        // Drawer and strip
        assertFalse(viewModel.uiState.value.isOutlineDrawerOpen)
        viewModel.setOutlineDrawerOpen(true)
        assertTrue(viewModel.uiState.value.isOutlineDrawerOpen)

        assertFalse(viewModel.uiState.value.isThumbnailStripVisible)
        viewModel.toggleThumbnailStrip()
        assertTrue(viewModel.uiState.value.isThumbnailStripVisible)
        advanceUntilIdle()
    }

    private class FakeDocumentRepository : DocumentRepository {
        var savedViewState: ViewState? = null
        var lastUpdatedPage: Int = -1
        var lastUpdatedTotalPages: Int = -1

        override fun getRecents(): Flow<List<DocumentFile>> = flowOf(emptyList())
        override fun getFavorites(): Flow<List<DocumentFile>> = flowOf(emptyList())
        override fun getAllDeviceDocuments(): Flow<List<DocumentFile>> = flowOf(emptyList())
        override suspend fun registerOpenedDocument(uri: Uri): Result<DocumentFile> = Result.failure(UnsupportedOperationException())
        override suspend fun toggleFavorite(document: DocumentFile) {}
        override suspend fun removeFromRecents(uri: String) {}
        override suspend fun updatePageProgress(uri: String, lastPage: Int, pageCount: Int) {
            lastUpdatedPage = lastPage
            lastUpdatedTotalPages = pageCount
        }
        override suspend fun getViewState(uri: String): ViewState? = savedViewState
        override suspend fun saveViewState(viewState: ViewState) {
            savedViewState = viewState
        }
        override suspend fun clearAllRecents() {}
        override suspend fun replaceDocument(oldUri: String, newUri: Uri): Result<DocumentFile> = Result.failure(UnsupportedOperationException())
    }
}
