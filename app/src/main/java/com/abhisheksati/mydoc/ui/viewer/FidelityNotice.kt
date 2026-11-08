package com.abhisheksati.mydoc.ui.viewer

import com.abhisheksati.mydoc.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.delay
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.DocumentFormats
import com.abhisheksati.mydoc.domain.model.FormatDescriptor
import com.abhisheksati.mydoc.domain.model.RenderFidelity

/**
 * What the reader is told about how faithfully their document survived.
 *
 * A format's fidelity is declared in [DocumentFormats]; the engines can still fall below it at
 * runtime (a .docx that only yields a text scrape, a .ppt whose shapes had to be scraped, a sheet
 * cut off at a safety cap). [fidelityDisclosure] combines both so the worst observed level wins,
 * which is the whole point: the registry's promise is never used to cover an engine's fallback.
 */
@Immutable
data class FidelityDisclosure(
    val headline: String,
    val notes: List<String>
)

/**
 * @param observedFidelity the level the engine actually achieved, when it knows. Only downgrades.
 * @param extraNotes runtime gaps the registry cannot know about, phrased for a reader.
 */
fun fidelityDisclosure(
    descriptor: FormatDescriptor?,
    observedFidelity: RenderFidelity? = null,
    extraNotes: List<String> = emptyList()
): FidelityDisclosure? {
    val declared = descriptor?.fidelity
    val effective = when {
        declared == null -> observedFidelity
        observedFidelity != null && observedFidelity.ordinal > declared.ordinal -> observedFidelity
        else -> declared
    } ?: return null

    val headline = fidelityNote(effective)
    val notes = extraNotes + descriptor?.limitations.orEmpty()

    return when {
        headline != null && notes.isNotEmpty() -> FidelityDisclosure(headline, notes)
        headline != null -> FidelityDisclosure(headline, emptyList())
        notes.isNotEmpty() -> FidelityDisclosure(notes.first(), notes.drop(1))
        else -> null
    }
}

fun fidelityDisclosureFor(
    extension: String?,
    observedFidelity: RenderFidelity? = null,
    extraNotes: List<String> = emptyList()
): FidelityDisclosure? =
    fidelityDisclosure(DocumentFormats.forExtension(extension), observedFidelity, extraNotes)

/** Short name of a fidelity level, for the metadata rows in Document details. */
@Composable
fun RenderFidelity.label(): String = when (this) {
    RenderFidelity.FIXED_LAYOUT -> stringResource(R.string.fidelity_full_layout)
    RenderFidelity.STRUCTURED_REFLOW -> stringResource(R.string.fidelity_relaid)
    RenderFidelity.TEXT_ONLY -> stringResource(R.string.fidelity_text_only)
    RenderFidelity.RECOVERED_SCRAPE -> stringResource(R.string.fidelity_recovered)
    RenderFidelity.NONE -> stringResource(R.string.fidelity_not_rendered)
}

/**
 * The one disclosure strip, drawn over every viewer's content area.
 *
 * It carries its own surface because it sits on a document canvas the reader may have tinted
 * sepia or inverted, where plain muted text would vanish. It is chrome, so viewers hide it with
 * the rest of the controls; the complete list of gaps stays in Document details.
 */
@Composable
fun FidelityNotice(
    disclosure: FidelityDisclosure?,
    modifier: Modifier = Modifier,
    dismissalKey: String? = null
) {
    if (disclosure == null) return
    var expanded by rememberSaveable(disclosure, dismissalKey) { mutableStateOf(false) }
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val scrollState = rememberScrollState()
    val accessibility = LocalAccessibilityManager.current
    LaunchedEffect(expanded, disclosure, scrollState.isScrollInProgress) {
        if (expanded && !scrollState.isScrollInProgress) {
            delay(accessibility?.calculateRecommendedTimeoutMillis(3_000, containsText = true) ?: 3_000)
            expanded = false
        }
    }

    androidx.compose.foundation.layout.Box(modifier.padding(horizontal = AppSpacing.paddingSmall)) {
        Surface(shape = AppRadius.cardShape, color = colors.surface) {
            IconButton(onClick = { expanded = !expanded }) {
                Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.rendering_information),
                    tint = colors.accent, modifier = Modifier.size(20.dp))
            }
        }
        if (expanded) Popup(alignment = Alignment.TopStart, onDismissRequest = { expanded = false }) {
            Surface(shape = AppRadius.cardShape, color = colors.surface,
                border = BorderStroke(AppSpacing.hairline, colors.line), shadowElevation = 4.dp) {
                Column(Modifier.widthIn(max = 320.dp).heightIn(max = 320.dp).verticalScroll(scrollState).padding(AppSpacing.paddingMedium),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(disclosure.headline, style = typography.captionStrong, color = colors.textPrimary)
                    disclosure.notes.forEach { note ->
                        Text(note, style = typography.micro, color = colors.textSecondary)
                    }
                }
            }
        }
    }
}
