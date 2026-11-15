package com.abhisheksati.mydoc.macrobenchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Produces the bundled baseline profile. Run on a device or emulator:
 *   ./gradlew :macrobenchmark:connectedBenchmarkAndroidTest \
 *     -Pandroid.testInstrumentationRunnerArguments.class=com.abhisheksati.mydoc.macrobenchmark.BaselineProfileGenerator
 *
 * Then copy the generated baseline-prof.txt into app/src/main/baseline-prof.txt.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun profile() = rule.collect(
        packageName = TARGET_PACKAGE,
        includeInStartupProfile = true
    ) {
        pressHome()
        startActivityAndWait()

        device.wait(Until.hasObject(By.text("Library")), 8_000)
        device.findObject(By.text("Library"))?.click()
        device.waitForIdle(1_000)
        device.wait(Until.hasObject(By.text("Internal storage")), 8_000)
        device.swipe(
            device.displayWidth / 2,
            device.displayHeight * 3 / 4,
            device.displayWidth / 2,
            device.displayHeight / 4,
            12
        )
        device.waitForIdle(1_000)

        device.findObject(By.text("Tools"))?.click()
        device.waitForIdle(1_000)
        device.wait(Until.hasObject(By.text("Merge PDF")), 8_000)

        device.findObject(By.text("Home"))?.click()
        device.waitForIdle(1_000)
        device.wait(Until.hasObject(By.text("MyDoc")), 8_000)
    }
}
