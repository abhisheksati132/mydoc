package com.abhisheksati.mydoc.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cold-start budget for the shipped build. Run with:
 *   ./gradlew :macrobenchmark:connectedBenchmarkAndroidTest
 *
 * CompilationMode.None is used because baseline-profile compilation needs a rooted device or
 * AOSP image. On such a device, swap in CompilationMode.Partial(baselineProfileMode = Require)
 * to measure the profile users actually get.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartup() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD,
        compilationMode = CompilationMode.None(),
        setupBlock = { pressHome() }
    ) {
        startActivityAndWait()
    }
}
