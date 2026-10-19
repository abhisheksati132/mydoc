package com.abhisheksati.mydoc.tools.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhisheksati.mydoc.designsystem.components.AppBottomSheet
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.tools.security.SignatureStorage

private data class SignatureStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

/**
 * Enterprise Signature Pad for PDF documents with smooth Bezier curve pen strokes,
 * typed script cursive font mode, multi-color ink palette, and undo history.
 */
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

    var isDrawMode by remember { mutableStateOf(true) }
    var typedName by remember { mutableStateOf("") }

    val inkColors = listOf(
        Color(0xFF000000), // Pure Black
        Color(0xFF1565C0), // Legal Navy Blue
        Color(0xFFD32F2F)  // Crimson Red
    )
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    val activeInkColor = inkColors[selectedColorIndex]

    val strokes = remember { mutableStateListOf<SignatureStroke>() }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    AppBottomSheet(
        isOpen = true,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = AppSpacing.paddingLarge)
        ) {
            // Header with mode toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppSpacing.minTouchTarget),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isDrawMode) "Draw Signature" else "Type Signature",
                    style = typography.title,
                    color = colors.textPrimary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Mode toggle pill
                    Surface(
                        color = colors.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Row(modifier = Modifier.padding(2.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isDrawMode) colors.accent else Color.Transparent)
                                    .clickable { isDrawMode = true }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Draw,
                                    contentDescription = "Draw",
                                    tint = if (isDrawMode) Color.Black else colors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (!isDrawMode) colors.accent else Color.Transparent)
                                    .clickable { isDrawMode = false }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.TextFields,
                                    contentDescription = "Type",
                                    tint = if (!isDrawMode) Color.Black else colors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(AppSpacing.minTouchTarget)
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = colors.textSecondary)
                    }
                }
            }

            Text(
                text = "Signatures are encrypted with hardware-backed AES-256 in Android Keystore.",
                style = typography.caption,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Ink color selector & Undo bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color choices
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    inkColors.forEachIndexed { index, color ->
                        val isSelected = selectedColorIndex == index
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) colors.accent else Color.Gray.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { selectedColorIndex = index }
                        )
                    }
                }

                if (isDrawMode && strokes.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            if (strokes.isNotEmpty()) strokes.removeAt(strokes.size - 1)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Undo,
                            contentDescription = "Undo stroke",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Signature Canvas / Input Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(AppSpacing.chipCornerRadius))
                    .background(Color.White)
                    .border(1.dp, colors.divider, RoundedCornerShape(AppSpacing.chipCornerRadius))
            ) {
                if (isDrawMode) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(activeInkColor) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentPoints = listOf(offset)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        currentPoints = currentPoints + change.position
                                    },
                                    onDragEnd = {
                                        if (currentPoints.isNotEmpty()) {
                                            strokes.add(SignatureStroke(currentPoints, activeInkColor, 5f))
                                            currentPoints = emptyList()
                                        }
                                    }
                                )
                            }
                    ) {
                        // Draw completed smooth Bezier strokes
                        for (stroke in strokes) {
                            drawSmoothStroke(stroke.points, stroke.color, stroke.strokeWidth)
                        }
                        // Draw active in-progress stroke
                        if (currentPoints.isNotEmpty()) {
                            drawSmoothStroke(currentPoints, activeInkColor, 5f)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicTextField(
                            value = typedName,
                            onValueChange = { typedName = it },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = activeInkColor,
                                fontSize = 32.sp,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(activeInkColor),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (typedName.isEmpty()) {
                            Text(
                                text = "Type your name here...",
                                style = typography.body.copy(fontSize = 20.sp, fontStyle = FontStyle.Italic),
                                color = Color.Gray.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.paddingMedium))

            // Actions: Clear and Save
            val hasContent = (isDrawMode && strokes.isNotEmpty()) || (!isDrawMode && typedName.isNotBlank())
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
                            strokes.clear()
                            currentPoints = emptyList()
                            typedName = ""
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
                        .background(if (hasContent) colors.accent else colors.surfaceVariant)
                        .clickable(enabled = hasContent) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val bmp = if (isDrawMode) {
                                renderStrokesToBitmap(strokes)
                            } else {
                                renderTextToBitmap(typedName, activeInkColor)
                            }
                            SignatureStorage.saveSignature(context, bmp)
                            bmp.recycle()
                            onSignatureSaved()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Save Signature",
                        style = typography.body,
                        color = if (hasContent) colors.background else colors.textSecondary
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSmoothStroke(
    points: List<Offset>,
    color: Color,
    strokeWidth: Float
) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        drawCircle(color = color, radius = strokeWidth / 2f, center = points[0])
        return
    }

    val path = androidx.compose.ui.graphics.Path()
    path.moveTo(points[0].x, points[0].y)
    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val curr = points[i]
        val midX = (prev.x + curr.x) / 2f
        val midY = (prev.y + curr.y) / 2f
        path.quadraticTo(prev.x, prev.y, midX, midY)
    }
    path.lineTo(points.last().x, points.last().y)

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun renderStrokesToBitmap(strokes: List<SignatureStroke>): Bitmap {
    val width = 700
    val height = 350
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)

    for (stroke in strokes) {
        if (stroke.points.isEmpty()) continue
        val paint = Paint().apply {
            color = stroke.color.toArgb()
            strokeWidth = stroke.strokeWidth * 1.5f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
        val path = Path()
        path.moveTo(stroke.points[0].x, stroke.points[0].y)
        for (i in 1 until stroke.points.size) {
            val prev = stroke.points[i - 1]
            val curr = stroke.points[i]
            val midX = (prev.x + curr.x) / 2f
            val midY = (prev.y + curr.y) / 2f
            path.quadTo(prev.x, prev.y, midX, midY)
        }
        path.lineTo(stroke.points.last().x, stroke.points.last().y)
        canvas.drawPath(path, paint)
    }

    return bmp
}

private fun renderTextToBitmap(text: String, color: Color): Bitmap {
    val width = 700
    val height = 350
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)

    val paint = Paint().apply {
        this.color = color.toArgb()
        textSize = 68f
        typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    val x = width / 2f
    val y = (height / 2f) - ((paint.descent() + paint.ascent()) / 2f)
    canvas.drawText(text, x, y, paint)

    return bmp
}
