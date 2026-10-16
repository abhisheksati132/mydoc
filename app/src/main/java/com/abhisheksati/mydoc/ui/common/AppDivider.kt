package com.abhisheksati.mydoc.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.abhisheksati.mydoc.ui.theme.Spacing

@Composable
fun AppDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = Spacing.listDividerThickness
) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        thickness = thickness,
        color = MaterialTheme.colorScheme.outline
    )
}
