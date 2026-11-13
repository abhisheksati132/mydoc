package com.abhisheksati.mydoc.ui.viewer

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.abhisheksati.mydoc.R
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
    val copy = problemCopy(LocalContext.current, failure, extension)
    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        AppEmptyState(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.paddingLarge),
            title = copy.title,
            message = copy.message,
            actionLabel = copy.actionLabel,
            onAction = onRetry ?: onBackClick
        )
    }
}

private class ProblemCopy(val title: String, val message: String, val actionLabel: String)

private fun problemCopy(context: Context, failure: LoadFailure, extension: String?): ProblemCopy {
    val named = extension?.takeIf { it.isNotBlank() }?.uppercase()
        ?.let { context.getString(R.string.problem_named_file, it) }
        ?: context.getString(R.string.problem_this_document)
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
            context.getString(R.string.problem_unsupported_title),
            context.getString(R.string.problem_unsupported_message, named),
            context.getString(R.string.problem_choose_another)
        )
        LoadFailure.CORRUPT_DOCUMENT -> copy(
            context.getString(R.string.problem_corrupt_title),
            context.getString(R.string.problem_corrupt_message, named),
            context.getString(R.string.problem_retry)
        )
        LoadFailure.EMPTY_DOCUMENT -> copy(
            context.getString(R.string.problem_empty_title),
            context.getString(R.string.problem_empty_message, named),
            context.getString(R.string.problem_choose_another)
        )
        LoadFailure.PERMISSION_EXPIRED -> copy(
            context.getString(R.string.problem_inaccessible_title),
            context.getString(R.string.problem_inaccessible_message, named),
            context.getString(R.string.problem_select_file)
        )
        LoadFailure.PASSWORD_PROTECTED -> copy(
            context.getString(R.string.problem_protected_title),
            context.getString(R.string.problem_protected_message, named),
            context.getString(R.string.problem_choose_another)
        )
        LoadFailure.EXCEEDS_LIMITS -> copy(
            context.getString(R.string.problem_too_large_title),
            context.getString(R.string.problem_too_large_message, named),
            context.getString(R.string.problem_choose_another)
        )
        LoadFailure.CONVERSION_FAILED -> copy(
            context.getString(R.string.problem_conversion_title),
            context.getString(R.string.problem_conversion_message, named),
            context.getString(R.string.problem_retry)
        )
        LoadFailure.FEATURE_UNAVAILABLE -> copy(
            context.getString(R.string.problem_feature_title),
            context.getString(R.string.problem_feature_message, named),
            context.getString(R.string.problem_choose_another)
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
