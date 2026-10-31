> [!NOTE]
> **Historical Audit Snapshot (October 2026)**
> This document is a historical audit report representing an earlier state of the codebase.
> Current security policies, architecture, updater hardening, and privacy guarantees are documented in [README.md](../../README.md), [SECURITY.md](../../SECURITY.md), and [improvement.md](../../improvement.md).

# Independent Principal Engineering Code Audit & Architecture Review

**Application:** MyDoc (`com.abhisheksati.mydoc`)  
**Architecture:** Kotlin, Jetpack Compose (BOM 2024.12.01), Material 3, MVVM, Room 2.6.1, MuPDF (Fitz 1.24.4), LibVLC 3.6.5  
**Review Scope:** Full repository review across all 202 non-build files (no sampling).  
**Phase:** Phase A (Audit Only - Zero Source Files Modified).

---

## 1. Executive Summary

### Overall Health Score: 4.8 / 10

#### Score Justification:
- **Core Document Viewer & PDF Toolkit (+2.5):** MuPDF integration for PDF rendering, page manipulation (split, merge, compress), and custom XML parsers for DOCX, PPTX, and XLSX operate locally without external cloud dependencies.
- **Privacy & Offline Integrity (-2.0):** Despite being billed as a strictly offline, privacy-first app, the codebase contains:
  1. A critical dynamic code loader in `LibreOfficeEngineManager.kt` that fetches external APKs from third-party mirrors and executes native `.so` binaries with world-readable permissions.
  2. The invasive `QUERY_ALL_PACKAGES` permission in `AndroidManifest.xml`, exposing every installed app on the user's device.
- **Stability & Crash Risks (-1.5):**
  1. Missing `USE_BIOMETRIC` permission in the manifest causes a fatal `SecurityException` crash when biometric lock is triggered on Android 9/10/11.
  2. Direct bitmask mismatch in `AudioExtractor.kt` passing `MediaExtractor` flags to `MediaCodec` buffer info causes audio muxing corruption or runtime `IllegalArgumentException`.
- **Concurrency & Compose Lifecycle Pitfalls (-1.0):**
  1. Direct reads of `StateFlow.value` in composition (`HomeScreen.kt`, `MainActivity.kt`) bypass recomposition triggers and lifecycle awareness.
  2. Video player lifecycle and resume position logic suffers from state loss and orientation lock glitches when returning from the Android home screen.
- **Architectural Debt & Redundancy (-0.7):**
  1. Two parallel design/theming systems (`ui.theme` and `designsystem.tokens`) coexist, splitting styling across the app.
  2. Duplicate multimedia engines: both Google Media3 ExoPlayer and VideoLAN LibVLC are bundled, bloating APK size unnecessarily.

---

## 2. Top 10 Critical & High Issues

| # | ID | Severity | Category | File & Line | Summary |
|---|---|---|---|---|---|
| 1 | **SEC-01** | **CRITICAL** | Security & Privacy | `LibreOfficeEngineManager.kt:63-93, 443-461` | Dynamic code loading: downloads APKs from F-Droid over HTTP/HTTPS, sets native `.so` files to world-readable, and executes `System.load()`. |
| 2 | **SEC-02** | **CRITICAL** | Crashes & Lifecycle | `BiometricLockHelper.kt:58` | Missing `android.permission.USE_BIOMETRIC` in `AndroidManifest.xml` triggers fatal `SecurityException` when app lock authenticates. |
| 3 | **SEC-03** | **CRITICAL** | Security & Privacy | `AndroidManifest.xml:25` | `QUERY_ALL_PACKAGES` permission declared. Severe privacy violation; exposes user's full app catalog and violates Google Play policy. |
| 4 | **COR-01** | **HIGH** | Correctness & Bugs | `AudioExtractor.kt:74` | `bufferInfo.flags = extractor.sampleFlags` directly mixes incompatible bitmasks, corrupting audio muxing or crashing HAL. |
| 5 | **SEC-04** | **HIGH** | Security & Privacy | `ApkDownloader.kt:112`, `PackageInstallerHelper.kt:42` | Updater does not enforce SHA-256 (bypassed if `.sha256` asset is omitted) and does not verify APK signature/package name before install. |
| 6 | **LIF-01** | **HIGH** | Crashes & Lifecycle | `MainActivity.kt:131-143`, `VideoPlayerScreen.kt:466-487` | Video playback state and orientation are lost on Home exit; app relaunches into Files tab in forced landscape mode. |
| 7 | **PERF-01** | **HIGH** | Concurrency & Threading | `HomeScreen.kt:180`, `MainActivity.kt:205` | Direct reads of `StateFlow.value` inside Compose bodies skip recomposition updates and leak collectors across lifecycle states. |
| 8 | **ARCH-01** | **HIGH** | Architecture & Quality | `ui/theme/*` vs `designsystem/tokens/*` | Dual parallel theming and component hierarchy with divergent tokens, colors, and duplicate UI elements (`AppDivider`, `EmptyState`). |
| 9 | **PERF-02** | **HIGH** | Memory & Performance | `PdfViewerScreen.kt:642`, `TileBitmapCache.kt:28` | `BoxWithConstraints` inside scrolling list items forces double layout measurement; high-res page bitmaps allocated without explicit recycling. |
| 10 | **COR-02** | **HIGH** | Correctness & Bugs | `LibreOfficeKit.java:31`, `LibreOfficeDocumentRenderer.kt` | Dead/broken JNI bridge attempting to link against non-existent `libsofficeapp.so`, crashing with `UnsatisfiedLinkError`. |

---

## 3. Comprehensive Findings by Category

### Category 1: Correctness and Bugs

#### COR-01: Incompatible Bitmask Assignment in `AudioExtractor`
- **File:** `app/src/main/java/com/abhisheksati/mydoc/media/audio/AudioExtractor.kt:74`
- **Code:**
  ```kotlin
  bufferInfo.flags = extractor.sampleFlags
  ```
- **Evidence & Impact:** `MediaExtractor.sampleFlags` returns values like `SAMPLE_FLAG_SYNC` (1) and `SAMPLE_FLAG_PARTIAL_FRAME` (2). `MediaCodec.BufferInfo.flags` expects `BUFFER_FLAG_KEY_FRAME` (1), `BUFFER_FLAG_CODEC_CONFIG` (2), `BUFFER_FLAG_END_OF_STREAM` (4). Assigning `SAMPLE_FLAG_PARTIAL_FRAME` (2) into `bufferInfo.flags` causes `MediaMuxer` to interpret the sample as a codec configuration packet (`BUFFER_FLAG_CODEC_CONFIG`), resulting in corrupted audio output or an `IllegalArgumentException` crash on OEM hardware encoders.
- **Recommended Fix:** Map extractor flags explicitly:
  ```kotlin
  bufferInfo.flags = if ((extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
      MediaCodec.BUFFER_FLAG_KEY_FRAME
  } else 0
  ```
- **Effort:** Small (S) | **Risk:** Very Low

#### COR-02: Broken Native LibreOffice JNI Integration
- **File:** `app/src/main/java/org/libreoffice/kit/LibreOfficeKit.java:31`
- **Code:**
  ```java
  System.loadLibrary("sofficeapp");
  ```
- **Evidence & Impact:** `libsofficeapp.so` is not bundled in the APK's `jniLibs`. Calling this crashes immediately with `java.lang.UnsatisfiedLinkError: Couldn't load sofficeapp`. The app already has standalone XML parsers for DOCX, PPTX, and XLSX.
- **Recommended Fix:** Delete `org.libreoffice.kit.*` and `LibreOffice*Renderer.kt`. Route all formats strictly to the internal Kotlin XML engines (`DocxParser`, `PptxParser`, `SpreadsheetParser`).
- **Effort:** Medium (M) | **Risk:** Low

#### COR-03: Video Player Seek-to-Resume Race Condition
- **File:** `app/src/main/java/com/abhisheksati/mydoc/ui/media/VideoPlayerScreen.kt:406-412`
- **Code:**
  ```kotlin
  if (!hasResumedPosition && pendingResumePosition > 0L) {
      mediaPlayer.time = pendingResumePosition
      currentPositionMs = pendingResumePosition
      hasResumedPosition = true
  }
  ```
- **Evidence & Impact:** Setting `mediaPlayer.time` immediately inside `MediaPlayer.Event.Playing` races with the native decoder establishing demuxer timestamps. On high-bitrate files, LibVLC overrides the time back to `0L` when the first video keyframe decodes.
- **Recommended Fix:** Defer `mediaPlayer.time` seek until `mediaPlayer.length > 0` and `mediaPlayer.isSeekable` returns `true`.
- **Effort:** Small (S) | **Risk:** Low

---

### Category 2: Crashes, Lifecycle and Threading

#### LIF-01: State Loss and Landscape Lock on Home Exit
- **File:** `app/src/main/java/com/abhisheksati/mydoc/MainActivity.kt:131-143`, `VideoPlayerScreen.kt:466-487`
- **Evidence & Impact:** When a user presses Home during video playback, `activeMediaItem` is asynchronously restored in a `lifecycleScope.launch` in `onCreate`. Because `activeMediaItem` is initially `null`, `setContent` immediately renders `HomeScreen` (Files tab). Since the device was held in landscape, the Files tab opens in landscape mode. When playback resumes, the position is not consistently restored.
- **Recommended Fix:**
  1. Synchronously restore the active media item URI before the initial composition or display a smooth splash/loading transition.
  2. In `VideoPlayerScreen` disposal, explicitly restore `requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED` before unbinding views.
- **Effort:** Medium (M) | **Risk:** Low

#### LIF-02: Coroutine Leak in `VideoPlayerScreen.onDispose`
- **File:** `app/src/main/java/com/abhisheksati/mydoc/ui/media/VideoPlayerScreen.kt:470`
- **Code:**
  ```kotlin
  kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
      mediaRepository.savePlaybackPosition(item.uri.toString(), saveTime, dur)
  }
  ```
- **Evidence & Impact:** Creates an unmanaged `CoroutineScope` with no supervisor job and no lifecycle binding. If the process is backgrounded and killed by the OS immediately after exit, the SQLite transaction is aborted, losing resume timestamps.
- **Recommended Fix:** Inject the application-scoped `CoroutineScope` from `MyDocApp` to perform fire-and-forget database persistence that outlives Composable disposal.
- **Effort:** Small (S) | **Risk:** Low

#### LIF-03: Screen Sleep / Inactivity Timeout During Video Playback
- **File:** `app/src/main/java/com/abhisheksati/mydoc/ui/media/VideoPlayerScreen.kt:318-326`
- **Code:**
  ```kotlin
  DisposableEffect(activity, isPlaying) {
      if (isPlaying) {
          activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      } else {
          activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      }
      onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
  }
  ```
- **Evidence & Impact:** When video controls overlay is shown, or during playback stalls/rebuffering, `isPlaying` momentarily toggles to false, clearing `FLAG_KEEP_SCREEN_ON`. The phone's standard display timeout then kicks in and turns the screen off while the user is watching video.
- **Recommended Fix:** Maintain `FLAG_KEEP_SCREEN_ON` for the entire active lifecycle of `VideoPlayerScreen`, only clearing it when the user explicitly navigates away from the player.
- **Effort:** Small (S) | **Risk:** Very Low

---

### Category 3: Memory and Performance

#### PERF-01: Direct `StateFlow.value` Reads Inside Composition
- **File:** `app/src/main/java/com/abhisheksati/mydoc/ui/home/HomeScreen.kt:180`, `MainActivity.kt:205`
- **Code:**
  ```kotlin
  initialPage = viewModel.currentTab.value.ordinal
  if (!isAppUnlocked && app.settingsRepository.isBiometricLockEnabled.value)
  ```
- **Evidence & Impact:** Flagged by Android Lint as `StateFlowValueCalledInComposition`. Reading `.value` directly inside a composable does not register a Compose state snapshot read. If the underlying `StateFlow` emits a new value, the composable will not recompose.
- **Recommended Fix:** Always use `collectAsStateWithLifecycle()` to observe `StateFlow` instances.
- **Effort:** Small (S) | **Risk:** Very Low

#### PERF-02: `BoxWithConstraints` Inside Lazy Page Items
- **File:** `app/src/main/java/com/abhisheksati/mydoc/ui/viewer/pdf/PdfViewerScreen.kt:642-647`
- **Evidence & Impact:** `PdfPageItem` wraps each page in a `BoxWithConstraints` but never inspects `constraints.maxWidth` or `constraints.maxHeight`. `BoxWithConstraints` defers composition and runs an extra layout measurement pass for every page item scrolled into view, causing dropped frames on 120Hz displays.
- **Recommended Fix:** Replace `BoxWithConstraints` with standard `Box(modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio))`.
- **Effort:** Small (S) | **Risk:** Very Low

#### PERF-03: Unrecycled Bitmaps in PDF Continuous Scroll
- **File:** `app/src/main/java/com/abhisheksati/mydoc/ui/viewer/pdf/PdfViewerScreen.kt:620-634`
- **Evidence & Impact:** Fast scrolling through a 200+ page PDF generates dozens of `Bitmap` instances via `viewModel.renderPageBitmap(pageIndex, 1.8f)`. When discarded by Compose, these bitmaps are left to the Dalvik/ART Garbage Collector rather than being recycled or pooled into `inBitmap`, creating severe GC pauses and memory churn.
- **Recommended Fix:** Integrate `TileBitmapCache` bitmap pooling to reuse allocated byte buffers for incoming pages.
- **Effort:** Medium (M) | **Risk:** Medium

---

### Category 4: Security and Privacy

#### SEC-01: Dynamic Code Loading and External APK Download
- **File:** `app/src/main/java/com/abhisheksati/mydoc/rendering/engine/libreoffice/LibreOfficeEngineManager.kt:63-93, 443-461`
- **Evidence & Impact:** The manager downloads an external APK from F-Droid mirrors (`f-droid.org`, `mirror.cyberbits.eu`), extracts `.so` files to `app_libreoffice/lib/`, runs `file.setReadable(true, false)` (world-readable), and calls `System.load(file.absolutePath)`. This is a textbook Dynamic Code Loading (DCL) vulnerability that completely breaks the offline security posture.
- **Recommended Fix:** Delete `LibreOfficeEngineManager.kt` entirely.
- **Effort:** Medium (M) | **Risk:** Low

#### SEC-02: Missing Biometric Permission Crash
- **File:** `app/src/main/java/com/abhisheksati/mydoc/security/BiometricLockHelper.kt:58`
- **Evidence & Impact:** `prompt.authenticate()` requires `android.permission.USE_BIOMETRIC`. Android Lint flags this as a fatal compilation error. At runtime on API 28–30, missing this permission throws `java.lang.SecurityException`, immediately crashing the app when the user attempts to unlock.
- **Recommended Fix:** Add `<uses-permission android:name="android.permission.USE_BIOMETRIC" />` to `app/src/main/AndroidManifest.xml`.
- **Effort:** Small (S) | **Risk:** None

#### SEC-03: Excessive `QUERY_ALL_PACKAGES` Permission
- **File:** `app/src/main/AndroidManifest.xml:25`
- **Evidence & Impact:** Declares `<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />`. A local document viewer does not need total visibility into all installed user applications.
- **Recommended Fix:** Remove `QUERY_ALL_PACKAGES`. If APK management is retained, declare specific intent queries (`<queries>`) for package installers.
- **Effort:** Small (S) | **Risk:** Low

#### SEC-04: Updater Integrity and Signature Verification Gaps
- **File:** `app/src/main/java/com/abhisheksati/mydoc/update/ApkDownloader.kt:112`, `PackageInstallerHelper.kt:42`
- **Evidence & Impact:** If a release tag does not upload a `.sha256` asset, `expectedSha256` is `null`, and the downloader installs the APK without checksum verification. Additionally, `PackageInstallerHelper` does not parse the downloaded APK's `PackageInfo` to confirm that the package name matches `com.abhisheksati.mydoc` and that its signing certificate matches the currently running app.
- **Recommended Fix:**
  1. Make SHA-256 validation mandatory; fail download if hash is missing or mismatched.
  2. In `PackageInstallerHelper`, call `context.packageManager.getPackageArchiveInfo(apkFile.path, PackageManager.GET_SIGNING_CERTIFICATES)` and verify package name and certificate signatures before launching the installation intent.
- **Effort:** Medium (M) | **Risk:** Low

---

### Category 5: Data and Persistence

#### DATA-01: Deprecated `MediaStore.Images.Media.DATA` Direct File Paths
- **File:** `app/src/main/java/com/abhisheksati/mydoc/data/repository/MediaRepositoryImpl.kt:88`
- **Evidence & Impact:** Direct use of `MediaStore.MediaColumns.DATA` is deprecated on Android 10+ (API 29+) and returns virtual or restricted paths on Android 11+. If the file is modified or moved, raw `File(path)` operations fail.
- **Recommended Fix:** Store content URIs (`content://media/external/...`) and query metadata via `ContentResolver.openFileDescriptor`.
- **Effort:** Medium (M) | **Risk:** Low

#### DATA-02: Missing Room Migration Strategy
- **File:** `app/src/main/java/com/abhisheksati/mydoc/data/local/AppDatabase.kt:22`
- **Evidence & Impact:** `RoomDatabase` is built with `fallbackToDestructiveMigration()`. If a new database version is published without explicit migrations, all user favorites, viewing states, and playback resume timestamps are wiped.
- **Recommended Fix:** Enable `exportSchema = true` and provide explicit migration definitions (`Migration(1, 2)`).
- **Effort:** Medium (M) | **Risk:** Medium

---

### Category 6: Build, Dependencies and Release

#### BLD-01: Hardcoded Debug Keystore for Release Builds
- **File:** `app/build.gradle.kts:24-31, 50`
- **Evidence & Impact:** Release builds are signed with `keystore/debug.keystore` using hardcoded password `"android"` checked into git. Any party can sign a malicious APK with the same certificate and overwrite the app.
- **Recommended Fix:** Configure release signing via Gradle properties or environment variables (`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`), falling back to debug keystore only for local developer builds.
- **Effort:** Small (S) | **Risk:** Low

#### BLD-02: Redundant Media3 ExoPlayer Dependencies
- **File:** `app/build.gradle.kts:108-110`
- **Evidence & Impact:** Both Media3 ExoPlayer and LibVLC are linked. ExoPlayer is completely unused in the UI, adding ~6MB to the APK and inflating build times.
- **Recommended Fix:** Remove `androidx-media3-exoplayer`, `androidx-media3-ui`, and `androidx-media3-session`.
- **Effort:** Small (S) | **Risk:** Very Low

---

### Category 7: Dead Code, Unused and Redundant Code

#### DEAD-01: Duplicate Theming and Common Components
- **File:** `app/src/main/java/com/abhisheksati/mydoc/ui/theme/*`, `ui/common/*`
- **Evidence & Impact:** `ui.theme` duplicates `designsystem.tokens`, and `ui/common/AppDivider.kt` duplicates `designsystem/components/AppDivider.kt`.
- **Recommended Fix:** Migrate callers to `designsystem` and delete redundant packages.
- **Effort:** Small (S) | **Risk:** Low

#### DEAD-02: Redundant Top Version Display in Settings
- **File:** `app/src/main/java/com/abhisheksati/mydoc/ui/settings/SettingsScreen.kt:180-240`
- **Evidence & Impact:** Version name is displayed twice in Settings (top hero card and bottom about section).
- **Recommended Fix:** Remove the top version display card.
- **Effort:** Small (S) | **Risk:** Very Low

---

### Category 8: Architecture and Code Quality

#### ARCH-02: 1,920-Line God Composable in `VideoPlayerScreen`
- **File:** `app/src/main/java/com/abhisheksati/mydoc/ui/media/VideoPlayerScreen.kt`
- **Evidence & Impact:** Houses VLC engine initialization, gesture recognition (brightness, volume, seek), audio track selection, subtitle dialogs, sleep timers, equalizer, screenshot captures, and orientation listeners all within one monolithic composable.
- **Recommended Fix:** Extract UI subcomponents (`VideoPlayerControls`, `VideoTrackSelectionDialog`, `VideoGestureHandler`) into dedicated files.
- **Effort:** Large (L) | **Risk:** Medium

---

### Category 9: UI, UX and Design System Consistency

#### UI-01: Nested "Box-Inside-Box" Clutter in Super Tools
- **File:** `app/src/main/java/com/abhisheksati/mydoc/tools/ui/ToolsScreen.kt:120-180`
- **Evidence & Impact:** Super Tools renders nested border cards and glowing pill containers, conflicting with the user's explicit minimal, un-cluttered design rules.
- **Recommended Fix:** Remove nested inner containers and simplify tool action cards to clean single-surface rows.
- **Effort:** Small (S) | **Risk:** Very Low

#### UI-02: Navigation Bar Inconsistency Across Tabs
- **File:** `app/src/main/java/com/abhisheksati/mydoc/ui/home/HomeScreen.kt:220-290`
- **Evidence & Impact:** Navigation bar styling diverges between the Files tab and the other tabs (Documents, Media, Tools).
- **Recommended Fix:** Standardize navigation bar across all tabs to match the clean Files tab specification.
- **Effort:** Small (S) | **Risk:** Very Low

---

### Category 10: Tests and Reliability

#### TST-01: Zero Unit Tests for Cryptographic Vault & Media Muxing
- **Evidence & Impact:** While basic repository and update tests exist (33 passing unit tests), `VaultManager` (AES-GCM encryption), `AudioExtractor` (bitmask muxing), and `PdfToolsEngine` have zero unit test coverage.
- **Recommended Fix:** Add JUnit tests covering vault encryption/decryption roundtrips, corrupted file opening, and sample flag bitmask mappings.
- **Effort:** Medium (M) | **Risk:** Very Low

---

## 4. Verification & Appendix

### Build Status:
- `./gradlew testDebugUnitTest`: **PASSED (33 tests pass in 6.4s)**
- `./gradlew assembleDebug`: **PASSED**

### Android Lint Report Summary:
- **Total Issues Found:** 9 Errors, 129 Warnings
- **Key Verbatim Lint Errors:**
  1. `BiometricLockHelper.kt:58`: `MissingPermission`: Missing permissions required by `BiometricPrompt.authenticate`: `android.permission.USE_BIOMETRIC`
  2. `AudioExtractor.kt:74`: `WrongConstant`: Must be one or more of: `MediaCodec.BUFFER_FLAG_KEY_FRAME`, `MediaCodec.BUFFER_FLAG_CODEC_CONFIG`, `MediaCodec.BUFFER_FLAG_END_OF_STREAM`
  3. `HomeScreen.kt:180`: `StateFlowValueCalledInComposition`: StateFlow.value should not be called within composition
  4. `MainActivity.kt:205`: `StateFlowValueCalledInComposition`: StateFlow.value should not be called within composition
  5. `PdfViewerScreen.kt:620`: `ProduceStateDoesNotAssignValue`: A produceState call must assign a statement to value
  6. `ThumbnailStrip.kt:85`: `ProduceStateDoesNotAssignValue`: A produceState call must assign a statement to value
  7. `PhotoViewerScreen.kt:129`: `ProduceStateDoesNotAssignValue`: A produceState call must assign a statement to value
