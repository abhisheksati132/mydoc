plugins {
    alias(libs.plugins.android.test)
}

android {
    namespace = "com.abhisheksati.mydoc.macrobenchmark"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Emulator numbers are indicative only, but a suppressed run is the only way CI or a
        // dev machine without a spare phone can catch frame-time regressions before shipping.
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
    }

    // Benchmarks drive the real app, not a copy of it.
    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    buildTypes {
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.benchmark.macro.junit4)
    implementation(libs.uiautomator)
}
