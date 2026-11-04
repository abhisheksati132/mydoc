package com.abhisheksati.mydoc.ui.viewer.controls

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange

/**
 * Watches a deliberate horizontal fling without consuming it, and acts only when the reader has
 * nowhere left to go in this direction. Mid-document page turns stay page turns; a fling past the
 * first or last page walks to the neighbouring document, and root tabs keep their own swipe.
 *
 * The detection keys on Unit and reads the latest callbacks through rememberUpdatedState: the
 * surrounding viewer recomposes continuously while scrolling, and keying on the lambdas would
 * restart the detector mid-gesture and drop every fling.
 */
@Composable
fun Modifier.edgeFlingToDocument(
    atFirstPage: () -> Boolean,
    atLastPage: () -> Boolean,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?
): Modifier {
    val currentAtFirst = rememberUpdatedState(atFirstPage)
    val currentAtLast = rememberUpdatedState(atLastPage)
    val currentPrevious = rememberUpdatedState(onPrevious)
    val currentNext = rememberUpdatedState(onNext)

    return pointerInput(Unit) {
        awaitEachGesture {
            var dx = 0f
            var dy = 0f
            var sawDown = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val pressed = event.changes.filter { it.pressed }
                if (pressed.isEmpty()) {
                    if (sawDown) break
                    continue
                }
                sawDown = true
                pressed.forEach { change ->
                    // positionChange() is the non-mutating read, so the pager underneath still gets
                    // the full gesture; the first event reports a zero delta.
                    val delta = change.positionChange()
                    dx += delta.x
                    dy += delta.y
                }
            }
            if (kotlin.math.abs(dx) > 220f && kotlin.math.abs(dx) > kotlin.math.abs(dy) * 1.5f) {
                if (dx < 0f && currentAtLast.value()) currentNext.value?.invoke()
                else if (dx > 0f && currentAtFirst.value()) currentPrevious.value?.invoke()
            }
        }
    }
}
