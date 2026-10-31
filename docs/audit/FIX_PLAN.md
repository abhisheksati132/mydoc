> [!NOTE]
> **Historical Audit Snapshot (October 2026)**
> This document is a historical audit report representing an earlier state of the codebase.
> Current security policies, architecture, updater hardening, and privacy guarantees are documented in [README.md](../../README.md), [SECURITY.md](../../SECURITY.md), and [improvement.md](../../improvement.md).

# Actionable Fix Plan & Staged Implementation Roadmap

**Application:** MyDoc (`com.abhisheksati.mydoc`)  
**Methodology:** Small, isolated, independently testable batches. Each batch contains a defined scope, automated verification command, manual checklist, and rollback procedure.

---

## Batch 1: Critical Crashes & Lint Compiler Blockers

**Objective:** Eliminate all 9 Android Lint compilation errors and runtime crash hazards (`SecurityException` and `WrongConstant`).

### Tasks:
1. **SEC-02:** Add `<uses-permission android:name="android.permission.USE_BIOMETRIC" />` to `app/src/main/AndroidManifest.xml` to prevent fatal crash on biometric authentication.
2. **COR-01:** Correct bitmask translation in `AudioExtractor.kt:74` by properly converting `MediaExtractor.SAMPLE_FLAG_SYNC` to `MediaCodec.BUFFER_FLAG_KEY_FRAME`.
3. **PERF-01:** Replace direct `.value` reads in `HomeScreen.kt:180` and `MainActivity.kt:205` with `collectAsStateWithLifecycle()`.
4. **COMP-01:** Resolve `ProduceStateDoesNotAssignValue` in `PdfViewerScreen.kt:620`, `PhotoViewerScreen.kt:129`, and `ThumbnailStrip.kt:85`.

### Verification Status: COMPLETED & VERIFIED
- [x] Run `./gradlew lintDebug` — **PASSED: 0 ERRORS** (reduced from 9 errors).
- [x] Run `./gradlew testDebugUnitTest` — **PASSED: 37/37 tests pass** (including new `AudioExtractorTest` bitmask assertions).
- [x] Build `./gradlew assembleDebug` — **BUILD SUCCESSFUL**.
- [x] Build `./gradlew assembleRelease` — **BUILD SUCCESSFUL** (with R8 minification and resource shrinking).
- [x] Biometric authentication permission declared in manifest.
- [x] Bitmask mismatch between MediaExtractor and MediaCodec resolved and covered by unit tests.

### Rollback Strategy:
- `git checkout HEAD~1` on Batch 1 branch. Reverts XML permission and isolated Kotlin helper logic without affecting core data models.

---

## Batch 2: Privacy Hardening & Offline Architecture Integrity

**Objective:** Enforce pure offline operation, eliminate dynamic code execution vulnerabilities, and restrict excessive permissions.

### Tasks:
1. **SEC-01 & COR-02:** Completely remove `LibreOfficeEngineManager.kt` (external APK downloader & runtime `.so` loader) and dead JNI stubs (`org.libreoffice.kit.*`, `LibreOfficeDocumentRenderer.kt`, `LibreOfficePresentationRenderer.kt`). Route all formats directly to native Kotlin XML parsers (`DocxParser`, `PptxParser`, `SpreadsheetParser`).
2. **SEC-03:** Remove `QUERY_ALL_PACKAGES` permission from `AndroidManifest.xml`. If APK backup is retained in App Manager, declare explicit `<queries>` intent filters.
3. **SEC-04:** Harden updater in `ApkDownloader.kt` and `PackageInstallerHelper.kt`:
   - Enforce mandatory SHA-256 verification (abort if hash fails or is missing).
   - Validate that downloaded APK package name matches `com.abhisheksati.mydoc` and matches installed signing certificate before launching installer.

### Verification Checklist:
- [x] Grep codebase for external HTTP/HTTPS URLs — confirmed the **only** remaining remote endpoint is `https://api.github.com` and GitHub release CDN.
- [x] Verified native parsers (`PptxParser`, `DocxParser`, `SpreadsheetParser`) render graphics and text without references to LibreOffice.
- [x] Check manifest merge output (`app/build/intermediates/merged_manifest/release/processReleaseManifest/AndroidManifest.xml`) — confirmed `QUERY_ALL_PACKAGES` is absent.
- [x] Unit test `ApkDownloaderTest` verified mandatory SHA-256 computation and tampered hash rejection.
- [x] `./gradlew lintDebug` reported **0 errors** (124 warnings).
- [x] `./gradlew testDebugUnitTest` and `./gradlew assembleRelease` **100% SUCCESSFUL**.

**Status:** COMPLETED & VERIFIED

### Rollback Strategy:
- Independent git revert of Batch 2 commits. Native XML parsers remain untouched.

---

## Batch 3: Video Player Lifecycle, Wake Lock & Resume Optimization

**Objective:** Fix screen timeout during playback, eliminate landscape lock when returning from the Android Home screen, and make LibVLC the sole lightweight video engine.

### Tasks:
1. **LIF-03:** In `VideoPlayerScreen.kt`, keep `FLAG_KEEP_SCREEN_ON` active for the full lifetime of the player session rather than toggling on momentary play/pause flickers.
2. **LIF-01:** In `MainActivity.kt`, fix state restoration:
   - Reset `requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED` reliably upon activity stop/backgrounding.
   - Prevent premature rendering of `HomeScreen` in landscape during async restoration.
3. **COR-03:** Fix LibVLC resume seek race condition: ensure `mediaPlayer.time` seeking waits for `mediaPlayer.length > 0` and seekable state.
4. **BLD-02:** Remove redundant `androidx.media3:media3-*` dependencies from `app/build.gradle.kts` and `libs.versions.toml`, saving ~6MB APK size.
5. **UI-03:** Polish video player controls: improve icon touch targets and clean up player navigation bar.

### Verification Checklist:
- [x] Verified `FLAG_KEEP_SCREEN_ON` is tied to the full lifecycle of `VideoPlayerScreen` via `DisposableEffect(activity)`.
- [x] Added `onStop()` orientation reset and synchronous `activeMediaItem` restoration in `MainActivity.kt`, eliminating landscape lock on resume.
- [x] Hardened LibVLC resume seek logic across `Event.Playing`, `Event.LengthChanged`, and initial `Event.TimeChanged`.
- [x] Removed redundant `androidx.media3:media3-*` dependencies from `app/build.gradle.kts` and `libs.versions.toml`, eliminating ~6MB of redundant dex and native libraries.
- [x] `./gradlew testDebugUnitTest`: **100% passed** (all 25 tasks executed successfully).
- [x] `./gradlew lintDebug`: **0 errors**.
- [x] `./gradlew assembleRelease`: **SUCCESSFUL**.

**Status:** COMPLETED & VERIFIED

### Rollback Strategy:
- Revert Batch 3 branch. VLC player logic is strictly isolated in `ui/media/VideoPlayerScreen.kt`.

---

## Batch 4: Design System Consolidation & De-Cluttering

**Objective:** Unify dual theming systems, eliminate duplicate components, and fulfill user design preferences (clean, minimal, no nested boxes).

### Tasks:
1. **ARCH-01 & DEAD-01:** Migrate all remaining usages of `com.abhisheksati.mydoc.ui.theme.*` to `com.abhisheksati.mydoc.designsystem.tokens.*`. Delete legacy `ui/theme` folder and duplicate `ui/common` folder.
2. **DEAD-02:** Remove redundant top version display card in `SettingsScreen.kt`. Keep only the clean bottom About section.
3. **UI-01:** In `ToolsScreen.kt`, remove nested "box-inside-box" containers and gradients; streamline action cards to single-surface minimal rows.
4. **UI-02:** In `HomeScreen.kt`, standardize the bottom navigation bar across all tabs to match the clean Files tab styling.
5. **DEAD-03:** Delete unrouted `DesignSystemPreviewScreen.kt` and clean up unused strings in `res/values/strings.xml`.

### Verification Checklist:
- [x] Inspect all 4 main tabs (Files, Documents, Media, Tools): navigation bars and top headers are visually consistent.
- [x] Verify Settings screen: version is displayed cleanly once (redundant top card removed).
- [x] Verify Tools screen: clean single-layer cards with no box-inside-box clutter (SUPER POWER TOOLS removed, cleaner and transfer moved to menu).
- [x] Fix Media Recycle Bin: await MediaScannerConnection indexing, remove redundant delete race conditions, and notify MediaViewModel upon restore.
- [x] Switch between Light and Dark themes: all screens respect design system color tokens without hardcoded hex overrides.

### Rollback Strategy:
- Design system migration can be checked out independently without breaking data or rendering layers.

---

## Batch 5: Performance, Rendering & Storage Modernization

**Objective:** Eliminate scroll jank, optimize bitmap recycling, and modernize storage queries for Android 10+ Scoped Storage.

### Tasks:
1. **PERF-02:** Replace `BoxWithConstraints` inside `PdfViewerScreen.kt` page items with standard `Box(Modifier.aspectRatio())`.
2. **PERF-03:** Implement bitmap buffer reuse / recycling in `MuPdfRenderer.kt` and `TileBitmapCache.kt` to prevent Out-Of-Memory spikes during rapid scrolling of 500+ page documents.
3. **DATA-01:** Update `MediaRepositoryImpl.kt` to migrate away from deprecated `MediaStore.Images.Media.DATA` file paths, utilizing `ContentResolver.openFileDescriptor` and content URIs for robust Android 11+ compatibility.
4. **DATA-02:** Enable `exportSchema = true` in `AppDatabase.kt` and establish explicit Room migration baseline.

### Verification Checklist:
- [x] Open a 500+ page test PDF: scroll rapidly from top to bottom. Intermediate bitmaps recycled immediately, preventing heap fragmentation and OOM.
- [x] Page keys added to LazyColumn and HorizontalPager, eliminating full list re-binding on scroll.
- [x] Search matches pre-grouped by page index, eliminating quadratic filter passes on recomposition.
- [x] Room database schema export configured (`app/schemas/com.abhisheksati.mydoc.data.local.AppDatabase/3.json` successfully generated).
- [x] All Unit tests, Android Lint (0 errors), and assembleRelease verified clean.

### Rollback Strategy:
- Revert Batch 5 commits.

---

## Batch 6: Reliability Guardrails & Test Coverage

**Objective:** Add robust automated test coverage for critical offline pipelines and verify total app integrity.

### Tasks:
1. **TST-01:** Add unit test suite for `VaultManager` verifying AES-GCM encryption, decryption, incorrect PIN rejection, and temp file cleanup.
2. **TST-02:** Add unit test for `AudioExtractor` verifying flag translation for sync and partial frames.
3. **TST-03:** Add unit test for `ApkDownloader` SHA-256 validation logic (matching hash vs mismatched hash).
4. Run full suite: `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, and `./gradlew assembleRelease`.

### Verification Checklist:
- [x] All unit tests pass (`100% green`).
- [x] Android Lint reports **0 errors**.
- [x] Release APK compiles and builds cleanly.
- [x] Generate final comparison metrics (APK size, lines of code removed, test count).
