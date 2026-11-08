package com.abhisheksati.mydoc.designsystem.animation

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PressFeedbackTest {
    @Test fun pressAndReleaseInTheSameFrameStillHoldFeedbackForNinetyMilliseconds() = runTest {
        val source = MutableInteractionSource()
        val events = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            animatePressFeedback(source, { testScheduler.currentTime },
                { events.add("dip"); delay(10_000) }, { events.add("return") }, { events.add("reset") })
        }
        val press = PressInteraction.Press(Offset.Zero)
        source.emit(press)
        source.emit(PressInteraction.Release(press))
        runCurrent()
        assertEquals(listOf("dip"), events)
        advanceTimeBy(89); runCurrent()
        assertEquals(listOf("dip"), events)
        advanceTimeBy(1); runCurrent()
        assertEquals(listOf("dip", "return"), events)
    }

    @Test fun scrollingCancelsFeedbackImmediatelyWithoutAReleaseBounce() = runTest {
        val source = MutableInteractionSource()
        val events = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            animatePressFeedback(source, { testScheduler.currentTime },
                { events.add("dip"); delay(10_000) }, { events.add("return") }, { events.add("reset") })
        }
        val press = PressInteraction.Press(Offset.Zero)
        source.emit(press); runCurrent()
        source.emit(PressInteraction.Cancel(press)); runCurrent()
        assertEquals(listOf("dip", "reset"), events)
        advanceTimeBy(500); runCurrent()
        assertEquals(listOf("dip", "reset"), events)
    }
}
