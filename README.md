# mydoc

An offline-first, personal-use document viewer, media player, and PDF toolkit for Android built with Kotlin, Jetpack Compose, Material 3, Room, Fitz MuPDF, and LibVLC.

Designed with a content-first philosophy: **AMOLED pure black (`#000000`)** dark theme, **Crisp White (`#FFFFFF`)** light theme, clean minimal UI, zero ads, zero telemetry/analytics, zero accounts, and strict offline operation.

---

## Capabilities & Supported Formats

### 1. Document Viewing & PDF Toolkit
| Format | Extension | Engine | Capabilities |
| :--- | :--- | :--- | :--- |
| **PDF** | `.pdf` | **Artifex MuPDF Fitz** | Sub-pixel vector rasterization, pinch-to-zoom (up to 500%), continuous vertical scroll, horizontal page-flip, dual-page book spread, in-document search with bounding highlights, table of contents/outline drawer, thumbnail strip, password protection, and on-device OCR text extraction. |
| **Word** | `.docx`, `.doc` | **MuPDF Fitz** | Smooth layout rendering, fling scrolling, pinch-zoom, and in-document search. |
| **PowerPoint** | `.pptx`, `.ppt` | **MuPDF Fitz / Native** | Slide-by-slide viewing, swipe navigation, thumbnail slide strip. |
| **Excel & CSV** | `.xlsx`, `.csv`, `.tsv` | **2D Grid Engine** | High-performance virtualized grid with frozen column headers (`A, B, C...`), frozen row headers (`1, 2, 3...`), multi-sheet tab selector, readable auto-column widths, and in-sheet cell search. |
| **Text** | `.txt` | **Native Text View** | Instant loading, typography scaling, in-document search. |
| **PDF Tools** | `.pdf` | **On-Device Engine** | Merge, split, compress, reorder pages, sign PDF with encrypted signature pad, image-to-PDF scan, and watermark removal. |

### 2. Media Player & Photos
* **Video Player**: Powered exclusively by the official **VideoLAN LibVLC 3.6.5** engine. Supports hardware decoding, multi-track audio, subtitle selection, double-tap seek, gestures (brightness/volume/seek), playback speed controls, audio boost up to 200%, continuous screen-on wake lock during playback, and automatic playback position resumption.
* **Photo Viewer**: High-resolution zoomable viewer with EXIF metadata inspector, full-screen immersive mode, and multi-format support (JPEG, PNG, WebP, GIF).
* **Media Recycle Bin**: 30-day auto-purge recycle bin with instant MediaStore re-indexing via `MediaScannerConnection` upon restore or permanent deletion.

### 3. File Manager & Security
* **Storage Manager**: Dual-pane file and directory browser with search, multi-selection, ZIP compression/extraction, file checksum calculation (SHA-256, MD5), and storage volume capacity meters.
* **Safe Vault**: Hardware-backed AES-256-GCM encrypted vault with biometric authentication, in-memory preview decryption, and multi-pass zero-fill file shredding.
* **PC Wi-Fi Transfer**: Local Wi-Fi HTTP file server for high-speed cable-free transfers to/from computer browsers without internet access.

---

## Security & Privacy Architecture

* **Strictly Offline**: The app contains zero analytics, telemetry, tracking SDKs, or background cloud sync. The only network capability is the optional GitHub self-updater.
* **Hardened Self-Updater**:
  - Optional silent check (throttled to at most once per 24 hours; fails silently if offline).
  - Manual "Check for updates" in Settings.
  - Mandatory **SHA-256 integrity verification** on downloaded APK assets prior to installation.
  - Package identity and signature verification in `PackageInstallerHelper` preventing spoofed package installation.
* **Storage Access Framework (SAF)**: Persists explicit URI permissions (`takePersistableUriPermission`) across device reboots without broad external storage exposure.
* **Room Schema Versioning**: Explicit Room schema export enabled with baseline schema tracking (`app/schemas/...`) preventing data loss across upgrades.

---

## Architecture & Package Structure

Built following strict **MVVM + Clean Architecture**:

```
app/src/main/java/com/abhisheksati/mydoc/
├── data/
│   ├── local/              // Room Database (AppDatabase), DAOs, Entities, SettingsRepository
│   └── repository/         // StorageRepositoryImpl, MediaRepositoryImpl, DocumentRepositoryImpl
├── domain/
│   ├── model/              // DocumentFile, MediaItem, StorageItem, TrashItem, ViewState
│   └── repository/         // Core Repository interfaces
├── designsystem/
│   ├── components/         // AppTabHeader, AppDialog, AppEmptyState, AppSearchField
│   └── tokens/             // AppColors, AppTypography, AppSpacing (Unified Design Tokens)
├── media/
│   └── audio/              // AudioExtractor (video soundtrack extraction)
├── rendering/
│   ├── engine/             // DocumentRenderer, MuPdfRenderer, AndroidPdfRenderer
│   ├── cache/              // TileBitmapCache (heap-bounded LruCache with bitmap recycling)
│   └── spreadsheet/        // Streaming OpenXML & CSV parser, SheetData
├── security/
│   └── vault/              // VaultManager (AES-256-GCM encryption & secure shredder)
├── tools/                  // PDF Toolkit, Storage Cleaner, Wi-Fi Transfer
├── update/                 // GitHubUpdateService, ApkDownloader, PackageInstallerHelper
├── viewmodel/              // HomeViewModel, FileManagerViewModel, MediaViewModel, ViewerViewModel
└── ui/
    ├── home/               // DocumentsTabContent, FloatingPillDock, HomeScreen
    ├── filemanager/        // FileManagerScreen, TrashBinSheet, StorageItemComponents
    ├── media/              // MediaTabContent, VideoPlayerScreen, PhotoViewerScreen
    ├── viewer/             // Master viewer, PDF viewer, Spreadsheet 2D grid viewer
    └── settings/           // SettingsScreen (unified preferences & updater)
```

---

## Building from Source

### Prerequisites
* **JDK 17 or 21**
* **Android SDK** (API Level 26 through 35, Build-Tools 35.0.0)

### Build Commands

```bash
# Clone the repository
git clone https://github.com/abhisheksati132/mydoc.git
cd mydoc

# Run all unit tests (100% green)
./gradlew testDebugUnitTest

# Run Android Lint (0 errors)
./gradlew lintDebug

# Build Release APK (with R8 minification and resource shrinking)
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/
```

---

## Third-Party Licenses

* **Artifex MuPDF Fitz**: Licensed under the [GNU Affero General Public License (AGPL v3)](https://www.gnu.org/licenses/agpl-3.0.html).
* **VideoLAN LibVLC**: Licensed under the [GNU Lesser General Public License (LGPL v2.1+)](https://www.gnu.org/licenses/lgpl-2.1.html).
* **AndroidX & Jetpack Compose**: Licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
* **ZXing**: Licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
