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
import androidx.compose.ui.unit.IntOffset

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.abhisheksati.mydoc.data.local.AppThemeMode
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.security.AppLockScreen
import com.abhisheksati.mydoc.security.BiometricLockHelper
import com.abhisheksati.mydoc.designsystem.preview.DesignSystemPreviewScreen
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.tools.ui.ToolRunnerSheet
import com.abhisheksati.mydoc.tools.viewmodel.ToolsViewModel
import com.abhisheksati.mydoc.ui.home.HomeScreen
import com.abhisheksati.mydoc.ui.settings.SettingsScreen
import com.abhisheksati.mydoc.domain.model.MediaItem
import com.abhisheksati.mydoc.ui.media.PhotoViewerScreen
import com.abhisheksati.mydoc.ui.media.VideoPlayerScreen
import com.abhisheksati.mydoc.ui.viewer.DocumentViewerScreen
import com.abhisheksati.mydoc.update.UpdateManager
import com.abhisheksati.mydoc.viewmodel.FileManagerViewModel
import com.abhisheksati.mydoc.viewmodel.HomeViewModel
import com.abhisheksati.mydoc.viewmodel.MediaViewModel
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private sealed interface ActiveScreen {
        data object Lock : ActiveScreen
        data object DesignSystemPreview : ActiveScreen
        data object Settings : ActiveScreen
        data class Video(val item: MediaItem) : ActiveScreen
        data class Photo(val item: MediaItem, val list: List<MediaItem>) : ActiveScreen
        data class Document(val document: DocumentFile) : ActiveScreen
        data object Home : ActiveScreen
    }

    companion object {
        const val ACTION_QUICK_SCAN = "com.abhisheksati.mydoc.action.QUICK_SCAN"
        val pageTurnEvents = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
        val quickScanEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    }

    private val homeViewModel: HomeViewModel by viewModels {
        val app = application as MyDocApp
        HomeViewModel.Factory(app.documentRepository, app.settingsRepository)
    }

    private val toolsViewModel: ToolsViewModel by viewModels {
        ToolsViewModel.Factory(application)
    }

    private val mediaViewModel: MediaViewModel by viewModels {
        val app = application as MyDocApp
        MediaViewModel.Factory(app.mediaRepository)
    }

    private val fileManagerViewModel: FileManagerViewModel by viewModels {
        val app = application as MyDocApp
        FileManagerViewModel.Factory(app.storageRepository)
    }

    private lateinit var updateManager: UpdateManager
    private var currentDocument by mutableStateOf<DocumentFile?>(null)
    private var activeMediaItem by mutableStateOf<MediaItem?>(null)
    private var activeMediaList by mutableStateOf<List<MediaItem>>(emptyList())
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

        if (savedInstanceState != null) {
            val mediaUriStr = savedInstanceState.getString("saved_active_media_uri")
            val mediaDisplayName = savedInstanceState.getString("saved_active_media_name") ?: "Video"
            val mediaIsVideo = savedInstanceState.getBoolean("saved_active_media_is_video", true)
            val mediaDuration = savedInstanceState.getLong("saved_active_media_duration", 0L)
            if (mediaUriStr != null) {
                val uri = Uri.parse(mediaUriStr)
                val restoredItem = MediaItem(
                    id = uri.hashCode().toLong(),
                    uri = uri,
                    displayName = mediaDisplayName,
                    sizeBytes = 0L,
                    mimeType = if (mediaIsVideo) "video/*" else "image/*",
                    dateAddedSec = System.currentTimeMillis() / 1000,
                    durationMs = mediaDuration,
                    isVideo = mediaIsVideo
                )
                activeMediaItem = restoredItem
                activeMediaList = listOf(restoredItem)

                // Refresh complete details from Room repository in background
                lifecycleScope.launch {
                    val mediaItem = app.mediaRepository.getMediaItem(uri)
                    if (mediaItem != null) {
                        activeMediaItem = mediaItem
                        activeMediaList = listOf(mediaItem)
                    }
                }
            }
        }

        setContent {
            val themeMode by app.settingsRepository.themeMode.collectAsStateWithLifecycle()
            val isBiometricLockEnabled by app.settingsRepository.isBiometricLockEnabled.collectAsStateWithLifecycle()
            val isDark = when (themeMode) {
                AppThemeMode.AMOLED_DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
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

            AppDesignSystemTheme(darkTheme = isDark) {
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

                BackHandler(enabled = !isDesignSystemPreviewOpen && !isSettingsOpen && selectedTool == null && activeMediaItem != null) {
                    requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    activeMediaItem = null
                    activeMediaList = emptyList()
                }

                BackHandler(enabled = !isDesignSystemPreviewOpen && !isSettingsOpen && selectedTool == null && activeMediaItem == null && currentDocument != null) {
                    currentDocument = null
                }

                val activeScreen: ActiveScreen = when {
                    !isAppUnlocked && isBiometricLockEnabled -> ActiveScreen.Lock
                    isDesignSystemPreviewOpen -> ActiveScreen.DesignSystemPreview
                    isSettingsOpen -> ActiveScreen.Settings
                    activeMediaItem != null -> {
                        val media = activeMediaItem!!
                        if (media.isVideo) ActiveScreen.Video(media)
                        else ActiveScreen.Photo(media, activeMediaList)
                    }
                    currentDocument != null -> ActiveScreen.Document(currentDocument!!)
                    else -> ActiveScreen.Home
                }

                LaunchedEffect(activeScreen) {
                    if (activeScreen is ActiveScreen.Home) {
                        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
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
                                targetState is ActiveScreen.Document || targetState is ActiveScreen.Photo || targetState is ActiveScreen.Video -> {
                                    (slideInVertically(animationSpec = enterOffsetSpring) { it / 8 } +
                                     scaleIn(initialScale = 0.94f, animationSpec = enterSpring) +
                                     fadeIn(animationSpec = tween(220)))
                                    .togetherWith(
                                     fadeOut(animationSpec = tween(160)) +
                                     scaleOut(targetScale = 0.96f, animationSpec = exitSpring)
                                    )
                                }
                                initialState is ActiveScreen.Document || initialState is ActiveScreen.Photo || initialState is ActiveScreen.Video -> {
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
                                    onDesignSystemPreviewClick = { isDesignSystemPreviewOpen = true }
                                )
                            }
                            is ActiveScreen.Video -> {
                                VideoPlayerScreen(
                                    item = screen.item,
                                    mediaRepository = app.mediaRepository,
                                    onBackClick = {
                                        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                        activeMediaItem = null
                                        activeMediaList = emptyList()
                                    }
                                )
                            }
                            is ActiveScreen.Photo -> {
                                val initialIdx = screen.list.indexOfFirst { it.uri == screen.item.uri }.coerceAtLeast(0)
                                PhotoViewerScreen(
                                    items = if (screen.list.isNotEmpty()) screen.list else listOf(screen.item),
                                    initialIndex = initialIdx,
                                    mediaRepository = app.mediaRepository,
                                    onDeletePhoto = { deletedItem ->
                                        activeMediaList = activeMediaList.filter { it.uri != deletedItem.uri }
                                        if (activeMediaList.isEmpty()) {
                                            activeMediaItem = null
                                        }
                                    },
                                    onBackClick = {
                                        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                        activeMediaItem = null
                                        activeMediaList = emptyList()
                                    }
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
                                        onToolsClick = {
                                            toolsViewModel.selectTool(PdfTool.COMPRESS, preselectedUri = Uri.parse(activeDoc.uri))
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
                                    mediaViewModel = mediaViewModel,
                                    fileManagerViewModel = fileManagerViewModel,
                                    isDarkTheme = isDark,
                                    onToggleTheme = {
                                        val nextTheme = if (isDark) AppThemeMode.LIGHT else AppThemeMode.AMOLED_DARK
                                        app.settingsRepository.setThemeMode(nextTheme)
                                    },
                                    onDocumentClick = { doc -> currentDocument = doc },
                                    onOpenFile = { file -> handleOpenFile(file) },
                                    onOpenMedia = { item, list ->
                                        activeMediaItem = item
                                        activeMediaList = list
                                    },
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
            lifecycleScope.launch {
                try {
                    val app = application as MyDocApp
                    val mimeType = try {
                        contentResolver.getType(uri) ?: intent.type ?: ""
                    } catch (_: SecurityException) {
                        intent.type ?: ""
                    }
                    val path = uri.path?.lowercase(Locale.ROOT) ?: ""
                    val isImage = mimeType.startsWith("image/") ||
                            path.endsWith(".jpg") || path.endsWith(".jpeg") ||
                            path.endsWith(".png") || path.endsWith(".webp") ||
                            path.endsWith(".gif") || path.endsWith(".bmp") ||
                            path.endsWith(".heic") || path.endsWith(".avif")
                    val isVideo = mimeType.startsWith("video/") || mimeType.startsWith("audio/") ||
                            path.endsWith(".mp4") || path.endsWith(".mkv") ||
                            path.endsWith(".webm") || path.endsWith(".3gp") ||
                            path.endsWith(".mov") || path.endsWith(".mp3") ||
                            path.endsWith(".wav") || path.endsWith(".aac")

                    if (isImage || isVideo) {
                        val mediaItem = try {
                            app.mediaRepository.getMediaItem(uri)
                        } catch (_: Exception) { null }
                        if (mediaItem != null) {
                            activeMediaItem = mediaItem
                            activeMediaList = listOf(mediaItem)
                            return@launch
                        }
                    }

                    val result = app.documentRepository.registerOpenedDocument(uri)
                    result.onSuccess { doc ->
                        currentDocument = doc
                    }.onFailure { e ->
                        Toast.makeText(
                            this@MainActivity,
                            "Could not open file: ${e.localizedMessage ?: "Unknown error"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MainActivity,
                        "Unable to access file: ${e.localizedMessage ?: "Permission denied"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun handleOpenFile(file: File) {
        val name = file.name.lowercase(Locale.ROOT)
        val app = application as MyDocApp

        // 1. Media (Images, Videos, and Audio)
        val isImage = name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
                name.endsWith(".webp") || name.endsWith(".gif") || name.endsWith(".bmp") || name.endsWith(".heic")
        val isVideo = name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".webm") ||
                name.endsWith(".avi") || name.endsWith(".mov") || name.endsWith(".3gp")
        val isAudio = name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".aac") ||
                name.endsWith(".flac") || name.endsWith(".m4a") || name.endsWith(".ogg") || name.endsWith(".wma")

        if (isImage || isVideo || isAudio) {
            val uri = Uri.fromFile(file)
            val mediaItem = MediaItem(
                id = file.absolutePath.hashCode().toLong(),
                uri = uri,
                displayName = file.name,
                sizeBytes = file.length(),
                mimeType = if (isImage) "image/*" else if (isAudio) "audio/*" else "video/*",
                dateAddedSec = file.lastModified() / 1000L,
                isVideo = isVideo || isAudio
            )
            activeMediaItem = mediaItem
            activeMediaList = listOf(mediaItem)
            return
        }

        // 2. APK Files -> Install via PackageInstaller
        if (name.endsWith(".apk")) {
            try {
                val apkUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Unable to install APK: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // 3. Document or Code/Text
        val isDocument = name.endsWith(".pdf") || name.endsWith(".epub") || name.endsWith(".xps") ||
                name.endsWith(".oxps") || name.endsWith(".cbz") ||
                name.endsWith(".docx") || name.endsWith(".doc") ||
                name.endsWith(".xlsx") || name.endsWith(".xls") || name.endsWith(".csv") ||
                name.endsWith(".pptx") || name.endsWith(".ppt") || name.endsWith(".txt") ||
                name.endsWith(".md") || name.endsWith(".json") || name.endsWith(".xml") ||
                name.endsWith(".log") || name.endsWith(".kt") || name.endsWith(".java") ||
                name.endsWith(".py") || name.endsWith(".html") || name.endsWith(".css") ||
                name.endsWith(".js") || name.endsWith(".ts") || name.endsWith(".c") ||
                name.endsWith(".cpp") || name.endsWith(".h") || name.endsWith(".sh") ||
                name.endsWith(".sql") || name.endsWith(".yaml") || name.endsWith(".yml") ||
                name.endsWith(".ini") || name.endsWith(".conf") || name.endsWith(".gradle") ||
                name.endsWith(".properties") || name.endsWith(".bat")

        if (isDocument) {
            val uri = Uri.fromFile(file)
            lifecycleScope.launch {
                val res = app.documentRepository.registerOpenedDocument(uri)
                res.onSuccess { doc ->
                    currentDocument = doc
                }.onFailure { err ->
                    Toast.makeText(this@MainActivity, "Could not open: ${err.message}", Toast.LENGTH_SHORT).show()
                }
            }
            return
        }

        // 4. Fallback: Generic Android open chooser
        try {
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            Toast.makeText(this, "No app available to open this file", Toast.LENGTH_SHORT).show()
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

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (activeMediaItem?.isVideo == true) {
                try {
                    val params = android.app.PictureInPictureParams.Builder()
                        .setAspectRatio(android.util.Rational(16, 9))
                        .build()
                    enterPictureInPictureMode(params)
                } catch (_: Exception) {}
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        activeMediaItem?.let { item ->
            outState.putString("saved_active_media_uri", item.uri.toString())
            outState.putString("saved_active_media_name", item.displayName)
            outState.putBoolean("saved_active_media_is_video", item.isVideo)
            outState.putLong("saved_active_media_duration", item.durationMs)
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (!isInPictureInPictureMode && activeMediaItem == null) {
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    override fun onStop() {
        super.onStop()
        // Reset orientation when exiting to Home or switching apps so resuming app doesn't stay landscape locked
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode) {
            // Keep PiP orientation
        } else if (activeMediaItem == null) {
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}
