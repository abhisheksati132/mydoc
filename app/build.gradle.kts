plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

fun getGitCommitCount(): Int {
    return try {
        val process = ProcessBuilder("git", "rev-list", "--count", "HEAD").start()
        val count = process.inputStream.bufferedReader().readText().trim().toIntOrNull()
        count ?: 1
    } catch (_: Exception) {
        1
    }
}

val commitCount = getGitCommitCount()
// Start version 1.1.x series cleanly at 1.1.0 (baseline at commit 97)
val patchNumber = (commitCount - 97).coerceAtLeast(0)

android {
    namespace = "com.abhisheksati.mydoc"
    compileSdk = 35

    signingConfigs {
        create("shared") {
            storeFile = file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "mydoc"
            keyPassword = "android"
        }
    }

    defaultConfig {
        applicationId = "com.abhisheksati.mydoc"
        minSdk = 26
        targetSdk = 35
        versionCode = commitCount
        versionName = "1.1.$patchNumber"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("shared")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("shared")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(libs.mupdf.fitz)
    implementation(libs.pdfbox.android)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // LibVLC official VideoLAN engine (Sole high-performance video player)
    implementation("org.videolan.android:libvlc-all:3.6.5")

    // Phase 8: Coil for image loading, caching & GIF/WebP support
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.video)

    // Phase 8: ExifInterface for offline EXIF orientation and metadata
    implementation(libs.androidx.exifinterface)

    // ML Kit On-Device Text Recognition (Offline OCR)
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // ML Kit On-Device Document Scanner (Edge detection, perspective fix, multi-page)
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")

    // ZXing for Wi-Fi Transfer QR code generation
    implementation("com.google.zxing:core:3.5.3")

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation("net.sf.kxml:kxml2:2.3.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    debugImplementation(libs.compose.ui.tooling)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
