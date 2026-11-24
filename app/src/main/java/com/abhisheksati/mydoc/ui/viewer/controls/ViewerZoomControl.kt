package com.abhisheksati.mydoc.ui.viewer.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.R
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** An alternative to pinch gestures, shared by page and slide readers. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ViewerZoomControl(zoomState: ZoomState, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val percent = (zoomState.scale * 100).roundToInt()
    val percentageLabel = stringResource(R.string.viewer_zoom_percent, percent)
    val zoomLabel = stringResource(R.string.viewer_zoom)
    val actionLabel = stringResource(R.string.viewer_zoom_action, percent)
    FilledTonalButton(onClick = { open = true }, modifier = modifier.semantics { contentDescription = actionLabel }) {
        Icon(Icons.Outlined.ZoomIn, contentDescription = null)
        Text(stringResource(R.string.viewer_zoom_percent, percent), Modifier.padding(start = 8.dp))
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(zoomLabel) },
            text = {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.viewer_zoom_hint), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.viewer_zoom_percent, percent), style = MaterialTheme.typography.headlineSmall)
                    Slider(
                        value = zoomState.scale,
                        onValueChange = { scale ->
                            zoomState.stopAnimation()
                            val offset = ZoomMath.calculateCentroidOffset(
                                zoomState.offset, zoomState.scale, scale,
                                Offset(zoomState.viewportSize.width / 2, zoomState.viewportSize.height / 2),
                                zoomState.viewportSize
                            )
                            zoomState.updateScaleAndOffset(scale, offset)
                        },
                        valueRange = ZoomMath.MIN_SCALE..ZoomMath.MAX_SCALE,
                        modifier = Modifier.semantics { contentDescription = zoomLabel; stateDescription = percentageLabel }
                    )
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1f, 1.5f, 2f, 3f).forEach { scale ->
                            TextButton(modifier = Modifier.semantics { selected = kotlin.math.abs(zoomState.scale - scale) < 0.01f }, onClick = {
                                scope.launch { zoomState.animateTo(scale, Offset.Zero) }
                            }) { Text(stringResource(R.string.viewer_zoom_percent, (scale * 100).roundToInt())) }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { open = false }) { Text(stringResource(R.string.action_done)) }
            },
            dismissButton = {
                TextButton(onClick = { scope.launch { zoomState.reset(animate = true) } }) {
                    Text(stringResource(R.string.viewer_zoom_reset))
                }
            }
        )
    }
}
