# MyDoc privacy policy

**Effective 15 November 2026.** This policy covers the MyDoc Android app
(`com.abhisheksati.mydoc`) distributed through
[GitHub Releases](https://github.com/abhisheksati132/mydoc/releases).

## The short version

MyDoc does not collect, sell, or transmit your data. There is no account, no
analytics, no advertising, and no remote crash reporting. Everything the app does
with your documents happens on your phone.

## What stays on your device

- Documents, photos, and videos you open — never uploaded, never copied unless
  you choose to share or export.
- Recents, favourites, bookmarks, reading positions, and playback positions.
- Search index, OCR results, extracted text, and thumbnails.
- PDF signatures you save and PDFs you create or edit.
- Your app-lock PIN (stored only as a salted hash) and biometric preferences.
- Crash reports, if the app crashes — written to a private file. They leave the
  app only when you open **Settings → About → Last crash report** and press
  **Copy** or share them yourself.

Android's system backup may include app data. Backup rules exclude the crash-log
folder; you can disable backup entirely in Android settings.

## Network access

MyDoc makes network requests only in these cases, all over HTTPS (cleartext HTTP
is disabled by the network security configuration):

1. **Update checks** against `api.github.com` when you launch the app (if enabled)
   or press **Check** in Settings → About. GitHub, as any web server, receives
   your IP address and request details. MyDoc sends no identifiers and reads only
   public release metadata.
2. **Update downloads** from `github.com` when you choose to install an update.
   Every download is verified against the release's published SHA-256 and the
   app's pinned signing certificate before Android is asked to install it.
3. **Opening links you tap**, such as View source or Report an issue, which hand
   the URL to your browser.

OCR runs on-device with models bundled inside the APK. The app does not fetch
models or send images to any OCR service.

## Permissions and why they exist

- **All-files access** (Android 11+) — optional. Only requested when you browse
  folders outside the system picker. Declining leaves Recents, the system file
  picker, and every document tool working. Revoke it any time in Android
  settings; MyDoc falls back to the picker.
- **Photos / video / audio** (Android 13+) — to list media in the Library and
  play them.
- **Camera** — only while you are scanning; no background use.
- **Install unknown apps** — only for in-app updates you start yourself.
- **Biometric** — optional app lock; authentication is handled by Android.

## Third parties

- **GitHub** hosts releases and serves update checks (see above).
- **Google ML Kit** text recognition is bundled and runs locally. It contacts no
  Google service for OCR.
- **LibreOffice engine** is an optional companion APK built from LibreOffice
  source and runs in an isolated process with network access blocked.

MyDoc contains no advertising SDKs and no analytics SDKs.

## Children

MyDoc is a general-purpose document tool and collects no personal information
from anyone, including children.

## Changes

Material changes to this policy will be published in this file and noted in
[CHANGELOG.md](CHANGELOG.md). The current version always lives at
`https://github.com/abhisheksati132/mydoc/blob/main/PRIVACY.md`.

## Contact

Questions: open an issue at
https://github.com/abhisheksati132/mydoc/issues. Security reports: see
[SECURITY.md](SECURITY.md).
