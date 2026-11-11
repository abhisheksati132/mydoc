package com.abhisheksati.mydoc.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.components.AppAlertDialog

@Composable
internal fun OpenSourceLicensesDialog(onDismiss: () -> Unit) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.open_source_licenses)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "MyDoc",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Copyright (c) 2026 Abhishek Sati\n" +
                        "Licensed under the GNU Affero General Public License v3.0 (AGPL-3.0).\n\n" +
                        "In accordance with AGPL-3.0 § 13, complete corresponding source code is available at:\n" +
                        "https://github.com/Abhisheksati/MyDoc\n\n" +
                        "This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; " +
                        "without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "MuPDF Fitz Engine (v1.28.5)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Copyright (c) 2006-2026 Artifex Software, Inc.\n" +
                        "Licensed under the GNU Affero General Public License v3.0 (AGPL-3.0).\n" +
                        "Provides vector document rendering, structured text quad search, and TOC outlines.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "LibreOffice Companion Engine",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Copyright (c) 2000-2026 The Document Foundation and contributors.\n" +
                        "Licensed under LGPL-3.0 and MPL-2.0.\n" +
                        "Processes office formats (.docx, .xlsx, .pptx, .odt, .ods, .odp) in an isolated sandbox worker.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Typefaces (SIL Open Font License 1.1)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "- Fraunces: Copyright (c) 2020 Undercase Type (Phaedra Charles, Flavia Zimbardi)\n" +
                        "- Inter: Copyright (c) 2016-2026 Rasmus Andersson\n" +
                        "- JetBrains Mono: Copyright (c) 2020-2026 JetBrains s.r.o.\n" +
                        "All bundled fonts are licensed under the SIL Open Font License, Version 1.1.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Third-Party Libraries",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "- AndroidX Jetpack, Compose, Material 3, CameraX, Media3 (Apache 2.0)\n" +
                        "- Kotlin Coroutines, Serialization (Apache 2.0)\n" +
                        "- Haze 2.1.0 blur & glass optics by Chris Banes (Apache 2.0)\n" +
                        "- Coil image loading (Apache 2.0)\n" +
                        "- Google ML Kit OCR text recognition (Subject to Google APIs & ML Kit Terms of Service)",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}
