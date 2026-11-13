package com.abhisheksati.mydoc.ui.media

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.abhisheksati.mydoc.R
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.domain.model.MediaItem

@Composable
fun ZoomableImage(
    item: MediaItem,
    isFillScale: Boolean,
    displayRotation: Float,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    isFlippedHorizontal: Boolean = false
) {
    val context = LocalContext.current
    var scale by remember(item.uri) { mutableFloatStateOf(1f) }
    var offset by remember(item.uri) { mutableStateOf(Offset.Zero) }
    var loadFailed by remember(item.uri) { mutableStateOf(false) }

    val contentScale = if (isFillScale) ContentScale.Crop else ContentScale.Fit

    val imageRequest = remember(item.uri) {
        ImageRequest.Builder(context)
            .data(item.uri)
            .crossfade(true)
            .build()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(item.uri) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        scale = if (scale > 1.2f) 1f else 2.5f
                        offset = Offset.Zero
                    }
                )
            }
            .pointerInput(item.uri) {
                awaitEachGesture {
                    do {
                        val event = awaitPointerEvent()
                        val isMultiTouch = event.changes.size > 1
                        val isZoomed = scale > 1.05f

                        if (isMultiTouch || isZoomed) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()

                            if (zoomChange != 1f || (isZoomed && panChange != Offset.Zero)) {
                                val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                                scale = newScale
                                if (newScale > 1.02f) {
                                    val maxPanX = (this.size.width.toFloat() * (newScale - 1f)) / 2f
                                    val maxPanY = (this.size.height.toFloat() * (newScale - 1f)) / 2f
                                    offset = Offset(
                                        x = (offset.x + panChange.x).coerceIn(-maxPanX, maxPanX),
                                        y = (offset.y + panChange.y).coerceIn(-maxPanY, maxPanY)
                                    )
                                } else {
                                    offset = Offset.Zero
                                }
                                event.changes.forEach {
                                    if (it.positionChanged()) it.consume()
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = item.displayName,
            contentScale = contentScale,
            onError = { loadFailed = true },
            onSuccess = { loadFailed = false },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = if (isFlippedHorizontal) -scale else scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                    rotationZ = displayRotation
                }
        )
        if (loadFailed) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.BrokenImage,
                    contentDescription = null,
                    tint = LocalAppColors.current.textSecondary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.photo_load_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalAppColors.current.textSecondary
                )
            }
        }
    }
}
