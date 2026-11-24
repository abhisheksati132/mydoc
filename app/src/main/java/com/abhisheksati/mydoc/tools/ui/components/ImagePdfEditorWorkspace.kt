package com.abhisheksati.mydoc.tools.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Preview first on phones; a scrollable editing rail keeps controls usable in landscape. */
@Composable
internal fun ImagePdfEditorWorkspace(
    preview: @Composable () -> Unit,
    filmstrip: @Composable () -> Unit,
    actions: @Composable () -> Unit,
    settings: @Composable () -> Unit,
    createButton: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val controlsMaxHeight = maxHeight * 0.48f
        if (maxWidth > maxHeight) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f).fillMaxHeight()) { preview() }
                Surface(Modifier.weight(0.8f).fillMaxHeight(), shape = AppRadius.glassShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column {
                        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                            filmstrip()
                            actions()
                            settings()
                        }
                        createButton()
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp)) { preview() }
                Column(Modifier.fillMaxWidth().heightIn(max = controlsMaxHeight).verticalScroll(rememberScrollState())) {
                    filmstrip()
                    actions()
                    settings()
                }
                createButton()
            }
        }
    }
}
