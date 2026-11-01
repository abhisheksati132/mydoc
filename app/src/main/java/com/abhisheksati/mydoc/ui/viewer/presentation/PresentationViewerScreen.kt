package com.abhisheksati.mydoc.ui.viewer.presentation

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.runtime.derivedStateOf
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.DropdownMenu
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.abhisheksati.mydoc.designsystem.components.AppDivider
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.rendering.engine.presentation.PptxParser
import com.abhisheksati.mydoc.rendering.engine.presentation.PptxPresentation
import com.abhisheksati.mydoc.rendering.engine.presentation.PptxSlide
import com.abhisheksati.mydoc.rendering.engine.presentation.VisualElement
import com.abhisheksati.mydoc.rendering.engine.presentation.VisualElementType
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.ui.viewer.controls.PageJumpDialog
import com.abhisheksati.mydoc.ui.viewer.controls.PrintHelper
import com.abhisheksati.mydoc.ui.viewer.controls.SearchOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun openPresentationInOfficeApp(context: Context, document: DocumentFile) {
    try {
        val uri = Uri.parse(document.uri)
        val ext = document.extension.lowercase()
        val mimeType = when (ext) {
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "ppt" -> "application/vnd.ms-powerpoint"
            else -> document.mimeType ?: "application/vnd.ms-powerpoint"
        }

        val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
        val targetFile = File(sharedDir, document.displayName)

        if (!targetFile.exists() || targetFile.length() == 0L) {
            val stream = com.abhisheksati.mydoc.data.local.DocumentStreamResolver.openStream(context, uri, document.displayName)
            stream?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        val shareUri = if (targetFile.exists() && targetFile.length() > 0L) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                targetFile
            )
        } else {
            uri
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(shareUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(Intent.createChooser(intent, "Open presentation in"))
    } catch (_: Exception) {
        try {
            val playStoreIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.docs.editors.slides")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(playStoreIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "Install Google Slides or Microsoft PowerPoint to view full graphics", Toast.LENGTH_LONG).show()
        }
    }
}

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
    val app = context.applicationContext as? MyDocApp

    var cachedFileOnDisk by remember { mutableStateOf<File?>(null) }
    var presentationData by remember { mutableStateOf<PptxPresentation?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var readingMode by remember { mutableStateOf(ReadingMode.NORMAL) }
    var isVisualMode by remember { mutableStateOf(true) }
    var isMenuOpen by remember { mutableStateOf(false) }
    var isJumpDialogOpen by remember { mutableStateOf(false) }

    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var currentMatchIndex by remember { mutableIntStateOf(0) }

    var isThumbnailStripOpen by remember { mutableStateOf(false) }
    var isSlideshowMode by remember { mutableStateOf(false) }
    var isNotesSheetOpen by remember { mutableStateOf(false) }

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(document.uri) {
        onDispose {
            cachedFileOnDisk?.delete()
            cachedFileOnDisk = null
        }
    }

    LaunchedEffect(document.uri) {
        isLoading = true
        errorMessage = null
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(document.uri)
                val ext = document.extension.lowercase().ifBlank { "pptx" }
                val cacheDir = File(context.cacheDir, "pres_cache").apply { mkdirs() }
                val tempFile = File(cacheDir, "pres_${System.currentTimeMillis()}.$ext")
                cachedFileOnDisk = tempFile

                val inputStream = com.abhisheksati.mydoc.data.local.DocumentStreamResolver.openStream(context, uri, document.displayName)
                    ?: throw IllegalStateException("Could not open presentation stream")
                inputStream.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // Parse with native Kotlin XML parser
                try {
                    val parsed = tempFile.inputStream().use { PptxParser().parse(it, context.cacheDir) }
                    presentationData = parsed
                } catch (e: Throwable) {
                    android.util.Log.w("PresentationViewer", "Native parser failed: ${e.message}")
                }

                // If native failed or had 0 slides, perform deep raw string/image fallback extraction
                if (presentationData == null || presentationData?.slides.isNullOrEmpty() || presentationData?.slides?.firstOrNull()?.title == "Empty Presentation") {
                    try {
                        val fallback = PptxParser().parseBinaryPptFallback(tempFile.readBytes())
                        if (fallback.slides.isNotEmpty() && fallback.slides.firstOrNull()?.title != "Empty Presentation") {
                            presentationData = fallback
                        }
                    } catch (_: Throwable) {}
                }

                isLoading = false
            } catch (t: Throwable) {
                android.util.Log.e("PresentationViewer", "Error loading presentation: ${document.displayName}", t)
                errorMessage = t.localizedMessage ?: "Failed to read presentation"
                isLoading = false
            }
        }
    }

    val rawSlides = presentationData?.slides ?: emptyList()
    val isNativeEmpty = rawSlides.isEmpty() || (rawSlides.size == 1 && rawSlides.first().title == "Empty Presentation")
    val effectiveSlideCount = if (!isNativeEmpty) rawSlides.size else 0
    val pagerState = rememberPagerState(pageCount = { effectiveSlideCount.coerceAtLeast(1) })
    val lazyListState = rememberLazyListState()
    var isContinuousVertical by remember { mutableStateOf(false) }
    val effectiveSlides: List<PptxSlide> = if (!isNativeEmpty) rawSlides else emptyList()

    val currentSlideIndex by remember {
        derivedStateOf {
            if (isContinuousVertical) lazyListState.firstVisibleItemIndex.coerceIn(0, (effectiveSlideCount - 1).coerceAtLeast(0))
            else pagerState.currentPage
        }
    }

    // Smart Resume: Restore last viewed slide on open
    LaunchedEffect(effectiveSlideCount) {
        if (effectiveSlideCount > 0) {
            withContext(Dispatchers.IO) {
                val savedState = app?.documentRepository?.getViewState(document.uri)
                val targetSlide = savedState?.lastPage?.coerceIn(0, effectiveSlideCount - 1) ?: 0
                if (targetSlide > 0) {
                    withContext(Dispatchers.Main) {
                        pagerState.scrollToPage(targetSlide)
                        lazyListState.scrollToItem(targetSlide)
                    }
                }
            }
        }
    }

    // Smart Resume: Persist slide progress on change
    LaunchedEffect(currentSlideIndex, effectiveSlideCount) {
        if (effectiveSlideCount > 0) {
            withContext(Dispatchers.IO) {
                app?.documentRepository?.updatePageProgress(document.uri, currentSlideIndex, effectiveSlideCount)
                app?.documentRepository?.saveViewState(
                    com.abhisheksati.mydoc.domain.model.ViewState(
                        uri = document.uri,
                        lastPage = currentSlideIndex,
                        readingMode = readingMode
                    )
                )
            }
        }
    }

    // Reset zoom when swiping slides
    LaunchedEffect(currentSlideIndex) {
        zoomScale = 1f
        panOffset = Offset.Zero
    }

    // Volume key slide navigation
    LaunchedEffect(effectiveSlideCount, isContinuousVertical) {
        if (effectiveSlideCount > 0) {
            com.abhisheksati.mydoc.MainActivity.pageTurnEvents.collect { isNext ->
                val current = if (isContinuousVertical) lazyListState.firstVisibleItemIndex else pagerState.currentPage
                val target = if (isNext) {
                    (current + 1).coerceAtMost(effectiveSlideCount - 1)
                } else {
                    (current - 1).coerceAtLeast(0)
                }
                if (target != current) {
                    if (isContinuousVertical) {
                        lazyListState.animateScrollToItem(target)
                    } else {
                        pagerState.animateScrollToPage(target)
                    }
                }
            }
        }
    }

    // In-slide keyword search match indices
    val searchMatches = remember(searchQuery, effectiveSlides) {
        if (searchQuery.length < 2) emptyList<Int>()
        else {
            val list = mutableListOf<Int>()
            effectiveSlides.forEachIndexed { idx, slide ->
                val inTitle = slide.title.contains(searchQuery, ignoreCase = true)
                val inContent = slide.content.any { it.contains(searchQuery, ignoreCase = true) }
                val inVisual = slide.visualElements.any { el ->
                    el.paragraphs.any { p -> p.plainText.contains(searchQuery, ignoreCase = true) } ||
                    el.tableRows.any { row -> row.any { cell -> cell.contains(searchQuery, ignoreCase = true) } }
                }
                if (inTitle || inContent || inVisual) {
                    list.add(idx)
                }
            }
            list
        }
    }

    BackHandler(enabled = isSlideshowMode || isSearchOpen) {
        if (isSearchOpen) {
            isSearchOpen = false
            searchQuery = ""
        } else if (isSlideshowMode) {
            isSlideshowMode = false
        }
    }

    val colors = com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors.current
    val isAppDark = colors.background == com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors.background

    val bgColor = when (readingMode) {
        ReadingMode.INVERTED_DARK -> Color(0xFF000000)
        ReadingMode.SEPIA -> Color(0xFFF4ECD8)
        ReadingMode.EYE_COMFORT -> Color(0xFFF7F3E9)
        ReadingMode.NORMAL -> if (isAppDark) Color(0xFF000000) else colors.background
    }

    val cardBgColor = when (readingMode) {
        ReadingMode.INVERTED_DARK -> Color(0xFF0A0A0C)
        ReadingMode.SEPIA -> Color(0xFFEADBBE)
        ReadingMode.EYE_COMFORT -> Color(0xFFECE4D0)
        ReadingMode.NORMAL -> if (isAppDark) Color(0xFF0A0A0C) else Color.White
    }

    val textColor = when (readingMode) {
        ReadingMode.INVERTED_DARK -> Color(0xFFEDEDED)
        ReadingMode.SEPIA -> Color(0xFF3A3125)
        ReadingMode.EYE_COMFORT -> Color(0xFF2C261F)
        ReadingMode.NORMAL -> colors.textPrimary
    }

    val secondaryTextColor = when (readingMode) {
        ReadingMode.INVERTED_DARK -> Color(0xFFA0A0A5)
        ReadingMode.SEPIA -> Color(0xFF5A4E3A)
        ReadingMode.EYE_COMFORT -> Color(0xFF5C5245)
        ReadingMode.NORMAL -> colors.textSecondary
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (!isSlideshowMode) {
                Column(
                    modifier = Modifier.background(bgColor)
                ) {
                    if (isSearchOpen) {
                        SearchOverlay(
                            visible = true,
                            query = searchQuery,
                            matchCount = searchMatches.size,
                            currentMatchIndex = currentMatchIndex,
                            onQueryChanged = {
                                searchQuery = it
                                currentMatchIndex = 0
                            },
                            onNextMatch = {
                                if (searchMatches.isNotEmpty()) {
                                    currentMatchIndex = (currentMatchIndex + 1) % searchMatches.size
                                    scope.launch { pagerState.animateScrollToPage(searchMatches[currentMatchIndex]) }
                                }
                            },
                            onPreviousMatch = {
                                if (searchMatches.isNotEmpty()) {
                                    currentMatchIndex = (currentMatchIndex - 1 + searchMatches.size) % searchMatches.size
                                    scope.launch { pagerState.animateScrollToPage(searchMatches[currentMatchIndex]) }
                                }
                            },
                            onCloseSearch = {
                                isSearchOpen = false
                                searchQuery = ""
                            }
                        )
                    } else {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = document.displayName,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
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
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            ),
                                            color = colors.accent
                                        )
                                        if (effectiveSlideCount > 0) {
                                            Text(
                                                text = "•",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = secondaryTextColor.copy(alpha = 0.5f)
                                            )
                                            Text(
                                                text = "$effectiveSlideCount slides",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
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
                                IconButton(onClick = { isSearchOpen = true }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = "Search Slides",
                                        tint = secondaryTextColor
                                    )
                                }

                                // Continuous Vertical vs Horizontal toggle button
                                IconButton(onClick = {
                                    val newMode = !isContinuousVertical
                                    isContinuousVertical = newMode
                                    scope.launch {
                                        if (newMode) {
                                            lazyListState.scrollToItem(pagerState.currentPage)
                                        } else {
                                            pagerState.scrollToPage(lazyListState.firstVisibleItemIndex)
                                        }
                                    }
                                }) {
                                    Icon(
                                        imageVector = if (isContinuousVertical) Icons.Outlined.ViewCarousel else Icons.Outlined.SwapVert,
                                        contentDescription = if (isContinuousVertical) "Switch to Horizontal Paging" else "Switch to Continuous Vertical",
                                        tint = if (isContinuousVertical) colors.accent else secondaryTextColor
                                    )
                                }

                                // Slideshow Mode
                                IconButton(onClick = { isSlideshowMode = true }) {
                                    Icon(
                                        imageVector = Icons.Outlined.PlayArrow,
                                        contentDescription = "Start Slideshow",
                                        tint = colors.accent
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
                                        onDismissRequest = { isMenuOpen = false },
                                        shape = RoundedCornerShape(16.dp),
                                        containerColor = colors.surface,
                                        border = BorderStroke(1.dp, colors.divider),
                                        shadowElevation = 8.dp,
                                        tonalElevation = 4.dp
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
                                                val newMode = !isContinuousVertical
                                                isContinuousVertical = newMode
                                                scope.launch {
                                                    if (newMode) {
                                                        lazyListState.scrollToItem(pagerState.currentPage)
                                                    } else {
                                                        pagerState.scrollToPage(lazyListState.firstVisibleItemIndex)
                                                    }
                                                }
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
                                                isVisualMode = !isVisualMode
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
                                                readingMode = when (readingMode) {
                                                    ReadingMode.NORMAL -> ReadingMode.INVERTED_DARK
                                                    ReadingMode.INVERTED_DARK -> ReadingMode.SEPIA
                                                    ReadingMode.SEPIA -> ReadingMode.EYE_COMFORT
                                                    ReadingMode.EYE_COMFORT -> ReadingMode.NORMAL
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Jump to Slide...") },
                                            leadingIcon = {
                                                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                                            },
                                            onClick = {
                                                isMenuOpen = false
                                                isJumpDialogOpen = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Thumbnail Strip") },
                                            leadingIcon = {
                                                Icon(Icons.Outlined.ViewCarousel, contentDescription = null)
                                            },
                                            onClick = {
                                                isMenuOpen = false
                                                isThumbnailStripOpen = !isThumbnailStripOpen
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Speaker Notes") },
                                            leadingIcon = {
                                                Icon(Icons.Outlined.Description, contentDescription = null)
                                            },
                                            onClick = {
                                                isMenuOpen = false
                                                isNotesSheetOpen = true
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
                                                openPresentationInOfficeApp(context, document)
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
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
                        )
                    }
                    AppDivider()
                }
            }
        },
        bottomBar = {
            if (effectiveSlideCount > 0 && !isSlideshowMode) {
                PresentationBottomBar(
                    effectiveSlideCount = effectiveSlideCount,
                    currentSlideIndex = currentSlideIndex,
                    effectiveSlides = effectiveSlides,
                    isThumbnailStripOpen = isThumbnailStripOpen,
                    onToggleThumbnailStrip = { isThumbnailStripOpen = !isThumbnailStripOpen },
                    onSelectSlide = { index ->
                        scope.launch {
                            if (isContinuousVertical) {
                                lazyListState.animateScrollToItem(index)
                            } else {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    },
                    onPreviousSlide = {
                        scope.launch {
                            if (currentSlideIndex > 0) {
                                if (isContinuousVertical) lazyListState.animateScrollToItem(currentSlideIndex - 1)
                                else pagerState.animateScrollToPage(currentSlideIndex - 1)
                            }
                        }
                    },
                    onNextSlide = {
                        scope.launch {
                            if (currentSlideIndex < effectiveSlideCount - 1) {
                                if (isContinuousVertical) lazyListState.animateScrollToItem(currentSlideIndex + 1)
                                else pagerState.animateScrollToPage(currentSlideIndex + 1)
                            }
                        }
                    },
                    onOpenJumpDialog = { isJumpDialogOpen = true },
                    onOpenNotesSheet = { isNotesSheetOpen = true },
                    bgColor = bgColor,
                    cardBgColor = cardBgColor,
                    textColor = textColor,
                    secondaryTextColor = secondaryTextColor
                )
            }
        },
        containerColor = bgColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isSlideshowMode) PaddingValues(0.dp) else innerPadding)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else if (errorMessage != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(AppSpacing.paddingLarge)
                ) {
                    Text(
                        text = errorMessage ?: "Error reading presentation",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { openPresentationInOfficeApp(context, document) }) {
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open with PowerPoint / Google Slides")
                    }
                }
            } else if (effectiveSlideCount == 0) {
                PresentationEmptyRecoveryCard(
                    cardBgColor = cardBgColor,
                    textColor = textColor,
                    secondaryTextColor = secondaryTextColor,
                    onOpenExternal = {
                        openPresentationInOfficeApp(context, document)
                    }
                )
            } else if (isContinuousVertical) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = AppSpacing.paddingSmall, vertical = AppSpacing.paddingSmall),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(effectiveSlideCount, key = { it }) { pageIndex ->
                        val slide = effectiveSlides.getOrNull(pageIndex)
                        if (slide != null) {
                            val slideAspect = if (slide.aspectRatio.isFinite() && slide.aspectRatio > 0.1f) slide.aspectRatio else (16f / 9f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(slideAspect),
                                contentAlignment = Alignment.Center
                            ) {
                                val hasMeaningfulVisuals = slide.visualElements.any {
                                    it.type == VisualElementType.IMAGE ||
                                    it.tableRows.isNotEmpty() ||
                                    it.paragraphs.any { p -> p.runs.any { r -> r.text.isNotBlank() } }
                                }

                                if (isVisualMode && hasMeaningfulVisuals) {
                                    VisualSlideCard(
                                        slide = slide,
                                        cardBgColor = cardBgColor,
                                        defaultTextColor = textColor,
                                        highlightQuery = if (isSearchOpen) searchQuery else ""
                                    )
                                } else {
                                    OutlineSlideCard(
                                        slide = slide,
                                        cardBgColor = cardBgColor,
                                        textColor = if (cardBgColor.isColorDark()) Color(0xFFEDEDED) else Color(0xFF1E293B),
                                        highlightQuery = if (isSearchOpen) searchQuery else ""
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (isSlideshowMode) 0.dp else AppSpacing.paddingMedium)
                            .pointerInput(isSlideshowMode) {
                                detectTapGestures(
                                    onTap = { offset ->
                                        if (isSlideshowMode) {
                                            val w = size.width
                                            if (offset.x < w * 0.25f) {
                                                if (pagerState.currentPage > 0) {
                                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                                }
                                            } else {
                                                if (pagerState.currentPage < effectiveSlideCount - 1) {
                                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                                }
                                            }
                                        }
                                    },
                                    onDoubleTap = {
                                        zoomScale = if (zoomScale > 1.2f) 1f else 2.5f
                                        panOffset = Offset.Zero
                                    }
                                )
                            }
                            .then(
                                if (zoomScale > 1.05f) {
                                    Modifier.pointerInput(zoomScale) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            zoomScale = (zoomScale * zoom).coerceIn(1f, 5f)
                                            if (zoomScale > 1f) {
                                                val maxPan = 500f * (zoomScale - 1f)
                                                panOffset = Offset(
                                                    x = (panOffset.x + pan.x).coerceIn(-maxPan, maxPan),
                                                    y = (panOffset.y + pan.y).coerceIn(-maxPan, maxPan)
                                                )
                                            } else {
                                                panOffset = Offset.Zero
                                            }
                                        }
                                    }
                                } else Modifier
                            )
                            .graphicsLayer {
                                scaleX = zoomScale
                                scaleY = zoomScale
                                translationX = panOffset.x
                                translationY = panOffset.y
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val slide = effectiveSlides.getOrNull(pageIndex)
                        if (slide != null) {
                            val hasMeaningfulVisuals = slide.visualElements.any {
                                it.type == VisualElementType.IMAGE ||
                                it.tableRows.isNotEmpty() ||
                                it.paragraphs.any { p -> p.runs.any { r -> r.text.isNotBlank() } }
                            }

                            if (isVisualMode && hasMeaningfulVisuals) {
                                VisualSlideCard(
                                    slide = slide,
                                    cardBgColor = cardBgColor,
                                    defaultTextColor = textColor,
                                    highlightQuery = if (isSearchOpen) searchQuery else ""
                                )
                            } else {
                                OutlineSlideCard(
                                    slide = slide,
                                    cardBgColor = cardBgColor,
                                    textColor = if (cardBgColor.isColorDark()) Color(0xFFEDEDED) else Color(0xFF1E293B),
                                    highlightQuery = if (isSearchOpen) searchQuery else ""
                                )
                            }
                        }
                    }
                }

                // In Slideshow mode: Minimal floating exit button
                if (isSlideshowMode) {
                    IconButton(
                        onClick = { isSlideshowMode = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(AppSpacing.paddingMedium)
                            .background(Color(0x88000000), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Exit Slideshow",
                            tint = Color.White
                        )
                    }
                }
            }

            PageJumpDialog(
                isOpen = isJumpDialogOpen,
                currentPage = currentSlideIndex,
                pageCount = effectiveSlideCount,
                onJump = { page ->
                    scope.launch {
                        if (isContinuousVertical) lazyListState.scrollToItem(page)
                        else pagerState.scrollToPage(page)
                    }
                },
                onDismiss = { isJumpDialogOpen = false }
            )

            // Speaker Notes Bottom Sheet
            if (isNotesSheetOpen) {
                val curNotes = effectiveSlides.getOrNull(currentSlideIndex)?.notes ?: "No speaker notes for this slide."
                ModalBottomSheet(
                    onDismissRequest = { isNotesSheetOpen = false },
                    containerColor = cardBgColor
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppSpacing.paddingLarge)
                            .navigationBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Speaker Notes — Slide ${currentSlideIndex + 1}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            IconButton(onClick = { isNotesSheetOpen = false }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Close", tint = textColor)
                            }
                        }
                        Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))
                        Text(
                            text = curNotes,
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor,
                            lineHeight = 24.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }
            }
        }
    }
}

