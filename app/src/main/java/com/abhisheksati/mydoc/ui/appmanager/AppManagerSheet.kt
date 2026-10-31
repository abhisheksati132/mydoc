package com.abhisheksati.mydoc.ui.appmanager

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.abhisheksati.mydoc.tools.appmanager.AppInfoItem
import com.abhisheksati.mydoc.tools.appmanager.AppManagerHelper
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import kotlinx.coroutines.launch
import java.io.File
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppManagerSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
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
        loadApps()
    }

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps
        else apps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

        val isOled = colors.background == com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors.background

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isOled) Color(0xFF10121A) else colors.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
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
                            text = "Installed Apps & APKs",
                            style = typography.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        Text(
                            text = "${filteredApps.size} applications found",
                            style = typography.caption.copy(fontSize = 12.sp),
                            color = colors.textSecondary
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar & Filters
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search installed applications...", color = colors.textSecondary, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.textSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = colors.textSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
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
                    label = { Text("Include System Apps", fontSize = 12.sp) },
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
                    CircularProgressIndicator(color = colors.accent)
                }
            } else if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No applications found", color = colors.textSecondary, style = typography.body)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(bottom = 24.dp),
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
                                        Toast.makeText(
                                            context,
                                            "Extracted to Downloads/MyDoc/ExtractedAPKs/${file.name}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }.onFailure { err ->
                                        Toast.makeText(context, "Failed: ${err.message}", Toast.LENGTH_SHORT).show()
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
                                        Toast.makeText(context, "Extract error: ${err.message}", Toast.LENGTH_SHORT).show()
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

    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
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
                    .clip(RoundedCornerShape(10.dp))
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
                    style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${app.versionName} • ${formatFileSize(app.apkSize)}",
                    style = typography.caption.copy(fontSize = 11.sp),
                    color = colors.textSecondary
                )
                Text(
                    text = app.packageName,
                    style = typography.caption.copy(fontSize = 10.sp),
                    color = colors.textSecondary.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Actions
            if (isExtracting) {
                CircularProgressIndicator(
                    color = colors.accent,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onExtract,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FileDownload,
                            contentDescription = "Extract APK",
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
                            contentDescription = "Share APK",
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
                            contentDescription = "Launch",
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
        context.startActivity(Intent.createChooser(shareIntent, "Share APK"))
    } catch (e: Exception) {
        Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
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
