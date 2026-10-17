package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme

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

    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val effectiveDismiss = onDismissRequest ?: onDismiss ?: {}

    AlertDialog(
        onDismissRequest = effectiveDismiss,
        shape = RoundedCornerShape(AppSpacing.cornerRadius),
        containerColor = colors.surface,
        modifier = Modifier.border(
            width = AppSpacing.dividerThickness,
            color = colors.divider,
            shape = RoundedCornerShape(AppSpacing.cornerRadius)
        ),
        title = {
            Text(
                text = title,
                style = typography.title,
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
                        .height(AppSpacing.dividerThickness)
                        .border(AppSpacing.dividerThickness, colors.divider)
                )
            }
        },
        confirmButton = {
            if (confirmLabel != null && onConfirm != null) {
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(AppSpacing.chipCornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDestructive) colors.error else colors.accent,
                        contentColor = colors.background
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = AppSpacing.minTouchTarget)
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
                    shape = RoundedCornerShape(AppSpacing.chipCornerRadius),
                    modifier = Modifier.defaultMinSize(minHeight = AppSpacing.minTouchTarget)
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
