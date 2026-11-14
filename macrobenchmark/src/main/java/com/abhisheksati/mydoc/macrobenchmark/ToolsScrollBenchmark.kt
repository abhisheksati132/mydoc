package com.abhisheksati.mydoc.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Scrolling frame budget. The Tools grid is the densest always-present Compose surface (24 glass
 * cards over 6 categories), so it measures real frame pacing on a fresh install with no documents.
 */
@RunWith(AndroidJUnit4::class)
class ToolsScrollBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun toolsGridFling() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.WARM,
        compilationMode = CompilationMode.None(),
        setupBlock = {
            startActivityAndWait()
            device.wait(Until.hasObject(By.text("Tools")), 5_000)
            device.findObject(By.text("Tools"))?.click()
            device.waitForIdle(1_000)
        }
    ) {
        flingGrid()
    }

    private fun MacrobenchmarkScope.flingGrid() {
        val centerX = device.displayWidth / 2
        val fromY = (device.displayHeight * 0.68f).toInt()
        val toY = (device.displayHeight * 0.32f).toInt()
        repeat(4) {
            device.swipe(centerX, fromY, centerX, toY, 12)
            device.waitForIdle(250)
        }
        repeat(4) {
            device.swipe(centerX, toY, centerX, fromY, 12)
            device.waitForIdle(250)
        }
    }
}
