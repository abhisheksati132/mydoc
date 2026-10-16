package com.abhisheksati.mydoc.ui.viewer.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.domain.model.ReadingMode

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.StayCurrentPortrait
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.BrightnessMedium

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerTopBar(
    title: String,
    visible: Boolean,
    readingMode: ReadingMode,
    isContinuousScroll: Boolean,
    hasOutline: Boolean,
    isKeepScreenOn: Boolean = true,
    isBookmarked: Boolean = false,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onToggleReadingMode: () -> Unit,
    onToggleScrollMode: () -> Unit,
    onOutlineClick: () -> Unit,
    onShareClick: () -> Unit,
    onToggleBookmark: () -> Unit = {},
    onBrightnessClick: () -> Unit = {},
    onOpenWithClick: () -> Unit = {},
    onPrintClick: () -> Unit = {},
    onToggleKeepScreenOn: () -> Unit = {},
    onToggleOrientation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isMenuOpen by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            actions = {
                if (hasOutline) {
                    IconButton(onClick = onOutlineClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.FormatListBulleted,
                            contentDescription = "Outline",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onToggleReadingMode) {
                    Icon(
                        imageVector = Icons.Outlined.Brightness4,
                        contentDescription = "Reading Mode: ${readingMode.name}",
                        tint = if (readingMode != ReadingMode.NORMAL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onToggleScrollMode) {
                    Icon(
                        imageVector = if (isContinuousScroll) Icons.Outlined.SwapVert else Icons.Outlined.ViewDay,
                        contentDescription = if (isContinuousScroll) "Continuous Scroll" else "Page Flip",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onShareClick) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = stringResource(R.string.share),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onToggleBookmark) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isBookmarked) "Remove Bookmark" else "Bookmark Page",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(onClick = { isMenuOpen = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "More Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = isMenuOpen,
                        onDismissRequest = { isMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Screen Dimmer") },
                            leadingIcon = {
                                Icon(Icons.Outlined.BrightnessMedium, contentDescription = null)
                            },
                            onClick = {
                                isMenuOpen = false
                                onBrightnessClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Open with another app") },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                            },
                            onClick = {
                                isMenuOpen = false
                                onOpenWithClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Print") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Print, contentDescription = null)
                            },
                            onClick = {
                                isMenuOpen = false
                                onPrintClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isKeepScreenOn) "Keep Screen On: Yes" else "Keep Screen On: No") },
                            leadingIcon = {
                                Icon(Icons.Outlined.StayCurrentPortrait, contentDescription = null)
                            },
                            onClick = {
                                isMenuOpen = false
                                onToggleKeepScreenOn()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rotate Screen") },
                            leadingIcon = {
                                Icon(Icons.Outlined.ScreenRotation, contentDescription = null)
                            },
                            onClick = {
                                isMenuOpen = false
                                onToggleOrientation()
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            )
        )
    }
}
