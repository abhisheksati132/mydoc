package com.abhisheksati.mydoc.ui.viewer

import com.abhisheksati.mydoc.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
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
 *
 * The disclosure is surfaced in Document details rather than as a floating strip over the page,
 * keeping the reading canvas free of chrome.
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