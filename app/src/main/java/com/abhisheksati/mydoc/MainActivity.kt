package com.abhisheksati.mydoc

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.abhisheksati.mydoc.designsystem.components.AppSnackbarHost
import com.abhisheksati.mydoc.designsystem.components.LocalGlassEffectMode
import com.abhisheksati.mydoc.designsystem.motion.LocalReducedMotion
import com.abhisheksati.mydoc.designsystem.motion.rememberReducedMotion
import com.abhisheksati.mydoc.data.local.AppThemeMode
import com.abhisheksati.mydoc.designsystem.animation.AppMotion
import com.abhisheksati.mydoc.designsystem.animation.LocalAnimatedVisibilityScope
import com.abhisheksati.mydoc.designsystem.animation.LocalSharedTransitionScope
import com.abhisheksati.mydoc.designsystem.animation.documentHero
import com.abhisheksati.mydoc.designsystem.animation.documentHeroKey
import com.abhisheksati.mydoc.designsystem.preview.DesignSystemPreviewScreen
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.toMediaItem
import com.abhisheksati.mydoc.security.AppLockScreen
import com.abhisheksati.mydoc.security.BiometricLockHelper
import com.abhisheksati.mydoc.tools.ui.ToolRunnerSheet
import com.abhisheksati.mydoc.tools.viewmodel.ToolsViewModel
import com.abhisheksati.mydoc.ui.home.HomeScreen
import com.abhisheksati.mydoc.ui.search.SpotlightSearchSheet
import com.abhisheksati.mydoc.ui.settings.SettingsScreen
import com.abhisheksati.mydoc.ui.viewer.DocumentViewerScreen
import com.abhisheksati.mydoc.update.UpdateManager
import com.abhisheksati.mydoc.util.DocumentSiblings
import com.abhisheksati.mydoc.util.UserMessages
import com.abhisheksati.mydoc.util.shareFile
import com.abhisheksati.mydoc.viewmodel.HomeViewModel
import java.io.File
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import com.abhisheksati.mydoc.designsystem.components.showMessage

@OptIn(ExperimentalSharedTransitionApi::class)
class MainActivity : ComponentActivity() {

    private sealed interface ActiveScreen {
        data object Lock : ActiveScreen
        data object DesignSystemPreview : ActiveScreen
        data object Settings : ActiveScreen
        data object Search : ActiveScreen
        data class Document(val document: DocumentFile) : ActiveScreen
        data class Photo(val item: com.abhisheksati.mydoc.domain.model.MediaItem, val allPhotos: List<com.abhisheksati.mydoc.domain.model.MediaItem>) : ActiveScreen
        data class Video(val item: com.abhisheksati.mydoc.domain.model.MediaItem) : ActiveScreen
        data class Audio(val item: com.abhisheksati.mydoc.domain.model.MediaItem) : ActiveScreen
        data object Home : ActiveScreen
    }

    private fun ActiveScreen.isViewer(): Boolean =
        this is ActiveScreen.Document || this is ActiveScreen.Photo ||
            this is ActiveScreen.Video || this is ActiveScreen.Audio

    // Peer screens slide in from the side rather than carrying a shared element.
    private fun ActiveScreen.isPeer(): Boolean =
        this is ActiveScreen.Settings || this is ActiveScreen.DesignSystemPreview ||
            this is ActiveScreen.Search

    companion object {
        const val ACTION_QUICK_SCAN = "com.abhisheksati.mydoc.action.QUICK_SCAN"
        val pageTurnEvents = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
        val quickScanEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    }

    private val homeViewModel: HomeViewModel by viewModels {
        val app = application as MyDocApp
        HomeViewModel.Factory(app.documentRepository, app.settingsRepository, app.libraryRepository)
    }

    private val toolsViewModel: ToolsViewModel by viewModels {
        ToolsViewModel.Factory(application)
    }

    private val fileManagerViewModel: com.abhisheksati.mydoc.viewmodel.FileManagerViewModel by viewModels {
        val app = application as MyDocApp
        com.abhisheksati.mydoc.viewmodel.FileManagerViewModel.Factory(app.storageRepository, app.settingsRepository)
    }

    private val storageViewModel: com.abhisheksati.mydoc.ui.storage.StorageViewModel by viewModels {
        val app = application as MyDocApp
        com.abhisheksati.mydoc.ui.storage.StorageViewModel.Factory(this, app.storageRepository)
    }

    private lateinit var updateManager: UpdateManager
    private var currentDocument by mutableStateOf<DocumentFile?>(null)
    private var docSiblings by mutableStateOf<List<DocumentFile>>(emptyList())
    private var currentPhoto by mutableStateOf<Pair<com.abhisheksati.mydoc.domain.model.MediaItem, List<com.abhisheksati.mydoc.domain.model.MediaItem>>?>(null)
    private var currentVideo by mutableStateOf<com.abhisheksati.mydoc.domain.model.MediaItem?>(null)
    private var currentAudio by mutableStateOf<com.abhisheksati.mydoc.domain.model.MediaItem?>(null)
    private var isSettingsOpen by mutableStateOf(false)
    private var isSpotlightSearchOpen by mutableStateOf(false)
    private var isDiscoveryRequested by mutableStateOf(false)
    private var isDesignSystemPreviewOpen by mutableStateOf(false)
    private var isAppUnlocked by mutableStateOf(true)

    /**
     * An incoming VIEW/SEND intent, held until the app is unlocked. Handling it immediately read
     * the document and copied it into the offline cache before anyone had authenticated, so the
     * lock protected the screen but not the bytes.
     */
    private var pendingIntent: Intent? = null
    // The launch intent is handled before setContent, so the event flow has no subscriber yet.
    // The flag makes a cold Quick Scan survive until composition can read it.
    private val pendingQuickScan = java.util.concurrent.atomic.AtomicBoolean(false)

    @Volatile
    private var isContentReady = false

    // The splash may be held this long at most; a stalled first frame must not trap the user.
    private val splashDeadlineMs = SystemClock.elapsedRealtime() + 1_200L

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition {
            !isContentReady && SystemClock.elapsedRealtime() < splashDeadlineMs
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // uiMode is not in configChanges, so a dark-mode switch recreates the activity; without
        // this, Settings/Search were dumped back to Home. The lock flag stays fail-closed.
        if (savedInstanceState != null) {
            isSettingsOpen = savedInstanceState.getBoolean("settings_open")
            isSpotlightSearchOpen = savedInstanceState.getBoolean("spotlight_open")
            isDesignSystemPreviewOpen = savedInstanceState.getBoolean("design_preview_open")
        }

        val app = application as MyDocApp
        if (app.settingsRepository.isBiometricLockEnabled.value) {
            isAppUnlocked = false
        }

        updateManager = UpdateManager(this)

        // Silent launch update check (throttled to max once every 24h, respecting user setting)
        if (app.settingsRepository.checkUpdatesOnLaunch.value) {
            lifecycleScope.launch {
                updateManager.checkForUpdates(isLaunchCheck = true)
            }
        }

        handleIncomingIntentWhenUnlocked(intent)

        setContent {
            LaunchedEffect(Unit) { isContentReady = true }

            val themeMode by app.settingsRepository.themeMode.collectAsStateWithLifecycle()
            val amoledBlack by app.settingsRepository.amoledBlack.collectAsStateWithLifecycle()
            val glassEffectMode by app.settingsRepository.glassEffectMode.collectAsStateWithLifecycle()
            val hapticsEnabled by app.settingsRepository.isHapticFeedbackEnabled.collectAsStateWithLifecycle()
            val autoResumeVideo by app.settingsRepository.autoResumeVideo.collectAsStateWithLifecycle()
            val videoAutoLoop by app.settingsRepository.videoAutoLoop.collectAsStateWithLifecycle()
            val isBiometricLockEnabled by app.settingsRepository.isBiometricLockEnabled.collectAsStateWithLifecycle()
            val isDark = when (themeMode) {
                AppThemeMode.BLACK, AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            val isBlack = themeMode == AppThemeMode.BLACK || (isDark && amoledBlack)
            val reducedMotion by rememberReducedMotion()

            // The app can force light/dark independently of the system, so the
            // bars must follow the app theme rather than system night mode.
            LaunchedEffect(isDark) {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }

            val confirmCredentialLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    isAppUnlocked = true
                }
            }

            val attemptUnlock = {
                BiometricLockHelper.promptBiometricAuthentication(
                    activity = this@MainActivity,
                    confirmCredentialLauncher = confirmCredentialLauncher,
                    onSuccess = { isAppUnlocked = true },
                    onError = { errMsg ->
                        this@MainActivity.showMessage(errMsg)
                    }
                )
            }

            LaunchedEffect(isAppUnlocked, isBiometricLockEnabled) {
                if (!isAppUnlocked && isBiometricLockEnabled) {
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                    attemptUnlock()
                } else {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            // Drain whatever the launcher handed us, but only once there is an unlocked session
            // to put it in.
            LaunchedEffect(isAppUnlocked) {
                if (isAppUnlocked) {
                    pendingIntent?.let { incoming ->
                        pendingIntent = null
                        handleIncomingIntent(incoming)
                    }
                }
            }

            AppDesignSystemTheme(
                darkTheme = isDark,
                amoledBlack = isBlack
            ) {
                CompositionLocalProvider(
                    LocalGlassEffectMode provides glassEffectMode,
                    LocalReducedMotion provides reducedMotion,
                    com.abhisheksati.mydoc.designsystem.tokens.LocalHapticsEnabled provides hapticsEnabled
                ) {
                    val selectedTool by toolsViewModel.selectedTool.collectAsStateWithLifecycle()

                // The tile used to post into a replay-zero shared flow whose only subscriber was
                // HomeScreen, so tapping it from the viewer, settings or search dropped the request
                // on the floor. The request is state now, and a pending one resolves to Home.
                var quickScanRequested by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    if (pendingQuickScan.getAndSet(false)) quickScanRequested = true
                    MainActivity.quickScanEvents.collect { quickScanRequested = true }
                }

                val photo = currentPhoto
                val video = currentVideo
                val audio = currentAudio
                val doc = currentDocument
                val activeScreen: ActiveScreen = when {
                    !isAppUnlocked && isBiometricLockEnabled -> ActiveScreen.Lock
                    quickScanRequested -> ActiveScreen.Home
                    isDesignSystemPreviewOpen && BuildConfig.DEBUG -> ActiveScreen.DesignSystemPreview
                    isSettingsOpen -> ActiveScreen.Settings
                    isSpotlightSearchOpen -> ActiveScreen.Search
                    photo != null -> ActiveScreen.Photo(photo.first, photo.second)
                    video != null -> ActiveScreen.Video(video)
                    audio != null -> ActiveScreen.Audio(audio)
                    doc != null -> ActiveScreen.Document(doc)
                    else -> ActiveScreen.Home
                }

                // One back entry point, unwinding in the same order the screens are
                // resolved above. Lock is excluded on purpose: back must still exit.
                val popActiveScreen: () -> Unit = {
                    when {
                        isDesignSystemPreviewOpen -> isDesignSystemPreviewOpen = false
                        isSettingsOpen -> isSettingsOpen = false
                        isSpotlightSearchOpen -> isSpotlightSearchOpen = false
                        selectedTool != null -> toolsViewModel.closeTool()
                        currentPhoto != null -> currentPhoto = null
                        currentVideo != null -> currentVideo = null
                        currentAudio != null -> currentAudio = null
                        currentDocument != null -> currentDocument = null
                    }
                }

                val isRootShowing =
                    activeScreen == ActiveScreen.Home || activeScreen == ActiveScreen.Lock
                BackHandler(enabled = !isRootShowing || selectedTool != null) {
                    popActiveScreen()
                }

                // Shared by the library lists and the search destination: one item,
                // one routing rule, no duplicated open logic.
                val openLibraryItem: (com.abhisheksati.mydoc.domain.model.LibraryItem) -> Unit = { item ->
                    when (item.kind) {
                        com.abhisheksati.mydoc.domain.model.MediaKind.DOCUMENT -> {
                            lifecycleScope.launch {
                                val res = app.documentRepository.registerOpenedDocument(Uri.parse(item.uri))
                                res.onSuccess { doc ->
                                    currentDocument = doc
                                }.onFailure {
                                    currentDocument = DocumentFile(
                                        uri = item.uri,
                                        displayName = item.name,
                                        sizeBytes = item.sizeBytes,
                                        mimeType = item.mimeType,
                                        lastOpenedTimestamp = System.currentTimeMillis()
                                    )
                                }
                            }
                        }
                        com.abhisheksati.mydoc.domain.model.MediaKind.PHOTO -> {
                            val m = item.toMediaItem()
                            val currentHomeState = homeViewModel.uiState.value
                            val allLibraryItems = currentHomeState.libraryItems.ifEmpty { currentHomeState.recentLibraryItems }
                            val allPhotos = allLibraryItems
                                .filter { it.kind == com.abhisheksati.mydoc.domain.model.MediaKind.PHOTO }
                                .map { it.toMediaItem() }
                            currentPhoto = Pair(m, if (allPhotos.any { it.uri == m.uri }) allPhotos else listOf(m))
                        }
                        com.abhisheksati.mydoc.domain.model.MediaKind.VIDEO -> {
                            currentVideo = item.toMediaItem()
                        }
                        com.abhisheksati.mydoc.domain.model.MediaKind.AUDIO -> {
                            currentAudio = item.toMediaItem()
                        }
                    }
                }

                // Sibling list is recomputed on every document change, so every open path —
                // library, recents, browse, intent — gets the same swipe-to-next-document behaviour.
                LaunchedEffect(currentDocument?.uri) {
                    // Enumerated off the main thread and cancellable: reading a folder's file
                    // metadata here used to stall the exact moment a document was opening, and a
                    // scan abandoned by a newer document must not publish stale siblings after it.
                    docSiblings = DocumentSiblings
                        .readableDocumentsIn(currentDocument?.uri?.toPathFile())
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = LocalAppColors.current.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // One shared-element host for the whole app: the row you tapped
                        // becomes the document you are reading.
                        SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
                            val sharedScope = this
                            AnimatedContent(
                                targetState = activeScreen,
                                transitionSpec = {
                                    val targetIsDetail = targetState.isViewer()
                                    val initialIsDetail = initialState.isViewer()
                                    val targetIsPeer = targetState.isPeer()
                                    val initialIsPeer = initialState.isPeer()
                                    val targetIsDocument = targetState is ActiveScreen.Document
                                    val initialIsDocument = initialState is ActiveScreen.Document

                                    when {
                                        // Home list -> document viewer (and back): the hero carries it.
                                        targetIsDocument && initialState is ActiveScreen.Home ->
                                            AppMotion.heroEnter() togetherWith AppMotion.heroExit()
                                        initialIsDocument && targetState is ActiveScreen.Home ->
                                            AppMotion.heroEnter() togetherWith AppMotion.heroExit()
                                        // Opening a document / photo / video / audio.
                                        targetIsDetail -> AppMotion.detailEnter() togetherWith AppMotion.detailExit()
                                        // Coming back from one.
                                        initialIsDetail -> AppMotion.detailReturnEnter() togetherWith AppMotion.detailReturnExit()
                                        // Pushing Settings / Design System to the right.
                                        targetIsPeer -> AppMotion.peerEnter(forward = true) togetherWith AppMotion.peerExit(forward = true)
                                        // Popping back to the left.
                                        initialIsPeer -> AppMotion.peerEnter(forward = false) togetherWith AppMotion.peerExit(forward = false)
                                        else -> AppMotion.neutralEnter() togetherWith AppMotion.neutralExit()
                                    }
                                },
                                label = "screenTransition"
                            ) { screen ->
                                val visibilityScope = this
                                CompositionLocalProvider(
                                    LocalSharedTransitionScope provides sharedScope,
                                    LocalAnimatedVisibilityScope provides visibilityScope
                                ) {
                                    when (screen) {
                                        ActiveScreen.Lock -> {
                                            AppLockScreen(
                                                onUnlockClick = { attemptUnlock() }
                                            )
                                        }
                                        ActiveScreen.DesignSystemPreview -> {
                                            DesignSystemPreviewScreen(
                                                onBackClick = { isDesignSystemPreviewOpen = false }
                                            )
                                        }
                                        ActiveScreen.Settings -> {
                                            SettingsScreen(
                                                updateManager = updateManager,
                                                settingsRepository = app.settingsRepository,
                                                onBackClick = { isSettingsOpen = false },
                                                onDesignSystemPreviewClick = if (BuildConfig.DEBUG) {
                                                    { isDesignSystemPreviewOpen = true }
                                                } else {
                                                    {}
                                                },
                                                onClearRecent = {
                                                    app.libraryRepository.clearAllRecents()
                                                }
                                            )
                                        }
                                        ActiveScreen.Search -> {
                                            // Collected here, not at the activity root, so list
                                            // updates recompose only this destination.
                                            val libraryState by homeViewModel.uiState.collectAsStateWithLifecycle()
                                            SpotlightSearchSheet(
                                                initialContents = isDiscoveryRequested,
                                                items = libraryState.libraryItems.ifEmpty { libraryState.recentLibraryItems },
                                                onItemClick = { item ->
                                                    isSpotlightSearchOpen = false
                                                    openLibraryItem(item)
                                                },
                                                onDismiss = { isSpotlightSearchOpen = false },
                                                onBrowseFolders = { homeViewModel.selectTab(com.abhisheksati.mydoc.domain.model.HomeTab.LIBRARY) }
                                            )
                                        }
                                        is ActiveScreen.Photo -> {
                                            com.abhisheksati.mydoc.ui.media.PhotoViewerScreen(
                                                items = screen.allPhotos.ifEmpty { listOf(screen.item) },
                                                initialIndex = screen.allPhotos.indexOf(screen.item).coerceAtLeast(0),
                                                mediaRepository = app.mediaRepository,
                                                onBackClick = { currentPhoto = null },
                                                onDeletePhoto = { currentPhoto = null }
                                            )
                                        }
                                        is ActiveScreen.Video -> {
                                            com.abhisheksati.mydoc.ui.media.VideoPlayerScreen(
                                                item = screen.item,
                                                mediaRepository = app.mediaRepository,
                                                autoResume = autoResumeVideo,
                                                autoLoop = videoAutoLoop,
                                                onBackClick = { currentVideo = null }
                                            )
                                        }
                                        is ActiveScreen.Audio -> {
                                            com.abhisheksati.mydoc.ui.media.AudioPlayerScreen(
                                                item = screen.item,
                                                onBackClick = { currentAudio = null }
                                            )
                                        }
                                        is ActiveScreen.Document -> {
                                            val activeDoc = screen.document
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                DocumentViewerScreen(
                                                    document = activeDoc,
                                                    onBackClick = { currentDocument = null },
                                                    onShareClick = { shareDocument(activeDoc) },
                                                    onSwipeToNextDocument = { openSiblingDocument(1) },
                                                    onSwipeToPreviousDocument = { openSiblingDocument(-1) },
                                                    onDocumentUpdated = { newDoc ->
                                                        currentDocument = newDoc
                                                    },
                                                    onSelectTool = { tool ->
                                                        toolsViewModel.selectTool(tool, preselectedUri = Uri.parse(activeDoc.uri))
                                                    },
                                                    modifier = Modifier.documentHero(documentHeroKey(activeDoc.uri))
                                                )

                                                if (selectedTool != null) {
                                                    ToolRunnerSheet(
                                                        viewModel = toolsViewModel,
                                                        onOpenResultFile = { file, _ ->
                                                            val uri = Uri.fromFile(file)
                                                            val toolSuffix = selectedTool?.defaultSuffix ?: "result"
                                                            val baseDocName = currentDocument?.displayName?.substringBeforeLast('.')
                                                            lifecycleScope.launch {
                                                                val myApp = application as MyDocApp
                                                                val res = myApp.documentRepository.registerOpenedDocument(uri)
                                                                res.onSuccess { doc ->
                                                                    val friendlyName = if (!baseDocName.isNullOrBlank()) {
                                                                        "${baseDocName}_${toolSuffix}.${file.extension}"
                                                                    } else {
                                                                        doc.displayName
                                                                    }
                                                                    toolsViewModel.closeTool()
                                                                    currentDocument = doc.copy(displayName = friendlyName)
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        ActiveScreen.Home -> {
                                            com.abhisheksati.mydoc.ui.onboarding.FirstRunGuide(onExplore = {
                                                isDiscoveryRequested = true
                                                isSpotlightSearchOpen = true
                                            })
                                            HomeScreen(
                                                quickScanRequested = quickScanRequested,
                                                onQuickScanShown = { quickScanRequested = false },
                                                viewModel = homeViewModel,
                                                toolsViewModel = toolsViewModel,
                                                fileManagerViewModel = fileManagerViewModel,
                                                storageViewModel = storageViewModel,
                                                updateManager = updateManager,
                                                onLibraryItemClick = openLibraryItem,
                                                onDocumentClick = { doc -> currentDocument = doc },
                                                onOpenFile = { file, siblings -> handleOpenFile(file, siblings) },
                                                onSettingsClick = { isSettingsOpen = true },
                                                onSpotlightSearch = { isDiscoveryRequested = false; isSpotlightSearchOpen = true },
                                                onShareDocument = { doc -> shareDocument(doc) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        AppSnackbarHost()
                    }
                }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("settings_open", isSettingsOpen)
        outState.putBoolean("spotlight_open", isSpotlightSearchOpen)
        outState.putBoolean("design_preview_open", isDesignSystemPreviewOpen)
    }

    override fun onStop() {
        super.onStop()
        val app = application as MyDocApp
        if (app.settingsRepository.isBiometricLockEnabled.value) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
            isAppUnlocked = false
        }
    }

    override fun onRestart() {
        super.onRestart()
        val app = application as MyDocApp
        if (app.settingsRepository.isBiometricLockEnabled.value) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
            isAppUnlocked = false
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val app = application as MyDocApp
        if (app.settingsRepository.volumeKeyPageTurning.value && currentDocument != null) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    pageTurnEvents.tryEmit(true)
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    pageTurnEvents.tryEmit(false)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val app = application as MyDocApp
        if (app.settingsRepository.volumeKeyPageTurning.value && currentDocument != null) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntentWhenUnlocked(intent)
    }

    private fun handleIncomingIntentWhenUnlocked(intent: Intent?) {
        if (intent == null) return
        if (isAppUnlocked) handleIncomingIntent(intent) else pendingIntent = intent
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        if (intent.action == ACTION_QUICK_SCAN) {
            pendingQuickScan.set(true)
            quickScanEvents.tryEmit(Unit)
            return
        }

        val uri: Uri? = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
            }
            else -> null
        }

        if (uri != null) {
            // Consume the intent so subsequent launches or crashes do not re-trigger in an infinite loop
            intent.action = null
            intent.data = null
            openUriOrFile(uri = uri, mimeType = intent.type)
        }
    }

    private fun handleOpenFile(file: File, siblingFiles: List<File>? = null) {
        val uri = Uri.fromFile(file)
        openUriOrFile(uri = uri, fileName = file.name, file = file, siblingFiles = siblingFiles)
    }

    /** Steps to the neighbouring document; false when the current one is already at the end of the folder. */
    private fun openSiblingDocument(delta: Int): Boolean {
        val current = currentDocument ?: return false
        val siblings = docSiblings
        if (siblings.size < 2) return false
        // The opened document can carry a content:// or file:// URI while the sibling list is
        // built from File objects, so the two never agree on `uri` — path, then name, is the
        // only identity they share.
        val currentPath = runCatching { Uri.parse(current.uri).path }.getOrNull()
        val index = siblings.indexOfFirst { sib ->
            val sibPath = runCatching { Uri.parse(sib.uri).path }.getOrNull()
            (currentPath != null && sibPath == currentPath) || sib.displayName == current.displayName
        }
        if (index < 0) return false
        val next = siblings.getOrNull(index + delta) ?: return false
        currentDocument = next
        return true
    }

    /** A real on-disk path for a URI string, or null for content:// URIs, which have no sibling folder to walk. */
    private fun String.toPathFile(): File? =
        runCatching { Uri.parse(this).path?.let(::File) }.getOrNull()?.takeIf { it.isFile }

    private fun openUriOrFile(
        uri: Uri,
        mimeType: String? = null,
        fileName: String? = null,
        file: File? = null,
        siblingFiles: List<File>? = null
    ) {
        val detectedMime = mimeType ?: contentResolver.getType(uri) ?: when {
            fileName?.endsWith(".jpg", true) == true || fileName?.endsWith(".jpeg", true) == true -> "image/jpeg"
            fileName?.endsWith(".png", true) == true -> "image/png"
            fileName?.endsWith(".webp", true) == true -> "image/webp"
            fileName?.endsWith(".gif", true) == true -> "image/gif"
            fileName?.endsWith(".mp4", true) == true -> "video/mp4"
            fileName?.endsWith(".mkv", true) == true -> "video/x-matroska"
            fileName?.endsWith(".webm", true) == true -> "video/webm"
            fileName?.endsWith(".mp3", true) == true -> "audio/mpeg"
            fileName?.endsWith(".m4a", true) == true -> "audio/mp4"
            fileName?.endsWith(".aac", true) == true -> "audio/aac"
            fileName?.endsWith(".flac", true) == true -> "audio/flac"
            fileName?.endsWith(".wav", true) == true -> "audio/wav"
            else -> "*/*"
        }

        val displayName = fileName ?: uri.lastPathSegment ?: "Media"

        when {
            detectedMime.startsWith("image/") -> {
                val mediaItem = com.abhisheksati.mydoc.domain.model.MediaItem(
                    id = uri.hashCode().toLong(),
                    uri = uri,
                    displayName = displayName,
                    sizeBytes = file?.length() ?: 0L,
                    mimeType = detectedMime,
                    dateAddedSec = System.currentTimeMillis() / 1000L,
                    filePath = file?.absolutePath ?: uri.path
                )

                val imageExts = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif")
                val photos: List<com.abhisheksati.mydoc.domain.model.MediaItem> = when {
                    !siblingFiles.isNullOrEmpty() -> {
                        siblingFiles.map { f ->
                            val fMime = when (f.extension.lowercase()) {
                                "png" -> "image/png"
                                "webp" -> "image/webp"
                                "gif" -> "image/gif"
                                else -> "image/jpeg"
                            }
                            com.abhisheksati.mydoc.domain.model.MediaItem(
                                id = f.absolutePath.hashCode().toLong(),
                                uri = Uri.fromFile(f),
                                displayName = f.name,
                                sizeBytes = f.length(),
                                mimeType = fMime,
                                dateAddedSec = f.lastModified() / 1000L,
                                filePath = f.absolutePath
                            )
                        }
                    }
                    file != null && file.parentFile?.isDirectory == true -> {
                        val parent = file.parentFile
                        val siblings = parent?.listFiles { f ->
                            f.isFile && !f.name.startsWith(".") && f.extension.lowercase() in imageExts
                        }?.sortedBy { it.name.lowercase() }
                        siblings?.map { f ->
                            val fMime = when (f.extension.lowercase()) {
                                "png" -> "image/png"
                                "webp" -> "image/webp"
                                "gif" -> "image/gif"
                                else -> "image/jpeg"
                            }
                            com.abhisheksati.mydoc.domain.model.MediaItem(
                                id = f.absolutePath.hashCode().toLong(),
                                uri = Uri.fromFile(f),
                                displayName = f.name,
                                sizeBytes = f.length(),
                                mimeType = fMime,
                                dateAddedSec = f.lastModified() / 1000L,
                                filePath = f.absolutePath
                            )
                        } ?: listOf(mediaItem)
                    }
                    else -> listOf(mediaItem)
                }

                currentPhoto = Pair(mediaItem, if (photos.any { it.uri == mediaItem.uri }) photos else listOf(mediaItem))
            }
            detectedMime.startsWith("video/") -> {
                val mediaItem = com.abhisheksati.mydoc.domain.model.MediaItem(
                    id = uri.hashCode().toLong(),
                    uri = uri,
                    displayName = displayName,
                    sizeBytes = file?.length() ?: 0L,
                    mimeType = detectedMime,
                    dateAddedSec = System.currentTimeMillis() / 1000L,
                    isVideo = true,
                    filePath = file?.absolutePath ?: uri.path
                )
                currentVideo = mediaItem
            }
            detectedMime.startsWith("audio/") -> {
                val mediaItem = com.abhisheksati.mydoc.domain.model.MediaItem(
                    id = uri.hashCode().toLong(),
                    uri = uri,
                    displayName = displayName,
                    sizeBytes = file?.length() ?: 0L,
                    mimeType = detectedMime,
                    dateAddedSec = System.currentTimeMillis() / 1000L,
                    isAudio = true,
                    filePath = file?.absolutePath ?: uri.path
                )
                currentAudio = mediaItem
            }
            else -> {
                lifecycleScope.launch {
                    try {
                        val app = application as MyDocApp
                        val result = app.documentRepository.registerOpenedDocument(uri)
                        result.onSuccess { doc ->
                            currentDocument = doc
                        }.onFailure { e ->
                            if (file != null) {
                                try {
                                    val sharedDir = File(cacheDir, "shared").apply { if (!exists()) mkdirs() }
                                    val shareFile = if (file.parentFile?.absolutePath == sharedDir.absolutePath) {
                                        file
                                    } else {
                                        val dest = File(sharedDir, file.name)
                                        file.copyTo(dest, overwrite = true)
                                        dest
                                    }
                                    val shareUri = FileProvider.getUriForFile(this@MainActivity, "${packageName}.fileprovider", shareFile)
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(shareUri, "*/*")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    startActivity(Intent.createChooser(intent, "Open with"))
                                } catch (_: Exception) {
                                    this@MainActivity.showMessage(UserMessages.readFailure("MainActivity", e).resolve(this@MainActivity))
                                }
                            } else {
                                this@MainActivity.showMessage(UserMessages.readFailure("MainActivity", e).resolve(this@MainActivity))
                            }
                        }
                    } catch (e: Exception) {
                        this@MainActivity.showMessage(UserMessages.readFailure("MainActivity", e).resolve(this@MainActivity))
                    }
                }
            }
        }
    }

    private fun shareDocument(document: DocumentFile) {
        if (!shareFile(this, Uri.parse(document.uri), document.mimeType, "Share ${document.displayName}")) {
            showMessage("No app available to share this file")
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_MODERATE) {
            com.abhisheksati.mydoc.ui.viewer.controls.ZoomTileCache.clear()
        }
        com.abhisheksati.mydoc.rendering.cache.TileBitmapCache.trimAll(level)
    }
}
