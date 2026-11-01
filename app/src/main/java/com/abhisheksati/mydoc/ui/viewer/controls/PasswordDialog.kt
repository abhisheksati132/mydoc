package com.abhisheksati.mydoc.ui.viewer.controls

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing as Spacing
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme

@Composable
fun PasswordDialog(
    isOpen: Boolean,
    isIncorrect: Boolean,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit
) {
    if (!isOpen) return

    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = "Password Protected",
                style = AppTheme.typography.bodyLargeStrong
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "This document is encrypted. Enter the password to unlock it.",
                    style = AppTheme.typography.body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.medium))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = isIncorrect,
                    shape = AppRadius.microShape,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isIncorrect) {
                    Spacer(modifier = Modifier.height(Spacing.extraSmall))
                    Text(
                        text = "Incorrect password. Please try again.",
                        style = AppTheme.typography.caption,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(password) },
                shape = AppRadius.cardShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Unlock")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onCancel,
                shape = AppRadius.cardShape
            ) {
                Text("Cancel")
            }
        },
        shape = AppRadius.sheetShape,
        containerColor = MaterialTheme.colorScheme.surface
    )
}
