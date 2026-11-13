package com.abhisheksati.mydoc.tools.ui.components

import com.abhisheksati.mydoc.R
import android.annotation.SuppressLint
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.components.AppBottomSheet
import com.abhisheksati.mydoc.designsystem.components.showMessage
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.DocumentCanvasColors
import com.abhisheksati.mydoc.designsystem.tokens.rememberAppHaptics
import com.abhisheksati.mydoc.tools.security.SignatureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class SignatureStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

@SuppressLint("ThreadConstraint")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignPdfSheet(
    onDismiss: () -> Unit,
    onSignatureSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val haptics = rememberAppHaptics()
    val saveScope = rememberCoroutineScope()

    var isDrawMode by remember { mutableStateOf(true) }
    var typedName by remember { mutableStateOf("") }

    val inkColors = listOf(
        DocumentCanvasColors.inkBlack,
        DocumentCanvasColors.inkNavy,
        DocumentCanvasColors.inkCrimson
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
                .verticalScroll(rememberScrollState())
                .padding(bottom = AppSpacing.paddingLarge)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isDrawMode) "Draw signature" else "Type signature",
                    style = typography.titleLarge,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    color = scheme.surfaceContainerHigh,
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        FilledIconToggleButton(
                            checked = isDrawMode,
                            onCheckedChange = { isDrawMode = it },
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconToggleButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = scheme.onSurfaceVariant,
                                checkedContainerColor = scheme.primary,
                                checkedContentColor = scheme.onPrimary
                            ),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Draw,
                                contentDescription = stringResource(R.string.sign_draw),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        FilledIconToggleButton(
                            checked = !isDrawMode,
                            onCheckedChange = { isDrawMode = !it },
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconToggleButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = scheme.onSurfaceVariant,
                                checkedContainerColor = scheme.primary,
                                checkedContentColor = scheme.onPrimary
                            ),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.TextFields,
                                contentDescription = stringResource(R.string.sign_type),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_close), tint = scheme.onSurfaceVariant)
                }
            }

            Text(
                text = stringResource(R.string.sign_encrypted_note),
                style = typography.bodySmall,
                color = scheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val inkColorNames = listOf(
                        stringResource(R.string.sign_ink_black),
                        stringResource(R.string.sign_ink_navy),
                        stringResource(R.string.sign_ink_crimson)
                    )
                    val genericInkName = stringResource(R.string.sign_ink_color)
                    inkColors.forEachIndexed { index, color ->
                        val isSelected = selectedColorIndex == index
                        val colorDesc = inkColorNames.getOrElse(index) { genericInkName }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) scheme.primary else scheme.outline,
                                    shape = CircleShape
                                )
                                .semantics {
                                    contentDescription = colorDesc
                                    role = Role.RadioButton
                                    selected = isSelected
                                }
                                .clickable(onClickLabel = colorDesc) { selectedColorIndex = index }
                        )
                    }
                }

                if (isDrawMode && strokes.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            if (strokes.isNotEmpty()) strokes.removeAt(strokes.size - 1)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Undo,
                            contentDescription = stringResource(R.string.sign_undo_stroke),
                            tint = scheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // White paper keeps ink readable in every theme. cardShape, not roundShape: a
            // 200dp-tall box clamped to a stadium reads as a pill, not as a sheet of paper.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(AppRadius.cardShape)
                    .background(Color.White)
                    .border(1.dp, scheme.outlineVariant, AppRadius.cardShape)
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
                        for (stroke in strokes) {
                            drawSmoothStroke(stroke.points, stroke.color, stroke.strokeWidth)
                        }
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
                        // Placeholder and field share one style, so the signature does not
                        // change size the moment the user starts typing.
                        val signatureStyle = typography.displaySmall.copy(
                            color = activeInkColor,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Normal
                        )
                        BasicTextField(
                            value = typedName,
                            onValueChange = { typedName = it },
                            textStyle = signatureStyle,
                            cursorBrush = SolidColor(activeInkColor),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (typedName.isEmpty()) {
                            Text(
                                text = stringResource(R.string.sign_type_hint),
                                style = signatureStyle.copy(color = Color.Black.copy(alpha = 0.38f))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            val hasContent = (isDrawMode && strokes.isNotEmpty()) || (!isDrawMode && typedName.isNotBlank())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        strokes.clear()
                        currentPoints = emptyList()
                        typedName = ""
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = AppRadius.cardShape
                ) {
                    Text(stringResource(R.string.sign_clear))
                }

                Button(
                    onClick = {
                        haptics.confirm()
                        val bmp = if (isDrawMode) {
                            renderStrokesToBitmap(strokes)
                        } else {
                            renderTextToBitmap(typedName, activeInkColor)
                        }
                        // Keystore-backed AES plus a disk write: this must not run on the click
                        // thread, and the sheet only closes once the save actually landed.
                        saveScope.launch(Dispatchers.IO) {
                            val saved = SignatureStorage.saveSignature(context, bmp)
                            bmp.recycle()
                            if (saved) {
                                withContext(Dispatchers.Main) { onSignatureSaved() }
                            } else {
                                context.showMessage("Could not save the signature")
                            }
                        }
                    },
                    enabled = hasContent,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = AppRadius.cardShape
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.sign_save))
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
    val targetWidth = 700
    val targetHeight = 350
    val bmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)

    val allPoints = strokes.flatMap { it.points }
    if (allPoints.isEmpty()) return bmp

    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE
    for (pt in allPoints) {
        if (pt.x < minX) minX = pt.x
        if (pt.y < minY) minY = pt.y
        if (pt.x > maxX) maxX = pt.x
        if (pt.y > maxY) maxY = pt.y
    }

    val strokeBoundWidth = (maxX - minX).coerceAtLeast(1f)
    val strokeBoundHeight = (maxY - minY).coerceAtLeast(1f)

    val padding = 30f
    val availableWidth = (targetWidth - padding * 2).coerceAtLeast(1f)
    val availableHeight = (targetHeight - padding * 2).coerceAtLeast(1f)

    val scale = minOf(availableWidth / strokeBoundWidth, availableHeight / strokeBoundHeight, 2.0f)
    val contentWidth = strokeBoundWidth * scale
    val contentHeight = strokeBoundHeight * scale
    val offsetX = padding + (availableWidth - contentWidth) / 2f - minX * scale
    val offsetY = padding + (availableHeight - contentHeight) / 2f - minY * scale

    for (stroke in strokes) {
        if (stroke.points.isEmpty()) continue
        val paint = Paint().apply {
            color = stroke.color.toArgb()
            strokeWidth = (stroke.strokeWidth * scale).coerceIn(2f, 16f)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
        val path = Path()
        val firstX = stroke.points[0].x * scale + offsetX
        val firstY = stroke.points[0].y * scale + offsetY
        path.moveTo(firstX, firstY)
        for (i in 1 until stroke.points.size) {
            val prevX = stroke.points[i - 1].x * scale + offsetX
            val prevY = stroke.points[i - 1].y * scale + offsetY
            val currX = stroke.points[i].x * scale + offsetX
            val currY = stroke.points[i].y * scale + offsetY
            val midX = (prevX + currX) / 2f
            val midY = (prevY + currY) / 2f
            path.quadTo(prevX, prevY, midX, midY)
        }
        val lastX = stroke.points.last().x * scale + offsetX
        val lastY = stroke.points.last().y * scale + offsetY
        path.lineTo(lastX, lastY)
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

    val maxTextWidth = (width - 64).toFloat()
    val measuredWidth = paint.measureText(text)
    if (measuredWidth > maxTextWidth && maxTextWidth > 0f) {
        paint.textSize = (paint.textSize * (maxTextWidth / measuredWidth)).coerceAtLeast(24f)
    }

    val x = width / 2f
    val y = (height / 2f) - ((paint.descent() + paint.ascent()) / 2f)
    canvas.drawText(text, x, y, paint)

    return bmp
}
