package com.abhisheksati.mydoc

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.abhisheksati.mydoc.ui.home.HomeScreen
import com.abhisheksati.mydoc.ui.settings.SettingsScreen
import com.abhisheksati.mydoc.ui.viewer.DocumentViewerScreen
import com.abhisheksati.mydoc.update.UpdateManager
import com.abhisheksati.mydoc.viewmodel.HomeViewModel
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

    private lateinit var updateManager: UpdateManager
    private var currentDocument by mutableStateOf<DocumentFile?>(null)
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
                    } else {
                        val activeDoc = currentDocument
                        if (activeDoc != null) {
                            DocumentViewerScreen(
                                document = activeDoc,
                                onBackClick = { currentDocument = null },
                                onShareClick = { shareDocument(activeDoc) }
                            )
                        } else {
                            HomeScreen(
                                viewModel = homeViewModel,
                                onDocumentClick = { doc -> currentDocument = doc },
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
