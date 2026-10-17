package com.abhisheksati.mydoc.ui.viewer.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.rendering.engine.presentation.PptxParser
import com.abhisheksati.mydoc.rendering.engine.presentation.PptxPresentation
import com.abhisheksati.mydoc.rendering.engine.presentation.PptxSlide
import com.abhisheksati.mydoc.ui.common.AppDivider
import com.abhisheksati.mydoc.ui.theme.Spacing
import com.abhisheksati.mydoc.ui.viewer.controls.PageJumpDialog
import com.abhisheksati.mydoc.ui.viewer.controls.PrintHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresentationViewerScreen(
    document: DocumentFile,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var presentationData by remember { mutableStateOf<PptxPresentation?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var readingMode by remember { mutableStateOf(ReadingMode.NORMAL) }
    var isMenuOpen by remember { mutableStateOf(false) }
    var isJumpDialogOpen by remember { mutableStateOf(false) }

    LaunchedEffect(document.uri) {
        isLoading = true
        errorMessage = null
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(document.uri)
                val inputStream = com.abhisheksati.mydoc.data.local.DocumentStreamResolver.openStream(context, uri, document.displayName)
                    ?: throw IllegalStateException("Could not open input stream")
                val parsed = inputStream.use { PptxParser().parse(it) }
                presentationData = parsed
                isLoading = false
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to read presentation"
                isLoading = false
            }
        }
    }

    val slides = presentationData?.slides ?: emptyList()
    val pagerState = rememberPagerState(pageCount = { slides.size })

    // Volume key slide navigation
    LaunchedEffect(slides.size) {
        if (slides.isNotEmpty()) {
            com.abhisheksati.mydoc.MainActivity.pageTurnEvents.collect { isNext ->
                val target = if (isNext) {
                    (pagerState.currentPage + 1).coerceAtMost(slides.size - 1)
                } else {
                    (pagerState.currentPage - 1).coerceAtLeast(0)
                }
                if (target != pagerState.currentPage) {
                    pagerState.animateScrollToPage(target)
                }
            }
        }
    }

    val bgColor = when (readingMode) {
        ReadingMode.INVERTED_DARK -> Color.Black
        ReadingMode.SEPIA -> Color(0xFFF4ECD8)
        ReadingMode.NORMAL -> MaterialTheme.colorScheme.background
    }

    val cardBgColor = when (readingMode) {
        ReadingMode.INVERTED_DARK -> Color(0xFF111111)
        ReadingMode.SEPIA -> Color(0xFFEADBBE)
        ReadingMode.NORMAL -> MaterialTheme.colorScheme.surface
    }

    val textColor = when (readingMode) {
        ReadingMode.INVERTED_DARK -> Color.White
        ReadingMode.SEPIA -> Color(0xFF3A3125)
        ReadingMode.NORMAL -> MaterialTheme.colorScheme.onBackground
    }

    val secondaryTextColor = when (readingMode) {
        ReadingMode.INVERTED_DARK -> Color(0xFFAAAAAA)
        ReadingMode.SEPIA -> Color(0xFF5A4E3A)
        ReadingMode.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(modifier = Modifier.background(bgColor)) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = document.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                        IconButton(onClick = {
                            readingMode = when (readingMode) {
                                ReadingMode.NORMAL -> ReadingMode.INVERTED_DARK
                                ReadingMode.INVERTED_DARK -> ReadingMode.SEPIA
                                ReadingMode.SEPIA -> ReadingMode.NORMAL
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Brightness4,
                                contentDescription = "Toggle Reading Mode",
                                tint = if (readingMode != ReadingMode.NORMAL) MaterialTheme.colorScheme.primary else secondaryTextColor
                            )
                        }
                        IconButton(onClick = onShareClick) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "Share",
                                tint = secondaryTextColor
                            )
                        }
                        Box {
                            IconButton(onClick = { isMenuOpen = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "Options",
                                    tint = secondaryTextColor
                                )
                            }
                            DropdownMenu(
                                expanded = isMenuOpen,
                                onDismissRequest = { isMenuOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Open with another app") },
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                                    },
                                    onClick = {
                                        isMenuOpen = false
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(Uri.parse(document.uri), document.mimeType)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        try {
                                            context.startActivity(Intent.createChooser(intent, "Open with"))
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "No app available", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Print") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Print, contentDescription = null)
                                    },
                                    onClick = {
                                        isMenuOpen = false
                                        PrintHelper.printDocument(context, document)
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = bgColor)
                )
                AppDivider()
            }
        },
        bottomBar = {
            if (slides.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor)
                        .navigationBarsPadding()
                ) {
                    AppDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.medium, vertical = Spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (pagerState.currentPage > 0) {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                            },
                            enabled = pagerState.currentPage > 0
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Previous Slide",
                                tint = if (pagerState.currentPage > 0) textColor else secondaryTextColor.copy(alpha = 0.3f)
                            )
                        }

                        Text(
                            text = "Slide ${pagerState.currentPage + 1} of ${slides.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            modifier = Modifier
                                .clickable { isJumpDialogOpen = true }
                                .padding(Spacing.small)
                        )

                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (pagerState.currentPage < slides.size - 1) {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            },
                            enabled = pagerState.currentPage < slides.size - 1
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                contentDescription = "Next Slide",
                                tint = if (pagerState.currentPage < slides.size - 1) textColor else secondaryTextColor.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        },
        containerColor = bgColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "Error reading presentation",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(Spacing.large)
                )
            } else if (slides.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val slide = slides[pageIndex]
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.medium),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .background(cardBgColor, RoundedCornerShape(8.dp))
                                .border(1.dp, secondaryTextColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(Spacing.large)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = slide.title,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    modifier = Modifier.padding(bottom = Spacing.medium)
                                )

                                for (item in slide.content) {
                                    Row(modifier = Modifier.padding(vertical = Spacing.extraSmall)) {
                                        Text(
                                            text = "• ",
                                            fontWeight = FontWeight.Bold,
                                            color = textColor,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = item,
                                            fontSize = 15.sp,
                                            color = textColor,
                                            lineHeight = 22.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            PageJumpDialog(
                isOpen = isJumpDialogOpen,
                currentPage = pagerState.currentPage,
                pageCount = slides.size,
                onJump = { page ->
                    scope.launch { pagerState.scrollToPage(page) }
                },
                onDismiss = { isJumpDialogOpen = false }
            )
        }
    }
}
