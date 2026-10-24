package com.abhisheksati.mydoc

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.abhisheksati.mydoc.data.local.AppThemeMode
import com.abhisheksati.mydoc.designsystem.preview.DesignSystemPreviewScreen
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.toMediaItem
import com.abhisheksati.mydoc.security.AppLockScreen
import com.abhisheksati.mydoc.security.BiometricLockHelper
import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.tools.ui.ToolRunnerSheet
import com.abhisheksati.mydoc.tools.viewmodel.ToolsViewModel
import com.abhisheksati.mydoc.ui.home.HomeScreen
import com.abhisheksati.mydoc.ui.settings.SettingsScreen
import com.abhisheksati.mydoc.ui.viewer.DocumentViewerScreen
import com.abhisheksati.mydoc.update.UpdateManager
import com.abhisheksati.mydoc.viewmodel.HomeViewModel
import java.io.File
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private sealed interface ActiveScreen {
        data object Lock : ActiveScreen
        data object DesignSystemPreview : ActiveScreen
        data object Settings : ActiveScreen
        data class Document(val document: DocumentFile) : ActiveScreen
        data class Photo(val item: com.abhisheksati.mydoc.domain.model.MediaItem, val allPhotos: List<com.abhisheksati.mydoc.domain.model.MediaItem>) : ActiveScreen
        data class Video(val item: com.abhisheksati.mydoc.domain.model.MediaItem) : ActiveScreen
        data class Audio(val item: com.abhisheksati.mydoc.domain.model.MediaItem) : ActiveScreen
        data object Home : ActiveScreen
    }

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
        com.abhisheksati.mydoc.viewmodel.FileManagerViewModel.Factory(app.storageRepository)
    }

    private val storageViewModel: com.abhisheksati.mydoc.ui.storage.StorageViewModel by viewModels {
        val app = application as MyDocApp
        com.abhisheksati.mydoc.ui.storage.StorageViewModel.Factory(this, app.storageRepository)
    }

    private lateinit var updateManager: UpdateManager
    private var currentDocument by mutableStateOf<DocumentFile?>(null)
    private var currentPhoto by mutableStateOf<Pair<com.abhisheksati.mydoc.domain.model.MediaItem, List<com.abhisheksati.mydoc.domain.model.MediaItem>>?>(null)
    private var currentVideo by mutableStateOf<com.abhisheksati.mydoc.domain.model.MediaItem?>(null)
    private var currentAudio by mutableStateOf<com.abhisheksati.mydoc.domain.model.MediaItem?>(null)
    private var isSettingsOpen by mutableStateOf(false)
    private var isDesignSystemPreviewOpen by mutableStateOf(false)
    private var isAppUnlocked by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as MyDocApp
        if (app.settingsRepository.isBiometricLockEnabled.value) {
            isAppUnlocked = false
        }

        updateManager = UpdateManager(this)

        // Silent launch update check (throttled to max once every 24h)
        lifecycleScope.launch {
            updateManager.checkForUpdates(isLaunchCheck = true)
        }

        handleIncomingIntent(intent)

        setContent {
            val themeMode by app.settingsRepository.themeMode.collectAsStateWithLifecycle()
            val amoledBlack by app.settingsRepository.amoledBlack.collectAsStateWithLifecycle()
            val isBiometricLockEnabled by app.settingsRepository.isBiometricLockEnabled.collectAsStateWithLifecycle()
            val isDark = when (themeMode) {
                AppThemeMode.BLACK, AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            val isBlack = themeMode == AppThemeMode.BLACK || (isDark && amoledBlack)

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
                        Toast.makeText(this@MainActivity, errMsg, Toast.LENGTH_SHORT).show()
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

            AppDesignSystemTheme(
                darkTheme = isDark,
                dynamicColor = false,
                amoledBlack = isBlack
            ) {
                val selectedTool by toolsViewModel.selectedTool.collectAsStateWithLifecycle()

                // Intercept back presses smoothly so the app never exits abruptly
                BackHandler(enabled = isDesignSystemPreviewOpen) {
                    isDesignSystemPreviewOpen = false
                }

                BackHandler(enabled = !isDesignSystemPreviewOpen && isSettingsOpen) {
                    isSettingsOpen = false
                }

                BackHandler(enabled = !isDesignSystemPreviewOpen && !isSettingsOpen && selectedTool != null) {
                    toolsViewModel.closeTool()
                }

                BackHandler(enabled = !isDesignSystemPreviewOpen && !isSettingsOpen && currentPhoto != null) {
                    currentPhoto = null
                }

                BackHandler(enabled = !isDesignSystemPreviewOpen && !isSettingsOpen && currentVideo != null) {
                    currentVideo = null
                }

                BackHandler(enabled = !isDesignSystemPreviewOpen && !isSettingsOpen && currentAudio != null) {
                    currentAudio = null
                }

                BackHandler(enabled = !isDesignSystemPreviewOpen && !isSettingsOpen && selectedTool == null && currentDocument != null) {
                    currentDocument = null
                }

                val activeScreen: ActiveScreen = when {
                    !isAppUnlocked && isBiometricLockEnabled -> ActiveScreen.Lock
                    isDesignSystemPreviewOpen -> ActiveScreen.DesignSystemPreview
                    isSettingsOpen -> ActiveScreen.Settings
                    currentPhoto != null -> ActiveScreen.Photo(currentPhoto!!.first, currentPhoto!!.second)
                    currentVideo != null -> ActiveScreen.Video(currentVideo!!)
                    currentAudio != null -> ActiveScreen.Audio(currentAudio!!)
                    currentDocument != null -> ActiveScreen.Document(currentDocument!!)
                    else -> ActiveScreen.Home
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = LocalAppColors.current.background
                ) {
                    AnimatedContent(
                        targetState = activeScreen,
                        transitionSpec = {
                            val enterSpring = spring<Float>(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy)
                            val exitSpring = spring<Float>(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
                            val enterOffsetSpring = spring<IntOffset>(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy)
                            val exitOffsetSpring = spring<IntOffset>(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)

                            when {
                                targetState is ActiveScreen.Document || targetState is ActiveScreen.Photo || targetState is ActiveScreen.Video || targetState is ActiveScreen.Audio -> {
                                    (slideInVertically(animationSpec = enterOffsetSpring) { it / 8 } +
                                     scaleIn(initialScale = 0.94f, animationSpec = enterSpring) +
                                     fadeIn(animationSpec = tween(220)))
                                    .togetherWith(
                                     fadeOut(animationSpec = tween(160)) +
                                     scaleOut(targetScale = 0.96f, animationSpec = exitSpring)
                                    )
                                }
                                initialState is ActiveScreen.Document || initialState is ActiveScreen.Photo || initialState is ActiveScreen.Video || initialState is ActiveScreen.Audio -> {
                                    (fadeIn(animationSpec = tween(200)) +
                                     scaleIn(initialScale = 0.96f, animationSpec = enterSpring))
                                    .togetherWith(
                                     slideOutVertically(animationSpec = exitOffsetSpring) { it / 8 } +
                                     scaleOut(targetScale = 0.94f, animationSpec = exitSpring) +
                                     fadeOut(animationSpec = tween(180))
                                    )
                                }
                                targetState is ActiveScreen.Settings || targetState is ActiveScreen.DesignSystemPreview -> {
                                    (slideInHorizontally(animationSpec = enterOffsetSpring) { it / 4 } +
                                     fadeIn(animationSpec = tween(220)))
                                    .togetherWith(
                                     slideOutHorizontally(animationSpec = exitOffsetSpring) { -it / 6 } +
                                     fadeOut(animationSpec = tween(180))
                                    )
                                }
                                initialState is ActiveScreen.Settings || initialState is ActiveScreen.DesignSystemPreview -> {
                                    (slideInHorizontally(animationSpec = enterOffsetSpring) { -it / 6 } +
                                     fadeIn(animationSpec = tween(220)))
                                    .togetherWith(
                                     slideOutHorizontally(animationSpec = exitOffsetSpring) { it / 4 } +
                                     fadeOut(animationSpec = tween(180))
                                    )
                                }
                                else -> {
                                    (fadeIn(animationSpec = tween(240)) + scaleIn(initialScale = 0.97f, animationSpec = enterSpring))
                                    .togetherWith(fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 1.02f, animationSpec = exitSpring))
                                }
                            }
                        },
                        label = "screenTransition"
                    ) { screen ->
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
                                    onDesignSystemPreviewClick = { isDesignSystemPreviewOpen = true },
                                    onClearRecent = {
                                        lifecycleScope.launch {
                                            app.libraryRepository.clearAllRecents()
                                        }
                                    }
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
                                    onBackClick = { currentVideo = null }
                                )
                            }
                            is ActiveScreen.Audio -> {
                                com.abhisheksati.mydoc.ui.media.AudioPlayerScreen(
                                    item = screen.item,
                                    onBackClick = { currentAudio = null },
                                    isDarkTheme = isDark
                                )
                            }
                            is ActiveScreen.Document -> {
                                val activeDoc = screen.document
                                Box(modifier = Modifier.fillMaxSize()) {
                                    DocumentViewerScreen(
                                        document = activeDoc,
                                        onBackClick = { currentDocument = null },
                                        onShareClick = { shareDocument(activeDoc) },
                                        onDocumentUpdated = { newDoc ->
                                            currentDocument = newDoc
                                        },
                                        onSelectTool = { tool ->
                                            toolsViewModel.selectTool(tool, preselectedUri = Uri.parse(activeDoc.uri))
                                        }
                                    )

                                    if (selectedTool != null) {
                                        ToolRunnerSheet(
                                            viewModel = toolsViewModel,
                                            onOpenResultFile = { file, _ ->
                                                val uri = Uri.fromFile(file)
                                                lifecycleScope.launch {
                                                    val myApp = application as MyDocApp
                                                    val res = myApp.documentRepository.registerOpenedDocument(uri)
                                                    res.onSuccess { doc ->
                                                        toolsViewModel.closeTool()
                                                        currentDocument = doc
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            ActiveScreen.Home -> {
                                HomeScreen(
                                    viewModel = homeViewModel,
                                    toolsViewModel = toolsViewModel,
                                    fileManagerViewModel = fileManagerViewModel,
                                    storageViewModel = storageViewModel,
                                    updateManager = updateManager,
                                    isDarkTheme = isDark,
                                    onToggleTheme = {
                                        val nextTheme = if (isDark) AppThemeMode.LIGHT else AppThemeMode.DARK
                                        app.settingsRepository.setThemeMode(nextTheme)
                                    },
                                    onLibraryItemClick = { item ->
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
                                                currentPhoto = Pair(m, listOf(m))
                                            }
                                            com.abhisheksati.mydoc.domain.model.MediaKind.VIDEO -> {
                                                currentVideo = item.toMediaItem()
                                            }
                                            com.abhisheksati.mydoc.domain.model.MediaKind.AUDIO -> {
                                                currentAudio = item.toMediaItem()
                                            }
                                        }
                                    },
                                    onDocumentClick = { doc -> currentDocument = doc },
                                    onOpenFile = { file -> handleOpenFile(file) },
                                    onSettingsClick = { isSettingsOpen = true },
                                    onShareDocument = { doc -> shareDocument(doc) }
                                )
                            }
                        }
                    }
                }
            }
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
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        if (intent.action == ACTION_QUICK_SCAN) {
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

    private fun handleOpenFile(file: File) {
        val uri = Uri.fromFile(file)
        openUriOrFile(uri = uri, fileName = file.name, file = file)
    }

    private fun openUriOrFile(uri: Uri, mimeType: String? = null, fileName: String? = null, file: File? = null) {
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
                currentPhoto = Pair(mediaItem, listOf(mediaItem))
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
                                    val shareUri = FileProvider.getUriForFile(this@MainActivity, "${packageName}.fileprovider", file)
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(shareUri, "*/*")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    startActivity(Intent.createChooser(intent, "Open with"))
                                } catch (_: Exception) {
                                    Toast.makeText(this@MainActivity, "Could not open file: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(this@MainActivity, "Could not open file: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Unable to access file: ${e.localizedMessage ?: "Permission denied"}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun shareDocument(document: DocumentFile) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = document.mimeType
            putExtra(Intent.EXTRA_STREAM, Uri.parse(document.uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share ${document.displayName}"))
    }
}
