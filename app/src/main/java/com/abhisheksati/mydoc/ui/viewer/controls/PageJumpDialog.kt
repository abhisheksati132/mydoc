package com.abhisheksati.mydoc.ui.viewer.controls

import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.abhisheksati.mydoc.designsystem.components.OverlaySystemBars
import com.abhisheksati.mydoc.designsystem.tokens.AppTheme

@Composable
fun PageJumpDialog(
    isOpen: Boolean,
    currentPage: Int,
    pageCount: Int,
    onJump: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    var inputPage by remember { mutableStateOf((currentPage + 1).toString()) }
    var isError by remember { mutableStateOf(false) }

    fun submit() {
        val parsed = inputPage.trim().toIntOrNull()
        if (parsed != null && parsed in 1..pageCount) {
            onJump(parsed - 1)
            onDismiss()
        } else {
            isError = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.pagejump_title),
                style = AppTheme.typography.titleSmall
            )
        },
        text = {
            OverlaySystemBars()
            Column {
                Text(
                    text = stringResource(R.string.pagejump_range, pageCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(AppSpacing.medium))
                OutlinedTextField(
                    value = inputPage,
                    onValueChange = {
                        inputPage = it.filter { char -> char.isDigit() }
                        isError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = isError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(onGo = { submit() }),
                    label = { Text(stringResource(R.string.pagejump_label)) },
                    supportingText = {
                        if (isError) {
                            Text(
                                text = stringResource(R.string.pagejump_invalid, pageCount),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = { submit() }) {
                Text("Go")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
