package com.abhisheksati.mydoc.ui.viewer.controls

import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.abhisheksati.mydoc.designsystem.components.OverlaySystemBars
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
                text = stringResource(R.string.password_title),
                style = AppTheme.typography.titleSmall
            )
        },
        text = {
            OverlaySystemBars()
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.password_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(AppSpacing.medium))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = isIncorrect,
                    supportingText = if (isIncorrect) {
                        { Text(stringResource(R.string.password_incorrect)) }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(password) }) {
                Text(stringResource(R.string.password_unlock))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
