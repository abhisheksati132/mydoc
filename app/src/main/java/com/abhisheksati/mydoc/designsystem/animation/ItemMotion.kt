package com.abhisheksati.mydoc.designsystem.animation

import android.annotation.SuppressLint
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.ui.Modifier

/**
 * List-motion grammar: an item that is added fades in, one that is removed fades out,
 * and everything else glides to its new slot. Reduced motion removes the modifier
 * entirely so lists stay still.
 *
 * These cannot be extensions on Modifier: animateItem is itself a member extension of
 * the item scopes, so the scope receiver must live on the helper.
 */
@SuppressLint("ModifierFactoryExtensionFunction")
fun LazyItemScope.itemMotion(reducedMotion: Boolean): Modifier =
    if (reducedMotion) {
        Modifier
    } else {
        Modifier.animateItem(
            fadeInSpec = AppMotion.fadeBase,
            placementSpec = AppMotion.placement(),
            fadeOutSpec = AppMotion.fadeFast
        )
    }

@SuppressLint("ModifierFactoryExtensionFunction")
fun LazyGridItemScope.itemMotion(reducedMotion: Boolean): Modifier =
    if (reducedMotion) {
        Modifier
    } else {
        Modifier.animateItem(
            fadeInSpec = AppMotion.fadeBase,
            placementSpec = AppMotion.placement(),
            fadeOutSpec = AppMotion.fadeFast
        )
    }
