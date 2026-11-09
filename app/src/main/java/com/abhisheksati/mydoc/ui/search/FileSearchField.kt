package com.abhisheksati.mydoc.ui.search

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.components.AppSearchField
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius

/** One accessible field for indexed-library and current-folder queries. Scope is shown beside it. */
@Composable
internal fun FileSearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier, autoFocus: Boolean = true) {
    val label = stringResource(R.string.search_files_label)
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(autoFocus) {
        if (autoFocus) { focus.requestFocus(); keyboard?.show() }
    }
    AppSearchField(
        query = query,
        onQueryChanged = onQueryChange,
        placeholder = stringResource(R.string.search_name_or_type),
        shape = AppRadius.cardShape,
        modifier = modifier.focusRequester(focus).semantics { contentDescription = label }
    )
}