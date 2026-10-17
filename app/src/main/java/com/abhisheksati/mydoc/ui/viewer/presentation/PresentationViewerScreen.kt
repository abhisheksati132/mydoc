package com.abhisheksati.mydoc.ui.viewer.presentation

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
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

    LaunchedEffect(document.uri) {
        isLoading = true
        errorMessage = null
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(document.uri)
                val inputStream = com.abhisheksati.mydoc.data.local.DocumentStreamResolver.openStream(context, uri, document.displayName)
                    ?: throw IllegalStateException("Could not open presentation stream")
                val parsed = inputStream.use { PptxParser().parse(it, context.cacheDir) }
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

    // Smart Resume: Restore last viewed slide on open
    LaunchedEffect(slides.size) {
        if (slides.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                val savedState = app?.documentRepository?.getViewState(document.uri)
                val targetSlide = savedState?.lastPage?.coerceIn(0, slides.size - 1) ?: 0
                if (targetSlide > 0) {
                    withContext(Dispatchers.Main) {
                        pagerState.scrollToPage(targetSlide)
                    }
                }
            }
        }
    }

    // Smart Resume: Persist slide progress on change
    LaunchedEffect(pagerState.currentPage, slides.size) {
        if (slides.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                app?.documentRepository?.updatePageProgress(document.uri, pagerState.currentPage, slides.size)
                app?.documentRepository?.saveViewState(
                    com.abhisheksati.mydoc.domain.model.ViewState(
                        uri = document.uri,
                        lastPage = pagerState.currentPage,
                        readingMode = readingMode
                    )
                )
            }
        }
    }

    // Reset zoom when swiping slides
    LaunchedEffect(pagerState.currentPage) {
        zoomScale = 1f
        panOffset = Offset.Zero
    }

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

    // In-slide keyword search match indices
    val searchMatches = remember(searchQuery, slides) {
        if (searchQuery.length < 2) emptyList<Int>()
        else {
            val list = mutableListOf<Int>()
            slides.forEachIndexed { idx, slide ->
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

    val uiTextColor = MaterialTheme.colorScheme.onSurface
    val uiSecondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    val bgColor = when (readingMode) {
        ReadingMode.INVERTED_DARK -> Color(0xFF0F0F10)
        ReadingMode.SEPIA -> Color(0xFFF4ECD8)
        ReadingMode.NORMAL -> MaterialTheme.colorScheme.background
    }

    val cardBgColor = when (readingMode) {
        ReadingMode.INVERTED_DARK -> Color(0xFF1E1E22)
        ReadingMode.SEPIA -> Color(0xFFEADBBE)
        ReadingMode.NORMAL -> Color.White
    }

    val textColor = when (readingMode) {
        ReadingMode.INVERTED_DARK -> Color(0xFFEDEDED)
        ReadingMode.SEPIA -> Color(0xFF3A3125)
        ReadingMode.NORMAL -> uiTextColor
    }

    val secondaryTextColor = when (readingMode) {
        ReadingMode.INVERTED_DARK -> Color(0xFFA0A0A5)
        ReadingMode.SEPIA -> Color(0xFF5A4E3A)
        ReadingMode.NORMAL -> uiSecondaryTextColor
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
                                // In-Slide Keyword Search
                                IconButton(onClick = { isSearchOpen = true }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = "Search Slides",
                                        tint = secondaryTextColor
                                    )
                                }

                                // Fullscreen Slideshow / Presenter Mode
                                IconButton(onClick = { isSlideshowMode = true }) {
                                    Icon(
                                        imageVector = Icons.Outlined.PlayArrow,
                                        contentDescription = "Start Slideshow",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Toggle between Visual Slides and Outline View
                                IconButton(
                                    onClick = {
                                        isVisualMode = !isVisualMode
                                        val modeName = if (isVisualMode) "Visual Slide View" else "Outline Text View"
                                        Toast.makeText(context, modeName, Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isVisualMode) Icons.Default.Slideshow else Icons.AutoMirrored.Filled.Article,
                                        contentDescription = "Toggle Visual / Outline",
                                        tint = if (isVisualMode) MaterialTheme.colorScheme.primary else secondaryTextColor
                                    )
                                }

                                // Toggle Reading Mode
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

                                // Share
                                IconButton(onClick = onShareClick) {
                                    Icon(
                                        imageVector = Icons.Outlined.Share,
                                        contentDescription = "Share",
                                        tint = secondaryTextColor
                                    )
                                }

                                // More options
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
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
                        )
                    }
                    AppDivider()
                }
            }
        },
        bottomBar = {
            if (slides.isNotEmpty() && !isSlideshowMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor)
                        .navigationBarsPadding()
                ) {
                    // Slide Thumbnail Carousel (when open)
                    AnimatedVisibility(visible = isThumbnailStripOpen) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bgColor)
                        ) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = AppSpacing.paddingSmall),
                                contentPadding = PaddingValues(horizontal = AppSpacing.paddingMedium),
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
                            ) {
                                items(slides.size) { index ->
                                    val isSelected = index == pagerState.currentPage
                                    val slide = slides[index]
                                    Box(
                                        modifier = Modifier
                                            .width(96.dp)
                                            .height(58.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(cardBgColor)
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0x33888888),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .clickable {
                                                scope.launch { pagerState.animateScrollToPage(index) }
                                            }
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else textColor
                                            )
                                            Text(
                                                text = slide.title,
                                                fontSize = 9.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = secondaryTextColor
                                            )
                                        }
                                    }
                                }
                            }
                            AppDivider()
                        }
                    }

                    AppDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = AppSpacing.paddingMedium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Previous slide
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

                        // Thumbnail carousel toggle button
                        IconButton(onClick = { isThumbnailStripOpen = !isThumbnailStripOpen }) {
                            Icon(
                                imageVector = Icons.Outlined.ViewCarousel,
                                contentDescription = "Toggle Thumbnails",
                                tint = if (isThumbnailStripOpen) MaterialTheme.colorScheme.primary else secondaryTextColor
                            )
                        }

                        // Slide counter & page jump
                        Text(
                            text = "Slide ${pagerState.currentPage + 1} of ${slides.size}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = textColor,
                            modifier = Modifier
                                .clickable { isJumpDialogOpen = true }
                                .padding(AppSpacing.paddingSmall)
                        )

                        // Speaker Notes button
                        val currentSlide = slides.getOrNull(pagerState.currentPage)
                        val hasNotes = !currentSlide?.notes.isNullOrBlank()
                        IconButton(
                            onClick = { isNotesSheetOpen = true },
                            enabled = hasNotes
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = "Speaker Notes",
                                tint = if (hasNotes) MaterialTheme.colorScheme.primary else secondaryTextColor.copy(alpha = 0.25f)
                            )
                        }

                        // Next slide
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
                .padding(if (isSlideshowMode) PaddingValues(0.dp) else innerPadding)
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
                    modifier = Modifier.padding(AppSpacing.paddingLarge)
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
                                                if (pagerState.currentPage < slides.size - 1) {
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
                        val hasMeaningfulVisuals = slide.visualElements.any {
                            it.type == VisualElementType.IMAGE ||
                            it.tableRows.isNotEmpty() ||
                            it.paragraphs.any { p -> p.runs.any { r -> r.text.isNotBlank() } }
                        }

                        if (isVisualMode && hasMeaningfulVisuals) {
                            // Rich visual slide rendering
                            VisualSlideCard(
                                slide = slide,
                                cardBgColor = cardBgColor,
                                defaultTextColor = textColor,
                                highlightQuery = if (isSearchOpen) searchQuery else ""
                            )
                        } else {
                            // Outline / clean text bullet points view
                            OutlineSlideCard(
                                slide = slide,
                                cardBgColor = cardBgColor,
                                textColor = if (cardBgColor.isColorDark()) Color(0xFFEDEDED) else Color(0xFF1E293B),
                                highlightQuery = if (isSearchOpen) searchQuery else ""
                            )
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
                currentPage = pagerState.currentPage,
                pageCount = slides.size,
                onJump = { page ->
                    scope.launch { pagerState.scrollToPage(page) }
                },
                onDismiss = { isJumpDialogOpen = false }
            )

            // Speaker Notes Bottom Sheet
            if (isNotesSheetOpen) {
                val curNotes = slides.getOrNull(pagerState.currentPage)?.notes ?: "No speaker notes for this slide."
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
                                text = "Speaker Notes — Slide ${pagerState.currentPage + 1}",
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

/**
 * Helper to draw Microsoft preset vector shapes using Canvas & Path.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPresetShape(
    preset: String,
    bg: Color,
    border: Color
) {
    val path = Path()
    val w = size.width
    val h = size.height

    when {
        preset.contains("rightArrow", ignoreCase = true) -> {
            path.moveTo(0f, h * 0.25f)
            path.lineTo(w * 0.6f, h * 0.25f)
            path.lineTo(w * 0.6f, 0f)
            path.lineTo(w, h * 0.5f)
            path.lineTo(w * 0.6f, h)
            path.lineTo(w * 0.6f, h * 0.75f)
            path.lineTo(0f, h * 0.75f)
            path.close()
        }
        preset.contains("leftArrow", ignoreCase = true) -> {
            path.moveTo(w, h * 0.25f)
            path.lineTo(w * 0.4f, h * 0.25f)
            path.lineTo(w * 0.4f, 0f)
            path.lineTo(0f, h * 0.5f)
            path.lineTo(w * 0.4f, h)
            path.lineTo(w * 0.4f, h * 0.75f)
            path.lineTo(w, h * 0.75f)
            path.close()
        }
        preset.contains("leftRightArrow", ignoreCase = true) -> {
            path.moveTo(w * 0.3f, 0f)
            path.lineTo(0f, h * 0.5f)
            path.lineTo(w * 0.3f, h)
            path.lineTo(w * 0.3f, h * 0.7f)
            path.lineTo(w * 0.7f, h * 0.7f)
            path.lineTo(w * 0.7f, h)
            path.lineTo(w, h * 0.5f)
            path.lineTo(w * 0.7f, 0f)
            path.lineTo(w * 0.7f, h * 0.3f)
            path.lineTo(w * 0.3f, h * 0.3f)
            path.close()
        }
        preset.contains("upArrow", ignoreCase = true) -> {
            path.moveTo(w * 0.5f, 0f)
            path.lineTo(w, h * 0.4f)
            path.lineTo(w * 0.75f, h * 0.4f)
            path.lineTo(w * 0.75f, h)
            path.lineTo(w * 0.25f, h)
            path.lineTo(w * 0.25f, h * 0.4f)
            path.lineTo(0f, h * 0.4f)
            path.close()
        }
        preset.contains("downArrow", ignoreCase = true) -> {
            path.moveTo(w * 0.25f, 0f)
            path.lineTo(w * 0.75f, 0f)
            path.lineTo(w * 0.75f, h * 0.6f)
            path.lineTo(w, h * 0.6f)
            path.lineTo(w * 0.5f, h)
            path.lineTo(0f, h * 0.6f)
            path.lineTo(w * 0.25f, h * 0.6f)
            path.close()
        }
        preset.contains("chevron", ignoreCase = true) -> {
            path.moveTo(0f, 0f)
            path.lineTo(w * 0.75f, 0f)
            path.lineTo(w, h * 0.5f)
            path.lineTo(w * 0.75f, h)
            path.lineTo(0f, h)
            path.lineTo(w * 0.25f, h * 0.5f)
            path.close()
        }
        preset.contains("pentagon", ignoreCase = true) -> {
            path.moveTo(0f, 0f)
            path.lineTo(w * 0.8f, 0f)
            path.lineTo(w, h * 0.5f)
            path.lineTo(w * 0.8f, h)
            path.lineTo(0f, h)
            path.close()
        }
        preset.contains("hexagon", ignoreCase = true) -> {
            path.moveTo(w * 0.25f, 0f)
            path.lineTo(w * 0.75f, 0f)
            path.lineTo(w, h * 0.5f)
            path.lineTo(w * 0.75f, h)
            path.lineTo(w * 0.25f, h)
            path.lineTo(0f, h * 0.5f)
            path.close()
        }
        preset.contains("triangle", ignoreCase = true) -> {
            path.moveTo(w * 0.5f, 0f)
            path.lineTo(w, h)
            path.lineTo(0f, h)
            path.close()
        }
        preset.contains("diamond", ignoreCase = true) -> {
            path.moveTo(w * 0.5f, 0f)
            path.lineTo(w, h * 0.5f)
            path.lineTo(w * 0.5f, h)
            path.lineTo(0f, h * 0.5f)
            path.close()
        }
        preset.contains("callout", ignoreCase = true) -> {
            path.moveTo(0f, 0f)
            path.lineTo(w, 0f)
            path.lineTo(w, h * 0.75f)
            path.lineTo(w * 0.45f, h * 0.75f)
            path.lineTo(w * 0.25f, h)
            path.lineTo(w * 0.30f, h * 0.75f)
            path.lineTo(0f, h * 0.75f)
            path.close()
        }
        preset.contains("star", ignoreCase = true) -> {
            val cx = w / 2f
            val cy = h / 2f
            val rOuter = minOf(w, h) / 2f
            val rInner = rOuter * 0.4f
            for (i in 0 until 10) {
                val angle = (i * Math.PI / 5.0 - Math.PI / 2.0).toFloat()
                val r = if (i % 2 == 0) rOuter else rInner
                val x = cx + r * kotlin.math.cos(angle)
                val y = cy + r * kotlin.math.sin(angle)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
        }
        preset.contains("line", ignoreCase = true) || preset.contains("connector", ignoreCase = true) -> {
            path.moveTo(0f, h * 0.5f)
            path.lineTo(w, h * 0.5f)
            drawPath(path, color = if (border != Color.Transparent) border else bg, style = Stroke(width = 2.dp.toPx()))
            return
        }
        else -> return
    }

    drawPath(path, color = bg)
    if (border != Color.Transparent) {
        drawPath(
            path,
            color = border,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

/**
 * Ensures text has high contrast and readability against its immediate background.
 */
private fun ensureTextLegibility(rawColor: Color?, containerBg: Color): Color {
    val isContainerDark = containerBg.isColorDark()
    if (rawColor == null || rawColor == Color.Transparent) {
        return if (isContainerDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    }
    val bgLum = (0.299f * containerBg.red + 0.587f * containerBg.green + 0.114f * containerBg.blue)
    val textLum = (0.299f * rawColor.red + 0.587f * rawColor.green + 0.114f * rawColor.blue)
    val contrastRatio = (maxOf(bgLum, textLum) + 0.05f) / (minOf(bgLum, textLum) + 0.05f)
    if (contrastRatio < 2.5f) {
        return if (isContainerDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    }
    return rawColor
}

/**
 * Maps PowerPoint / font face names to Jetpack Compose FontFamilies.
 */
private fun mapFontFamily(name: String?): FontFamily {
    if (name.isNullOrBlank()) return FontFamily.Default
    val lower = name.lowercase().trim()
    return when {
        lower.contains("times") || lower.contains("georgia") || lower.contains("garamond") ||
        lower.contains("palatino") || lower.contains("serif") || lower.contains("cambria") ||
        lower.contains("baskerville") || lower.contains("didot") || lower.contains("minion") ||
        lower.contains("bookman") || lower.contains("century") -> FontFamily.Serif

        lower.contains("courier") || lower.contains("consolas") || lower.contains("mono") ||
        lower.contains("menlo") || lower.contains("inconsolata") || lower.contains("source code") ||
        lower.contains("fira code") || lower.contains("jetbrains") -> FontFamily.Monospace

        lower.contains("comic") || lower.contains("script") || lower.contains("handwriting") ||
        lower.contains("chalkboard") || lower.contains("brush") || lower.contains("lucida handwriting") ||
        lower.contains("kalam") || lower.contains("caveat") -> FontFamily.Cursive

        else -> FontFamily.SansSerif
    }
}

/**
 * Text composable that highlights matched search keywords in yellow.
 */
@Composable
private fun HighlightableText(
    text: String,
    query: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight = FontWeight.Normal,
    fontStyle: FontStyle = FontStyle.Normal,
    fontFamily: FontFamily = FontFamily.Default,
    textDecoration: TextDecoration = TextDecoration.None,
    color: Color,
    textAlign: TextAlign = TextAlign.Start,
    modifier: Modifier = Modifier
) {
    if (query.isNotBlank() && query.length >= 2 && text.contains(query, ignoreCase = true)) {
        val annotated = buildAnnotatedString {
            var searchStart = 0
            val lowerText = text.lowercase()
            val lowerQuery = query.lowercase()

            while (searchStart < text.length) {
                val matchIdx = lowerText.indexOf(lowerQuery, searchStart)
                if (matchIdx == -1) {
                    append(text.substring(searchStart))
                    break
                }
                if (matchIdx > searchStart) {
                    append(text.substring(searchStart, matchIdx))
                }
                withStyle(
                    SpanStyle(
                        background = Color(0x99FFEB3B),
                        color = Color(0xFF1E293B),
                        fontWeight = fontWeight,
                        fontStyle = fontStyle,
                        fontFamily = fontFamily,
                        textDecoration = textDecoration
                    )
                ) {
                    append(text.substring(matchIdx, matchIdx + query.length))
                }
                searchStart = matchIdx + query.length
            }
        }
        Text(
            text = annotated,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontFamily = fontFamily,
            textDecoration = textDecoration,
            textAlign = textAlign,
            modifier = modifier
        )
    } else {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontFamily = fontFamily,
            textDecoration = textDecoration,
            color = color,
            textAlign = textAlign,
            modifier = modifier
        )
    }
}

/**
 * Renders the slide canvas with all its visual elements: images, shapes, diagrams, and tables.
 */
@Composable
private fun VisualSlideCard(
    slide: PptxSlide,
    cardBgColor: Color,
    defaultTextColor: Color,
    highlightQuery: String = ""
) {
    val slideBg = slide.backgroundColor ?: cardBgColor
    val isBgDark = slideBg.isColorDark()
    val baseContentColor = if (isBgDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val containerAspect = maxWidth / maxHeight
        val slideAspect = if (slide.aspectRatio > 0.1f) slide.aspectRatio else (16f / 9f)
        val cardModifier = if (containerAspect > slideAspect) {
            Modifier
                .fillMaxHeight()
                .aspectRatio(slideAspect)
        } else {
            Modifier
                .fillMaxWidth()
                .aspectRatio(slideAspect)
        }

        Box(
            modifier = cardModifier
                .background(slideBg, RoundedCornerShape(8.dp))
                .border(1.dp, Color(0x33888888), RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val totalWidth = maxWidth
                val totalHeight = maxHeight
                val referenceHeight = 540f
                val scaleFactor = (totalHeight.value / referenceHeight).coerceIn(0.55f, 2.5f)

                for (element in slide.visualElements) {
                    val elementWidth = (totalWidth * element.bounds.width).coerceAtLeast(1.dp)
                    val elementHeight = (totalHeight * element.bounds.height).coerceAtLeast(1.dp)
                    val offsetX = totalWidth * element.bounds.left
                    val offsetY = totalHeight * element.bounds.top

                    Box(
                        modifier = Modifier
                            .offset(x = offsetX, y = offsetY)
                            .size(width = elementWidth, height = elementHeight)
                            .clipToBounds()
                    ) {
                        when (element.type) {
                            VisualElementType.IMAGE -> {
                                val bytes = element.imageBytes
                                if (bytes != null && bytes.isNotEmpty()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(bytes)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Slide Image",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            VisualElementType.SHAPE, VisualElementType.TEXT_BOX -> {
                                val preset = element.shapePreset
                                val isVectorPreset = preset != null && (
                                    preset.contains("Arrow", ignoreCase = true) ||
                                    preset.contains("triangle", ignoreCase = true) ||
                                    preset.contains("diamond", ignoreCase = true) ||
                                    preset.contains("star", ignoreCase = true) ||
                                    preset.contains("chevron", ignoreCase = true) ||
                                    preset.contains("pentagon", ignoreCase = true) ||
                                    preset.contains("hexagon", ignoreCase = true) ||
                                    preset.contains("callout", ignoreCase = true) ||
                                    preset.contains("line", ignoreCase = true) ||
                                    preset.contains("connector", ignoreCase = true)
                                )

                                val containerBg = if (element.backgroundColor != null && element.backgroundColor != Color.Transparent) {
                                    element.backgroundColor!!
                                } else {
                                    slideBg
                                }

                                if (preset != null && isVectorPreset) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val bg = element.backgroundColor ?: Color(0x22888888)
                                        val border = element.borderColor ?: Color(0x44888888)
                                        drawPresetShape(preset, bg, border)
                                    }
                                }

                                val shapeModifier = Modifier
                                    .fillMaxSize()
                                    .then(
                                        if (!isVectorPreset && element.backgroundColor != null && element.backgroundColor != Color.Transparent) {
                                            Modifier.background(
                                                color = element.backgroundColor,
                                                shape = if (element.isRounded) RoundedCornerShape(8.dp) else if (element.isEllipse) CircleShape else RectangleShape
                                            )
                                        } else Modifier
                                    )
                                    .then(
                                        if (!isVectorPreset && element.borderColor != null) {
                                            Modifier.border(
                                                width = (element.borderWidthDp.takeIf { it > 0f } ?: 1f).dp,
                                                color = element.borderColor,
                                                shape = if (element.isRounded) RoundedCornerShape(8.dp) else if (element.isEllipse) CircleShape else RectangleShape
                                            )
                                        } else Modifier
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp)

                                val paraCount = element.paragraphs.size
                                val autoFitScale = if (paraCount >= 4 && element.bounds.height < 0.35f) 0.85f else 1.0f
                                val elementScale = scaleFactor * autoFitScale
                                val isSingleTitle = paraCount == 1 && element.bounds.height < 0.22f

                                Box(
                                    modifier = shapeModifier,
                                    contentAlignment = if (isSingleTitle) Alignment.Center else Alignment.TopStart
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = if (isSingleTitle) Arrangement.Center else Arrangement.Top
                                    ) {
                                        for ((pIdx, para) in element.paragraphs.withIndex()) {
                                            val indentSpaces = "   ".repeat(para.indentLevel.coerceIn(0, 5))
                                            val bulletStr = when {
                                                para.isBullet || para.bulletChar != null -> {
                                                    val symbol = para.bulletChar ?: "•"
                                                    "$indentSpaces$symbol "
                                                }
                                                para.indentLevel > 0 -> indentSpaces
                                                else -> ""
                                            }

                                            val firstRun = para.runs.firstOrNull()
                                            val bulletColor = ensureTextLegibility(firstRun?.color, containerBg)
                                            val bulletSize = ((firstRun?.fontSizeSp ?: 14f) * elementScale).coerceIn(8f, 44f).sp

                                            val annotated = buildAnnotatedString {
                                                if (bulletStr.isNotEmpty()) {
                                                    withStyle(
                                                        SpanStyle(
                                                            color = bulletColor,
                                                            fontSize = bulletSize,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = mapFontFamily(firstRun?.fontFamily)
                                                        )
                                                    ) {
                                                        append(bulletStr)
                                                    }
                                                }

                                                for (run in para.runs) {
                                                    val runColor = ensureTextLegibility(run.color, containerBg)
                                                    val isMatch = highlightQuery.length >= 2 && run.text.contains(highlightQuery, ignoreCase = true)
                                                    val scaledSp = (run.fontSizeSp * elementScale).coerceIn(8f, 44f).sp

                                                    val textDec = when {
                                                        run.isUnderline && run.isStrikeThrough -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                                                        run.isUnderline -> TextDecoration.Underline
                                                        run.isStrikeThrough -> TextDecoration.LineThrough
                                                        else -> TextDecoration.None
                                                    }

                                                    withStyle(
                                                        SpanStyle(
                                                            color = runColor,
                                                            fontSize = scaledSp,
                                                            fontWeight = if (run.isBold) FontWeight.Bold else FontWeight.Normal,
                                                            fontStyle = if (run.isItalic) FontStyle.Italic else FontStyle.Normal,
                                                            fontFamily = mapFontFamily(run.fontFamily),
                                                            textDecoration = textDec,
                                                            background = if (isMatch) Color(0x66FFEB3B) else Color.Transparent
                                                        )
                                                    ) {
                                                        append(run.text)
                                                    }
                                                }
                                            }

                                            if (annotated.isNotEmpty()) {
                                                val baseSp = ((firstRun?.fontSizeSp ?: 14f) * elementScale).coerceIn(8f, 44f)
                                                Text(
                                                    text = annotated,
                                                    textAlign = para.alignment,
                                                    lineHeight = (baseSp * 1.25f).sp,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(bottom = if (pIdx < element.paragraphs.size - 1) 3.dp else 0.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            VisualElementType.TABLE -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0x11000000), RoundedCornerShape(4.dp))
                                        .border(1.dp, Color(0x33888888), RoundedCornerShape(4.dp))
                                        .padding(4.dp)
                                ) {
                                    for (row in element.tableRows) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            for (cellText in row) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .border(0.5.dp, Color(0x22888888))
                                                        .padding(2.dp)
                                                ) {
                                                    HighlightableText(
                                                        text = cellText,
                                                        query = highlightQuery,
                                                        fontSize = (10f * scaleFactor).coerceIn(7f, 16f).sp,
                                                        color = ensureTextLegibility(defaultTextColor, slideBg)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Outline mode: Clean bullet-point summary of slide text with keyword highlighting.
 */
@Composable
private fun OutlineSlideCard(
    slide: PptxSlide,
    cardBgColor: Color,
    textColor: Color,
    highlightQuery: String = ""
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val containerAspect = maxWidth / maxHeight
        val slideAspect = if (slide.aspectRatio > 0.1f) slide.aspectRatio else (16f / 9f)
        val cardModifier = if (containerAspect > slideAspect) {
            Modifier
                .fillMaxHeight()
                .aspectRatio(slideAspect)
        } else {
            Modifier
                .fillMaxWidth()
                .aspectRatio(slideAspect)
        }

        Box(
            modifier = cardModifier
                .background(cardBgColor, RoundedCornerShape(8.dp))
                .border(1.dp, Color(0x33888888), RoundedCornerShape(8.dp))
                .padding(AppSpacing.paddingLarge)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                HighlightableText(
                    text = slide.title,
                    query = highlightQuery,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.padding(bottom = AppSpacing.paddingMedium)
                )

                val allPoints = if (slide.content.isNotEmpty()) {
                    slide.content
                } else {
                    slide.visualElements
                        .flatMap { it.paragraphs }
                        .map { it.plainText.trim() }
                        .filter { it.isNotBlank() && it != slide.title }
                }

                if (allPoints.isEmpty()) {
                    Text(
                        text = "No additional bullet points on this slide.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.5f),
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(top = AppSpacing.paddingSmall)
                    )
                } else {
                    for (item in allPoints) {
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Text(
                                text = "• ",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 15.sp
                            )
                            HighlightableText(
                                text = item,
                                query = highlightQuery,
                                fontSize = 15.sp,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Color.isColorDark(): Boolean {
    val lum = 0.299f * red + 0.587f * green + 0.114f * blue
    return lum < 0.5f
}
