# mydoc

An offline-first, personal-use, view-only document viewer for Android built with Kotlin, Jetpack Compose, and Material 3. 

Designed with a content-first philosophy: **AMOLED pure black (`#000000`)** dark theme, **Crisp White (`#FFFFFF`)** light theme, zero decorative bloat, zero ads, zero accounts, and zero background analytics.

---

## Supported Formats

| Format | File Extension | Engine | Highlights |
| :--- | :--- | :--- | :--- |
| **PDF** | `.pdf` | **Artifex MuPDF Fitz** | Sub-pixel vector rasterization, pinch-to-zoom (up to 500%), double-tap zoom, continuous vertical scroll & horizontal page-flip, in-document search with bounding highlights, table of contents/outline drawer, thumbnail strip, password protection. |
| **Word** | `.docx`, `.doc` | **MuPDF Fitz** | Seamless page layout, smooth fling scroll, pinch-zoom, and search. |
| **PowerPoint** | `.pptx`, `.ppt` | **MuPDF Fitz** | Slide-by-slide viewing, swipe navigation, thumbnail slide strip. |
| **Excel & CSV** | `.xlsx`, `.csv`, `.tsv` | **Dedicated 2D Grid Engine** | High-performance virtualized grid with frozen column headers (`A, B, C...`), frozen row headers (`1, 2, 3...`), multi-sheet tab selector, readable auto-column widths, in-sheet cell search, and tap-to-inspect cell details. |
| **Text** | `.txt` | **Native Text View** | Instant loading, typography scaling, in-document search. |

---

## Key Features

* **True AMOLED Black Mode**: Inverted luminance page rendering (`#000000` true black background, white text) that eliminates glare and saves battery on OLED displays. Also includes Sepia mode (`#F4ECD8`).
* **Storage Access Framework (SAF)**: Opens documents securely without broad file system permissions. Automatically persists URI permissions across app reboots (`takePersistableUriPermission`).
* **System Integration ("Open with...")**: Registers `ACTION_VIEW` and `ACTION_SEND` intent filters for all supported MIME types (opens directly from WhatsApp, Telegram, email, and file managers).
* **Local Persistence (Room DB)**: Indexes recent documents, favorite bookmarks, and remembers per-file viewport states (last-read page number, zoom level, and reading mode).
* **In-App GitHub Self-Updater**: 
  - Silent launch check (throttled to **max once every 24 hours**; fails silently if offline).
  - Manual "Check for updates" in Settings.
  - Streams APK directly from your GitHub Releases with real-time download progress and SHA-256 integrity verification.
  - Handles Android Unknown App Sources permission flow cleanly.

---

## Architecture & Package Structure

Built following strict **MVVM + Clean Architecture**:

```
app/src/main/java/com/abhisheksati/mydoc/
├── data/
│   ├── local/              // Room Database, DAOs, Entities
│   └── repository/         // DocumentRepositoryImpl (SAF & metadata resolver)
├── domain/
│   ├── model/              // DocumentFile, AppRelease, ViewState, ReadingMode
│   └── repository/         // Repository interfaces
├── rendering/
│   ├── engine/             // DocumentRenderer, MuPdfRenderer
│   ├── cache/              // TileBitmapCache (heap-bounded LruCache)
│   └── spreadsheet/        // Streaming OpenXML & CSV parser, SheetData
├── update/                 // GitHubUpdateService, ApkDownloader, PackageInstallerHelper
├── viewmodel/              // HomeViewModel, ViewerViewModel, SpreadsheetViewModel
└── ui/
    ├── theme/              // AMOLED Black & Crisp White color palettes, typography, 6dp shapes
    ├── home/               // Flat document list with search and filter chips
    ├── viewer/             // Master viewer, PDF viewer, Spreadsheet 2D grid viewer
    └── settings/           // Settings screen with self-updater
```

---

## Building from Source

### Prerequisites
* **JDK 21** (e.g., Microsoft OpenJDK 21 or Eclipse Temurin 21)
* **Android SDK** (API Level 26 through 35, Build-Tools 34.0.0 or 35.0.0)

### Build Commands

```bash
# Clone the repository
git clone https://github.com/abhisheksati132/mydoc.git
cd mydoc

# Run all unit tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Build release APK
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release-unsigned.apk
```

---

## Release Pipeline & Keystore Management

### 1. Generating a Release Keystore
To sign release APKs, create a keystore **outside of this repository**:

```bash
keytool -genkeypair -v \
  -keystore ~/mydoc-release.jks \
  -alias mydoc \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

> **Warning**: Keep `mydoc-release.jks` and its passwords in a secure location (e.g., password manager / offline backup). Never commit keystore files into Git.

### 2. GitHub Actions Automated Release Setup
This repository includes automated CI/CD workflows:
* **CI Build (`.github/workflows/build.yml`)**: Compiles and tests every push to `main`, saving `mydoc-debug-apk` under the GitHub Actions tab.
* **Release Builder (`.github/workflows/release.yml`)**: Automatically packages an APK, generates a `.sha256` checksum, and creates a GitHub Release whenever a version tag is pushed.

### 3. Cutting a New Release
1. Increment `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Commit and push:
   ```bash
   git add app/build.gradle.kts
   git commit -m "Bump version to 1.0.1"
   git push origin main
   ```
3. Push a version tag:
   ```bash
   git tag v1.0.1
   git push origin v1.0.1
   ```
4. GitHub Actions will build the APK and publish the release. The installed `mydoc` app will automatically detect it and offer a one-tap update!

---

## Third-Party Licenses

This project incorporates:
* **Artifex MuPDF Fitz**: Licensed under the [GNU Affero General Public License (AGPL v3)](https://www.gnu.org/licenses/agpl-3.0.html).
* **AndroidX & Jetpack Compose**: Licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
