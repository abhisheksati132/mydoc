# MyDoc

A high-performance document reader, media viewer, and PDF toolkit for Android, built with Jetpack Compose and Material 3.

---

## Features

### Document Viewer
- **PDF Engine**: Vector sub-pixel rendering with pinch-to-zoom (up to 500%), vertical continuous scroll, page-jump slider, full-text search, and table-of-contents outline drawer.
- **E-Books & Office**: Built-in support for `.epub`, `.docx`, `.pptx`, `.xlsx`, and `.txt` files with reading progress memory and night mode.
- **AMOLED Night Mode**: Pitch-black canvas mode with white text inversion for comfortable reading and OLED battery savings.

### PDF Toolkit
- **Merge & Split**: Combine multiple PDFs into a single document or extract specific page ranges.
- **Compress & Optimize**: Reduce PDF file size with adjustable compression levels.
- **Scan & OCR**: Camera-based document scanner with perspective correction and on-device text extraction.
- **Security & Forms**: Add signatures, watermarks, password protect, and unlock encrypted files.

### Media Player & Photos
- **Video Player**: Hardware-accelerated playback (LibVLC) supporting MKV, MP4, WebM, and AVI with gesture-based volume, brightness, and seek controls.
- **Photo Viewer**: High-resolution image canvas with zoom, EXIF metadata inspector, and Recycle Bin integration.

### Material You Design
- **Dynamic Theming**: Extracts system wallpaper color schemes on Android 12+ (API 31+).
- **Pure AMOLED Black**: Dedicated setting toggle for true `#000000` deep black dark mode.
- **Ergonomic Navigation**: Signature floating pill dock, responsive haptic feedback, and modern M3 shapes.

---

## Tech Stack

- **Language**: Kotlin 2.1.0
- **UI Framework**: Jetpack Compose (BOM 2024.12.01) with Material 3 (1.3.1)
- **Document Rendering**: Artifex MuPDF Fitz
- **Media Playback**: VideoLAN LibVLC Android 3.6.5
- **Local Storage**: Android Room 2.6.1 & Jetpack DataStore
- **Image Loading**: Coil 2.7.0
- **Target SDK**: Android 15 (API 35) • **Minimum SDK**: Android 8.0 (API 26)

---

## Privacy & Security

- **100% Offline**: All rendering, conversions, and tool operations execute on-device.
- **Zero Telemetry**: No tracking, no user profiling, no analytics SDKs.
- **Zero Ads**: Completely ad-free experience.

---

## Building from Source

### Prerequisites
- Android Studio Ladybug or newer
- JDK 21

### Build APK
```bash
# Debug build
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest
```

---

## License

This project is licensed under the GNU Affero General Public License v3.0 (AGPLv3) / LGPL v2.1. See `LICENSE` for details.
