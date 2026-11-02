package com.abhisheksati.mydoc.ui.viewer.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.ui.viewer.controls.SearchOverlay

/**
 * Top app bar with search integration, mode switching (slideshow, continuous, visual/outline),
 * and dropdown options for the presentation viewer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresentationTopBar(
    document: DocumentFile,
    effectiveSlideCount: Int,
    isSlideshowMode: Boolean,
    isSearchOpen: Boolean,
    searchQuery: String,
    searchMatchesCount: Int,
    currentMatchIndex: Int,
    onQueryChanged: (String) -> Unit,
    onNextMatch: () -> Unit,
    onPreviousMatch: () -> Unit,
    onCloseSearch: () -> Unit,
    onOpenSearch: () -> Unit,
    onBackClick: () -> Unit,
    isContinuousVertical: Boolean,
    onToggleContinuousVertical: () -> Unit,
    onStartSlideshow: () -> Unit,
    isVisualMode: Boolean,
    onToggleVisualMode: () -> Unit,
    readingMode: ReadingMode,
    onCycleReadingMode: () -> Unit,
    onOpenJumpDialog: () -> Unit,
    onToggleThumbnailStrip: () -> Unit,
    onOpenNotesSheet: () -> Unit,
    onShareClick: () -> Unit,
    onOpenInOfficeApp: () -> Unit,
    onPrintClick: () -> Unit,
    bgColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    modifier: Modifier = Modifier
) {
    if (isSlideshowMode) return

    var isMenuOpen by remember { mutableStateOf(false) }

    Column(modifier = modifier.background(bgColor)) {
        if (isSearchOpen) {
            SearchOverlay(
                visible = true,
                query = searchQuery,
                matchCount = searchMatchesCount,
                currentMatchIndex = currentMatchIndex,
                onQueryChanged = onQueryChanged,
                onNextMatch = onNextMatch,
                onPreviousMatch = onPreviousMatch,
                onCloseSearch = onCloseSearch
            )
        } else {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = document.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = document.extension.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (effectiveSlideCount > 0) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = secondaryTextColor.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "$effectiveSlideCount slides",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = secondaryTextColor
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                },
                actions = {
                    // Search
                    IconButton(onClick = onOpenSearch) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search Slides",
                            tint = secondaryTextColor
                        )
                    }

                    // Continuous Vertical vs Horizontal toggle button
                    IconButton(onClick = onToggleContinuousVertical) {
                        Icon(
                            imageVector = if (isContinuousVertical) Icons.Outlined.ViewCarousel else Icons.Outlined.SwapVert,
                            contentDescription = if (isContinuousVertical) "Switch to Horizontal Paging" else "Switch to Continuous Vertical",
                            tint = if (isContinuousVertical) MaterialTheme.colorScheme.primary else secondaryTextColor
                        )
                    }

                    // Slideshow Mode
                    IconButton(onClick = onStartSlideshow) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = "Start Slideshow",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // More Options
                    Box {
                        IconButton(onClick = { isMenuOpen = true }) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = "More Options",
                                tint = secondaryTextColor
                            )
                        }
                        DropdownMenu(
                            expanded = isMenuOpen,
                            onDismissRequest = { isMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isContinuousVertical) "Switch to Horizontal Pager" else "Switch to Continuous Vertical") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isContinuousVertical) Icons.Outlined.ViewCarousel else Icons.Outlined.SwapVert,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    isMenuOpen = false
                                    onToggleContinuousVertical()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isVisualMode) "Switch to Outline View" else "Switch to Visual View") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isVisualMode) Icons.AutoMirrored.Filled.Article else Icons.Default.Slideshow,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    isMenuOpen = false
                                    onToggleVisualMode()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(when (readingMode) {
                                        ReadingMode.NORMAL -> "Theme: Dark"
                                        ReadingMode.INVERTED_DARK -> "Theme: Sepia"
                                        ReadingMode.SEPIA -> "Theme: Eye Comfort"
                                        ReadingMode.EYE_COMFORT -> "Theme: Light"
                                    })
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Brightness4, contentDescription = null)
                                },
                                onClick = {
                                    isMenuOpen = false
                                    onCycleReadingMode()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Jump to Slide...") },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                                },
                                onClick = {
                                    isMenuOpen = false
                                    onOpenJumpDialog()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Thumbnail Strip") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.ViewCarousel, contentDescription = null)
                                },
                                onClick = {
                                    isMenuOpen = false
                                    onToggleThumbnailStrip()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Speaker Notes") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Description, contentDescription = null)
                                },
                                onClick = {
                                    isMenuOpen = false
                                    onOpenNotesSheet()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share Presentation") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Share, contentDescription = null)
                                },
                                onClick = {
                                    isMenuOpen = false
                                    onShareClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Open in Office App") },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                                },
                                onClick = {
                                    isMenuOpen = false
                                    onOpenInOfficeApp()
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
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
