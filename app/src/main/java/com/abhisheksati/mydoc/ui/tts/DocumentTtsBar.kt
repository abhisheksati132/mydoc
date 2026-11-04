package com.abhisheksati.mydoc.ui.tts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme
import com.abhisheksati.mydoc.tools.tts.DocumentTtsManager
import com.abhisheksati.mydoc.tools.tts.TtsPlaybackState

@Composable
fun DocumentTtsBar(
    ttsManager: DocumentTtsManager,
    documentText: String,
    isVisible: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography

    val state by ttsManager.playbackState.collectAsState()
    val speed by ttsManager.speechRate.collectAsState()
    val currentUtterance by ttsManager.currentUtterance.collectAsState()

    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            color = colors.surface,
            shape = AppRadius.sheetShape,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Top row: status + close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(colors.accent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (state) {
                                TtsPlaybackState.PLAYING -> "Reading Aloud..."
                                TtsPlaybackState.PAUSED -> "Paused"
                                TtsPlaybackState.INITIALIZING -> "Initializing voice..."
                                TtsPlaybackState.ERROR -> "Voice error"
                                else -> "Read Aloud Ready"
                            },
                            style = typography.caption,
                            color = colors.textPrimary
                        )
                    }

                    IconButton(
                        onClick = {
                            ttsManager.stop()
                            onClose()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                // Utterance preview
                if (currentUtterance.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"$currentUtterance\"",
                        style = typography.micro,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom row: Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Play / Pause
                    Button(
                        onClick = {
                            when (state) {
                                TtsPlaybackState.PLAYING -> ttsManager.pause()
                                TtsPlaybackState.PAUSED -> ttsManager.resume()
                                else -> ttsManager.startReading(documentText)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = AppRadius.cardShape,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (state == TtsPlaybackState.PLAYING) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state == TtsPlaybackState.PLAYING) "Pause" else if (state == TtsPlaybackState.PAUSED) "Resume" else "Start",
                            color = Color.White,
                            style = AppTheme.typography.bodyStrong
                        )
                    }

                    // Stop
                    OutlinedButton(
                        onClick = { ttsManager.stop() },
                        shape = AppRadius.cardShape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = colors.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop", color = colors.error, style = AppTheme.typography.caption)
                    }

                    // Speed toggle pill
                    Surface(
                        color = colors.background,
                        shape = AppRadius.cardShape,
                        modifier = Modifier
                            .clip(AppRadius.cardShape)
                            .clickable {
                                val nextIdx = (speeds.indexOf(speed) + 1) % speeds.size
                                ttsManager.setSpeed(speeds[nextIdx])
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${speed}x",
                            style = if (speed != 1.0f) typography.captionStrong else typography.caption,
                            color = colors.accent
                        )
                    }
                }
            }
        }
    }
}
