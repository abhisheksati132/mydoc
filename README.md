<div align="center">

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="assets/brand/readme-hero-dark.svg">
  <img src="assets/brand/readme-hero-light.svg" width="1200" alt="MyDoc — Your files. In good order.">
</picture>

**Your Android file library, document reader, and PDF toolkit.**

Find the handout, pick up where you left off, and send the finished PDF from the same app.

<a href="https://github.com/abhisheksati132/mydoc/releases/latest">
  <img src="assets/brand/download-mydoc.svg" width="272" height="56" alt="Download the signed Android APK from GitHub">
</a>

Android 8.0+ · 100% on-device · Free and open source · [Changelog](CHANGELOG.md)

</div>

## One app, three jobs

| Find it | Read it | Finish it |
| --- | --- | --- |
| Pinned folders, saved searches, document and media categories, date-grouped photos, previews, and browsing inside ZIP archives. | PDF, Word, Excel, PowerPoint, EPUB, and text — with recents, restored reading positions, bookmarks, outline, search, and format-aware controls. | Scan pages, merge and reorder, compress, sign, or pull text out of a scan with the bundled OCR. |

Also inside: a local media player (tracks, external subtitles, speed, A–B loop, HTTPS playback) and a storage view that shows what fills your phone and moves files to the Recycle Bin instead of deleting them.

## Every format reads the same way

MyDoc has one viewer shell, not one per file type. PDF, Word, Excel, PowerPoint, and text all share the same top bar, the same bottom bar, the same contents drawer, bookmarks, search, appearance sheet, and the same tap-to-immerse behavior. Learn it once and it works for every document you open — including slide decks, which now use the shared chrome instead of a lookalike.

The chrome floats *over* the page instead of pushing it down, so the document you are reading gets the full screen. When you scroll, the bars condense; when you tap, they and the system bars fade away together.

## Designed to disappear

- **Material 3 Expressive** on top of the Ink & Seal palette — warm paper, graphite, a teal seal — with dynamic Monet color as an opt-in.
- **Liquid glass chrome** (Haze): the top and bottom bars are real blurred glass over the page, not opaque slabs, with a thin rim that catches the light.
- **Motion that flows**: spring-driven "snappy" and "settle" curves, container transforms, and figure-8 tab transitions instead of linear slides.
- **No noise**: no floating layout warnings, no nag banners inside your documents, no badges you did not ask for. Light, dark, and AMOLED Black, with brightness and keep-screen-on handled in the viewer.
- **Consistent by construction**: one set of design tokens (type, spacing, radii, motion) used across Library, Home, Tools, and every viewer, so screens cannot drift apart.

## Fast by measurement, not by claim

MyDoc ships with a [Macrobenchmark](macrobenchmark) suite that measures the release build the way users install it (R8 on, same shrinker):

- **Cold startup** via `StartupTimingMetric` — TTID median ~0.63 s on the reference emulator.
- **Frame timing** via `FrameTimingMetric` — a multi-fling scrolling pass over the Tools grid, the densest always-present Compose surface, reports P50/P90/P95/P99 frame CPU and overrun. On the reference emulator the median frame fits the 120 Hz budget (8.3 ms) comfortably while the P90 tail still overruns it; the emulator's software GPU exaggerates that tail, and a physical device is the real test.

Run it yourself on a physical device: `./gradlew :macrobenchmark:connectedBenchmarkAndroidTest`.

## Private by default

Parsing, rendering, OCR, search indexing, and every document tool run on your device. No account, no cloud, no telemetry. MyDoc asks for folder access only when you browse your own storage, and declines gracefully — Recents and the system file picker keep working without it.

## Get MyDoc for Android

1. Open [GitHub Releases](https://github.com/abhisheksati132/mydoc/releases/latest).
2. Download the **ARM64** APK for a 64-bit ARM phone, or **universal** if you are unsure.
3. Open the APK and follow Android's prompts. Signed APKs and SHA-256 files are listed together.

Already installed? Use **Settings → About** to check for updates.

## Build from source

Requirements: JDK 21, Android SDK with platform 37, and Git.

```bash
git clone https://github.com/abhisheksati132/mydoc.git
cd mydoc
./gradlew :app:assembleDebug          # debug APK (ABI-split; pick app-x86_64-debug.apk for emulators)
./gradlew :app:testDebugUnitTest      # ~600 unit tests
./gradlew :app:lintDebug              # Android lint
./gradlew :macrobenchmark:connectedBenchmarkAndroidTest   # frame + startup benchmarks
```

Gradle dependency verification is enabled (`gradle/verification-metadata.xml`), so new dependencies need checksums added before they build. Release signing uses environment-provided keystore values; see `app/build.gradle.kts` for the guard.

### Modules

| Module | Purpose |
| --- | --- |
| `:app` | The application — Library, viewers, tools, settings. |
| `:lint-rules` | Project-specific lint checks. |
| `:office-engine` | Optional LibreOffice-based companion APK for higher-fidelity Office layouts. |
| `:macrobenchmark` | Startup and frame-timing benchmarks that measure the release build. |

## Open source

MyDoc is built by [Abhishek Sati](https://github.com/abhisheksati132). [Report a bug or suggest an improvement](https://github.com/abhisheksati132/mydoc/issues). If MyDoc earns a place on your phone, a GitHub star helps others find it.

[AGPL-3.0 license](LICENSE) · [Third-party notices](THIRD_PARTY_NOTICES.md) · [Security policy](SECURITY.md)
