package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.DarkAppColors
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
    val isOled = colors.background == DarkAppColors.background
    val effectiveDismiss = onDismissRequest ?: onDismiss ?: {}

    AlertDialog(
        onDismissRequest = effectiveDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = if (isOled) Color(0xFF0E0F14) else colors.surface,
        modifier = Modifier.border(
            BorderStroke(
                0.8.dp,
                if (isOled) Color.White.copy(alpha = 0.08f) else colors.divider.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(24.dp)
        ),
        title = {
            Text(
                text = title,
                style = typography.title.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                color = colors.textPrimary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!message.isNullOrBlank()) {
                    Text(
                        text = message,
                        style = typography.body.copy(lineHeight = 20.sp),
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
                        .height(0.6.dp)
                        .background(
                            if (isOled) Color.White.copy(alpha = 0.06f) else colors.divider.copy(alpha = 0.5f)
                        )
                )
            }
        },
        confirmButton = {
            if (confirmLabel != null && onConfirm != null) {
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDestructive) colors.error else colors.accent,
                        contentColor = if (isDestructive || !isOled) Color.White else Color.Black
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = 40.dp)
                ) {
                    Text(
                        text = confirmLabel,
                        style = typography.label.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        dismissButton = {
            if (dismissLabel != null && onDismiss != null) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(
                        0.8.dp,
                        if (isOled) Color.White.copy(alpha = 0.12f) else colors.divider
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = 40.dp)
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
