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
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ViewCarousel
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
import com.abhisheksati.mydoc.rendering.engine.presentation.LibreOfficePresentationRenderer
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

        val sharedDir = File(context.cacheDir, "shared_presentations").apply { mkdirs() }
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

enum class PresentationEngineMode {
    LIBREOFFICE,
    NATIVE
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

    var activeEngine by remember { mutableStateOf(PresentationEngineMode.NATIVE) }
    var loRenderer by remember { mutableStateOf<LibreOfficePresentationRenderer?>(null) }
    var isLoAvailable by remember { mutableStateOf(false) }
    var loSlideCount by remember { mutableIntStateOf(0) }
    var loAspectRatio by remember { mutableFloatStateOf(16f / 9f) }
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
            loRenderer?.close()
            loRenderer = null
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

                val isLoReady = app?.libreOfficeEngineManager?.isReady() == true
                val isLoEnabled = app?.settingsRepository?.isLibreOfficeEngineEnabled?.value ?: true

                var loSuccess = false
                if (isLoReady && isLoEnabled) {
                    try {
                        val initOk = app?.libreOfficeEngineManager?.initNativeEngine() == true
                        if (initOk) {
                            val renderer = LibreOfficePresentationRenderer()
                            if (renderer.load(tempFile.absolutePath) && renderer.getSlideCount() > 0) {
                                loRenderer = renderer
                                loSlideCount = renderer.getSlideCount()
                                loAspectRatio = renderer.getAspectRatio()
                                isLoAvailable = true
                                activeEngine = PresentationEngineMode.LIBREOFFICE
                                loSuccess = true
                            } else {
                                renderer.close()
                            }
                        }
                    } catch (e: Throwable) {
                        android.util.Log.w("PresentationViewer", "LibreOffice load failed, will try native: ${e.message}")
                    }
                }

                // Parse with native parser (for native view toggle or background keyword search/notes)
                try {
                    val parsed = tempFile.inputStream().use { PptxParser().parse(it, context.cacheDir) }
                    presentationData = parsed
                    if (!loSuccess && parsed.slides.isNotEmpty() && parsed.slides.firstOrNull()?.title != "Empty Presentation") {
                        activeEngine = PresentationEngineMode.NATIVE
                    }
                } catch (e: Throwable) {
                    android.util.Log.w("PresentationViewer", "Native parser failed: ${e.message}")
                }

                // If native failed or had 0 slides, and LibreOffice wasn't initialized yet, try LibreOffice now!
                if (!loSuccess && (presentationData == null || presentationData?.slides.isNullOrEmpty() || presentationData?.slides?.firstOrNull()?.title == "Empty Presentation") && isLoReady) {
                    try {
                        val initOk = app?.libreOfficeEngineManager?.initNativeEngine() == true
                        if (initOk) {
                            val renderer = LibreOfficePresentationRenderer()
                            if (renderer.load(tempFile.absolutePath) && renderer.getSlideCount() > 0) {
                                loRenderer = renderer
                                loSlideCount = renderer.getSlideCount()
                                loAspectRatio = renderer.getAspectRatio()
                                isLoAvailable = true
                                activeEngine = PresentationEngineMode.LIBREOFFICE
                                loSuccess = true
                            } else {
                                renderer.close()
                            }
                        }
                    } catch (_: Throwable) {}
                }

                // If still 0 slides and not loaded in LibreOffice, perform deep raw string/image fallback extraction
                if (!loSuccess && (presentationData == null || presentationData?.slides.isNullOrEmpty() || presentationData?.slides?.firstOrNull()?.title == "Empty Presentation")) {
                    try {
                        val fallback = PptxParser().parseBinaryPptFallback(tempFile.readBytes())
                        if (fallback.slides.isNotEmpty() && fallback.slides.firstOrNull()?.title != "Empty Presentation") {
                            presentationData = fallback
                            activeEngine = PresentationEngineMode.NATIVE
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

    val effectiveSlideCount = if (activeEngine == PresentationEngineMode.LIBREOFFICE && loRenderer != null) {
        loSlideCount
    } else if (!isNativeEmpty) {
        rawSlides.size
    } else {
        0
    }

    val pagerState = rememberPagerState(pageCount = { effectiveSlideCount })

    val effectiveSlides: List<PptxSlide> = remember(activeEngine, rawSlides, loSlideCount, loAspectRatio) {
        if (activeEngine == PresentationEngineMode.LIBREOFFICE && loRenderer != null) {
            (0 until loSlideCount).map { idx ->
                val title = if (!isNativeEmpty) rawSlides.getOrNull(idx)?.title ?: loRenderer?.getPartName(idx) ?: "Slide ${idx + 1}"
                            else loRenderer?.getPartName(idx) ?: "Slide ${idx + 1}"
                val notes = if (!isNativeEmpty) rawSlides.getOrNull(idx)?.notes else null
                val content = if (!isNativeEmpty) rawSlides.getOrNull(idx)?.content ?: emptyList() else emptyList()
                PptxSlide(
                    slideNumber = idx + 1,
                    title = title,
                    content = content,
                    visualElements = emptyList(),
                    aspectRatio = loAspectRatio,
                    notes = notes
                )
            }
        } else if (!isNativeEmpty) {
            rawSlides
        } else {
            emptyList()
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
                    }
                }
            }
        }
    }

    // Smart Resume: Persist slide progress on change
    LaunchedEffect(pagerState.currentPage, effectiveSlideCount) {
        if (effectiveSlideCount > 0) {
            withContext(Dispatchers.IO) {
                app?.documentRepository?.updatePageProgress(document.uri, pagerState.currentPage, effectiveSlideCount)
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
    LaunchedEffect(effectiveSlideCount) {
        if (effectiveSlideCount > 0) {
            com.abhisheksati.mydoc.MainActivity.pageTurnEvents.collect { isNext ->
                val target = if (isNext) {
                    (pagerState.currentPage + 1).coerceAtMost(effectiveSlideCount - 1)
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
        ReadingMode.NORMAL -> if (isAppDark) Color(0xFF000000) else colors.background
    }

    val cardBgColor = when (readingMode) {
        ReadingMode.INVERTED_DARK -> Color(0xFF0A0A0C)
        ReadingMode.SEPIA -> Color(0xFFEADBBE)
        ReadingMode.NORMAL -> if (isAppDark) Color(0xFF0A0A0C) else Color.White
    }

    val textColor = when (readingMode) {
        ReadingMode.INVERTED_DARK -> Color(0xFFEDEDED)
        ReadingMode.SEPIA -> Color(0xFF3A3125)
        ReadingMode.NORMAL -> colors.textPrimary
    }

    val secondaryTextColor = when (readingMode) {
        ReadingMode.INVERTED_DARK -> Color(0xFFA0A0A5)
        ReadingMode.SEPIA -> Color(0xFF5A4E3A)
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
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "•",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = secondaryTextColor.copy(alpha = 0.5f)
                                        )
                                        Text(
                                            text = if (activeEngine == PresentationEngineMode.LIBREOFFICE) "LibreOffice HD Engine" else "Native Engine",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = secondaryTextColor
                                        )
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
                                // Engine Switcher Pill (if both LibreOffice and Native parsed)
                                if (isLoAvailable && !isNativeEmpty) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(100.dp))
                                            .background(
                                                if (activeEngine == PresentationEngineMode.LIBREOFFICE)
                                                    Color(0xFF10B981).copy(alpha = 0.12f)
                                                else
                                                    Color(0xFF3B82F6).copy(alpha = 0.12f)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (activeEngine == PresentationEngineMode.LIBREOFFICE)
                                                    Color(0xFF10B981).copy(alpha = 0.35f)
                                                else
                                                    Color(0xFF3B82F6).copy(alpha = 0.35f),
                                                shape = RoundedCornerShape(100.dp)
                                            )
                                            .clickable {
                                                activeEngine = if (activeEngine == PresentationEngineMode.LIBREOFFICE)
                                                    PresentationEngineMode.NATIVE
                                                else
                                                    PresentationEngineMode.LIBREOFFICE
                                            }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (activeEngine == PresentationEngineMode.LIBREOFFICE)
                                                            Color(0xFF10B981)
                                                        else
                                                            Color(0xFF3B82F6)
                                                    )
                                            )
                                            Text(
                                                text = if (activeEngine == PresentationEngineMode.LIBREOFFICE) "LibreOffice HD" else "Native",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 11.sp
                                                ),
                                                color = if (activeEngine == PresentationEngineMode.LIBREOFFICE)
                                                    Color(0xFF10B981)
                                                else
                                                    Color(0xFF3B82F6)
                                            )
                                        }
                                    }
                                }

                                // Search
                                IconButton(onClick = { isSearchOpen = true }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = "Search Slides",
                                        tint = secondaryTextColor
                                    )
                                }

                                // Slideshow Mode
                                IconButton(onClick = { isSlideshowMode = true }) {
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
                                        if (isLoAvailable && !isNativeEmpty) {
                                            DropdownMenuItem(
                                                text = {
                                                    Text(if (activeEngine == PresentationEngineMode.LIBREOFFICE) "Switch to Native Fast View" else "Switch to LibreOffice HD Engine")
                                                },
                                                leadingIcon = {
                                                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                                                },
                                                onClick = {
                                                    isMenuOpen = false
                                                    activeEngine = if (activeEngine == PresentationEngineMode.LIBREOFFICE)
                                                        PresentationEngineMode.NATIVE
                                                    else
                                                        PresentationEngineMode.LIBREOFFICE
                                                }
                                            )
                                        }
                                        if (activeEngine == PresentationEngineMode.NATIVE) {
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
                                        }
                                        DropdownMenuItem(
                                            text = {
                                                Text(when (readingMode) {
                                                    ReadingMode.NORMAL -> "Theme: Dark"
                                                    ReadingMode.INVERTED_DARK -> "Theme: Sepia"
                                                    ReadingMode.SEPIA -> "Theme: Light"
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
                                                    ReadingMode.SEPIA -> ReadingMode.NORMAL
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
                                items(effectiveSlideCount) { index ->
                                    val isSelected = index == pagerState.currentPage
                                    if (activeEngine == PresentationEngineMode.LIBREOFFICE && loRenderer != null) {
                                        LibreOfficeThumbnail(
                                            renderer = loRenderer!!,
                                            slideIndex = index,
                                            isSelected = isSelected,
                                            readingMode = readingMode,
                                            cardBgColor = cardBgColor,
                                            onClick = {
                                                scope.launch { pagerState.animateScrollToPage(index) }
                                            }
                                        )
                                    } else {
                                        val slide = effectiveSlides.getOrNull(index)
                                        Box(
                                            modifier = Modifier
                                                .width(96.dp)
                                                .height(58.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(cardBgColor)
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(10.dp)
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
                                                    text = slide?.title ?: "Slide ${index + 1}",
                                                    fontSize = 9.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = secondaryTextColor
                                                )
                                            }
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

                        IconButton(onClick = { isThumbnailStripOpen = !isThumbnailStripOpen }) {
                            Icon(
                                imageVector = Icons.Outlined.ViewCarousel,
                                contentDescription = "Toggle Thumbnails",
                                tint = if (isThumbnailStripOpen) MaterialTheme.colorScheme.primary else secondaryTextColor
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(cardBgColor)
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(100.dp))
                                .clickable { isJumpDialogOpen = true }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Slide ${pagerState.currentPage + 1} of $effectiveSlideCount",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = textColor
                            )
                        }

                        val currentSlide = effectiveSlides.getOrNull(pagerState.currentPage)
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

                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (pagerState.currentPage < effectiveSlideCount - 1) {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            },
                            enabled = pagerState.currentPage < effectiveSlideCount - 1
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                contentDescription = "Next Slide",
                                tint = if (pagerState.currentPage < effectiveSlideCount - 1) textColor else secondaryTextColor.copy(alpha = 0.3f)
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
                val isLoReady = app?.libreOfficeEngineManager?.isReady() == true
                PresentationEmptyRecoveryCard(
                    isLoReady = isLoReady,
                    cardBgColor = cardBgColor,
                    textColor = textColor,
                    secondaryTextColor = secondaryTextColor,
                    onForceLibreOffice = {
                        cachedFileOnDisk?.let { file ->
                            scope.launch(Dispatchers.IO) {
                                isLoading = true
                                var loSuccess = false
                                try {
                                    val initOk = app?.libreOfficeEngineManager?.initNativeEngine() == true
                                    if (initOk) {
                                        val renderer = LibreOfficePresentationRenderer()
                                        if (renderer.load(file.absolutePath) && renderer.getSlideCount() > 0) {
                                            loRenderer = renderer
                                            loSlideCount = renderer.getSlideCount()
                                            loAspectRatio = renderer.getAspectRatio()
                                            isLoAvailable = true
                                            activeEngine = PresentationEngineMode.LIBREOFFICE
                                            loSuccess = true
                                        }
                                    }
                                } catch (_: Throwable) {}

                                if (!loSuccess) {
                                    // Try deep native fallback so user is never stuck
                                    try {
                                        val fallback = PptxParser().parseBinaryPptFallback(file.readBytes())
                                        if (fallback.slides.isNotEmpty() && fallback.slides.firstOrNull()?.title != "Empty Presentation") {
                                            presentationData = fallback
                                            activeEngine = PresentationEngineMode.NATIVE
                                            loSuccess = true
                                        }
                                    } catch (_: Throwable) {}

                                    if (!loSuccess) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Opening in external presentation viewer...", Toast.LENGTH_SHORT).show()
                                            openPresentationInOfficeApp(context, document)
                                        }
                                    }
                                }
                                isLoading = false
                            }
                        }
                    },
                    onOpenExternal = {
                        openPresentationInOfficeApp(context, document)
                    }
                )
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
                        if (activeEngine == PresentationEngineMode.LIBREOFFICE && loRenderer != null) {
                            LibreOfficeSlideCard(
                                renderer = loRenderer!!,
                                slideIndex = pageIndex,
                                readingMode = readingMode,
                                cardBgColor = cardBgColor,
                                aspectRatio = loAspectRatio
                            )
                        } else {
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
                pageCount = effectiveSlideCount,
                onJump = { page ->
                    scope.launch { pagerState.scrollToPage(page) }
                },
                onDismiss = { isJumpDialogOpen = false }
            )

            // Speaker Notes Bottom Sheet
            if (isNotesSheetOpen) {
                val curNotes = effectiveSlides.getOrNull(pagerState.currentPage)?.notes ?: "No speaker notes for this slide."
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

@Composable
fun LibreOfficeSlideCard(
    renderer: LibreOfficePresentationRenderer,
    slideIndex: Int,
    readingMode: ReadingMode,
    cardBgColor: Color,
    aspectRatio: Float,
    modifier: Modifier = Modifier
) {
    var slideBitmap by remember(slideIndex, readingMode) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isSlideLoading by remember(slideIndex, readingMode) { mutableStateOf(true) }

    LaunchedEffect(slideIndex, readingMode) {
        isSlideLoading = true
        withContext(Dispatchers.IO) {
            val bmp = renderer.renderSlide(
                slideIndex = slideIndex,
                targetWidth = 1920,
                targetHeight = (1920 / aspectRatio.coerceAtLeast(0.5f)).toInt(),
                mode = readingMode
            )
            withContext(Dispatchers.Main) {
                slideBitmap = bmp
                isSlideLoading = false
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val containerAspect = if (maxHeight.value > 0f && maxWidth.value.isFinite() && maxHeight.value.isFinite()) {
            maxWidth / maxHeight
        } else (16f / 9f)
        val slideAspect = if (aspectRatio.isFinite() && aspectRatio > 0.1f) aspectRatio else (16f / 9f)
        val cardModifier = if (containerAspect > slideAspect) {
            Modifier
                .fillMaxHeight()
                .aspectRatio(slideAspect)
        } else {
            Modifier
                .fillMaxWidth()
                .aspectRatio(slideAspect)
        }

        // Double-Bezel Hardware Architecture (Outer shell + concentric inner presentation canvas)
        Box(
            modifier = cardModifier
                .clip(RoundedCornerShape(24.dp))
                .background(cardBgColor.copy(alpha = 0.5f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(cardBgColor)
                    .border(0.5.dp, Color.Black.copy(alpha = 0.06f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (slideBitmap != null && !slideBitmap!!.isRecycled) {
                    Image(
                        bitmap = slideBitmap!!.asImageBitmap(),
                        contentDescription = "Slide ${slideIndex + 1}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else if (isSlideLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "Slide ${slideIndex + 1}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun LibreOfficeThumbnail(
    renderer: LibreOfficePresentationRenderer,
    slideIndex: Int,
    isSelected: Boolean,
    readingMode: ReadingMode,
    cardBgColor: Color,
    onClick: () -> Unit
) {
    var thumbBitmap by remember(slideIndex, readingMode) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(slideIndex, readingMode) {
        withContext(Dispatchers.IO) {
            val bmp = renderer.renderSlide(slideIndex, 240, 135, readingMode)
            withContext(Dispatchers.Main) {
                thumbBitmap = bmp
            }
        }
    }

    Box(
        modifier = Modifier
            .width(96.dp)
            .height(58.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(cardBgColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (thumbBitmap != null && !thumbBitmap!!.isRecycled) {
            Image(
                bitmap = thumbBitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = "${slideIndex + 1}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
            )
        }
    }
}

@Composable
private fun PresentationEmptyRecoveryCard(
    isLoReady: Boolean,
    cardBgColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    onForceLibreOffice: () -> Unit,
    onOpenExternal: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Double-Bezel Hardware Architecture (Outer shell + concentric inner presentation canvas)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(32.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(cardBgColor)
                .border(0.5.dp, Color.Black.copy(alpha = 0.06f), RoundedCornerShape(26.dp))
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Eyebrow tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "PRESENTATION ENGINE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.5.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isLoReady) "Render with LibreOffice HD" else "Desktop Engine Available",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                    color = textColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isLoReady)
                        "This presentation has complex or proprietary slides. Tap below to render with the embedded LibreOffice engine."
                    else
                        "Install the optional LibreOffice Core Engine in Settings for 100% desktop rendering fidelity on all presentation formats.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryTextColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Button-in-Button CTA
                if (isLoReady) {
                    ButtonInButtonCta(
                        text = "Render with LibreOffice",
                        icon = Icons.Outlined.Refresh,
                        onClick = onForceLibreOffice
                    )
                } else {
                    ButtonInButtonCta(
                        text = "Open in External App",
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                        onClick = onOpenExternal
                    )
                }

                if (isLoReady) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Open with PowerPoint or Google Slides",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .clickable(onClick = onOpenExternal)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ButtonInButtonCta(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .padding(start = 22.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = Color.White
            )
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
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
    val safeBg = if (containerBg.isSpecified) containerBg else Color.White
    val isContainerDark = safeBg.isColorDark()
    if (rawColor == null || !rawColor.isSpecified || rawColor == Color.Transparent) {
        return if (isContainerDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    }
    val bgLum = (0.299f * safeBg.red + 0.587f * safeBg.green + 0.114f * safeBg.blue)
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
                val endIdx = minOf(matchIdx + query.length, text.length)
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
                    append(text.substring(matchIdx, endIdx))
                }
                searchStart = endIdx
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
        val containerAspect = if (maxHeight.value > 0f && maxWidth.value.isFinite() && maxHeight.value.isFinite()) {
            maxWidth / maxHeight
        } else (16f / 9f)
        val slideAspect = if (slide.aspectRatio.isFinite() && slide.aspectRatio > 0.1f) slide.aspectRatio else (16f / 9f)
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
                .clip(RoundedCornerShape(24.dp))
                .background(cardBgColor.copy(alpha = 0.5f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(slideBg, RoundedCornerShape(18.dp))
                    .border(0.5.dp, Color.Black.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val totalWidth = maxWidth
                val totalHeight = maxHeight
                val referenceHeight = 540f
                val rawScale = if (totalHeight.value.isFinite() && totalHeight.value > 0f) totalHeight.value / referenceHeight else 1f
                val scaleFactor = rawScale.coerceIn(0.55f, 2.5f)

                if (slide.visualElements.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(AppSpacing.paddingLarge),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (slide.title.isNotEmpty()) {
                            Text(
                                text = slide.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = defaultTextColor,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        for (line in slide.content) {
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodyLarge,
                                color = defaultTextColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                } else {
                    for (element in slide.visualElements) {
                        val bLeft = if (element.bounds.left.isFinite()) element.bounds.left.coerceIn(0f, 1f) else 0f
                        val bTop = if (element.bounds.top.isFinite()) element.bounds.top.coerceIn(0f, 1f) else 0f
                        val bWidth = if (element.bounds.width.isFinite()) element.bounds.width.coerceIn(0.001f, 1f) else 0.5f
                        val bHeight = if (element.bounds.height.isFinite()) element.bounds.height.coerceIn(0.001f, 1f) else 0.5f

                        val elementWidth = (totalWidth * bWidth).coerceAtLeast(1.dp)
                        val elementHeight = (totalHeight * bHeight).coerceAtLeast(1.dp)
                        val offsetX = totalWidth * bLeft
                        val offsetY = totalHeight * bTop
                        val isText = element.type == VisualElementType.TEXT_BOX || (element.type == VisualElementType.SHAPE && element.paragraphs.isNotEmpty())

                        Box(
                            modifier = Modifier
                                .offset(x = offsetX, y = offsetY)
                                .then(
                                    if (isText) {
                                        Modifier.width(elementWidth).heightIn(min = elementHeight)
                                    } else {
                                        Modifier.size(width = elementWidth, height = elementHeight).clipToBounds()
                                    }
                                )
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

                                    val shapeModifier = (if (isText) Modifier.fillMaxWidth().heightIn(min = elementHeight) else Modifier.fillMaxSize())
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
                                            modifier = if (isText) Modifier.fillMaxWidth() else Modifier.fillMaxSize(),
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
        val containerAspect = if (maxHeight.value > 0f && maxWidth.value.isFinite() && maxHeight.value.isFinite()) {
            maxWidth / maxHeight
        } else (16f / 9f)
        val slideAspect = if (slide.aspectRatio.isFinite() && slide.aspectRatio > 0.1f) slide.aspectRatio else (16f / 9f)
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
                .clip(RoundedCornerShape(24.dp))
                .background(cardBgColor.copy(alpha = 0.5f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(cardBgColor, RoundedCornerShape(18.dp))
                    .border(0.5.dp, Color.Black.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
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
}

private fun Color.isColorDark(): Boolean {
    if (!this.isSpecified) return false
    val lum = 0.299f * red + 0.587f * green + 0.114f * blue
    return lum < 0.5f
}
