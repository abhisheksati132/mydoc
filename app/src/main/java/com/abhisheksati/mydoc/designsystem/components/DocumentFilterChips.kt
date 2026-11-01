package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.domain.model.DocumentFilter

/**
 * The document format filter row. Home and Files render this same control.
 */
@Composable
fun DocumentFilterChips(
    selected: DocumentFilter,
    onSelected: (DocumentFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScrollWithFade(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DocumentFilter.entries.forEach { filter ->
            AppChip(
                label = filter.label,
                selected = filter == selected,
                onClick = { onSelected(filter) }
            )
        }
    }
}
