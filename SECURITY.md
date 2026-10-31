# Security Policy

## Supported Versions

Security fixes and updates are provided for the following releases:

| Version | Supported          |
| ------- | ------------------ |
| 1.3.x   | :white_check_mark: |
| < 1.3.0 | :x:                |

Users are encouraged to always use the latest release from the [GitHub Releases](https://github.com/abhisheksati132/mydoc/releases) page.

---

## Reporting a Vulnerability

We take the security of MyDoc and our users' privacy very seriously. If you believe you have discovered a vulnerability, please report it responsibly:

1. **GitHub Security Advisory (Preferred)**:
   Submit a private advisory via [GitHub Private Vulnerability Reporting](https://github.com/abhisheksati132/mydoc/security/advisories/new).
2. **Direct Contact**:
   Alternatively, contact the maintainer directly via GitHub profile email.

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

## Core Security Principles

### 1. 100% On-Device Processing
All document reading, parsing, conversion, and compression operations are performed locally using embedded native rendering libraries (MuPDF Fitz, Apache PDFBox Android, AndroidX Media3). User documents and media never leave the device.

### 2. Zero Telemetry & Tracking
MyDoc contains zero advertising SDKs, zero user tracking libraries, and zero crash reporting telemetry.

### 3. Transparent, Hardened Updates
The application checks exclusively for updates against public GitHub Releases. This check is:
- **Optional**: Can be completely disabled in the app's Settings under *Check for updates on launch*.
- **Domain Restricted**: Connections are strictly limited to `api.github.com`, `github.com`, and `*.githubusercontent.com`.
- **Integrity Verified**: Downloaded APKs require matching SHA-256 checksums from corresponding `.sha256` release assets.
- **Signature Enforced**: In release builds, the updater cryptographically validates the downloaded APK's signing certificate against pinned release fingerprints (`RELEASE_CERT_SHA256`) before installation.

### 4. Least-Privilege Sandboxing
- **FileProvider**: Internal storage and root device paths are never exposed. File sharing is restricted to designated, temporary cache subdirectories (`cache/updates/`, `cache/shared/`, `cache/tools_output/`).
- **WebView Hardening**: WebViews rendering local documents have network loads strictly blocked (`blockNetworkLoads = true`), local filesystem access disabled (`allowFileAccess = false`), and cross-origin resource access restricted.
- **Hardware-Backed Keystore**: Signatures and sensitive data are encrypted on-device with AES-256-GCM using hardware-backed Android Keystore keys and excluded from automated cloud and device backups.
