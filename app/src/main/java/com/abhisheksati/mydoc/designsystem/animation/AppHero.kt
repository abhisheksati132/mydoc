@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.abhisheksati.mydoc.designsystem.animation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Shared-element plumbing.
 *
 * A screen tree declares its participation once, at the navigation host, and any
 * descendant can then mark an element as the anchor for the transition with
 * [documentHero]. When the scope is absent (previews, sheets, dialogs) the modifier
 * is a no-op, so call sites stay unconditional.
 */
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
val LocalAnimatedVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

fun documentHeroKey(uri: String): String = "document:$uri"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.documentHero(key: String): Modifier {
    val sharedScope = LocalSharedTransitionScope.current ?: return this
    val visibilityScope = LocalAnimatedVisibilityScope.current ?: return this

    return with(sharedScope) {
        sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = visibilityScope,
            boundsTransform = { _, _ -> AppMotion.heroBounds() },
            resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds()
        )
    }
}
