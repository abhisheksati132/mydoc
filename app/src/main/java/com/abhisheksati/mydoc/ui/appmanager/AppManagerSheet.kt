package com.abhisheksati.mydoc.ui.appmanager

import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.util.UserMessages
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import com.abhisheksati.mydoc.designsystem.components.AppEmptyState
import com.abhisheksati.mydoc.designsystem.components.AppProgressIndicator
import com.abhisheksati.mydoc.designsystem.components.GlassSurface
import com.abhisheksati.mydoc.designsystem.components.OverlaySystemBars
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import androidx.compose.material3.MaterialTheme
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.designsystem.tokens.LocalIsOled
import com.abhisheksati.mydoc.tools.appmanager.AppInfoItem
import com.abhisheksati.mydoc.tools.appmanager.AppManagerHelper
import java.io.File
import kotlinx.coroutines.launch
import com.abhisheksati.mydoc.designsystem.components.showMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppManagerSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val coroutineScope = rememberCoroutineScope()
    val appHelper = remember { AppManagerHelper(context) }

    var apps by remember { mutableStateOf<List<AppInfoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(false) }
    var extractingPackage by remember { mutableStateOf<String?>(null) }

    fun loadApps() {
        coroutineScope.launch {
            isLoading = true
            apps = appHelper.getInstalledApps(includeSystemApps = showSystemApps)
            isLoading = false
        }
    }

    LaunchedEffect(showSystemApps) {
        // Run in the effect's own scope so a toggle cancels the prior scan instead of launching an
        // unowned coroutine that races it (stale list / isLoading).
        isLoading = true
        apps = appHelper.getInstalledApps(includeSystemApps = showSystemApps)
        isLoading = false
    }

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps
        else apps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    val isOled = LocalIsOled.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = colors.scrim.copy(alpha = 0.45f),
        dragHandle = null,
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        OverlaySystemBars()
        GlassSurface(
            shape = AppRadius.sheetShape,
            elevation = 16.dp,
            modifier = Modifier.fillMaxWidth().fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Tactile Glass Drag Handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 10.dp, bottom = 12.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(colors.inkMuted.copy(alpha = 0.35f))
                )

                // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Android,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.app_manager_title),
                            style = typography.heading,
                            color = colors.textPrimary
                        )
                        Text(
                            text = stringResource(R.string.app_count_found, filteredApps.size),
                            style = typography.caption,
                            color = colors.textSecondary
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close), tint = colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar & Filters
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                        Text(
                            stringResource(R.string.app_manager_search),
                            color = colors.textSecondary,
                            style = AppTheme.typography.label
                        )
                    },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.textSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.action_clear), tint = colors.textSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = AppRadius.cardShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.divider
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Toggle Chip for System Apps
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                FilterChip(
                    selected = showSystemApps,
                    onClick = { showSystemApps = !showSystemApps },
                    label = { Text(stringResource(R.string.app_manager_include_system), style = AppTheme.typography.caption) },
                    leadingIcon = {
                        if (showSystemApps) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AppProgressIndicator()
                }
            } else if (filteredApps.isEmpty()) {
                AppEmptyState(
                    message = if (searchQuery.isBlank()) {
                        stringResource(R.string.app_manager_empty)
                    } else {
                        stringResource(R.string.app_manager_no_match, searchQuery)
                    },
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        AppListItem(
                            app = app,
                            isExtracting = extractingPackage == app.packageName,
                            onExtract = {
                                coroutineScope.launch {
                                    extractingPackage = app.packageName
                                    val result = appHelper.extractApk(app)
                                    extractingPackage = null
                                    result.onSuccess { file ->
                                        context.showMessage(resources.getString(R.string.app_manager_extracted, file.name), long = true)
                                    }.onFailure { err ->
                                        context.showMessage(UserMessages.actionFailed(resources.getString(R.string.cd_extract_apk), err, "AppManager"))
                                    }
                                }
                            },
                            onShare = {
                                coroutineScope.launch {
                                    extractingPackage = app.packageName
                                    val result = appHelper.extractApk(app)
                                    extractingPackage = null
                                    result.onSuccess { file ->
                                        shareApk(context, file)
                                    }.onFailure { err ->
                                        context.showMessage(UserMessages.actionFailed(resources.getString(R.string.cd_extract_apk), err, "AppManager"))
                                    }
                                }
                            },
                            onOpen = {
                                if (!appHelper.openApp(app.packageName)) {
                                    appHelper.openAppDetails(app.packageName)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
private fun AppListItem(
    app: AppInfoItem,
    isExtracting: Boolean,
    onExtract: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val iconBitmap = remember(app.icon) { app.icon?.toImageBitmap() }

    GlassSurface(
        shape = AppRadius.cardShape,
        elevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(AppRadius.cardShape)
                    .background(colors.background),
                contentAlignment = Alignment.Center
            ) {
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = app.appName,
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Android,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = typography.bodyStrong,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.app_version_line, app.versionName, formatFileSize(app.apkSize)),
                    style = typography.micro,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (app.isSplitApk) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(
                        shape = AppRadius.pillShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = stringResource(R.string.app_split_apk, app.splitApkFiles.size + 1),
                            style = typography.micro,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = app.packageName,
                    style = typography.micro,
                    color = colors.textSecondary.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Actions
            if (isExtracting) {
                AppProgressIndicator()
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onExtract,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FileDownload,
                            contentDescription = stringResource(R.string.cd_extract_apk),
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = stringResource(R.string.cd_share_apk),
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpen,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = stringResource(R.string.cd_launch),
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun shareApk(context: Context, apkFile: File) {
    try {
        val sharedDir = File(context.cacheDir, "shared").apply { if (!exists()) mkdirs() }
        val shareFile = if (apkFile.parentFile?.absolutePath == sharedDir.absolutePath) {
            apkFile
        } else {
            val dest = File(sharedDir, apkFile.name)
            apkFile.copyTo(dest, overwrite = true)
            dest
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            shareFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.app_share_chooser)))
    } catch (e: Exception) {
        context.showMessage(context.getString(R.string.app_share_failed))
    }
}

private fun Drawable.toImageBitmap(): ImageBitmap? {
    return try {
        val bmp = if (this is BitmapDrawable && this.bitmap != null) {
            this.bitmap
        } else {
            val width = if (intrinsicWidth > 0) intrinsicWidth else 96
            val height = if (intrinsicHeight > 0) intrinsicHeight else 96
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            setBounds(0, 0, canvas.width, canvas.height)
            draw(canvas)
            bitmap
        }
        bmp.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

private fun formatFileSize(bytes: Long): String = com.abhisheksati.mydoc.domain.model.StorageVolume.formatBytes(bytes)
