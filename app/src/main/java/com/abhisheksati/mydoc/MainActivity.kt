package com.abhisheksati.mydoc

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.ui.home.HomeScreen
import com.abhisheksati.mydoc.ui.theme.MyDocTheme
import com.abhisheksati.mydoc.ui.viewer.DocumentPreviewScreen
import com.abhisheksati.mydoc.update.UpdateManager
import com.abhisheksati.mydoc.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels {
        val app = application as MyDocApp
        HomeViewModel.Factory(app.documentRepository)
    }

    private lateinit var updateManager: UpdateManager
    private var currentDocument by mutableStateOf<DocumentFile?>(null)
    private var isSettingsOpen by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        updateManager = UpdateManager(this)

        // Silent launch update check (throttled to max once every 24h)
        lifecycleScope.launch {
            updateManager.checkForUpdates(isLaunchCheck = true)
        }

        handleIncomingIntent(intent)

        setContent {
            MyDocTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val activeDoc = currentDocument
                    if (isSettingsOpen) {
                        com.abhisheksati.mydoc.ui.settings.SettingsScreen(
                            updateManager = updateManager,
                            onBackClick = { isSettingsOpen = false }
                        )
                    } else if (activeDoc != null) {
                        DocumentPreviewScreen(
                            document = activeDoc,
                            onBackClick = { currentDocument = null },
                            onToggleFavorite = { homeViewModel.toggleFavorite(activeDoc) },
                            onShare = { shareDocument(activeDoc) }
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
