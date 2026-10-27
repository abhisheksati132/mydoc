<div align="center">

<img src="app/src/main/res/ic_launcher-web.png" width="96" height="96" alt="MyDoc Logo" />

# MyDoc

**Fast. Private. Beautiful.**  
A modern, distraction-free document reader & PDF toolkit for Android.

[![Latest Release](https://img.shields.io/github/v/release/abhisheksati132/mydoc?style=for-the-badge&color=2563EB)](https://github.com/abhisheksati132/mydoc/releases/latest)
[![Android](https://img.shields.io/badge/Android-15%20%7C%20API%2026%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Offline](https://img.shields.io/badge/Privacy-100%25%20Offline-success?style=for-the-badge&logo=shield)](https://github.com/abhisheksati132/mydoc)

<br/>

[**Download Latest APK**](https://github.com/abhisheksati132/mydoc/releases/latest) • [**Features**](#-features) • [**Tech Stack**](#-built-with) • [**License**](#-license)

</div>

---

## ✨ Features

### 📖 Universal Reader
- **High-Performance PDF Engine**: Powered by Artifex MuPDF Fitz with sub-pixel vector rendering, continuous scroll, and up to 500% zoom.
- **Office & Books**: Native readers for `.docx`, `.xlsx`, `.pptx`, `.epub`, and plain text.
- **Reading Continuity**: Remembers your exact page, scroll position, and reading progress.

### 🛠️ On-Device PDF Toolkit
- **Organize**: Merge multiple documents, split ranges, extract, and rotate pages.
- **Optimize**: Compress file size with smart image downsampling and repair broken PDF streams.
- **Convert**: Images to PDF, PDF to image bundles (ZIP), and text extraction.
- **Security & Sign**: Draw digital signatures, add/remove AES-256 passwords, and strip metadata.
- **Camera Scanner**: Auto-edge detection, perspective crop, and crisp B&W document enhancements.

### ⚡ Tactile & Expressive Design
- **Fluid Physics**: Spring damping press physics, smooth animated list transitions, and interactive ripples.
- **Smart Organization**: Instant sub-category filtering (`PDF`, `Word`, `Excel`, `PPT`, `Text`).
- **OLED Ready**: Clean Light, Dark, and Pure AMOLED Black (`#000000`) modes.

### 🔒 100% Private & Offline
- **Zero Telemetry**: No analytics, no user tracking, no third-party network pings.
- **Zero Cloud Dependence**: 100% of processing happens locally on your device.
- **Zero Ads**: Free and open for everyone.

---

## 🚀 Quick Look

| Capability | Supported Formats / Tools |
|---|---|
| **Documents** | `PDF`, `DOCX`, `XLSX`, `PPTX`, `EPUB`, `TXT` |
| **Media Player** | Hardware-accelerated `MP4`, `MKV`, `MP3`, `FLAC` with gesture seek & PiP |
| **Tools** | Merge, Split, Compress, Sign, Encrypt, Decrypt, Convert, Rotate |
| **Scanner** | Real-time camera capture, perspective crop, multi-page PDF generation |
| **Updates** | Seamless built-in in-app updater connected to GitHub Releases |

---

## 🛠️ Built With

- **Language**: [Kotlin 2.1.0](https://kotlinlang.org/)
- **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3
- **Rendering Engine**: [Artifex MuPDF Fitz](https://mupdf.com/)
- **Media Engine**: [AndroidX Media3 ExoPlayer](https://developer.android.com/media/media3)
- **Local Storage**: Jetpack Room & DataStore
- **Async & Reactive**: Kotlin Coroutines & Flow

---

## 📦 Building from Source

```bash
# Clone the repository
git clone https://github.com/abhisheksati132/mydoc.git
cd mydoc

# Run tests
./gradlew testDebugUnitTest

# Assemble Release APK
./gradlew assembleRelease
```

---

## 📄 License

MyDoc is licensed under the **GNU Affero General Public License v3.0 (AGPLv3)**. See [LICENSE](LICENSE) for details.
