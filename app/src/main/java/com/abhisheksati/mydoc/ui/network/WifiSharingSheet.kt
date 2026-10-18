package com.abhisheksati.mydoc.ui.network

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.network.WifiServerManager
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiSharingSheet(
    wifiServerManager: WifiServerManager,
    rootDirectory: File,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val context = LocalContext.current

    val isRunning by wifiServerManager.isRunning.collectAsState()
    val serverUrl by wifiServerManager.serverUrl.collectAsState()
    val ip = remember { wifiServerManager.getLocalIpAddress() }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isRunning) Color(0xFF4CAF50).copy(alpha = 0.15f) else colors.accent.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = if (ip == null) Icons.Outlined.WifiOff else Icons.Outlined.Wifi,
                        contentDescription = null,
                        tint = if (isRunning) Color(0xFF4CAF50) else colors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "PC Wi-Fi Transfer",
                        style = typography.title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Text(
                        text = if (isRunning) "Server active on local network" else "Transfer files to/from PC without cables",
                        style = typography.caption,
                        color = if (isRunning) Color(0xFF4CAF50) else colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Visual Device-to-PC Graphic
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.background)
                            .border(1.dp, colors.divider, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Outlined.Smartphone, contentDescription = null, tint = colors.accent, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Phone", style = typography.caption, color = colors.textSecondary)
                }

                Box(contentAlignment = Alignment.Center) {
                    if (isRunning) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50).copy(alpha = 0.2f))
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.SyncAlt,
                        contentDescription = null,
                        tint = if (isRunning) Color(0xFF4CAF50) else colors.textSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.background)
                            .border(1.dp, colors.divider, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Outlined.Laptop, contentDescription = null, tint = colors.accent, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("PC / Mac", style = typography.caption, color = colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (ip == null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.background),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Wi-Fi Connection",
                            style = typography.body.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFEF5350)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Connect your phone to a Wi-Fi network or enable portable hotspot to share files with your computer.",
                            style = typography.caption,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (isRunning && serverUrl != null) {
                val currentUrl = serverUrl!!
                val qrBitmap = remember(currentUrl) {
                    try {
                        val writer = com.google.zxing.qrcode.QRCodeWriter()
                        val bitMatrix = writer.encode(currentUrl, com.google.zxing.BarcodeFormat.QR_CODE, 320, 320)
                        val bmp = android.graphics.Bitmap.createBitmap(320, 320, android.graphics.Bitmap.Config.RGB_565)
                        for (x in 0 until 320) {
                            for (y in 0 until 320) {
                                bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                            }
                        }
                        bmp
                    } catch (_: Exception) { null }
                }

                // Active Server Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.background),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF4CAF50).copy(alpha = 0.5f))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TYPE THIS ADDRESS IN YOUR PC BROWSER:",
                            style = typography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("MyDoc URL", currentUrl))
                                    Toast.makeText(context, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = currentUrl,
                                    style = typography.body.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = colors.accent
                                )
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (qrBitmap != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            androidx.compose.foundation.Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Scan to connect",
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Scan QR with another phone or tablet",
                                style = typography.caption.copy(fontSize = 11.sp),
                                color = colors.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(currentUrl))
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary)
                        ) {
                            Text("Open in Browser (Test Locally)", fontSize = 12.sp)
                        }
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.background),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("How it works:", style = typography.body.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("1. Connect phone and PC to the same Wi-Fi network", style = typography.caption, color = colors.textSecondary)
                        Text("2. Tap 'Start Transfer' below", style = typography.caption, color = colors.textSecondary)
                        Text("3. Open the link on your PC to upload & download files at Wi-Fi speeds", style = typography.caption, color = colors.textSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            if (isRunning) {
                Button(
                    onClick = { wifiServerManager.stopServer() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop Transfer", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { wifiServerManager.startServer(rootDirectory) },
                    enabled = ip != null,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Transfer", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
