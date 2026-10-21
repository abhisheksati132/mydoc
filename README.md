# MyDoc

A clean, offline-first document reader and PDF toolkit for Android, built with Jetpack Compose.

---

## Overview

MyDoc is built around **Shelves** — an intentional, distraction-free document hub designed for studying, reading, and working with files. It trades generic templates for a calm editorial aesthetic: neutral off-white or deep OLED surfaces, clean flat color for course shelves, distinct Bricolage Grotesque typography, and an immediate three-tab workflow:

- **Now**: Your current document hero card, active course shelves, and recent files.
- **Library**: A unified, date-grouped list of all your documents, books, and media with quick shelf and type filters.
- **Tools**: Essential PDF utilities organized into clean groups with one-tap actions.

---

## Key Features

### Document Reader
- **High-Performance PDF Engine**: Powered by Artifex MuPDF Fitz with sub-pixel vector rendering, smooth zoom up to 500%, vertical continuous scroll, page-jump slider, and table of contents outline drawer.
- **Office & E-Books**: Native rendering for `.docx`, `.pptx`, `.xlsx`, `.epub`, and plain text files.
- **Reading Comfort**: Neutral light canvas, soft dark mode, and pure AMOLED black (`#000000`) for OLED battery savings.
- **Reading Continuity**: Remembers exact reading page and position across all sessions.

### PDF Toolkit
- **Organize**: Merge multiple documents, split by page ranges, reorder or rotate pages.
- **Optimize**: Compress files with image downsampling, repair damaged streams, standardize to PDF/A.
- **Convert**: Image to PDF, PDF to Images (ZIP), extract text and markdown.
- **Security & Sign**: Sign documents, AES password encryption, password removal, and metadata stripping.
- **Scanner**: Camera-assisted document capture with auto-edge detection and black-and-white filters.

### Shelves Organization
- **Color-Coded Shelves**: Group course notes, research papers, and assignments into distinct shelves.
- **Folder Sync**: Link a shelf directly to any folder on your device storage with automatic file indexing.
- **Unified Library**: Switch between Docs, Photos, Videos, and Audio with date grouping (Today, Yesterday, This Month, Earlier).

---

## Tech Stack

- **Language**: Kotlin 2.1.0
- **UI Framework**: Jetpack Compose (BOM 2024.12.01) with Material 3
- **Typography**: Bricolage Grotesque (variable font)
- **PDF Engine**: Artifex MuPDF Fitz
- **Local Storage**: Room 2.6.1 & Jetpack DataStore
- **Media Playback**: LibVLC Android 3.6.5
- **Image Loading**: Coil 2.7.0
- **Target SDK**: Android 15 (API 35) • **Minimum SDK**: Android 8.0 (API 26)

---

## Privacy & Offline

- **100% Offline**: All rendering, conversions, and tool operations execute entirely on your device.
- **Zero Telemetry**: No tracking, analytics SDKs, or cloud dependencies.
- **Zero Ads**: Free, open, and private.

---

## Building from Source

### Prerequisites
- Android Studio Ladybug (2024.2.1) or newer
- JDK 21

### Build APK
```bash
# Compile Kotlin sources
./gradlew compileDebugKotlin

# Run unit tests
./gradlew testDebugUnitTest

# Assemble debug APK
./gradlew assembleDebug
```

---

## License

This project is licensed under the GNU Affero General Public License v3.0 (AGPLv3) / LGPL v2.1. See `LICENSE` for details.
