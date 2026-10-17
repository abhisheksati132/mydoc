package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography

/**
 * Standard designed empty/error/permission state component.
 * Rules:
 * - One short plain sentence plus at most one button.
 * - No illustrations, no emoji, no exclamation marks.
 * - Microcopy is calm, short, and specific.
 */
@Composable
fun AppEmptyState(
    message: String,
    title: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    onAction: (() -> Unit)? = onActionClick,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val effectiveAction = onAction ?: onActionClick

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(AppSpacing.large),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = typography.title,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(AppSpacing.small))
            }

            Text(
                text = message,
                style = typography.body,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )

            if (actionLabel != null && effectiveAction != null) {
                Spacer(modifier = Modifier.height(AppSpacing.large))

                Button(
                    onClick = effectiveAction,
                    shape = RoundedCornerShape(AppSpacing.cornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.surfaceVariant,
                        contentColor = colors.textPrimary
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = AppSpacing.minTouchTarget)
                ) {
                    Text(
                        text = actionLabel,
                        style = typography.label
                    )
                }
            }
        }
    }
}
