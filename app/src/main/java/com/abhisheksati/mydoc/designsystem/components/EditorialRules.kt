package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abhisheksati.mydoc.designsystem.animation.pressScaleEffect
import com.abhisheksati.mydoc.designsystem.tokens.AppRadius
import com.abhisheksati.mydoc.designsystem.tokens.AppSpacing
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.designsystem.tokens.withTabularFigures

/**
 * A printed rule. Two weights exist: [AppSpacing.rule] for section breaks and the
 * masthead underline, [AppSpacing.hairline] for dividers inside a list.
 */
@Composable
fun EditorialRule(
    modifier: Modifier = Modifier,
    thickness: Dp = AppSpacing.rule,
    color: Color = LocalAppColors.current.ink
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color)
    )
}

/**
 * The page head: kicker and folio meta on one line, the title on the next, and a
 * 2dp rule closing the block. Every root tab starts with this, so the reader always
 * knows which page they are on and how much is left.
 */
@Composable
fun EditorialMasthead(
    title: String,
    modifier: Modifier = Modifier,
    kicker: String? = null,
    meta: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Column(modifier = modifier.fillMaxWidth()) {
        if (kicker != null || meta != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = AppSpacing.screenPadding,
                        end = AppSpacing.screenPadding,
                        top = AppSpacing.medium
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
            ) {
                Text(
                    text = kicker.orEmpty(),
                    style = typography.overline,
                    color = colors.inkMuted
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = meta.orEmpty(),
                    style = typography.overline.withTabularFigures(),
                    color = colors.inkMuted
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = AppSpacing.screenPadding,
                    end = AppSpacing.xSmall,
                    top = if (kicker == null && meta == null) AppSpacing.medium else AppSpacing.xSmall,
                    bottom = AppSpacing.small
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = typography.title,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            actions?.invoke(this)
        }

        EditorialRule()
    }
}

/**
 * A section header in the vivid language: the title, an optional trailing action,
 * and a short 3dp spectrum underline — the signature stroke, in miniature.
 */
@Composable
fun AppSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = AppSpacing.screenPadding,
                    end = AppSpacing.screenPadding,
                    top = AppSpacing.medium,
                    bottom = AppSpacing.xSmall
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
        ) {
            Text(
                text = title,
                style = typography.heading,
                color = colors.ink,
                modifier = Modifier.weight(1f)
            )
            action?.invoke()
        }
        VividRule(
            modifier = Modifier
                .padding(start = AppSpacing.screenPadding, bottom = AppSpacing.small)
                .width(40.dp)
        )
    }
}

/** A soft accent pill for inline actions — the vivid replacement for the bare text link. */
@Composable
fun EditorialAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Box(
        modifier = modifier
            .clip(AppRadius.pillShape)
            .background(colors.accent.copy(alpha = 0.14f))
            .pressScaleEffect(onClick = onClick)
            .padding(horizontal = AppSpacing.small + 2.dp, vertical = AppSpacing.xSmall)
    ) {
        Text(
            text = label,
            style = typography.captionStrong,
            color = colors.accent
        )
    }
}
