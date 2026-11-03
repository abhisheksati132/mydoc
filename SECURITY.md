# Security Policy

## Supported Versions

Security fixes and updates are provided for the following releases:

| Version | Supported          |
| ------- | ------------------ |
| 1.5.x   | :white_check_mark: |
| < 1.5.0 | :x:                |

Current version: **1.5.13**

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

### 1. On-Device Document Processing
All document reading, parsing, conversion, and compression operations are performed locally using embedded native rendering libraries:
- **PDF Rendering:** MuPDF 1.28.5 Fitz native vector engine with viewport tiling and thread-safe locking.
- **Office Documents:** Word, Excel and PowerPoint files are parsed on-device by the app's own Kotlin OOXML parsers. No Apache POI, no third-party Office library and no cloud service is involved. Office-to-PDF conversion is no longer offered.
- **PDF Manipulation & Tools:** Apache PDFBox 2.0.27.0 (merge, split, compress, watermark, reorder, sign).
- **Text Recognition:** Google ML Kit on-device text recognition. The model runs locally; on devices that do not ship it, Google Play Services may download the model once. The scanned document itself is never sent.
- **Multimedia Engine:** Google AndroidX Media3 ExoPlayer (hardware-accelerated, zero JNI crash risk).

Document and media content never leaves the device through MyDoc. Outbound traffic is limited to three paths: the optional GitHub update check and download described in section 5; the optional one-time Google Play Services download of the ML Kit text-recognition model on devices that do not ship it; and the metrics transport bundled by ML Kit's Google DataTransport/Firebase components (see section 4).

### 2. No WebView in Document Pipeline
MyDoc does not use WebViews in its document rendering pipeline. Document parsing and layout are handled completely via native Kotlin and C++ vector renderers to prevent cross-site scripting (XSS), JavaScript injection, and WebView sandbox escape vulnerabilities.

### 3. ZIP Bomb & Untrusted Input Safety
Each OOXML parser applies its own bounds while decompressing (`.docx`, `.odt`, `.pptx`, `.xlsx`):
- Maximum single entry size: 25 MB
- Maximum entry count: 10,000 (Word, PowerPoint) and 2,000 (Excel)
- Maximum total uncompressed size: 100 MB (Excel)
- Maximum compression ratio: 100:1 (PowerPoint, anti-decompression bomb protection)

The shared `ZipSafeReader` (entry, total-size, count and ratio limits) is implemented and unit-tested but is **not yet wired into the parsers**, so the limits differ per format and total uncompressed size is not capped uniformly.

### 4. Zero Telemetry & Tracking
MyDoc contains zero advertising SDKs, zero user tracking libraries and zero crash reporting telemetry, and the app itself collects no analytics. The ML Kit text-recognition dependency does transitively bundle Google DataTransport/Firebase components in the merged manifest, which can emit Google metrics about model usage. If strict zero telemetry is required, that transport can be disabled or the ML Kit model bundled so Play Services is not contacted at runtime.

### 5. Transparent, Hardened Updates
The application checks exclusively for updates against public GitHub Releases. This check:
- **Defaults On**: Enabled by default and can be switched off in the app's Settings under *Check for updates on launch*.
- **Domain Restricted**: Connections are strictly limited to `api.github.com`, `github.com`, and `*.githubusercontent.com`.
- **Integrity Verified**: Downloaded APKs require matching SHA-256 checksums from corresponding `.sha256` release assets.
- **Signature Enforced**: In release builds, the updater cryptographically validates the downloaded APK's signing certificate against pinned release fingerprints (`RELEASE_CERT_SHA256`) before installation.

### 6. Least-Privilege Sandboxing & Encryption
- **FileProvider**: Internal storage and root device paths are never exposed. File sharing is restricted to designated cache subdirectories (`cache/updates/`, `cache/shared/`, `cache/tools_output/`) and the offline document cache (`files/saved_docs/`). Shared and opened files are always handed to other apps as `content://` URIs, never `file://`.
- **App Lock**: Biometric and screen lock protection is requested via `KeyguardManager.isDeviceSecure`, but the guard fails open: when the device has no screen lock, `BiometricLockHelper` calls `onSuccess` without any authentication. The lock is also not re-applied when the app leaves the foreground — there is no re-lock on `onStop`, so an unlocked session stays unlocked until the process is recreated. Treat it as a convenience barrier, not a strong boundary against a local attacker.
- **Android Keystore Encryption**: Signatures and sensitive data are encrypted on-device with AES-256-GCM using non-exportable Android Keystore keys, and the encrypted signature file is excluded from cloud and device backups. The code does not request StrongBox and does not verify at runtime that the key is hardware-backed, so on devices without a usable hardware keystore the key may be software-backed.
- **Permissions**: `MANAGE_EXTERNAL_STORAGE` is user-granted all-files access, used to browse and open documents anywhere in shared storage; `WRITE_EXTERNAL_STORAGE` is declared with `maxSdkVersion="29"` for saving on pre-scoped-storage releases; `REQUEST_INSTALL_PACKAGES` exists solely so the in-app updater can hand a signature-verified APK to the system installer.

### 7. Android Backup & Device Transfer
MyDoc opts into the platform backup system. If backup or device transfer is enabled by the user's Android settings, the OS may copy the app database (recent-document index), offline document copies under `saved_docs/`, and the user-pinned folder list in `pinned_folders_prefs.xml` to the user's own Google backup and onto a new device. This is OS behaviour, not an upload by MyDoc. Excluded from backup: `mydoc_settings.xml` (settings and app-lock state), `mydoc_updater.xml` (updater state), all cache directories, and the hardware-encrypted signature file. Users who do not want document copies in platform backup should disable backup for MyDoc in Android Settings.
