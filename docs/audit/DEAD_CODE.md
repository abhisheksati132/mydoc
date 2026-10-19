# Dead Code, Redundant Assets & Deprecated Logic Audit

**Target Application:** `com.abhisheksati.mydoc`  
**Evaluation Standard:** Identify all dead, unreachable, redundant, or orphaned code, and deliver an explicit **Safe-to-Remove Verdict** with verification prerequisites.

---

## 1. Major Architectural Redundancies & Dead Subsystems

### 1.1 Broken LibreOffice Subsystem (Complete Removal Recommended)
- **Files Affected:**
  1. `app/src/main/java/org/libreoffice/kit/Document.java`
  2. `app/src/main/java/org/libreoffice/kit/LibreOfficeKit.java`
  3. `app/src/main/java/org/libreoffice/kit/Office.java`
  4. `app/src/main/java/com/abhisheksati/mydoc/rendering/engine/libreoffice/LibreOfficeDocumentRenderer.kt`
  5. `app/src/main/java/com/abhisheksati/mydoc/rendering/engine/libreoffice/LibreOfficeEngineManager.kt`
  6. `app/src/main/java/com/abhisheksati/mydoc/rendering/engine/presentation/LibreOfficePresentationRenderer.kt`
- **Current State:**
  - `LibreOfficeKit.java` attempts to load native shared library `libsofficeapp.so` which is **not bundled in the APK** (it is a 120MB+ native C++ suite).
  - `LibreOfficeEngineManager.kt` contains hacky, unsafe network logic attempting to download external APKs from F-Droid mirrors at runtime. Because this fails or crashes on modern Android due to SELinux/W^X restrictions, the engine never successfully initializes and throws `UnsatisfiedLinkError`.
  - All DOCX, PPTX, and XLSX rendering in the app already has standalone native Kotlin XML parsers (`DocxParser.kt`, `PptxParser.kt`, `SpreadsheetParser.kt`).
- **Verdict:** **SAFE TO REMOVE COMPLETELY.**
- **Prerequisites to Verify:** Verify that `DocumentViewerScreen.kt` and `PresentationViewerScreen.kt` route directly to the custom XML parsers (`DocxParser`, `PptxParser`, `SpreadsheetParser`) without referencing `LibreOfficeDocumentRenderer`.

---

### 1.2 Dual Design System & Duplicate Theming Packages
The project contains two entire parallel theming packages written at different times:

| Legacy Package (`com.abhisheksati.mydoc.ui.theme`) | New Design System (`com.abhisheksati.mydoc.designsystem.tokens`) | Current Usage & Conflict | Verdict |
|---|---|---|---|
| `ui/theme/Color.kt` | `designsystem/tokens/AppColors.kt` | Different color hex definitions for primary, surface, and accents. | Consolidate to `designsystem/tokens` |
| `ui/theme/Shape.kt` | Material 3 Default / Inline shapes | Inconsistent corner radii (8dp vs 12dp vs 16dp). | Migrate to Design System tokens |
| `ui/theme/Spacing.kt` | `designsystem/tokens/AppSpacing.kt` | Duplicate 4dp/8dp/16dp grid definitions. | Remove `ui/theme/Spacing.kt` |
| `ui/theme/Theme.kt` | `designsystem/tokens/AppTheme.kt` | Both declare `MaterialTheme` wrappers with divergent color schemes. | Consolidate onto single `AppTheme.kt` |
| `ui/theme/Typography.kt` | `designsystem/tokens/AppTypography.kt` | Dual typography type hierarchies. | Consolidate to `AppTypography.kt` |
| `ui/common/AppDivider.kt` | `designsystem/components/AppDivider.kt` | Identical 1dp hairline divider composable. | Remove `ui/common/AppDivider.kt` |
| `ui/common/EmptyState.kt` | `designsystem/components/AppEmptyState.kt` | Duplicate empty state placeholder. | Remove `ui/common/EmptyState.kt` |

- **Verdict:** **SAFE TO CONSOLIDATE.** Keep `com.abhisheksati.mydoc.designsystem.*` as the single source of truth; migrate the 6 legacy references and delete `com.abhisheksati.mydoc.ui.theme` and `ui/common`.

---

### 1.3 Redundant ExoPlayer Dependencies (Dual Video Engine)
- **Locations:**
  - `app/build.gradle.kts:108-110`:
    ```kotlin
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    ```
- **Current State:**
  - `VideoPlayerScreen.kt` is implemented using LibVLC (`org.videolan.libvlc.MediaPlayer`).
  - Media3 ExoPlayer is completely redundant and never surfaced to the user.
- **Verdict:** **SAFE TO REMOVE.**
- **Impact:** Reduces compiled dex method count by ~14,000 methods and reduces APK download size by ~6MB.

---

### 1.4 Unused / Abandoned UI Components & Screens
1. **`designsystem/preview/DesignSystemPreviewScreen.kt`:**
   - Standalone composable with mock buttons and cards for design testing. Never referenced in `MainActivity.kt` or any navigation route.
   - **Verdict:** **SAFE TO REMOVE.**
2. **`baseline-prof.txt`:**
   - Contains only placeholder comments.
   - **Verdict:** Keep only if actual baseline profile generator is configured, otherwise safe to clean.
3. **Double Version Display in Settings (`SettingsScreen.kt:180-240`):**
   - Version name is rendered twice: once in the top hero header card, and again in the bottom "About MyDoc" section.
   - User explicitly requested removing the top redundant version display.
   - **Verdict:** **SAFE TO REMOVE TOP VERSION DISPLAY.**

---

## 2. Unused & Over-Engineered Methods in ViewModels & Repositories

| Class | Method / Field | Status | Why It Is Unused | Safe to Remove? |
|---|---|---|---|---|
| `StorageCleanerManager.kt` | `scanLargeFiles(thresholdBytes: Long)` | Unused | `StorageCleanerSheet.kt` only calls `scanEmptyFolders` and `scanDuplicates`. | Yes |
| `RecentDao.kt` | `deleteRecentById(id: Long)` | Unused | All recent deletions invoke `clearAllRecents()` or auto-expire. | Yes |
| `FavoriteDao.kt` | `getFavoriteCount()` | Unused | Home tab reads the full reactive `Flow<List<FavoriteDocumentEntity>>` directly. | Yes |
| `ViewStateDao.kt` | `deleteStateForUri(uri: String)` | Unused | View state records are overwritten via `OnConflictStrategy.REPLACE`, never individually deleted. | Yes |
| `AppManagerHelper.kt` | `getAllInstalledPackages()` | High Risk / Deprecated | Calls `pm.getInstalledPackages(0)` with `QUERY_ALL_PACKAGES`. | Safe to replace with targeted query |

---

## 3. Resource & Asset Redundancies

### 3.1 Unused Strings in `res/values/strings.xml`
- `pref_theme_amoled_desc` (AMOLED theme mode toggle was removed in favor of standard Dark theme).
- `ocr_engine_tesseract` (Tesseract was superseded by ML Kit on-device).
- `libreoffice_download_prompt` (Legacy prompt from broken dynamic engine).
- `storage_permission_rationale_secondary` (Duplicated rationale string).

### 3.2 Bundled Binary Assets
- `res/font/inter.ttf` (320 KB): Used throughout `AppTypography.kt`. **Must KEEP.**
- `app/keystore/debug.keystore`: Checked into version control. Should be generated dynamically or excluded from public git history.
