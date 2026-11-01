package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.animation.pressScaleEffect
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures

/**
 * One list row for the whole app: a 56/64dp line, a 20dp glyph if the item has a
 * kind, title and a single line of meta, then a hairline. No card, no tile, no
 * coloured container — the rule does the separating so the page keeps one texture.
 */
@Composable
fun EditorialRow(
    title: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    divider: Boolean = true,
    titleStyle: TextStyle? = null
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressScaleEffect(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(
                    minHeight = if (meta != null) AppSpacing.objectRowHeight else AppSpacing.listRowHeight
                )
                .padding(
                    start = AppSpacing.screenPadding,
                    end = AppSpacing.screenPadding,
                    top = AppSpacing.small,
                    bottom = AppSpacing.small
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
        ) {
            if (leading != null) {
                Box(
                    modifier = Modifier.width(AppSpacing.large),
                    contentAlignment = Alignment.CenterStart
                ) {
                    leading()
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = titleStyle ?: typography.bodyLargeStrong,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (meta != null) {
                    Text(
                        text = meta,
                        style = typography.caption.withTabularFigures(),
                        color = colors.inkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            trailing?.invoke()
        }

        if (divider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.screenPadding)
                    .height(AppSpacing.hairline)
                    .background(colors.line)
            )
        }
    }
}
