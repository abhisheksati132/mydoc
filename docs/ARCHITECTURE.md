# MyDoc System Architecture & Design Specification (v2.0.16)

---

## 🏛️ System Architecture Overview

MyDoc is an offline-first Android document reader, file manager, and PDF toolkit built on modern Android architectural guidelines, Jetpack Compose, and Material 3 Expressive.

```
┌────────────────────────────────────────────────────────┐
│                   Jetpack Compose UI                   │
│   (HomeScreen, FilesScreen, PdfViewerScreen, Tools)    │
└───────────────────────────▲────────────────────────────┘
                            │ StateFlow / Events
┌───────────────────────────┴────────────────────────────┐
│                       ViewModels                       │
│    (HomeViewModel, FileManagerViewModel, ViewerVM)     │
└───────────────────────────▲────────────────────────────┘
                            │ Use Cases / Repositories
┌───────────────────────────┴────────────────────────────┐
│                    Repository Layer                    │
│ (DocumentRepository, StorageRepository, LibraryRepo)   │
└─────────────▲────────────────────────────▲─────────────┘
              │                            │
┌─────────────┴──────────────┐  ┌──────────┴─────────────┐
│       Local Database       │  │     Document Engines   │
│    (Room AppDatabase)      │  │ • MuPDF Fitz 1.28.5    │
│  - recents, favorites,     │  │ • Apache PDFBox        │
│  - bookmarks, view states  │  │ • LibreOffice Companion│
│  - FTS4 search index       │  │ • ML Kit On-Device OCR │
└────────────────────────────┘  └────────────────────────┘
```

---

## 🧱 Module Architecture

The project is structured into three Gradle modules:

```
├── app/             # Main Android application (UI, ViewModels, Repositories, PDF Engine)
├── lint-rules/      # Custom Android Lint checks (enforces accessible tap target descriptions)
└── office-engine/   # Decoupled LibreOfficeKit worker (sandboxed companion application)
```

### Module Responsibilities

1. **`:app` (Main Application)**:
   - **UI & Presentation (`com.abhisheksati.mydoc.ui.*`)**: Declarative Compose components adhering to the *Ink & Seal* visual identity (Material 3 Expressive motion, Fraunces/Inter typography, and optional Haze liquid glass optics).
   - **ViewModels (`com.abhisheksati.mydoc.viewmodel.*`)**: Unidirectional data flow (UDF) powered by Kotlin Coroutines `StateFlow` and immutable UI state models.
   - **Data Layer (`com.abhisheksati.mydoc.data.*`)**: Room SQLite database (`mydoc.db`), MediaStore queries, and resilient scoped filesystem operations.
   - **PDF & Tools Engine (`com.abhisheksati.mydoc.tools.engine.*`)**: Modularized PDF manipulation engines (conversion, editing, optimization, page operations, security).

2. **`:office-engine` (Companion Office Module)**:
   - Houses native LibreOfficeKit binaries (v26.2.6.3), fonts, and runtime assets.
   - Runs in a dedicated worker process with seccomp filters blocking external socket connections.
   - Communicates via local `AF_UNIX` IPC using caller signature verification.
   - Keeps the core APK lean (~55 MB ARM64) while providing high-fidelity Office viewing.

3. **`:lint-rules` (Quality & Accessibility Lint)**:
   - Enforces design system and accessibility constraints (e.g., `UnlabeledTapTargetDetector` requiring `contentDescription` on icon-only tap targets).

---

## 🧩 Decomposed Subsystems

Core UI and engine files have been modularized into domain-focused components:

### 1. Files & Explorer Hub (`com.abhisheksati.mydoc.ui.files`)
- `FilesScreen.kt`: Top-level scaffold and navigation host.
- `FilesPanes.kt`: Adaptive single-pane and dual-pane layout rendering.
- `FilesComponents.kt`: Individual file/folder rows, badge indicators, and sub-category chips.
- `FileManagerHeaderComponents.kt`: Breadcrumbs, search toggle, view mode switcher, and folder tabs.
- `FileManagerActionSheet.kt`: Batch operations (Share, Move, Copy, Delete, Recycle Bin).
- `FileManagerDialogs.kt`: Safe delete, rename, and folder creation dialogs.

### 2. PDF Tools Engine (`com.abhisheksati.mydoc.tools.engine`)
- `PdfConversionEngine.kt`: Image to PDF, Text to PDF, and Office to PDF conversion pipelines.
- `PdfEditingEngine.kt`: Vector watermarking, dynamic page numbering, and annotations.
- `PdfOptimizationEngine.kt`: Stream compaction, JPEG recompression, downsampling, and rasterization.
- `PdfPageOperationsEngine.kt`: Split, merge, reorder, rotate, and page extraction.
- `PdfSecurityEngine.kt`: Password encryption, permissions stripping, and signature validation.
- `PdfToolUtils.kt`: Shared validation, bounding, and stream helpers.

### 3. Settings Hub (`com.abhisheksati.mydoc.ui.settings`)
- `SettingsCategoryIndex.kt`: Category navigation and landing screen.
- `SettingsComponents.kt`: Shared settings cards, switches, sliders, and option pickers.
- `OfficeEngineSettings.kt`: Dedicated companion engine installation, status, and uninstallation.
- `SettingsAboutSection.kt`: In-app updates checker, open-source licenses, and legal notices.

### 4. Playback, Discovery & Surfaces
- `tools/tts/TtsPlaybackService.kt`: Foreground `mediaPlayback` service owning the platform `MediaSession` and a `MediaStyle` notification, so reading stays audible with the screen off or the app backgrounded while the viewer is open. Narration is viewer-scoped: closing the document stops it because the page loader borrows the viewer's renderer. The session exposes a private public-version notification for the lock screen.
- `discovery/DiscoveryStore.kt`: Opt-in FTS4 index (`discovery.db`) of document text, queried with `snippet()` for the search sheet's "Inside documents" results; the index holds extracted text only, never file contents, and is rebuildable from disk.
- `widget/ContinueReadingWidget.kt`: `RemoteViews` `AppWidgetProvider` listing the three most recent documents; rows deep-link through `MainActivity` and re-render on open/close.
- `LauncherShortcuts.kt`: Dynamic `ShortcutManagerCompat` shortcuts (Scan, Search, Merge PDFs) routed through `MainActivity` intent actions.
- `tools/notes/NotesExporter.kt`: Renders saved highlights to Markdown or a `PdfDocument`-generated PDF, shared via the FileProvider.

---

## 🔒 Security & Privacy Boundaries

1. **Completely Local Processing**: Document rendering, text extraction, OCR, and file manipulation occur strictly on-device without telemetry or remote telemetry servers.
2. **Strict FileProvider Access**: File URIs are shared exclusively via scoped `content://` providers with temporary read grants. Raw `file://` URIs and external root traversal are blocked.
3. **Cryptographic Release Verification**: The in-app updater validates domain allowlists, enforces download caps, verifies SHA-256 checksums, and requires identical release certificate fingerprints (`MYDOC_RELEASE_CERT_SHA256`).
4. **Sandboxed Office IPC**: The LibreOfficeKit companion engine runs in a separate process with seccomp filters preventing outbound internet connections.
