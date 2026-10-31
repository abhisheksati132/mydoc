> [!NOTE]
> **Historical Audit Snapshot (October 2026)**
> This document is a historical audit report representing an earlier state of the codebase.
> Current security policies, architecture, updater hardening, and privacy guarantees are documented in [README.md](../../README.md), [SECURITY.md](../../SECURITY.md), and [improvement.md](../../improvement.md).

# Dependencies, Licenses, and Build Configuration Audit

**Target Application:** `com.abhisheksati.mydoc`  
**Build System:** Gradle 8.7.3 / Android Gradle Plugin 8.7.3 / Kotlin 2.1.0 / KSP 2.1.0-1.0.29  
**Target SDK:** 35 | **Compile SDK:** 35 | **Min SDK:** 26  
**JVM Target:** Java 17

---

## 1. Full Dependency Inventory & License Review

| Group & Artifact | Version | Declared Scope | License | Purpose & Usage in Code | Outdated / Risk Analysis |
|---|---|---|---|---|---|
| `androidx.core:core-ktx` | `1.15.0` | `implementation` | Apache 2.0 | Android KTX system extensions. | Current stable. No known CVE. |
| `androidx.lifecycle:lifecycle-runtime-ktx` | `2.8.7` | `implementation` | Apache 2.0 | Lifecycle coroutine scopes & observers. | Current stable. No known CVE. |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | `2.8.7` | `implementation` | Apache 2.0 | Compose ViewModel integration. | Current stable. No known CVE. |
| `androidx.lifecycle:lifecycle-runtime-compose` | `2.8.7` | `implementation` | Apache 2.0 | Lifecycle-aware StateFlow collection in Compose. | Current stable. Note: frequently bypassed in `HomeScreen.kt` and `MainActivity.kt`. |
| `androidx.activity:activity-compose` | `1.9.3` | `implementation` | Apache 2.0 | `ComponentActivity.setContent` entry point. | Current stable. No known CVE. |
| `androidx.compose:compose-bom` | `2024.12.01` | `platform` | Apache 2.0 | Jetpack Compose Bill of Materials (1.7.6 runtime). | Stable December 2024 BOM. |
| `androidx.compose.ui:ui` | BOM | `implementation` | Apache 2.0 | Jetpack Compose core UI primitives. | Safe. |
| `androidx.compose.ui:ui-graphics` | BOM | `implementation` | Apache 2.0 | Compose graphics and Canvas APIs. | Safe. |
| `androidx.compose.material3:material3` | BOM | `implementation` | Apache 2.0 | Material Design 3 UI components. | Safe. |
| `androidx.compose.material:material-icons-extended` | `1.7.6` | `implementation` | Apache 2.0 | Full extended icon vector set. | **HIGH APK SIZE RISK:** Bundles hundreds of vector drawables into compilation. R8 shrinks unused, but builds are slower. |
| `androidx.room:room-runtime` | `2.6.1` | `implementation` | Apache 2.0 | SQLite Room ORM database runtime. | Current stable. |
| `androidx.room:room-ktx` | `2.6.1` | `implementation` | Apache 2.0 | Coroutines Flow support for Room DAOs. | Current stable. |
| `androidx.room:room-compiler` | `2.6.1` | `ksp` | Apache 2.0 | Room annotation processor code generator. | Stable with KSP 2.1.0. |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | `1.10.1` | `implementation` | Apache 2.0 | Coroutine dispatchers (`Dispatchers.Main`, `Dispatchers.IO`). | Modern coroutine runtime. Safe. |
| `com.artifex.mupdf:fitz` | `1.24.4` | `implementation` | **AGPLv3 / Commercial** | High-performance PDF/XPS/CBZ native rendering & manipulation. | **CRITICAL LICENSE & NATIVE RISK:** AGPLv3 requires full open-sourcing if network-distributed, or purchase of a commercial license from Artifex. For personal offline use it is permissible, but must not be embedded in a commercial closed-source binary. |
| `androidx.camera:camera-core` | `1.4.1` | `implementation` | Apache 2.0 | CameraX camera controller primitives. | Stable camera stack. |
| `androidx.camera:camera-camera2` | `1.4.1` | `implementation` | Apache 2.0 | Camera2 pipeline implementation for CameraX. | Safe. |
| `androidx.camera:camera-lifecycle` | `1.4.1` | `implementation` | Apache 2.0 | Lifecycle binding for CameraX preview. | Safe. |
| `androidx.camera:camera-view` | `1.4.1` | `implementation` | Apache 2.0 | `PreviewView` Compose AndroidView integration. | Safe. |
| `androidx.media3:media3-exoplayer` | `1.5.1` | `implementation` | Apache 2.0 | Google Media3 ExoPlayer audio/video engine. | **REDUNDANT DUPLICATE:** App also includes LibVLC. If LibVLC is the primary player, ExoPlayer contributes ~6MB unused overhead. |
| `androidx.media3:media3-ui` | `1.5.1` | `implementation` | Apache 2.0 | Media3 PlayerView controls. | Redundant if LibVLC is default. |
| `androidx.media3:media3-session` | `1.5.1` | `implementation` | Apache 2.0 | Background media playback session. | Redundant if LibVLC is default. |
| `org.videolan.android:libvlc-all` | `3.6.5` | `implementation` | **LGPLv2.1+** | Official VideoLAN LibVLC multimedia engine with native codecs. | Bundles large `.so` binaries for all ABIs (~35MB). Requires ABI split or dynamic packaging. Dynamic linking compliant with LGPLv2.1. |
| `io.coil-kt:coil-compose` | `2.7.0` | `implementation` | Apache 2.0 | Asynchronous image loading for Compose (`AsyncImage`). | Current stable Coil 2.x. |
| `io.coil-kt:coil-gif` | `2.7.0` | `implementation` | Apache 2.0 | Animated GIF decoding decoder. | Safe. |
| `io.coil-kt:coil-video` | `2.7.0` | `implementation` | Apache 2.0 | Frame extraction decoder for video thumbnails. | Safe. |
| `androidx.exifinterface:exifinterface` | `1.3.7` | `implementation` | Apache 2.0 | EXIF metadata reader and orientation parser. | Stable. Safe. |
| `com.google.mlkit:text-recognition` | `16.0.1` | `implementation` | Google APIs Terms / Apache runtime | On-device offline OCR model runner. | **DOWNLOAD RISK:** Thin client. Depends on Google Play Services downloading the OCR model to the device dynamically on first use. If device is pure offline or ungoogled (AOSP/GrapheneOS), OCR fails silently unless bundled model variant is used. |
| `com.google.zxing:core` | `3.5.3` | `implementation` | Apache 2.0 | QR code generation matrix for local Wi-Fi sharing. | Safe. Low footprint. |
| `junit:junit` | `4.13.2` | `testImplementation` | EPL 1.0 | Unit test runner. | Standard. |
| `net.sf.kxml:kxml2` | `2.3.0` | `testImplementation` | BSD | Lightweight XML parser for unit tests. | Safe. |
| `androidx.test.ext:junit` | `1.2.1` | `androidTestImplementation` | Apache 2.0 | AndroidX instrumented test runner. | Safe. |
| `androidx.test.espresso:espresso-core` | `3.6.1` | `androidTestImplementation` | Apache 2.0 | UI testing automation. | Safe. |

---

## 2. Duplicate & Conflicting Dependencies

1. **Dual Video Engines: ExoPlayer (Media3 1.5.1) vs LibVLC (3.6.5):**
   - Both `androidx.media3:media3-exoplayer` and `org.videolan.android:libvlc-all` are compiled into the application.
   - User requirement explicitly specifies keeping LibVLC as the sole video player engine.
   - Removing `androidx.media3:*` dependencies will immediately eliminate ~6MB of redundant dex code, classes, and native extractors.

2. **Dual Theming Systems in Source Code:**
   - Source code maintains two distinct theme structures:
     - `com.abhisheksati.mydoc.ui.theme` (Legacy)
     - `com.abhisheksati.mydoc.designsystem.tokens` (New)
   - While not a Gradle dependency conflict, this represents significant internal coupling and binary overhead.

---

## 3. Signing, R8 / ProGuard, and Release Packaging Risks

### Hardcoded Debug Keystore for Release Builds
- **Location:** `app/build.gradle.kts:24-31`, lines 50, 58.
```kotlin
signingConfigs {
    create("shared") {
        storeFile = file("keystore/debug.keystore")
        storePassword = "android"
        keyAlias = "mydoc"
        keyPassword = "android"
    }
}
buildTypes {
    release {
        signingConfig = signingConfigs.getByName("shared")
    }
}
```
- **Evidence & Impact:** The `debug.keystore` is stored directly in git at `/app/keystore/debug.keystore`. Release APKs are signed with this publicly known keystore and password (`"android"`). Anyone can forge an APK update matching this signature. For local personal use this allows seamless installation over debug builds, but in production this breaks Android signature trust.

### ABI Splits & Native Library Sizing
- `app/build.gradle.kts:84-91` configures ABI splits:
```kotlin
splits {
    abi {
        isEnable = true
        reset()
        include("arm64-v8a", "armeabi-v7a", "x86_64")
        isUniversalApk = true
    }
}
```
- `isUniversalApk = true` creates a massive fat universal APK bundling MuPDF and LibVLC native `.so` files for all 3 architectures simultaneously, resulting in a ~90MB+ universal APK. Device-targeted APKs (`arm64-v8a`) are ~32MB.

### ProGuard / R8 Keep Rules Audit (`app/proguard-rules.pro`)
- ProGuard rules correctly preserve Fitz (MuPDF JNI) and LibVLC native entry points.
- Missing keep rules for Room generated classes and Reflection serialization in GitHub updater JSON parser. If R8 obfuscation is enabled in release, `GitHubUpdateService` JSON field mapping must be verified against minification.
