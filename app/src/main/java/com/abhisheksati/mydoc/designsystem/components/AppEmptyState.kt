package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography

/**
 * Empty state in the editorial voice: set left, on the same margin as the list it
 * replaces, with one line of instruction and at most one action. It is a paragraph,
 * not a billboard — no centred glyph, no bordered box, no vertical centring.
 *
 * [icon] is accepted for call-site compatibility and deliberately not drawn.
 */
@Composable
fun AppEmptyState(
    message: String,
    title: String? = null,
    icon: ImageVector = Icons.Outlined.Description,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    onAction: (() -> Unit)? = onActionClick,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val effectiveAction = onAction ?: onActionClick

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = AppSpacing.screenPadding,
                end = AppSpacing.screenPadding,
                top = AppSpacing.medium,
                bottom = AppSpacing.medium
            )
    ) {
        Text(
            text = title ?: message,
            style = typography.bodyLargeStrong,
            color = colors.ink,
            modifier = Modifier.widthIn(max = 360.dp)
        )
        if (title != null) {
            Spacer(modifier = Modifier.height(AppSpacing.xSmall))
            Text(
                text = message,
                style = typography.body,
                color = colors.inkMuted,
                modifier = Modifier.widthIn(max = 360.dp)
            )
        }
        if (actionLabel != null && effectiveAction != null) {
            Spacer(modifier = Modifier.height(AppSpacing.small))
            EditorialAction(label = actionLabel, onClick = effectiveAction)
        }
    }
}
