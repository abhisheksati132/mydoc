plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// --- Single Source of Truth for versioning -----------------------------------
// To release a new version:
//   1. Bump APP_VERSION_NAME below (e.g. "1.2.1")
//   2. Increment APP_VERSION_CODE by 1 (must always increase)
//   3. Build: ./gradlew assembleRelease
//   4. Tag & push: git tag v1.2.1 && git push origin v1.2.1
//   5. Create GitHub Release with tag "v1.2.1" â€” the in-app updater reads this tag
// -----------------------------------------------------------------------------
val APP_VERSION_NAME = "1.2.0"
val APP_VERSION_CODE = 117  // Increment by 1 every release, never decrease

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

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    defaultConfig {
        applicationId = "com.abhisheksati.mydoc"
        minSdk = 26
        targetSdk = 35
        versionCode = APP_VERSION_CODE
        versionName = APP_VERSION_NAME

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

    // LibVLC official VideoLAN engine
    implementation("org.videolan.android:libvlc-all:3.6.5")

    // Coil for document thumbnail loading & caching + GIF/Video frame decoding
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.video)

    // Phase 8: ExifInterface for offline EXIF orientation and metadata
    implementation(libs.androidx.exifinterface)

    // ML Kit On-Device Text Recognition (Offline OCR)
    implementation("com.google.mlkit:text-recognition:16.0.1")

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
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    debugImplementation(libs.compose.ui.tooling)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
