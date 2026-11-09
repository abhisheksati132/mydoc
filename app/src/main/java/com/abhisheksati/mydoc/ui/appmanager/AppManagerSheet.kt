package com.abhisheksati.mydoc.ui.appmanager

import androidx.compose.material.icons.automirrored.outlined.ArrowBack

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
import com.abhisheksati.mydoc.designsystem.components.AppProgressIndicator
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import androidx.compose.material3.MaterialTheme
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.tools.appmanager.AppInfoItem
import com.abhisheksati.mydoc.tools.appmanager.AppManagerHelper
import java.io.File
import kotlinx.coroutines.launch
import com.abhisheksati.mydoc.designsystem.components.showMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppManagerScreen(
    onDismiss: () -> Unit,
    onOpenApk: (java.io.File) -> Unit
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
    var tab by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
    val showSystemApps = tab == 1
    var apkFiles by remember { mutableStateOf<List<com.abhisheksati.mydoc.domain.model.StorageItem>>(emptyList()) }
    var largeAppsOnly by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var extractingPackage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tab) {
        // Run in the effect's own scope so a toggle cancels the prior scan instead of launching an
        // unowned coroutine that races it (stale list / isLoading).
        isLoading = true
        if (tab == 2) {
            apkFiles = (context.applicationContext as com.abhisheksati.mydoc.MyDocApp)
                .storageRepository.queryCategory(com.abhisheksati.mydoc.domain.model.FileCategory.APKS)
        } else {
            apps = appHelper.getInstalledApps(includeSystemApps = showSystemApps)
        }
        isLoading = false
    }

    val filteredApps = remember(apps, searchQuery, showSystemApps, largeAppsOnly) {
        apps.filter { it.isSystemApp == showSystemApps && (!largeAppsOnly || it.apkSize >= 100L * 1024L * 1024L) }.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }.sortedBy { it.appName.lowercase() }
    }

    val filteredApks = remember(apkFiles, searchQuery) {
        apkFiles.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    androidx.activity.compose.BackHandler(onBack = onDismiss)
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
                // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.viewer_back))
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.files_category_apps),
                            style = typography.heading,
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (tab == 2) stringResource(R.string.files_subtitle_many, filteredApks.size)
                            else stringResource(R.string.app_count_found, filteredApps.size),
                            style = typography.caption,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar & Filters
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                        Text(
                            stringResource(if (tab == 2) R.string.apps_search_apks else R.string.app_manager_search),
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

            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    label = { Text(stringResource(R.string.apps_installed_tab)) }
                )
                FilterChip(
                    selected = showSystemApps,
                    onClick = { tab = 1 },
                    label = { Text(stringResource(R.string.apps_system_tab)) }
                )
                FilterChip(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    label = { Text(stringResource(R.string.apps_install_files_tab)) }
                )
            }

            if (tab != 2) {
                FilterChip(
                    selected = largeAppsOnly,
                    onClick = { largeAppsOnly = !largeAppsOnly },
                    label = { Text(stringResource(R.string.apps_large_filter)) }
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
            } else if (tab == 2) {
                if (filteredApks.isEmpty()) {
                    com.abhisheksati.mydoc.ui.files.FilesEmptyState(
                        icon = Icons.Outlined.Android,
                        title = stringResource(R.string.apps_install_files_tab),
                        message = stringResource(R.string.apps_no_install_files)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(filteredApks, key = { it.path }) { item ->
                            com.abhisheksati.mydoc.ui.files.LocationRow(
                                title = item.name,
                                meta = item.formattedSize,
                                icon = Icons.Outlined.Android,
                                onClick = { onOpenApk(item.file) }
                            )
                        }
                    }
                }
            } else if (filteredApps.isEmpty()) {
                com.abhisheksati.mydoc.ui.files.FilesEmptyState(
                    icon = Icons.Outlined.Android,
                    title = stringResource(R.string.files_category_apps),
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
                                        context.showMessage(UserMessages.actionFailed(R.string.cd_extract_apk, err, "AppManager").resolve(context))
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
                                        context.showMessage(UserMessages.actionFailed(R.string.cd_extract_apk, err, "AppManager").resolve(context))
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
    var actionsOpen by remember { mutableStateOf(false) }

    Surface(
        color = colors.background,
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
                Box {
                    IconButton(onClick = { actionsOpen = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.cd_more_actions))
                    }
                    com.abhisheksati.mydoc.designsystem.components.AppDropdownMenu(
                        expanded = actionsOpen, onDismissRequest = { actionsOpen = false }
                    ) {
                        listOf(
                            R.string.cd_launch to onOpen,
                            R.string.cd_extract_apk to onExtract,
                            R.string.cd_share_apk to onShare
                        ).forEach { (label, action) ->
                            DropdownMenuItem(
                                text = { Text(stringResource(label)) },
                                onClick = { actionsOpen = false; action() }
                            )
                        }
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
