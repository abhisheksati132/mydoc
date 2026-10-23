package com.abhisheksati.mydoc.ui.viewer.controls

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.domain.model.ReadingMode
import com.abhisheksati.mydoc.domain.model.ReadingPrefs
import com.abhisheksati.mydoc.rendering.engine.OutlineItem
import com.abhisheksati.mydoc.ui.home.DocumentDetailsDialog

/**
 * Declares capabilities supported by a specific document format viewer.
 */
data class ViewerCapabilities(
    val supportsSearch: Boolean = true,
    val supportsOutline: Boolean = true,
    val supportsPageGrid: Boolean = true,
    val supportsSlideshow: Boolean = false,
    val supportsTts: Boolean = false,
    val supportsZoom: Boolean = true,
    val supportsPdfTools: Boolean = false,
    val supportsOcr: Boolean = false
)

/**
 * Standard colors for reading modes, unifying TextViewer, WordViewer, and PdfViewer.
 */
object ViewerThemeTokens {
    fun getBackgroundColor(readingMode: ReadingMode, isDarkTheme: Boolean, baseSurface: Color): Color {
        return when (readingMode) {
            ReadingMode.INVERTED_DARK -> Color.Black
            ReadingMode.SEPIA -> Color(0xFFF4ECD8)
            ReadingMode.EYE_COMFORT -> Color(0xFFF7F3E9)
            ReadingMode.NORMAL -> if (isDarkTheme) Color(0xFF000000) else baseSurface
        }
    }

    fun getTextColor(readingMode: ReadingMode, defaultPrimary: Color): Color {
        return when (readingMode) {
            ReadingMode.INVERTED_DARK -> Color.White
            ReadingMode.SEPIA -> Color(0xFF3A3125)
            ReadingMode.EYE_COMFORT -> Color(0xFF2C261F)
            ReadingMode.NORMAL -> defaultPrimary
        }
    }

    fun getSecondaryTextColor(readingMode: ReadingMode, defaultSecondary: Color): Color {
        return when (readingMode) {
            ReadingMode.INVERTED_DARK -> Color(0xFFAAAAAA)
            ReadingMode.SEPIA -> Color(0xFF5A4E3A)
            ReadingMode.EYE_COMFORT -> Color(0xFF5C5245)
            ReadingMode.NORMAL -> defaultSecondary
        }
    }
}

/**
 * Unified viewer chrome scaffold applied across ALL formats (PDF, DOCX, XLSX, TXT, etc.).
 * Includes:
 * 1. ViewerTopBar with Back, middle-truncated title, page subtitle, Search, Bookmark, and Overflow menu.
 * 2. SearchOverlay slot.
 * 3. Bottom controls (ViewerBottomBar) with scrubber, floating bubble, and labeled actions.
 * 4. Appearance bottom sheet (ReadingControlsBottomSheet).
 * 5. DocumentDetailsDialog.
 * 6. System bars immersion (transient swipe) with restore on exit.
 * 7. TTS bar and password dialog slots.
 */
@Composable
fun ViewerScaffold(
    document: DocumentFile,
    capabilities: ViewerCapabilities = ViewerCapabilities(),
    currentPage: Int = 0,
    pageCount: Int = 1,
    isControlsVisible: Boolean = true,
    onControlsVisibilityChange: ((Boolean) -> Unit)? = null,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    onSearchClick: (() -> Unit)? = null,
    isBookmarked: Boolean = false,
    onToggleBookmark: (() -> Unit)? = null,
    onOutlineClick: (() -> Unit)? = null,
    onPagesClick: (() -> Unit)? = null,
    onPageScrubbed: ((Int) -> Unit)? = null,
    sheetItems: List<OutlineItem> = emptyList(),
    onSheetSelect: ((Int) -> Unit)? = null,
    readingPrefs: ReadingPrefs = ReadingPrefs(),
    onReadingPrefsChange: ((ReadingPrefs) -> Unit)? = null,
    onToolsClick: (() -> Unit)? = null,
    onOcrClick: (() -> Unit)? = null,
    onTtsClick: (() -> Unit)? = null,
    onSlideshowClick: (() -> Unit)? = null,
    searchOverlay: @Composable (BoxScope.() -> Unit)? = null,
    ttsBar: @Composable (BoxScope.() -> Unit)? = null,
    passwordDialog: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as? MyDocApp
    val colors = LocalAppColors.current
    val isDark = colors.background == DarkAppColors.background

    var isAppearanceSheetOpen by remember { mutableStateOf(false) }
    var isDocumentDetailsOpen by remember { mutableStateOf(false) }

    // Reading immersion: hide system bars when controls are hidden
    val activity = context as? Activity
    val window = activity?.window
    val insetsController = remember(window) {
        if (window != null) {
            WindowCompat.getInsetsController(window, window.decorView).apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else null
    }

    LaunchedEffect(isControlsVisible, insetsController) {
        if (insetsController != null) {
            if (isControlsVisible) {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    DisposableEffect(insetsController) {
        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Keep screen on
    val currentView = LocalView.current
    DisposableEffect(readingPrefs.keepScreenOn) {
        currentView.keepScreenOn = readingPrefs.keepScreenOn
        onDispose {
            currentView.keepScreenOn = false
        }
    }

    // Screen brightness
    DisposableEffect(readingPrefs.brightness) {
        val lp = activity?.window?.attributes
        if (lp != null && readingPrefs.brightness >= 0f) {
            lp.screenBrightness = readingPrefs.brightness
            activity.window.attributes = lp
        }
        onDispose {
            val resetLp = activity?.window?.attributes
            if (resetLp != null) {
                resetLp.screenBrightness = -1f
                activity.window.attributes = resetLp
            }
        }
    }

    val backgroundColor = ViewerThemeTokens.getBackgroundColor(readingPrefs.readingMode, isDark, colors.surface)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Main viewer content slot
        content()

        // Top Bar
        ViewerTopBar(
            title = document.displayName,
            currentPage = currentPage,
            pageCount = pageCount,
            visible = isControlsVisible,
            isBookmarked = isBookmarked,
            onBackClick = onBackClick,
            onSearchClick = { onSearchClick?.invoke() },
            onToggleBookmark = {
                if (onToggleBookmark != null) {
                    onToggleBookmark()
                } else {
                    app?.settingsRepository?.toggleBookmark(document.uri, currentPage)
                }
            },
            onShareClick = onShareClick,
            onToolsClick = if (capabilities.supportsPdfTools) onToolsClick else null,
            onOcrClick = if (capabilities.supportsOcr) onOcrClick else null,
            onTtsClick = if (capabilities.supportsTts) onTtsClick else null,
            onSlideshowClick = if (capabilities.supportsSlideshow) onSlideshowClick else null,
            onDocumentDetailsClick = { isDocumentDetailsOpen = true },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Search overlay slot
        if (searchOverlay != null) {
            searchOverlay()
        }

        // Bottom Bar
        ViewerBottomBar(
            currentPage = currentPage,
            pageCount = pageCount,
            visible = isControlsVisible,
            hasOutline = capabilities.supportsOutline && sheetItems.isNotEmpty(),
            onOutlineClick = { onOutlineClick?.invoke() },
            onPagesClick = { onPagesClick?.invoke() },
            onAppearanceClick = { isAppearanceSheetOpen = true },
            onPageScrubbed = { targetPage -> onPageScrubbed?.invoke(targetPage) },
            sheetItems = sheetItems,
            onSheetSelect = onSheetSelect,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Floating TTS controller slot
        if (ttsBar != null) {
            ttsBar()
        }

        // Appearance bottom sheet
        ReadingControlsBottomSheet(
            isOpen = isAppearanceSheetOpen,
            onDismiss = { isAppearanceSheetOpen = false },
            readingMode = readingPrefs.readingMode,
            onReadingModeChange = { newMode ->
                val updated = readingPrefs.copy(readingMode = newMode)
                onReadingPrefsChange?.invoke(updated)
                app?.settingsRepository?.updateReadingPrefs { it.copy(readingMode = newMode) }
            },
            isContinuousScroll = readingPrefs.isContinuousScroll,
            onScrollModeChange = { continuous ->
                val updated = readingPrefs.copy(isContinuousScroll = continuous)
                onReadingPrefsChange?.invoke(updated)
                app?.settingsRepository?.updateReadingPrefs { it.copy(isContinuousScroll = continuous) }
            },
            isDualPageView = readingPrefs.isDualPageView,
            onDualPageViewChange = { dual ->
                val updated = readingPrefs.copy(isDualPageView = dual)
                onReadingPrefsChange?.invoke(updated)
                app?.settingsRepository?.updateReadingPrefs { it.copy(isDualPageView = dual) }
            },
            currentBrightness = readingPrefs.brightness,
            onBrightnessChange = { newBrightness ->
                val updated = readingPrefs.copy(brightness = newBrightness)
                onReadingPrefsChange?.invoke(updated)
                app?.settingsRepository?.updateReadingPrefs { it.copy(brightness = newBrightness) }
                val lp = activity?.window?.attributes
                if (lp != null) {
                    lp.screenBrightness = newBrightness
                    activity.window.attributes = lp
                }
            },
            isKeepScreenOn = readingPrefs.keepScreenOn,
            onKeepScreenOnChange = { keepOn ->
                val updated = readingPrefs.copy(keepScreenOn = keepOn)
                onReadingPrefsChange?.invoke(updated)
                app?.settingsRepository?.updateReadingPrefs { it.copy(keepScreenOn = keepOn) }
            },
            document = document,
            isFitPage = readingPrefs.isFitPage,
            onFitPageChange = { fitPage ->
                val updated = readingPrefs.copy(isFitPage = fitPage)
                onReadingPrefsChange?.invoke(updated)
                app?.settingsRepository?.updateReadingPrefs { it.copy(isFitPage = fitPage) }
            }
        )

        // Document Details Dialog
        if (isDocumentDetailsOpen) {
            DocumentDetailsDialog(
                document = document,
                onDismiss = { isDocumentDetailsOpen = false },
                onOpen = { isDocumentDetailsOpen = false },
                onShare = { onShareClick() },
                onRemove = { isDocumentDetailsOpen = false }
            )
        }

        // Password Dialog slot
        if (passwordDialog != null) {
            passwordDialog()
        }
    }
}
