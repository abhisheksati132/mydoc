package com.abhisheksati.mydoc.ui.viewer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.abhisheksati.mydoc.designsystem.components.AppEmptyState
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.domain.model.DocumentFormats
import com.abhisheksati.mydoc.domain.model.LoadFailure
import com.abhisheksati.mydoc.domain.model.RenderFidelity

/**
 * The one place that explains why a document is not on screen.
 *
 * Every branch names a real cause. Before this, containers with no reader were pushed into an
 * engine that could not parse them and the result was presented as a loaded document — a blank
 * sheet, a fabricated slide, or the raw markup of an RTF file.
 */
@Composable
fun DocumentProblemScreen(
    failure: LoadFailure,
    extension: String?,
    onBackClick: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val copy = problemCopy(failure, extension)
    AppEmptyState(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(AppSpacing.paddingLarge),
        title = copy.title,
        message = copy.message,
        actionLabel = copy.actionLabel,
        onAction = onRetry ?: onBackClick
    )
}

private class ProblemCopy(val title: String, val message: String, val actionLabel: String)

private fun problemCopy(failure: LoadFailure, extension: String?): ProblemCopy {
    val named = extension?.takeIf { it.isNotBlank() }?.uppercase()?.let { "$it file" } ?: "This document"
    val limitations = DocumentFormats.forExtension(extension)?.limitations.orEmpty()

    fun copy(title: String, head: String, action: String) = ProblemCopy(
        title = title,
        message = if (limitations.isEmpty()) head else buildString {
            append(head)
            limitations.forEach { append("\n• $it") }
        },
        actionLabel = action
    )

    return when (failure) {
        LoadFailure.UNSUPPORTED_FORMAT -> copy(
            "Format not supported",
            "$named uses a container MyDoc has no reader for, so nothing is shown rather than a " +
                "partial guess. Convert it to a supported format to open it here.",
            "Choose another file"
        )
        LoadFailure.CORRUPT_DOCUMENT -> copy(
            "Document is damaged",
            "$named could not be read. Its structure is broken or its contents are incomplete.",
            "Try again"
        )
        LoadFailure.EMPTY_DOCUMENT -> copy(
            "Document is empty",
            "$named opened successfully but contains no readable content.",
            "Choose another file"
        )
        LoadFailure.PERMISSION_EXPIRED -> copy(
            "Document inaccessible",
            "MyDoc lost permission to read $named. Pick it again to restore access.",
            "Select file"
        )
        LoadFailure.PASSWORD_PROTECTED -> copy(
            "Document is protected",
            "$named is encrypted or password-protected. Remove the protection, then open it again.",
            "Choose another file"
        )
        LoadFailure.EXCEEDS_LIMITS -> copy(
            "Document is too large",
            "$named is larger than the limits MyDoc applies to protect memory and battery.",
            "Choose another file"
        )
        LoadFailure.CONVERSION_FAILED -> copy(
            "Could not prepare pages",
            "MyDoc read $named but could not lay out its pages. It likely uses features our " +
                "converter does not implement.",
            "Try again"
        )
        LoadFailure.FEATURE_UNAVAILABLE -> copy(
            "Feature not available",
            "This part of $named cannot be shown in MyDoc.",
            "Choose another file"
        )
    }
}

/**
 * One-line honesty about how much of the document's design survived, for the disclosure banner
 * and the reader's format-info sheet.
 */
fun fidelityNote(fidelity: RenderFidelity): String? = when (fidelity) {
    RenderFidelity.FIXED_LAYOUT -> null
    RenderFidelity.STRUCTURED_REFLOW -> "Re-laid out for reading — pagination may differ from the original"
    RenderFidelity.TEXT_ONLY -> "Text only — this format's layout is not decoded"
    RenderFidelity.RECOVERED_SCRAPE -> "Recovered text — content may be out of order or incomplete"
    RenderFidelity.NONE -> "Nothing could be rendered"
}
