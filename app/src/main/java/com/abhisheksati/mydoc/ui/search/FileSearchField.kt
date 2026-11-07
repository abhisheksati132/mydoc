package com.abhisheksati.mydoc.ui.search

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import com.abhisheksati.mydoc.R
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
    OutlinedTextField(
        value = query, onValueChange = onQueryChange, singleLine = true,
        label = { Text(label) }, placeholder = { Text(stringResource(R.string.search_name_or_type)) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) ({
            IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_clear)) }
        }) else null,
        shape = AppRadius.cardShape,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        modifier = modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = label }
    )
}
