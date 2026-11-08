package com.abhisheksati.mydoc.designsystem.animation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Rect

/**
 * The motion contract. Crisp and fast, minimal bounce, one grammar:
 *
 * - Hero (list row -> viewer): the element itself carries the transition; containers cross-fade.
 * - Detail (container -> viewer): rises ~10% of the viewport, settles, never overshoots.
 * - Peer (tab <-> tab, screen <-> screen): shared horizontal axis, ~1/6 of the width.
 * - Press: a short 0.972 dip, then a restrained spring return. See PressScaleEffect.
 *
 * If a screen needs motion, it uses these — no ad-hoc springs.
 */
object AppMotion {
    const val FAST = 160
    const val BASE = 220

    /** Default settle: quick, effectively no overshoot. */
    fun <T> snappy(): FiniteAnimationSpec<T> =
        tween(BASE, easing = FastOutSlowInEasing)

    /** Softer settle for large surfaces. */
    fun <T> settle(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow)

    /** Shared jelly press timings; cancel and reduced-motion paths remain still. */
    fun <T> pressIn(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.72f, stiffness = 900f)
    fun <T> pressOut(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.62f, stiffness = 420f)

    /** Press-in/out spring with a hint of momentum. */
    fun <T> press(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium)

    val fadeFast: FiniteAnimationSpec<Float> = tween(FAST, easing = FastOutSlowInEasing)
    val fadeBase: FiniteAnimationSpec<Float> = tween(BASE, easing = FastOutSlowInEasing)

    /** Entering a document, photo, video or audio viewer. */
    fun detailEnter(): EnterTransition =
        slideInVertically(animationSpec = snappy(), initialOffsetY = { it / 24 }) + fadeIn(fadeBase)

    fun detailExit(): ExitTransition =
        fadeOut(fadeFast)

    /** Returning from a viewer back to the list it came from. */
    fun detailReturnEnter(): EnterTransition =
        fadeIn(fadeBase)

    fun detailReturnExit(): ExitTransition =
        slideOutVertically(animationSpec = snappy(), targetOffsetY = { it / 24 }) + fadeOut(fadeFast)

    /** Peer screens: Settings and other horizontally-stacked destinations. */
    fun peerEnter(forward: Boolean): EnterTransition =
        slideInHorizontally(animationSpec = snappy()) { width ->
            if (forward) width / 6 else -width / 6
        } + fadeIn(fadeBase)

    fun peerExit(forward: Boolean): ExitTransition =
        slideOutHorizontally(animationSpec = snappy()) { width ->
            if (forward) -width / 8 else width / 8
        } + fadeOut(fadeFast)

    /** Fallback for any two screens without a spatial relationship. */
    fun neutralEnter(): EnterTransition = fadeIn(fadeBase)
    fun neutralExit(): ExitTransition = fadeOut(fadeFast)

    /**
     * Hero hand-off: the shared element morphs between the two screens, so the
     * containers only cross-fade. Sliding as well would double the motion.
     */
    fun heroEnter(): EnterTransition =
        fadeIn(tween(durationMillis = BASE, delayMillis = 40, easing = FastOutSlowInEasing))

    fun heroExit(): ExitTransition = fadeOut(fadeFast)

    /** Bounds spec for the shared element itself: long, no overshoot. */
    fun heroBounds(): FiniteAnimationSpec<Rect> =
        spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow)
}
