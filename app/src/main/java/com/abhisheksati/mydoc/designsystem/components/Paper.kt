package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.tokens.PaperShape
import com.abhisheksati.mydoc.designsystem.tokens.SheetPalette
import kotlin.random.Random

/**
 * A low-density deterministic fibre speckle. It is drawn from a cached point
 * list, so scrolling the sheet does not re-randomise or re-allocate.
 */
fun Modifier.paperGrain(
    color: Color,
    alpha: Float = 0.05f,
    seed: Int = 4
): Modifier = drawWithCache {
    val random = Random(seed)
    val dots = List(700) {
        val x = random.nextFloat() * size.width
        val y = random.nextFloat() * size.height
        val weight = 0.35f + random.nextFloat() * 0.65f
        Triple(x, y, weight)
    }
    val grain = color.copy(alpha = alpha)
    onDrawBehind {
        dots.forEach { (x, y, weight) ->
            drawCircle(
                color = grain.copy(alpha = alpha * weight),
                radius = 0.65f,
                center = Offset(x, y)
            )
        }
    }
}

/**
 * A single printed sheet: ink-coloured shadow, fibre grain, hairline edge and
 * an optional folded corner. Content sits inside [BoxScope]; callers own padding.
 */
@Composable
fun PaperSurface(
    palette: SheetPalette,
    modifier: Modifier = Modifier,
    elevation: Dp = 7.dp,
    foldSize: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = PaperShape,
                ambientColor = Color.Black.copy(alpha = 0.16f),
                spotColor = Color.Black.copy(alpha = 0.10f)
            )
            .clip(PaperShape)
            .background(palette.sheet)
            .paperGrain(palette.ink)
            .border(1.dp, palette.rule, PaperShape)
            .then(
                if (foldSize > 0.dp) {
                    Modifier.drawWithContent {
                        drawContent()
                        drawFold(palette, foldSize.toPx())
                    }
                } else {
                    Modifier
                }
            ),
        content = content
    )
}

private fun DrawScope.drawFold(palette: SheetPalette, fold: Float) {
    val corner = Path().apply {
        moveTo(size.width - fold, 0f)
        lineTo(size.width, 0f)
        lineTo(size.width, fold)
        close()
    }
    drawPath(corner, color = palette.sheetEdge)
    drawLine(
        color = palette.rule,
        start = Offset(size.width - fold, 0f),
        end = Offset(size.width, fold),
        strokeWidth = 1f
    )
}
