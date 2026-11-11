# Security Policy

## Supported Versions

Security fixes and updates are provided for the following releases:

| Version | Supported          |
| ------- | ------------------ |
| 2.0.x   | :white_check_mark: |
| < 2.0.0 | :x:                |

Current version: **2.0.4** (`versionCode` 194), produced by the tag-triggered release workflow. Older
lines are not maintained: fixes land on the current build only, so the security
behaviour described below is the behaviour of the latest release, not of an older APK you may still
have installed.

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
- **Office Documents:** Word-family pages are converted locally using the bundled LibreOfficeKit engine in a dedicated companion worker process (`com.abhisheksati.mydoc.office`). The reader authenticates the companion worker via cryptographic package signature verification (`checkSignatures()`) before establishing IPC. PowerPoint and spreadsheet page conversion and the simpler reflow readers use MyDoc's Kotlin parsers. No document is sent to a cloud conversion service. See [Word engine boundaries and provenance](docs/WORD_ENGINE.md).
- **PDF Manipulation & Tools:** Apache PDFBox 2.0.27.0 (merge, split, compress, watermark, reorder, sign).
- **Text Recognition:** Google ML Kit on-device text recognition (`com.google.mlkit:text-recognition:16.0.1`, the bundled-model artifact). The models ship inside the APK — `assets/mlkit-google-ocr-models/**` plus `lib/<abi>/libmlkit_google_ocr_pipeline.so`, both read out of a built artifact — so nothing is downloaded on first use. The document itself is never sent.
- **Multimedia Engine:** Google AndroidX Media3 ExoPlayer (hardware-accelerated, zero JNI crash risk). Local media and HTTPS streaming only; cleartext HTTP traffic is disabled.

Document and media content does not leave the device through MyDoc: no code path uploads one. The only outbound connection MyDoc itself makes is the optional GitHub update check and download described in section 5. Separately, ML Kit's Google DataTransport/Firebase components are packaged in the APK and registered in its merged manifest (section 4). MyDoc never invokes them, and no traffic from them was observed while testing — that is a report about what was measured, not proof that they cannot transmit.

### 2. No WebView in Document Pipeline
MyDoc does not use WebViews in its document rendering pipeline. Document parsing and layout use Kotlin and native engines. A separate Settings dialog displays bundled, static license text in a WebView with scripting, file/content access, network loads and navigation disabled; it never receives a user document.

### 3. ZIP Bomb & Untrusted Input Safety
MyDoc's Kotlin OOXML parsers apply bounds while decompressing (`.docx` reflow, `.pptx`, `.xlsx`):
- Maximum single entry size: 25 MB
- Maximum entry count: 10,000 (Word, PowerPoint) and 2,000 (Excel)
- Maximum total uncompressed size: 100 MB (Excel)
- Maximum compression ratio: 100:1 (PowerPoint, anti-decompression bomb protection)

These bounds apply to `DocxParser`, `PptxParser`, and `SpreadsheetParser`. They are not a claim about LibreOffice's internal decompression. The Word page worker instead has a 64 MiB source limit, 60-second timeout, 256 MiB per-file output limit, and accepts 1–4000 PDF pages. It runs outside the reader, installs and verifies a kernel filter blocking internet sockets/connections and executable launches, and disables macros, active objects and link updates. The worker shares the app UID and is not a complete filesystem sandbox. A native memory failure can terminate the worker; the reader receives a conversion failure rather than claiming the document is damaged.

Protected Word previews remain encrypted with a random session key and are deleted when the viewer closes. Source passwords and preview keys are not persisted. A process interruption can leave an encrypted preview in private evictable cache; it does not leave a reusable unlocked cache. Source files remain unchanged.

### 4. No app-side telemetry
MyDoc contains no advertising SDK, no crash-reporting SDK and no analytics library, and its own code collects no usage data, identifiers or documents. Calling the whole APK "zero telemetry" would overstate that: ML Kit's `text-recognition` dependency transitively pulls in `com.google.android.datatransport`, `com.google.firebase` and `com.google.android.gms` artifacts, and the packaged APK's merged manifest registers their transport plumbing — `TransportBackendDiscovery` with a `cct.CctBackendFactory` backend, `JobInfoSchedulerService` and `AlarmManagerSchedulerBroadcastReceiver`. That is Google's own usage/metrics machinery: MyDoc does not configure it, name an endpoint for it, or call into it, and auditing this build observed no request from it and no document content leaving the device. Because it is third-party code inside the distributed artifact, the stronger claim — that nothing can ever leave the device — is not one this project makes from source inspection alone. OCR is the only feature that depends on ML Kit, so a build without that transport is a build without on-device OCR.

### 5. Transparent, Hardened Updates
The application checks exclusively for updates against public GitHub Releases. This check:
- **Defaults On**: Enabled by default and can be switched off in the app's Settings under *Check for updates on launch*.
- **Domain Restricted**: Connections are strictly limited to `api.github.com`, `github.com`, and `*.githubusercontent.com`.
- **Integrity Verified**: Downloaded APKs require matching SHA-256 checksums from corresponding `.sha256` release assets.
- **Signature Enforced**: In release builds, the updater cryptographically validates the downloaded APK's signing certificate against pinned release fingerprints (`RELEASE_CERT_SHA256`) before installation.

### 6. Least-Privilege Sandboxing & Encryption
- **FileProvider**: Internal storage and root device paths are never exposed. File sharing is restricted to designated cache subdirectories (`cache/updates/`, `cache/shared/`, `cache/tools_output/`) and the offline document cache (`files/saved_docs/`). Shared and opened files are always handed to other apps as `content://` URIs, never `file://`.
- **App Lock**: Settings refuses to enable the lock on a device with no screen credential, and the guard fails closed: when `KeyguardManager.isDeviceSecure` is false, `BiometricLockHelper` reports an error rather than calling `onSuccess`, so the documents stay behind `AppLockScreen`. The session re-locks whenever the app leaves the foreground — `MainActivity.onStop()` and `onRestart()` clear the unlocked state and re-arm `FLAG_SECURE` while the lock is enabled, and `onCreate()` starts locked — and a share or open intent received while locked is held until authentication succeeds. It remains a barrier against a person holding an unlocked phone, not against a rooted device or someone who knows the user's device credential.
- **Android Keystore Encryption**: Signatures and sensitive data are encrypted on-device with AES-256-GCM using non-exportable Android Keystore keys, and the encrypted signature file is excluded from cloud and device backups. The code does not request StrongBox and does not verify at runtime that the key is hardware-backed, so on devices without a usable hardware keystore the key may be software-backed.
- **Permissions**: `MANAGE_EXTERNAL_STORAGE` is user-granted all-files access, used to browse and open documents anywhere in shared storage; `WRITE_EXTERNAL_STORAGE` is declared with `maxSdkVersion="29"` for saving on pre-scoped-storage releases; `REQUEST_INSTALL_PACKAGES` exists solely so the in-app updater can hand a signature-verified APK to the system installer.

### 7. Android Backup & Device Transfer
MyDoc opts into the platform backup system and excludes the things that matter from it. The exclusions are in `app/src/main/res/xml/backup_rules.xml` (Android 11 and below) and, for Android 12+, `data_extraction_rules.xml`, where the same list is applied to **both** `cloud-backup` and `device-transfer`: the Room database `mydoc.db` with its `-wal` and `-shm` side files (recent documents, favourites, offline-copy records, trash entries, playback positions), the offline document copies under `saved_docs/`, the trash under `.trash/`, the pinned-folder list `pinned_folders_prefs.xml`, `mydoc_settings.xml` (settings and app-lock state), `mydoc_updater.xml` (updater state), all cache directories, and the Keystore-encrypted `user_signature.enc` — whose key could not migrate to another device anyway.

So the document index and any cached document copies are not part of OS backup or device transfer. Anything not named above is still eligible for whatever the platform decides to copy; a user who wants nothing copied should turn off backup for MyDoc in Android settings.

### 8. Storage permission: assessed, kept
`MANAGE_EXTERNAL_STORAGE` is the widest storage permission Android offers, and it is a real cost: while
granted, MyDoc can read every file in shared storage, not only documents, and Play requires the
all-files access justification for it.

It is kept because it is load-bearing for what the app promises. MyDoc's core behaviour is opening,
searching, converting, renaming, trashing and restoring files **wherever the user keeps them**, plus
in-app file management (move, delete, restore from a trash folder). SAF-only access cannot provide
that: a `content://` tree grant covers only the trees the user picked, cannot be remembered across
reinstall in the way the recent-documents index expects, and does not allow the rename/move/trash
operations the app performs. Storage Access Framework remains the fallback path — documents opened
through a picker are read through their URI, and `DocumentStreamResolver` resolves those URIs without
needing all-files access at all — so a user who refuses the permission still gets a working reader, just
not the file-manager half of the app.

What narrowing looks like, if it is ever done: keep all-files access for the file-management screens
only, make every open path go through SAF or the platform recents, and drop the permission for devices
where the user never uses those screens. That is a storage-model redesign, not a hardening fix, and it
is recorded here as an accepted, documented risk rather than silently tolerated.

### 9. Checked-in debug keystore: assessed, kept
`app/keystore/debug.keystore` (alias `mydoc`, store and key password `android`) is committed on purpose,
for debug builds only. `signingConfigs` binds it to the `shared` config, which only the `debug` build
type selects; the `release` build type uses the `release` config, whose `storeFile` is set **only** when
`MYDOC_KEYSTORE_PATH` points at a real file, and a release assembly without the four keystore
environment values fails the build rather than falling back. A debug and a release artifact therefore
cannot share a signer by accident, and the pinned `RELEASE_CERT_SHA256` check in the updater is the
second tripwire.

A public, well-known debug key is not a secret, so it needs no rotation and its presence in git history
leaks nothing that an attacker does not already have (the Android default debug keystore is public by
definition). What history does carry is every version of this file — which is why the keystore stays
debug-only by configuration, not by obscurity. Removing it would make debug installs diverge per
machine (each developer's own `~/.android/debug.keystore`) and break `adb install -r` for anyone
switching machines, so it is kept. If a real release keystore is ever committed, that is an incident:
the file must be removed, the key rotated, and the history rewritten — none of which applies to this one.

### 10. Not yet in place
- **Gradle dependency verification** is committed (`gradle/verification-metadata.xml`: 233 components, 381 SHA-256 entries, signature verification on with two unresolvable keys ignored and the upstream trust list Gradle wrote). It covers **artifacts** — the jars, aars and native libraries that would actually be executed. Gradle was configured with `verify-metadata="false"` because the first enforcing build failed on 32 buildscript POM/module metadata files that the generation pass never recorded checksums for; those files carry no code, so this is a documented scope reduction rather than a bypass, and an artifact swap in a mirrored repository is caught. `assembleDebug`, `testDebugUnitTest` and `lintDebug` all pass with verification enforced.
- **`armeabi-v7a` and `arm64-v8a` artifacts are not executed in CI.** The release workflow verifies every
  published APK's signature, ABI contents and package/version metadata, but the emulator is x86_64, so
  only the x86_64 split and the universal APK are launched. See the release body's "What CI ran" block.
