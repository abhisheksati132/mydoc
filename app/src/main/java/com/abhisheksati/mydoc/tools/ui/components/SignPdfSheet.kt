package com.abhisheksati.mydoc.tools.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.AppBottomSheet
import com.abhisheksati.mydoc.designsystem.components.AppDivider
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import androidx.compose.material3.ExperimentalMaterial3Api
import com.abhisheksati.mydoc.tools.security.SignatureStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignPdfSheet(
    onDismiss: () -> Unit,
    onSignatureSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val haptic = LocalHapticFeedback.current

    val points = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }

    AppBottomSheet(
        isOpen = true,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = AppSpacing.paddingLarge)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSpacing.minTouchTarget),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Draw Signature", style = typography.title, color = colors.textPrimary)
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(AppSpacing.minTouchTarget)
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = colors.textSecondary)
                }
            }

            Text(
                text = "Sign with your finger. Signature is encrypted in Android Keystore.",
                style = typography.caption,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))

            // Signature Canvas Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(AppSpacing.chipCornerRadius))
                    .background(Color.White)
                    .border(1.dp, colors.divider, RoundedCornerShape(AppSpacing.chipCornerRadius))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentStroke = listOf(offset)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentStroke = currentStroke + change.position
                            },
                            onDragEnd = {
                                if (currentStroke.isNotEmpty()) {
                                    points.add(currentStroke)
                                    currentStroke = emptyList()
                                }
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    val strokeStyle = Stroke(
                        width = 4.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )

                    // Draw completed strokes
                    for (stroke in points) {
                        for (i in 0 until stroke.size - 1) {
                            drawLine(
                                color = Color.Black,
                                start = stroke[i],
                                end = stroke[i + 1],
                                strokeWidth = strokeStyle.width,
                                cap = strokeStyle.cap
                            )
                        }
                    }

                    // Draw active stroke
                    for (i in 0 until currentStroke.size - 1) {
                        drawLine(
                            color = Color.Black,
                            start = currentStroke[i],
                            end = currentStroke[i + 1],
                            strokeWidth = strokeStyle.width,
                            cap = strokeStyle.cap
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))

            // Actions: Clear and Save
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.paddingSmall)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(AppSpacing.minTouchTarget)
                        .border(1.dp, colors.divider, RoundedCornerShape(AppSpacing.chipCornerRadius))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            points.clear()
                            currentStroke = emptyList()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Clear", style = typography.body, color = colors.textPrimary)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(AppSpacing.minTouchTarget)
                        .clip(RoundedCornerShape(AppSpacing.chipCornerRadius))
                        .background(if (points.isNotEmpty()) colors.accent else colors.surfaceVariant)
                        .clickable(enabled = points.isNotEmpty()) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val bmp = renderStrokesToBitmap(points)
                            SignatureStorage.saveSignature(context, bmp)
                            bmp.recycle()
                            onSignatureSaved()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Save Signature",
                        style = typography.body,
                        color = if (points.isNotEmpty()) colors.background else colors.textSecondary
                    )
                }
            }
        }
    }
}

private fun renderStrokesToBitmap(strokes: List<List<Offset>>): Bitmap {
    val width = 600
    val height = 300
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)

    val paint = Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    for (stroke in strokes) {
        val path = Path()
        stroke.forEachIndexed { i, pt ->
            if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
        }
        canvas.drawPath(path, paint)
    }

    return bmp
}
