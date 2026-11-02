# Security Policy

## Supported Versions

Security fixes and updates are provided for the following releases:

| Version | Supported          |
| ------- | ------------------ |
| 1.5.x   | :white_check_mark: |
| < 1.5.0 | :x:                |

Current version: **1.5.3**

Users are encouraged to always use the latest release from the [GitHub Releases](https://github.com/abhisheksati132/mydoc/releases) page.

---

## Platform & Supported SDKs

- **Minimum SDK:** API 26 (Android 8.0 Oreo)
- **Target SDK:** API 35 (Android 15)
- **Architecture:** `arm64-v8a`, `armeabi-v7a`, `x86_64`

---

## Reporting a Vulnerability

We take the security of MyDoc and our users' privacy very seriously. If you believe you have discovered a vulnerability, please report it responsibly:

1. **GitHub Security Advisory (Private)**:
   Submit a private advisory via [GitHub Private Vulnerability Reporting](https://github.com/abhisheksati132/mydoc/security/advisories/new).
2. **Direct Contact**:
   Alternatively, contact the maintainer directly via GitHub profile email or submit a private issue.

Please include:
- A clear description of the vulnerability.
- Steps to reproduce or a proof of concept.
- The affected version(s) of MyDoc and Android OS versions tested.
- Any potential impact.

### Response Timeline
- **Acknowledgement**: Within 48 hours.
- **Triage & Assessment**: Within 5 business days.
- **Fix & Public Advisory**: Once a patch has been tested and released.

Please do not open public issues detailing reproducible exploit details before a fix is published.

---

## Core Security & Architecture Principles

### 1. 100% On-Device Processing
All document reading, parsing, conversion, and compression operations are performed locally using embedded native rendering libraries:
- **PDF Rendering:** MuPDF 1.28.5 Fitz native vector engine with viewport tiling and thread-safe locking.
- **Office Conversion:** Apache POI & custom high-performance engine for Word (`.doc`, `.docx`), Excel (`.xls`, `.xlsx`), and PowerPoint (`.ppt`, `.pptx`).
- **PDF Manipulation & Tools:** Apache PDFBox 2.0.27.0 (merge, split, compress, watermark, reorder, sign).
- **Text Recognition:** Google ML Kit on-device text recognition (100% offline OCR).
- **Multimedia Engine:** Google AndroidX Media3 ExoPlayer (hardware-accelerated, zero JNI crash risk).

User documents and media never leave the device.

### 2. No WebView in Document Pipeline
MyDoc does not use WebViews in its document rendering pipeline. Document parsing and layout are handled completely via native Kotlin and C++ vector renderers to prevent cross-site scripting (XSS), JavaScript injection, and WebView sandbox escape vulnerabilities.

### 3. ZIP Bomb & Untrusted Input Safety
All ZIP/OOXML archive parsing (`.docx`, `.pptx`, `.xlsx`, `.odp`, `.odt`) is strictly guarded by `ZipSafeReader`:
- Maximum single entry size: 25 MB
- Maximum total uncompressed archive size: 200 MB
- Maximum entry count: 10,000 entries
- Maximum compression ratio: 100:1 (anti-decompression bomb protection)

### 4. Zero Telemetry & Tracking
MyDoc contains zero advertising SDKs, zero user tracking libraries, and zero crash reporting telemetry.

### 5. Transparent, Hardened Updates
The application checks exclusively for updates against public GitHub Releases. This check is:
- **Optional**: Can be completely disabled in the app's Settings under *Check for updates on launch*.
- **Domain Restricted**: Connections are strictly limited to `api.github.com`, `github.com`, and `*.githubusercontent.com`.
- **Integrity Verified**: Downloaded APKs require matching SHA-256 checksums from corresponding `.sha256` release assets.
- **Signature Enforced**: In release builds, the updater cryptographically validates the downloaded APK's signing certificate against pinned release fingerprints (`RELEASE_CERT_SHA256`) before installation.

### 6. Least-Privilege Sandboxing & Encryption
- **FileProvider**: Internal storage and root device paths are never exposed. File sharing is restricted to designated, temporary cache subdirectories (`cache/updates/`, `cache/shared/`, `cache/tools_output/`).
- **App Lock**: Biometric and screen lock protection is strictly guarded via `KeyguardManager.isDeviceSecure`.
- **Hardware-Backed Keystore**: Signatures and sensitive data are encrypted on-device with AES-256-GCM using hardware-backed Android Keystore keys and excluded from automated cloud and device backups.
