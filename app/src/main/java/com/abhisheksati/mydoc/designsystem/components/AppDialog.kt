package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.LocalIsOled
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography

@Composable
fun AppDialog(
    isOpen: Boolean,
    title: String,
    onDismissRequest: (() -> Unit)? = null,
    message: String? = null,
    confirmLabel: String? = "Confirm",
    onConfirm: (() -> Unit)? = null,
    dismissLabel: String? = "Cancel",
    onDismiss: (() -> Unit)? = null,
    isDestructive: Boolean = false,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    if (!isOpen) return

    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val isOled = LocalIsOled.current
    val effectiveDismiss = onDismissRequest ?: onDismiss ?: {}

    AlertDialog(
        onDismissRequest = effectiveDismiss,
        shape = AppRadius.modalShape,
        containerColor = if (isOled) colors.surfaceContainerHigh else colors.surface,
        modifier = Modifier.border(
            BorderStroke(
                1.dp,
                Brush.verticalGradient(listOf(colors.glassRimTop, colors.glassRimBottom))
            ),
            shape = AppRadius.modalShape
        ),
        title = {
            OverlaySystemBars()
            Text(
                text = title,
                style = typography.titleSmall,
                color = colors.textPrimary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!message.isNullOrBlank()) {
                    Text(
                        text = message,
                        style = typography.body,
                        color = colors.textSecondary
                    )
                }

                if (content != null) {
                    if (!message.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(AppSpacing.small))
                    }
                    content()
                }

                Spacer(modifier = Modifier.height(AppSpacing.small))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(colors.line)
                )
            }
        },
        confirmButton = {
            if (confirmLabel != null && onConfirm != null) {
                Button(
                    onClick = onConfirm,
                    shape = AppRadius.pillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDestructive) colors.error else colors.accent,
                        contentColor = if (isDestructive || !isOled) Color.White else Color.Black
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = 38.dp)
                ) {
                    Text(
                        text = confirmLabel,
                        style = typography.label
                    )
                }
            }
        },
        dismissButton = {
            if (dismissLabel != null && onDismiss != null) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = AppRadius.pillShape,
                    border = BorderStroke(
                        0.5.dp,
                        if (isOled) Color.White.copy(alpha = 0.12f) else colors.line
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = 38.dp)
                ) {
                    Text(
                        text = dismissLabel,
                        style = typography.label,
                        color = colors.textSecondary
                    )
                }
            }
        }
    )
}
