# Offline Office page rendering

## Optional engine delivery (1.9.8)

The `office-engine` Android application module owns the LibreOffice libraries, fonts and assets. They are no longer embedded in the main APK. Without the companion, MyDoc uses its built-in Word reflow, spreadsheet grid and presentation readers, with their existing format and fidelity limits. These readers do not provide equivalent LibreOffice compatibility. Protected OOXML containers are explicitly rejected rather than scraped into invented contents.

Settings → Reading → Enhanced Office support downloads the device ABI package from its versioned Office Engine component release. It verifies SHA-256, package/version and the signing certificate against both the installed app and pinned release certificate, then invokes Android installation. The same card offers removal. Android confirms installation and removal. Debug builds use the matching debug-signed CI engine artifact. Component releases are marked prerelease to exclude them from the app updater's latest-release endpoint; this also protects older app versions that do not filter companion assets.

The companion has its own UID and worker process. The client validates its signing identity and protocol before binding to an explicit service. The service requires a signature permission and checks the caller signing identity again. Read-only source and write-only result file descriptors cross the boundary, rather than arbitrary paths into the main application. Sources remain bounded to 64 MiB and PDFs to 256 MiB. Cancellation kills the companion worker; the next cold start removes abandoned private jobs. Protected preview PDFs remain encrypted and session-owned.

Removal deletes Android-managed companion assets and private data. Existing unprotected previews remain in MyDoc cache. Availability is checked before using conversion caches, so cached pages do not keep the enhanced viewer active after removal. Office content indexing requires the companion; filename search and tags remain usable without it. PDFs and other independent readers retain their own engines. Manual reflow, grid and slideshow inspection modes remain available where supported; enhanced default Office pages use LibreOffice.

CI installs the companion before Office fidelity and password tests and covers disabling the companion. Release builds sign both packages with the configured key, verify signature/package/version/ABI metadata, and publish ABI-specific engine APKs and checksums. New runtime results must be reported separately from the historical results below.

## Rendering pipeline

The primary Word, PowerPoint and Excel page viewers use LibreOfficeKit 26.2.6.3 in a dedicated `:word_engine` process, exports a local PDF, and displays it using the existing page reader. DOCX, DOTX, DOC, DOT, ODT, OTT, FODT and RTF use Writer. PPTX, PPTM, PPSX, POTX, PPT, PPS, POT, ODP, OTP and FODP use Impress. XLSX, XLSM, XLS, ODS, OTS and FODS use Calc. The existing service/process/build-task names keep their `word` prefix for compatibility; the queue, process and engine are shared across all three families.

The simpler Word parser remains available for unprotected reflow reading. Excel opens in page view by default; **Cell grid** in the overflow menu opens the searchable, cached-value grid, with Back returning to the original pages. CSV and TSV open directly in that grid. Page export follows workbook print areas, scaling, page breaks and visible sheets; it is a print layout rather than an interactive spreadsheet. LibreOffice may calculate formulas during import; the separate grid continues to show stored cached results. Presentations are static pages: animations, transitions and media playback are not reproduced. The existing independent slideshow/notes path remains available for unprotected decks and has its own rendering limits.

This replaces MyDoc's limited page reconstruction with an office layout engine: headers, footers, table-cell pictures, styles and section layout can survive conversion. It does not promise pixel-identical Microsoft Word output. Missing fonts can change pagination; external content, macros and active objects are disabled. Format details continue to disclose these limits.

## Execution and limits

- The source is copied without modification into a private, uniquely named job directory. Input is limited to 64 MiB.
- Conversions serialize, time out after 60 seconds, and are cancelled by terminating the dedicated worker. Native crashes leave the main reader alive.
- Before loading documents, the worker installs a kernel filter denying internet sockets, connections and executable launches, then verifies the restriction. The main reader and updater retain their normal network access. This is process hardening, not a claim of complete isolation from every file under the app UID.
- Native output files are limited to 256 MiB; accepted PDFs contain 1–4000 pages. A timed-out, cancelled or failed job is removed.
- The engine profile disables macros, active content and automatic external link updates. Bundled fonts make rendering independent of a network connection.
- Unprotected output is validated before atomic publication in a versioned, content-addressed cache.
- Protected Office documents prompt for the source password. Their derived PDFs are encrypted with a fresh random key, not the source password, and removed with the viewer session. Neither key nor source password is persisted. An interrupted process may leave an encrypted preview in Android's private evictable cache.

## Builds and provenance

Gradle runs `scripts/prepare_word_engine.py`. Python 3.9+, Android build tools, Java, and NDK 29.0.14206865 are required; the task installs the pinned NDK if absent. The first build downloads three verified upstream APKs (roughly 300 MiB total). Subsequent builds reuse `GRADLE_USER_HOME/caches/mydoc-word-engine`, or `MYDOC_WORD_ENGINE_CACHE` if supplied. Generated assets/libraries stay under `app/build/generated/word-engine`; binaries are not committed.

ABI-specific APKs carry one engine architecture; the universal APK carries all three and is substantially larger. See [engine provenance and licenses](../third_party/libreoffice/README.md). Engine updates require changing the version, all package hashes, source reference and cache version, followed by device fidelity checks.

## Verification scope

Device regressions cover headers/footers, merged table text, table-picture and colored-text pixels, all eight registered Word extensions, original-source preservation, concurrent opens, and missing/wrong/correct passwords. A protected preview must stay encrypted on disk and reopening the source must request authentication again. The tests also check that the worker's network restriction does not leak into the main process.

These synthetic and upstream regression fixtures establish particular capabilities, not universal Word compatibility. ARM binaries are prepared for both device ABIs; an x86_64 emulator does not verify their execution. Physical-device checks and a larger collection of independent Microsoft Word reference exports remain valuable before making stronger fidelity claims.

### Verified 2026-11-08

515 JVM tests and 24 API 35 x86_64 device tests passed, including the protected Word viewer's full unlock/text/close lifecycle. Debug lint completed with zero errors; debug, AndroidTest and R8-optimized release builds passed. The release build was signed using a temporary local test certificate, not a production release credential, and opened both the layout fixture and the protected fixture through the actual password dialog on the emulator. All 64-bit engine libraries have 16 KiB ELF LOAD alignment; this is a binary inspection, not execution on a 16 KiB device. ARM execution remains untested.

Measured APK sizes for this build: ARM64 approximately 100 MiB, ARMv7 91 MiB, x86_64 107 MiB, universal 267 MiB. These are whole-app download sizes and may change with subsequent builds.

## PowerPoint and Excel extension (current working changes)

Both converters now call the shared native queue, retaining source suffixes so Impress/Calc select the correct import filter. The cache identity includes engine revision, extension and source bytes; PDFs produced by the old custom renderers are not reused. Password prompts, private encrypted previews, cancellation and worker network restrictions apply to all Office page viewers. Native PDF bookmarks provide the outline when available. The original file is never rewritten.

Regression coverage was added for complete PPTX/XLSX packages with two slides/sheets, chart labels, tables and colored images/fills, a legacy binary XLS, cache reuse, source preservation, and protected PPTX/XLSX missing/wrong/correct password behavior. Test additions alone do not establish passing device results; see the current work report for actual execution status. The older verification and APK size measurements above refer to the Word-only implementation. Font substitution, unsupported Office features and unusual print settings still require reference-document comparisons.

### Browser-workspace verification for the Office extension

- `:app:testDebugUnitTest`: 521 passed, 1 skipped (522 total).
- `:app:compileDebugAndroidTestKotlin`: passed, including the new Impress/Calc fidelity and password-lifecycle tests.
- All three pinned LibreOffice packages were downloaded, signature/checksum verified, and prepared with NDK 29.0.14206865.
- The new device tests have not been executed in this workspace. No emulator is configured and the device bridge attempts to write to the read-only home directory. Compilation is not evidence of native runtime fidelity.
- `:app:lintDebug`: passed (zero errors).
