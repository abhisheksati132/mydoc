# MyDoc System Architecture & Design Specification

---

## 📋 High-Priority Refactoring Plan (Large File Decomposition)

Several core UI and engine files have accumulated substantial complexity and exceeded 1,000 lines. To ensure long-term maintainability, testability, and adherence to Single Responsibility principles, the following decomposition plan will be executed in upcoming minor releases.

### 1. `FileManagerScreen.kt` (~1,650 lines)
* **Current State:** Monolithic Compose file combining navigation state, path breadcrumbs, multi-select action bars, category filtering, search bars, file item rendering, sort bottom sheets, and deletion dialogs.
* **Target Package:** `com.abhisheksati.mydoc.ui.filemanager.components`
* **Decomposition Strategy:**
  - `FileManagerTopBar.kt`: Header with back button, breadcrumbs, search toggle, and view mode switcher.
  - `FileManagerMultiSelectBar.kt`: Batch action bar (Share, Move, Copy, Delete, Select All) with counter.
  - `FileManagerListContent.kt`: The `LazyColumn` file list, swipe-to-reveal actions, and pull-to-refresh container.
  - `FileListItemRow.kt`: Individual file/folder item row with thumbnail, metadata badges, and overflow menu.
  - `FileManagerCategoryChips.kt`: Sub-category filter pills (`PDF`, `Word`, `Excel`, `PPT`, `Text`).
  - `FileManagerSortSheet.kt`: Sort order selection modal sheet.
  - `FileManagerDeleteDialog.kt`: Confirmation dialog for single and bulk deletions.

---

### 2. `PdfToolsEngine.kt` (~1,730 lines)
* **Current State:** Single god-object implementing 20+ disparate document manipulation tasks spanning MuPDF Fitz, Apache PDFBox Android, and Google ML Kit.
* **Target Package:** `com.abhisheksati.mydoc.tools.engine.tasks`
* **Decomposition Strategy:**
  - Interface: `interface PdfToolTask { suspend fun execute(inputs: List<File>, options: ToolOptions, onProgress: (ToolProgress) -> Unit): File }`
  - `OrganizeTasks.kt`: Implementations for `MergeTask`, `SplitTask`, `ReorderTask`, `RotateTask`, `CropTask`, `ExtractPagesTask`.
  - `OptimizeTasks.kt`: Implementations for `CompressTask` (raster downsampling & stream compaction) and `RepairTask`.
  - `ConvertTasks.kt`: Implementations for `ImageToPdfTask`, `PdfToImagesTask`, `TextToPdfTask`, `PdfToTextMarkdownTask`, `PdfToOfficeTask`, `PdfToPdfATask`.
  - `SecurityTasks.kt`: Implementations for `ProtectTask`, `UnlockTask`, `RedactTask`, `StripMetadataTask`.
  - `EditTasks.kt`: Implementations for `WatermarkTask`, `PageNumberTask`, `AnnotateTask`, `FormFillTask`, `SignTask`.
  - `OcrTask.kt`: Dedicated ML Kit on-device OCR pipeline and searchable text layer generator.
  - `PdfToolsEngine.kt` (Façade): Reduced to ~150 lines acting as a router and dispatcher delegating to specialized tasks.

---

### 3. `ToolRunnerSheet.kt` (~1,220 lines)
* **Current State:** Manages the modal execution lifecycle for all 20+ PDF tools, including parameter configuration sheets, page pickers, compression sliders, watermark text inputs, progress indicators, and result preview cards.
* **Target Package:** `com.abhisheksati.mydoc.tools.ui.sheets`
* **Decomposition Strategy:**
  - `ToolOptionsContainer.kt`: Generic modal host handling sheet expansion, title bar, cancel, and start button.
  - `ToolProgressOverlay.kt`: Unified progress bar, stage description, and cancellation token handler.
  - `ToolResultCard.kt`: Success preview, file size delta pill, "Open Result" action, and "Share Result" button.
  - Domain-specific option sheets:
    - `CompressOptionsSheet.kt`: Slider for DPI / JPEG quality and rasterize toggles.
    - `WatermarkOptionsSheet.kt`: Text, opacity, rotation, and font scale controls.
    - `PageSelectionOptionsSheet.kt`: Page ranges, split intervals, and thumbnail selection grid.
    - `SecurityOptionsSheet.kt`: Password inputs and encryption strength selectors.

---

### 4. `SettingsScreen.kt` (~870 lines)
* **Current State:** Single composable hosting Theme, Reading, Media, Storage, Security, and Software Updates controls with multiple dialogs.
* **Target Package:** `com.abhisheksati.mydoc.ui.settings.sections`
* **Decomposition Strategy:**
  - `ThemeSettingsCard.kt`: Theme mode (Light, Dark, Black, System), AMOLED black toggle, dynamic color.
  - `ReadingSettingsCard.kt`: Default reading mode, fit width/page, keep screen on, high quality rendering.
  - `MediaSettingsCard.kt`: Video hardware acceleration, auto resume, background playback.
  - `SecuritySettingsCard.kt`: Biometric app lock toggle and device credential confirmation.
  - `UpdaterSettingsCard.kt`: Automatic launch check toggle, version badge, "Check for Updates" trigger, and progress bar.

---

### 5. `PresentationViewerScreen.kt` (~1,870 lines) & `PptxParser.kt` (~2,870 lines)
* **Current State:** Comprehensive OOXML presentation rendering and viewer system.
* **Target Package:** `com.abhisheksati.mydoc.rendering.engine.presentation`
* **Decomposition Strategy:**
  - `PptxThemeResolver.kt`: Theme color schemes, luminance modifiers, font schemes, and format schemes.
  - `PptxMasterInheritance.kt`: Slide master and layout placeholder coordinate and text property resolution.
  - `PptxShapeRenderer.kt`: Vector geometry paths, arrowheads, callouts, and preset shapes.
  - `PptxTableRenderer.kt`: Table grid, borders, and cell background rendering.
  - `PresentationSlideshowControls.kt`: Bottom thumbnail carousel, slide transition animations, and fullscreen controls.

---

## 🏛️ System Architecture Overview

MyDoc follows standard Android architectural best practices with clean layered separation:

```
┌────────────────────────────────────────────────────────┐
│                   Jetpack Compose UI                   │
│   (HomeScreen, FileManagerScreen, PdfViewerScreen)     │
└───────────────────────────▲────────────────────────────┘
                            │ StateFlow / Events
┌───────────────────────────┴────────────────────────────┐
│                       ViewModels                       │
│    (HomeViewModel, FileManagerViewModel, ViewerVM)     │
└───────────────────────────▲────────────────────────────┘
                            │ Use Cases / Repositories
┌───────────────────────────┴────────────────────────────┐
│                    Repository Layer                    │
│ (DocumentRepository, StorageRepository, MediaRepository) │
└─────────────▲────────────────────────────▲─────────────┘
              │                            │
┌─────────────┴──────────────┐  ┌──────────┴─────────────┐
│       Local Database       │  │     Document Engine    │
│    (Room AppDatabase)      │  │ (MuPDF, PDFBox, Word,  │
│  - recents, favorites,     │  │  Spreadsheet, ML Kit)  │
│  - bookmarks, view states  │  └────────────────────────┘
└────────────────────────────┘
```

### Layer Responsibilities

1. **UI Layer (`ui/`)**:
   - Declarative Jetpack Compose components snapping strictly to Material 3 and custom AMOLED color tokens.
   - Stateless composables consuming immutable UI states.

2. **ViewModel Layer (`viewmodel/`)**:
   - Manages UI state via Kotlin Coroutine `StateFlow`.
   - Exposes clear intents/actions; contains zero Android framework View/Context dependencies.

3. **Data / Repository Layer (`data/`)**:
   - Mediates between the local Room SQLite database (`mydoc.db`), MediaStore queries, and hardware filesystem operations.
   - Emits reactive streams via `Flow`.

4. **Document Engine Layer (`rendering/`, `converter/`, `tools/`)**:
   - **PDF Engine**: Powered by Artifex MuPDF Fitz JNI with sub-pixel rendering and viewport tile slicing (`renderRegion`).
   - **Word Engine**: Sandboxed `DocxWebRenderer` executing `docx-preview.js` in an offline WebView to generate vector PDF pages.
   - **Presentation Engine**: Dependency-free OOXML vector parser converting slides into crisp Android `PdfDocument` pages.
   - **Spreadsheet Engine**: Banded table parser with formula text evaluation.

5. **Security & Privacy Boundary**:
   - **100% On-Device Processing**: Documents, photos, and media are parsed and manipulated strictly in-process.
   - **Restricted FileProvider**: Root storage and external directory access are forbidden. URIs are granted exclusively from private cache subdirectories (`updates/`, `shared/`, `tools_output/`).
   - **Cryptographic Updater**: Automatic updates query public GitHub Releases with domain allowlisting, 200 MB caps, mandatory SHA-256 validation, and release certificate pinning.
