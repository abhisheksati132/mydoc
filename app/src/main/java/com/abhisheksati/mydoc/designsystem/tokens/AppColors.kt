package com.abhisheksati.mydoc.designsystem.tokens

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import com.abhisheksati.mydoc.domain.model.MediaKind

/**
 * The rim every glass surface, sheet, dialog and menu draws.
 *
 * On ivory a directional specular reads as glass: lit along the top, gone by mid-face, a whisper
 * at the bottom. On dark and AMOLED that same fade leaves a bright top edge and no bottom edge,
 * so every box looks half-drawn and the top line leaks across whatever sits behind the chrome.
 * There the rim is one even hairline around the whole shape.
 */
fun glassRimBrush(
    colors: AppColors,
    isDark: Boolean,
    top: Color = colors.glassRimTop,
    bottom: Color = colors.glassRimBottom
): Brush = if (isDark) {
    SolidColor(colors.line)
} else {
    Brush.verticalGradient(
        colorStops = arrayOf(
            0f to top,
            0.55f to Color.Transparent,
            1f to bottom.copy(alpha = bottom.alpha * 0.5f)
        )
    )
}

/**
 * Single source of truth for MyDoc color. "Paper under glass" design identity:
 * - Deep ink-navy dark theme (hero identity)
 * - Warm ivory paper light theme
 * - ONE accent color (Seal teal) used strictly for progress, primary actions, and selection
 *
 * Chrome surfaces (toolbars, pills, sheets) use refractive glass tokens.
 * Documents, preview tiles, and reading sheets use physical paper tokens.
 */
@Immutable
data class AppColors(
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val line: Color,
    val ink: Color,
    val inkMuted: Color,
    val highlight: Color,
    val accent: Color,
    val onAccent: Color,
    val accentPink: Color = Color(0xFFD94324), // Seal teal accent variant
    val accentCyan: Color = Color(0xFF2C4A7F), // Ink navy variant (legacy alias)
    val accentAmber: Color = Color(0xFFD97706),
    val accentGreen: Color = Color(0xFF286B49),

    // MediaKind Color Tokens (Harmonious editorial tags)
    val documentKind: Color,
    val onDocumentKind: Color,
    val documentContainer: Color,

    val photoKind: Color,
    val onPhotoKind: Color,
    val photoContainer: Color,

    val videoKind: Color,
    val onVideoKind: Color,
    val videoContainer: Color,

    val audioKind: Color,
    val onAudioKind: Color,
    val audioContainer: Color,

    // Glass System Tokens
    val glassTint: Color,
    val glassRimTop: Color,
    val glassRimBottom: Color,
    val glassInnerShadow: Color,
    val paperGrainColor: Color,

    // Semantic mappings
    val textPrimary: Color = ink,
    val inkPrimary: Color = ink,
    val textSecondary: Color = inkMuted,
    val textSubtle: Color = inkMuted.copy(alpha = 0.72f),
    val inkSecondary: Color = inkMuted,
    val surfaceElevated: Color = surface,
    val divider: Color = line,
    val cardBorder: Color = line,
    val track: Color = ink.copy(alpha = 0.12f),
    val accentVariant: Color = accent.copy(alpha = 0.14f),
    val accentContainer: Color = accent.copy(alpha = 0.14f),
    val error: Color = Color(0xFFD94324),
    val warning: Color = Color(0xFFD97706),
    val scrim: Color = Color(0x99000000),

    // Container mappings
    val primaryContainer: Color = surfaceMuted,
    val onPrimaryContainer: Color = ink,
    val secondaryContainer: Color = surfaceMuted,
    val onSecondaryContainer: Color = ink,
    val surfaceContainer: Color = surface,
    val surfaceVariant: Color = surfaceMuted,
    val surfaceContainerHigh: Color = surfaceMuted,
    val surfaceContainerLow: Color = surface,
    val outline: Color = line,
    val outlineVariant: Color = line
) {
    fun kindColor(kind: MediaKind): Color = when (kind) {
        MediaKind.DOCUMENT -> documentKind
        MediaKind.PHOTO -> photoKind
        MediaKind.VIDEO -> videoKind
        MediaKind.AUDIO -> audioKind
    }

    fun kindOnColor(kind: MediaKind): Color = when (kind) {
        MediaKind.DOCUMENT -> onDocumentKind
        MediaKind.PHOTO -> onPhotoKind
        MediaKind.VIDEO -> onVideoKind
        MediaKind.AUDIO -> onAudioKind
    }

    fun kindContainer(kind: MediaKind): Color = when (kind) {
        MediaKind.DOCUMENT -> documentContainer
        MediaKind.PHOTO -> photoContainer
        MediaKind.VIDEO -> videoContainer
        MediaKind.AUDIO -> audioContainer
    }

    companion object {
        /** Folds a Material 3 scheme into the Apple Liquid Glass token model. */
        fun fromMaterialScheme(scheme: ColorScheme, isDark: Boolean = false): AppColors = AppColors(
            background = scheme.background,
            surface = scheme.surface,
            surfaceMuted = scheme.surfaceContainerHigh,
            line = scheme.outlineVariant,
            ink = scheme.onBackground,
            inkMuted = scheme.onSurfaceVariant,
            highlight = scheme.primaryContainer,
            accent = scheme.primary,
            onAccent = scheme.onPrimary,
            accentPink = if (isDark) Color(0xFFC56A5E) else Color(0xFF8C3B34),
            accentCyan = if (isDark) Color(0xFF7C9CC0) else Color(0xFF33506E),
            accentAmber = if (isDark) Color(0xFFC79A55) else Color(0xFFA67C3D),
            accentGreen = if (isDark) Color(0xFF7FA98B) else Color(0xFF3A5A44),
            documentKind = if (isDark) Color(0xFFC56A5E) else Color(0xFF8C3B34),
            onDocumentKind = if (isDark) Color(0xFF2A0F0C) else Color(0xFFFFFFFF),
            documentContainer = if (isDark) Color(0xFF3A1D1B) else Color(0xFFF0E1DE),
            photoKind = if (isDark) Color(0xFF9C8CB0) else Color(0xFF5B4E6B),
            onPhotoKind = if (isDark) Color(0xFF1E1729) else Color(0xFFFFFFFF),
            photoContainer = if (isDark) Color(0xFF241F2E) else Color(0xFFE7E2EE),
            videoKind = if (isDark) Color(0xFFC0905A) else Color(0xFF8A5A2E),
            onVideoKind = if (isDark) Color(0xFF2A1B0A) else Color(0xFFFFFFFF),
            videoContainer = if (isDark) Color(0xFF3A2A10) else Color(0xFFF0E6D8),
            audioKind = if (isDark) Color(0xFF6FA6A6) else Color(0xFF2F5A5A),
            onAudioKind = if (isDark) Color(0xFF06231F) else Color(0xFFFFFFFF),
            audioContainer = if (isDark) Color(0xFF10353D) else Color(0xFFDCEAE9),
            // Low-alpha tint over a transparent Haze base: content supplies the
            // color (vibrancy); the tint only densifies the glass slightly.
            glassTint = scheme.surfaceContainerHighest.copy(alpha = if (isDark) 0.42f else 0.55f),
            // White specular top arc in light (reflects light, not a dark stroke);
            // bottom stop is halved again inside GlassSurface.
            glassRimTop = if (isDark) Color(0x38FFFFFF) else Color(0x59FFFFFF),
            glassRimBottom = if (isDark) Color(0x20FFFFFF) else Color(0x14000000),
            glassInnerShadow = if (isDark) Color(0x0F000000) else Color(0x08000000),
            paperGrainColor = Color(0x00000000),
            textPrimary = scheme.onBackground,
            textSecondary = scheme.onSurfaceVariant,
            divider = scheme.outlineVariant,
            cardBorder = scheme.outlineVariant,
            track = scheme.surfaceVariant,
            accentVariant = scheme.primary.copy(alpha = 0.16f),
            error = scheme.error,
            primaryContainer = scheme.primaryContainer,
            onPrimaryContainer = scheme.onPrimaryContainer,
            secondaryContainer = scheme.secondaryContainer,
            onSecondaryContainer = scheme.onSecondaryContainer,
            surfaceContainer = scheme.surfaceContainer,
            surfaceVariant = scheme.surfaceVariant,
            surfaceContainerHigh = scheme.surfaceContainerHigh,
            surfaceContainerLow = scheme.surfaceContainerLow,
            outline = scheme.outline,
            outlineVariant = scheme.outlineVariant
        )
    }
}

/** MyDoc "Ink & Seal" — quiet luxury: warm paper chrome, one Seal-teal accent, one brass micro-accent. */
val LightAppColors = AppColors(
    background = Color(0xFFF5F3EE),        // warm bone canvas
    surface = Color(0xFFFCFBF8),           // warm near-white cards
    surfaceMuted = Color(0xFFEDEAE2),      // warm stone wells
    line = Color(0xFFDEDACF),              // warm hairline
    ink = Color(0xFF1A1712),               // warm near-black label
    inkMuted = Color(0xFF6B6558),          // warm taupe secondary
    highlight = Color(0xFFDCEAE7),         // Seal tint
    accent = Color(0xFF0E5C57),            // Seal teal
    onAccent = Color(0xFFFFFFFF),
    accentPink = Color(0xFF8C3B34),        // brick (muted)
    accentCyan = Color(0xFF33506E),        // ink blue (muted)
    accentAmber = Color(0xFFA67C3D),       // brass
    accentGreen = Color(0xFF3A5A44),       // forest (muted)

    documentKind = Color(0xFF8C3B34),      // PDF — brick
    onDocumentKind = Color(0xFFFFFFFF),
    documentContainer = Color(0xFFF0E1DE),

    photoKind = Color(0xFF5B4E6B),         // photo — muted violet
    onPhotoKind = Color(0xFFFFFFFF),
    photoContainer = Color(0xFFE7E2EE),

    videoKind = Color(0xFF8A5A2E),         // video — clay
    onVideoKind = Color(0xFFFFFFFF),
    videoContainer = Color(0xFFF0E6D8),

    audioKind = Color(0xFF2F5A5A),         // audio — deep petrol
    onAudioKind = Color(0xFFFFFFFF),
    audioContainer = Color(0xFFDCEAE9),

    glassTint = Color(0x8CFCFBF8),         // warm translucent glass
    glassRimTop = Color(0x59FFFFFF),
    glassRimBottom = Color(0x14000000),
    glassInnerShadow = Color(0x08000000),
    paperGrainColor = Color(0x00000000)
)

/** Dark — warm graphite chrome, lifted Seal accent. */
val DarkAppColors = AppColors(
    background = Color(0xFF0F0E0C),        // warm near-black
    surface = Color(0xFF1A1815),           // graphite
    surfaceMuted = Color(0xFF24211C),      // elevated graphite
    line = Color(0xFF332F28),              // warm hairline
    ink = Color(0xFFF2EFE8),               // warm off-white label
    inkMuted = Color(0xFFA8A090),          // warm taupe secondary
    highlight = Color(0xFF123A36),         // Seal tint (dark)
    accent = Color(0xFF4FB0A6),            // Seal teal (lifted)
    onAccent = Color(0xFF06231F),
    accentPink = Color(0xFFC56A5E),
    accentCyan = Color(0xFF7C9CC0),
    accentAmber = Color(0xFFC79A55),       // brass (dark)
    accentGreen = Color(0xFF7FA98B),

    documentKind = Color(0xFFC56A5E),      // PDF
    onDocumentKind = Color(0xFF2A0F0C),
    documentContainer = Color(0xFF3A1D1B),

    photoKind = Color(0xFF9C8CB0),         // photo
    onPhotoKind = Color(0xFF1E1729),
    photoContainer = Color(0xFF241F2E),

    videoKind = Color(0xFFC0905A),         // video
    onVideoKind = Color(0xFF2A1B0A),
    videoContainer = Color(0xFF3A2A10),

    audioKind = Color(0xFF6FA6A6),         // audio
    onAudioKind = Color(0xFF06231F),
    audioContainer = Color(0xFF10353D),

    glassTint = Color(0x6B24211C),         // warm translucent glass (dark)
    glassRimTop = Color(0x38FFFFFF),
    glassRimBottom = Color(0x20FFFFFF),
    glassInnerShadow = Color(0x0F000000),
    paperGrainColor = Color(0x00000000)
)

/** Pure AMOLED zero-nit obsidian variant. */
val BlackAppColors = DarkAppColors.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF12100D),
    surfaceMuted = Color(0xFF1C1A16),
    line = Color(0xFF2A2721),
    ink = Color(0xFFF2EFE8),
    inkMuted = Color(0xFFA8A090),
    highlight = Color(0xFF123A36),
    glassTint = Color(0x6B1C1A16),
    glassRimTop = Color(0x40FFFFFF),
    glassRimBottom = Color(0x26FFFFFF),
    glassInnerShadow = Color(0x0F000000),
    surfaceContainer = Color(0xFF12100D),
    surfaceContainerHigh = Color(0xFF1C1A16),
    surfaceContainerLow = Color(0xFF000000),
    primaryContainer = Color(0xFF1C1A16),
    secondaryContainer = Color(0xFF1C1A16)
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

fun kindColor(kind: MediaKind, isDark: Boolean = false): Color {
    return if (isDark) {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFFC56A5E)
            MediaKind.PHOTO -> Color(0xFF9C8CB0)
            MediaKind.VIDEO -> Color(0xFFC0905A)
            MediaKind.AUDIO -> Color(0xFF6FA6A6)
        }
    } else {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFF8C3B34)
            MediaKind.PHOTO -> Color(0xFF5B4E6B)
            MediaKind.VIDEO -> Color(0xFF8A5A2E)
            MediaKind.AUDIO -> Color(0xFF2F5A5A)
        }
    }
}

fun kindContainer(kind: MediaKind, isDark: Boolean = false): Color {
    return if (isDark) {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFF3A1D1B)
            MediaKind.PHOTO -> Color(0xFF241F2E)
            MediaKind.VIDEO -> Color(0xFF3A2A10)
            MediaKind.AUDIO -> Color(0xFF10353D)
        }
    } else {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFFF0E1DE)
            MediaKind.PHOTO -> Color(0xFFE7E2EE)
            MediaKind.VIDEO -> Color(0xFFF0E6D8)
            MediaKind.AUDIO -> Color(0xFFDCEAE9)
        }
    }
}

fun onKindColor(kind: MediaKind, isDark: Boolean = false): Color {
    return Color(0xFFFFFFFF)
}

fun onKindContainer(kind: MediaKind, isDark: Boolean = false): Color {
    return if (isDark) {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFFE8B4AC)
            MediaKind.PHOTO -> Color(0xFFCBC0DC)
            MediaKind.VIDEO -> Color(0xFFE4C39A)
            MediaKind.AUDIO -> Color(0xFFB4DADA)
        }
    } else {
        when (kind) {
            MediaKind.DOCUMENT -> Color(0xFF5E241F)
            MediaKind.PHOTO -> Color(0xFF3A3148)
            MediaKind.VIDEO -> Color(0xFF5B3A1C)
            MediaKind.AUDIO -> Color(0xFF1E3A3A)
        }
    }
}

/**
 * Editorial Document Format colors.
 * Refined, vibrant format badges designed for modern Liquid Glass contrast.
 */
object DocumentFormatColors {
    val pdfTint = Color(0xFF8C3B34)     // brick
    val wordTint = Color(0xFF33506E)    // ink blue
    val excelTint = Color(0xFF3A5A44)   // forest
    val pptTint = Color(0xFF8A5A2E)     // clay
    val textTint = Color(0xFF5D5A52)    // warm gray
    val mediaTint = Color(0xFF5B4E6B)   // muted violet
    val zipTint = Color(0xFF6B5A3A)     // kraft

    fun getTint(extension: String): Color {
        return when (extension.lowercase()) {
            "pdf" -> pdfTint
            "docx", "doc" -> wordTint
            "xlsx", "xls", "csv" -> excelTint
            "pptx", "ppt" -> pptTint
            "txt", "md", "log", "json", "xml" -> textTint
            "jpg", "jpeg", "png", "webp", "gif", "mp4", "mp3" -> mediaTint
            "zip", "rar", "7z", "tar", "gz" -> zipTint
            else -> textTint
        }
    }

    fun getContainer(extension: String, isDark: Boolean = false): Color {
        return if (isDark) Color(0xFF1E283D) else Color(0xFFF1F5F9)
    }
}
