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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.abhisheksati.mydoc.viewmodel.HomeViewModel
import com.abhisheksati.mydoc.viewmodel.MediaViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        val pageTurnEvents = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
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

        setContent {
            val themeMode by app.settingsRepository.themeMode.collectAsState()
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

            LaunchedEffect(isAppUnlocked) {
                if (!isAppUnlocked && app.settingsRepository.isBiometricLockEnabled.value) {
                    attemptUnlock()
                }
            }

            AppDesignSystemTheme(darkTheme = isDark) {
                val selectedTool by toolsViewModel.selectedTool.collectAsState()

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
                    activeMediaItem = null
                    activeMediaList = emptyList()
                }

                BackHandler(enabled = !isDesignSystemPreviewOpen && !isSettingsOpen && selectedTool == null && activeMediaItem == null && currentDocument != null) {
                    currentDocument = null
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (!isAppUnlocked && app.settingsRepository.isBiometricLockEnabled.value) {
                        AppLockScreen(
                            onUnlockClick = { attemptUnlock() }
                        )
                    } else if (isDesignSystemPreviewOpen) {
                        DesignSystemPreviewScreen(
                            onBackClick = { isDesignSystemPreviewOpen = false }
                        )
                    } else if (isSettingsOpen) {
                        SettingsScreen(
                            updateManager = updateManager,
                            settingsRepository = app.settingsRepository,
                            onBackClick = { isSettingsOpen = false },
                            onDesignSystemPreviewClick = { isDesignSystemPreviewOpen = true }
                        )
                    } else if (activeMediaItem != null) {
                        val media = activeMediaItem!!
                        if (media.isVideo) {
                            VideoPlayerScreen(
                                item = media,
                                mediaRepository = app.mediaRepository,
                                onBackClick = {
                                    activeMediaItem = null
                                    activeMediaList = emptyList()
                                }
                            )
                        } else {
                            val initialIdx = activeMediaList.indexOfFirst { it.uri == media.uri }.coerceAtLeast(0)
                            PhotoViewerScreen(
                                items = if (activeMediaList.isNotEmpty()) activeMediaList else listOf(media),
                                initialIndex = initialIdx,
                                mediaRepository = app.mediaRepository,
                                onBackClick = {
                                    activeMediaItem = null
                                    activeMediaList = emptyList()
                                }
                            )
                        }
                    } else {
                        val activeDoc = currentDocument
                        if (activeDoc != null) {
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
                                                val app = application as MyDocApp
                                                val res = app.documentRepository.registerOpenedDocument(uri)
                                                res.onSuccess { doc ->
                                                    toolsViewModel.closeTool()
                                                    currentDocument = doc
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        } else {
                            HomeScreen(
                                viewModel = homeViewModel,
                                toolsViewModel = toolsViewModel,
                                mediaViewModel = mediaViewModel,
                                isDarkTheme = isDark,
                                onToggleTheme = {
                                    val nextTheme = if (isDark) AppThemeMode.LIGHT else AppThemeMode.AMOLED_DARK
                                    app.settingsRepository.setThemeMode(nextTheme)
                                },
                                onDocumentClick = { doc -> currentDocument = doc },
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

    override fun onRestart() {
        super.onRestart()
        val app = application as MyDocApp
        if (app.settingsRepository.isBiometricLockEnabled.value) {
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
                val app = application as MyDocApp
                val mimeType = contentResolver.getType(uri) ?: intent.type ?: ""
                val path = uri.path?.lowercase() ?: ""
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
                    val mediaItem = app.mediaRepository.getMediaItem(uri)
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
