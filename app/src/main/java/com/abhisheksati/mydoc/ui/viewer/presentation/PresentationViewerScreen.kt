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
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    var isVisualMode by remember { mutableStateOf(true) }
    var isMenuOpen by remember { mutableStateOf(false) }
    var isJumpDialogOpen by remember { mutableStateOf(false) }

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
        ReadingMode.NORMAL -> Color(0xFF1E293B)
    }

    val secondaryTextColor = when (readingMode) {
        ReadingMode.INVERTED_DARK -> Color(0xFFA0A0A5)
        ReadingMode.SEPIA -> Color(0xFF5A4E3A)
        ReadingMode.NORMAL -> Color(0xFF64748B)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier.background(bgColor)
            ) {
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
                            .height(56.dp)
                            .padding(horizontal = AppSpacing.paddingLarge),
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
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = textColor,
                            modifier = Modifier
                                .clickable { isJumpDialogOpen = true }
                                .padding(AppSpacing.paddingSmall)
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
                            .padding(AppSpacing.paddingMedium)
                            .pointerInput(Unit) {
                                detectTapGestures(
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
                        if (isVisualMode && slide.visualElements.isNotEmpty()) {
                            // Rich visual slide rendering
                            VisualSlideCard(
                                slide = slide,
                                cardBgColor = cardBgColor,
                                defaultTextColor = textColor
                            )
                        } else {
                            // Outline / clean text bullet points view
                            OutlineSlideCard(
                                slide = slide,
                                cardBgColor = cardBgColor,
                                textColor = textColor
                            )
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

/**
 * Renders the slide canvas with all its visual elements: images, shapes, diagrams, and tables.
 */
@Composable
private fun VisualSlideCard(
    slide: PptxSlide,
    cardBgColor: Color,
    defaultTextColor: Color
) {
    val slideBg = slide.backgroundColor ?: cardBgColor

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(slide.aspectRatio)
            .background(slideBg, RoundedCornerShape(8.dp))
            .border(1.dp, Color(0x33888888), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val totalWidth = maxWidth
            val totalHeight = maxHeight

            for (element in slide.visualElements) {
                val elementWidth = (totalWidth * element.bounds.width).coerceAtLeast(1.dp)
                val elementHeight = (totalHeight * element.bounds.height).coerceAtLeast(1.dp)
                val offsetX = totalWidth * element.bounds.left
                val offsetY = totalHeight * element.bounds.top

                Box(
                    modifier = Modifier
                        .offset(x = offsetX, y = offsetY)
                        .size(width = elementWidth, height = elementHeight)
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
                            val shapeModifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (element.backgroundColor != null) {
                                        Modifier.background(
                                            color = element.backgroundColor,
                                            shape = if (element.isRounded) RoundedCornerShape(8.dp) else if (element.isEllipse) CircleShape else RectangleShape
                                        )
                                    } else Modifier
                                )
                                .then(
                                    if (element.borderColor != null) {
                                        Modifier.border(
                                            width = 1.dp,
                                            color = element.borderColor,
                                            shape = if (element.isRounded) RoundedCornerShape(8.dp) else if (element.isEllipse) CircleShape else RectangleShape
                                        )
                                    } else Modifier
                                )
                                .padding(4.dp)

                            Box(
                                modifier = shapeModifier,
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    for (para in element.paragraphs) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = when (para.alignment) {
                                                TextAlign.Center -> Arrangement.Center
                                                TextAlign.End -> Arrangement.End
                                                else -> Arrangement.Start
                                            }
                                        ) {
                                            for (run in para.runs) {
                                                val runColor = run.color ?: defaultTextColor
                                                Text(
                                                    text = run.text,
                                                    fontSize = (run.fontSizeSp * 0.85f).coerceIn(9f, 32f).sp,
                                                    fontWeight = if (run.isBold) FontWeight.Bold else FontWeight.Normal,
                                                    fontStyle = if (run.isItalic) FontStyle.Italic else FontStyle.Normal,
                                                    color = runColor,
                                                    textAlign = para.alignment
                                                )
                                            }
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
                                                Text(
                                                    text = cellText,
                                                    fontSize = 10.sp,
                                                    color = defaultTextColor,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
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

/**
 * Outline mode: Clean bullet-point summary of slide text.
 */
@Composable
private fun OutlineSlideCard(
    slide: PptxSlide,
    cardBgColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(slide.aspectRatio)
            .background(cardBgColor, RoundedCornerShape(8.dp))
            .border(1.dp, Color(0x33888888), RoundedCornerShape(8.dp))
            .padding(AppSpacing.paddingLarge)
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
                modifier = Modifier.padding(bottom = AppSpacing.paddingMedium)
            )

            for (item in slide.content) {
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
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
