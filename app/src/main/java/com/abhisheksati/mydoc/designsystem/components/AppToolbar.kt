package com.abhisheksati.mydoc.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState

/**
 * The single app bar every screen uses, so back affordance, insets, title rhythm and
 * action placement stay identical across the Files, Settings, viewer and media surfaces.
 * Hosted on a tactile GlassSurface with specular gradient rim.
 *
 * Role split, deliberately: this bar carries a *screen* title on `titleLarge`; bars that name the
 * document being read (ViewerTopBar, PresentationTopBar, the media players) use the brand's
 * `documentName` role instead. The back affordance is the outlined arrow, matching the outlined
 * icon set the rest of the app is drawn from. Insets come from `contentInsets` and are applied
 * here once - callers must not pad for the status bar as well.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    titleContent: (@Composable () -> Unit)? = null,
    contentInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    containerColor: Color? = null,
    isGlass: Boolean = true,
    hazeState: HazeState? = null,
    motion: GlassMotionState? = null
) {
    val barColors = TopAppBarDefaults.topAppBarColors(
        containerColor = if (isGlass) Color.Transparent else (containerColor ?: MaterialTheme.colorScheme.surface),
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val topAppBarContent: @Composable () -> Unit = {
        TopAppBar(
            modifier = Modifier.fillMaxWidth(),
            title = {
                if (titleContent != null) {
                    titleContent()
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            actions = actions,
            colors = barColors,
            windowInsets = contentInsets
        )
    }

    if (isGlass) {
        GlassSurface(
            shape = RectangleShape,
            elevation = 4.dp,
            hazeState = hazeState,
            motion = motion,
            backgroundColor = containerColor,
            modifier = modifier.fillMaxWidth()
        ) {
            topAppBarContent()
        }
    } else {
        Column(modifier = modifier.fillMaxWidth()) {
            topAppBarContent()
        }
    }
}
