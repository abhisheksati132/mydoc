package com.abhisheksati.mydoc.ui.vault

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.security.BiometricLockHelper
import com.abhisheksati.mydoc.security.vault.VaultItem
import com.abhisheksati.mydoc.security.vault.VaultManager
import kotlinx.coroutines.launch
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSheet(
    onDismiss: () -> Unit,
    onOpenFile: (File) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val coroutineScope = rememberCoroutineScope()
    val vaultManager = remember { VaultManager(context) }

    var isUnlocked by remember { mutableStateOf(!BiometricLockHelper.isDeviceSecure(context)) }
    var items by remember { mutableStateOf<List<VaultItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun refreshItems() {
        coroutineScope.launch {
            isLoading = true
            items = vaultManager.getVaultItems()
            isLoading = false
        }
    }

    LaunchedEffect(isUnlocked) {
        if (isUnlocked) {
            refreshItems()
        }
    }

    // Auto-prompt biometric authentication if locked
    LaunchedEffect(Unit) {
        if (!isUnlocked && activity != null) {
            BiometricLockHelper.promptBiometricAuthentication(
                activity = activity,
                confirmCredentialLauncher = null,
                onSuccess = { isUnlocked = true },
                onError = { statusMessage = "Authentication failed: $it" }
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isUnlocked) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Encrypted Safe Vault",
                            style = typography.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (isUnlocked) "${items.size} encrypted files (AES-256-GCM)" else "Hardware-backed secure storage",
                            style = typography.caption.copy(fontSize = 12.sp),
                            color = colors.textSecondary
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isUnlocked) {
                // Locked Screen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(colors.accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Security,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Safe Vault is Locked",
                            style = typography.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Authenticate with fingerprint, face, or PIN to view your encrypted files.",
                            style = typography.body.copy(fontSize = 13.sp),
                            color = colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                if (activity != null) {
                                    BiometricLockHelper.promptBiometricAuthentication(
                                        activity = activity,
                                        confirmCredentialLauncher = null,
                                        onSuccess = { isUnlocked = true },
                                        onError = { statusMessage = it }
                                    )
                                } else {
                                    isUnlocked = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unlock Safe Vault", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.accent)
                }
            } else if (items.isEmpty()) {
                // Empty Vault
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EnhancedEncryption,
                            contentDescription = null,
                            tint = colors.accent.copy(alpha = 0.7f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Safe Vault is Empty",
                            style = typography.body.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Long-press any file in the File Manager and select 'Move to Safe Vault' to encrypt it with military-grade AES-256.",
                            style = typography.caption.copy(fontSize = 12.sp),
                            color = colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                // Vault Items List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        VaultItemCard(
                            item = item,
                            onPreview = {
                                coroutineScope.launch {
                                    val result = vaultManager.decryptToTemp(item)
                                    result.onSuccess { tempFile ->
                                        onOpenFile(tempFile)
                                    }.onFailure { err ->
                                        Toast.makeText(context, "Decryption error: ${err.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onRestore = {
                                coroutineScope.launch {
                                    val dest = android.os.Environment.getExternalStoragePublicDirectory(
                                        android.os.Environment.DIRECTORY_DOWNLOADS
                                    )
                                    val result = vaultManager.decryptAndRestore(item, dest)
                                    result.onSuccess { restoredFile ->
                                        Toast.makeText(
                                            context,
                                            "Decrypted & Restored to ${restoredFile.name}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        refreshItems()
                                    }.onFailure { err ->
                                        Toast.makeText(context, "Restore error: ${err.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onDelete = {
                                coroutineScope.launch {
                                    vaultManager.deleteVaultItem(item)
                                    Toast.makeText(context, "File permanently shredded", Toast.LENGTH_SHORT).show()
                                    refreshItems()
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
private fun VaultItemCard(
    item: VaultItem,
    onPreview: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()) }

    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPreview() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(colors.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.originalName,
                    style = typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${formatSize(item.originalSize)} • ${dateFormat.format(Date(item.dateAdded))}",
                    style = typography.caption.copy(fontSize = 11.sp),
                    color = colors.textSecondary
                )
            }

            // Restore / Decrypt action
            IconButton(onClick = onRestore, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Outlined.FileDownload,
                    contentDescription = "Decrypt & Restore",
                    tint = colors.accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Shred / Delete action
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Outlined.DeleteForever,
                    contentDescription = "Delete & Shred",
                    tint = colors.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    val df = DecimalFormat("#,##0.#")
    return "${df.format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
}
