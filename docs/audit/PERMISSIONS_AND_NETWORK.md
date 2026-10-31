> [!NOTE]
> **Historical Audit Snapshot (October 2026)**
> This document is a historical audit report representing an earlier state of the codebase.
> Current security policies, architecture, updater hardening, and privacy guarantees are documented in [README.md](../../README.md), [SECURITY.md](../../SECURITY.md), and [improvement.md](../../improvement.md).

# Permissions and Network Touchpoints Audit

**Target Application:** `com.abhisheksati.mydoc`  
**Core Goal:** Privacy-first, offline document viewer with GitHub self-updater as the *only* intended external network user.

---

## 1. Android Manifest Permissions Audit

Every permission declared in `app/src/main/AndroidManifest.xml` evaluated against necessity, principle of least privilege, and security risk:

| Permission | Declared In Manifest | Purpose in App | Is Really Needed? | Risk / Assessment |
|---|---|---|---|---|
| `android.permission.INTERNET` | Line 18 | GitHub self-update checking, APK downloading, and local Wi-Fi sharing socket. | **Conditional** | Required for self-updater and local Wi-Fi sharing server socket. **CRITICAL RISK:** Misused by `LibreOfficeEngineManager.kt` to download external APKs from third-party mirrors. |
| `android.permission.ACCESS_NETWORK_STATE` | Line 19 | Network availability check before querying GitHub releases. | **Yes** | Standard Android network state query. Safe. |
| `android.permission.POST_NOTIFICATIONS` | Line 20 | Update download progress notifications on Android 13+ (API 33+). | **Yes** | Standard runtime permission for foreground/background progress alerts. Safe. |
| `android.permission.REQUEST_INSTALL_PACKAGES` | Line 21 | Prompting system PackageInstaller to install self-updated APK. | **Yes** | Necessary for in-app self-updater. Required to open `ACTION_INSTALL_PACKAGE` intents on Android 8.0+. |
| `android.permission.READ_EXTERNAL_STORAGE` | Line 22 (`maxSdkVersion="32"`) | Legacy reading of device documents, photos, videos. | **Yes** | Proper legacy scoping using `maxSdkVersion="32"` for backward compatibility with Android 12 and below. |
| `android.permission.WRITE_EXTERNAL_STORAGE` | Line 23 (`maxSdkVersion="29"`) | Legacy writing of files/PDF tool outputs on Android 9 and below. | **Yes** | Proper legacy scoping using `maxSdkVersion="29"` for Android 9 (API 28) and below. |
| `android.permission.MANAGE_EXTERNAL_STORAGE` | Line 24 | Full storage access for offline document manager, cleaner, and PDF tools. | **Yes** | Essential for a standalone document manager and file manager. Requires user manual grant via `Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION`. |
| `android.permission.QUERY_ALL_PACKAGES` | Line 25 | Querying all installed apps in `AppManagerHelper.kt`. | **NO (CRITICAL LEAK)** | **Severe privacy violation.** Grants visibility into all installed packages on the user device. Google Play policy strictly bans this for document viewers. The app is a personal document viewer; querying all packages exposes private user app inventory. If app backup functionality is retained, intent filtering (`<queries>`) must be used instead. |
| `android.permission.CAMERA` | Line 26 | Document scanning tool via CameraX in `CameraScanSheet.kt`. | **Yes** | Necessary for scanning physical documents into PDF pages. Safe. |

### Missing Permission Flagged by Android Lint:
- **`android.permission.USE_BIOMETRIC` is MISSING from `AndroidManifest.xml`:**  
  *Code Location:* `BiometricLockHelper.kt:58`  
  *Impact:* When biometric authentication (`BiometricPrompt.authenticate`) is invoked on Android 9 and Android 10 devices, the system throws a runtime `SecurityException` crashing the process because the manifest fails to declare `android.permission.USE_BIOMETRIC`. Must be added.

---

## 2. Comprehensive Network Touchpoint Audit

The app claims to be strictly offline with the sole exception of the GitHub self-updater. Every class performing network operations or holding URLs was audited:

### Touchpoint 1: GitHub Self-Updater (`GitHubUpdateService.kt`)
- **Protocol:** HTTPS only (`https://api.github.com/repos/.../releases/latest`)
- **Library:** `HttpURLConnection` via standard Android Java runtime.
- **Request Headers:**
  - `Accept: application/vnd.github.v3+json`
  - `User-Agent: MyDoc-Updater`
- **Data Transmitted:** None. Pure GET request. No telemetry, device IDs, or user analytics sent.
- **Status:** **PASS** (Strictly functional, no data leak).

### Touchpoint 2: APK Downloader (`ApkDownloader.kt`)
- **Protocol:** HTTPS (Release asset URL hosted on `github.com` / `objects.githubusercontent.com`).
- **Library:** `HttpURLConnection` streaming directly to `context.cacheDir/updates/update.apk`.
- **Data Transmitted:** None. Streaming GET request.
- **Vulnerabilities Identified:**
  1. *Missing SHA-256 enforcement:* If GitHub release does not provide a `.sha256` asset file, `expectedSha256` defaults to `null`, and verification is silently bypassed.
  2. *Missing Package Name & Signature Check:* Downloaded APK is sent directly to `PackageInstallerHelper` without verifying package name (`com.abhisheksati.mydoc`) or signature match against currently installed certificate.
- **Status:** **CONDITIONAL PASS** (Needs cryptographic integrity hardening).

### Touchpoint 3: Local Wi-Fi Sharing (`WifiServerManager.kt`)
- **Protocol:** HTTP over local LAN (default port `8080` / fallback `8888`).
- **Library:** NanoHTTPD (`org.nanohttpd:nanohttpd:2.3.1`).
- **Network Scope:** Offline LAN only. Binds to local IP (`192.168.x.x` or hotspot IP).
- **Data Transmitted:** Local file streaming to browser clients on the same local Wi-Fi network. No external WAN traffic.
- **Security Assessment:** Unauthenticated local server. Anyone on the local Wi-Fi subnet knowing the IP and port can download shared files while sharing is active. UI must clearly show active status and provide an instant kill switch.
- **Status:** **PASS** (Operates entirely offline on local network).

### Touchpoint 4: LibreOffice Dynamic APK Downloader (`LibreOfficeEngineManager.kt`) — CRITICAL VIOLATION
- **File:** `rendering/engine/libreoffice/LibreOfficeEngineManager.kt` (Lines 63–93, 443–461)
- **Hardcoded External Download URLs:**
  - Primary: `https://f-droid.org/repo/org.documentfoundation.libreoffice_...apk`
  - Mirror 1: `https://mirror.cyberbits.eu/tdf/libreoffice/android/...apk`
  - Mirror 2: `https://ftp.osuosl.org/pub/tdf/libreoffice/android/...apk`
- **Behavior:**
  Downloads a 60MB+ full LibreOffice Viewer APK from third-party repositories in the background without user consent, unzips native shared libraries (`libsofficeapp.so`, etc.), writes them to internal storage with `setReadable(true, false)` (world-readable permissions!), and invokes `System.load(libFile.absolutePath)`.
- **Verdict:**
  1. **Direct violation of the offline privacy promise.**
  2. **Severe Dynamic Code Loading (DCL) vulnerability.** Executing native code downloaded over the internet without cryptographic signing validation allows arbitrary code execution.
  3. Google Play policy strictly prohibits downloading executable code (`.so`, `.dex`) from outside the store.
- **Recommendation:** **REMOVE IMMEDIATELY.** Remove all network download logic from `LibreOfficeEngineManager`. All document parsers must be local/bundled or use Android's native rendering primitives.

---

## 3. Telemetry, Tracking & SDK Audit

We verified the complete dependency tree and bytecode for hidden analytics, crash loggers, or trackers:

| Tracking / SDK Category | Found? | Evidence |
|---|---|---|
| Google Analytics / Firebase | **NO** | Zero Firebase/GMS analytics dependencies in `libs.versions.toml` or `build.gradle.kts`. |
| Crashlytics / Sentry / Bugsnag | **NO** | No crash reporting SDKs present. Crash traces stay on-device in logcat. |
| Advertising SDKs (AdMob, Unity, AppLovin) | **NO** | Zero ad libraries present. |
| Telemetry / Tracking Proxies | **NO** | Zero analytics pings in network layer. |
| Third-party Font CDN fetching | **NO** | `Inter.ttf` is bundled locally in `res/font/inter.ttf`. |

---

## 4. Privacy Conclusion
Except for the dangerous dynamic code loader in `LibreOfficeEngineManager.kt` (which must be expunged) and the excessive `QUERY_ALL_PACKAGES` permission (which must be removed), the application strictly adheres to an offline, privacy-first model. Once `LibreOfficeEngineManager` network fetching is deleted and `QUERY_ALL_PACKAGES` is dropped, the only internet activity in the entire app will be user-triggered GitHub release checks.
