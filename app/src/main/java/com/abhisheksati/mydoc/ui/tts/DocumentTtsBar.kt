package com.abhisheksati.mydoc.ui.tts

import com.abhisheksati.mydoc.R
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
import androidx.compose.ui.res.stringResource
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
                                TtsPlaybackState.PLAYING -> stringResource(R.string.tts_reading)
                                TtsPlaybackState.PAUSED -> stringResource(R.string.tts_paused)
                                TtsPlaybackState.INITIALIZING -> stringResource(R.string.tts_initializing)
                                TtsPlaybackState.ERROR -> stringResource(R.string.tts_error)
                                else -> stringResource(R.string.tts_ready)
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
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close), tint = colors.textSecondary, modifier = Modifier.size(20.dp))
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.onAccent
                        ),
                        shape = AppRadius.cardShape,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = if (state == TtsPlaybackState.PLAYING) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = colors.onAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(
                                when (state) {
                                    TtsPlaybackState.PLAYING -> R.string.tts_pause
                                    TtsPlaybackState.PAUSED -> R.string.tts_resume
                                    else -> R.string.tts_start
                                }
                            ),
                            color = colors.onAccent,
                            style = AppTheme.typography.bodyStrong
                        )
                    }

                    // Stop
                    OutlinedButton(
                        onClick = { ttsManager.stop() },
                        shape = AppRadius.cardShape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = colors.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.action_stop), color = colors.error, style = AppTheme.typography.caption)
                    }

                    // Speed toggle pill
                    Surface(
                        color = colors.background,
                        shape = AppRadius.cardShape,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
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

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.tts_engine_notice),
                    style = typography.micro,
                    color = colors.textSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
