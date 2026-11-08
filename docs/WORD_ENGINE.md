# Offline Word page rendering

The primary Word viewer uses LibreOfficeKit 26.2.6.3 in a dedicated `:word_engine` process, exports a local PDF, and displays it using the existing page reader. DOCX, DOTX, DOC, DOT, ODT, OTT, FODT and RTF use this path. The simpler parser remains available for unprotected reflow reading.

This replaces MyDoc's limited page reconstruction with an office layout engine: headers, footers, table-cell pictures, styles and section layout can survive conversion. It does not promise pixel-identical Microsoft Word output. Missing fonts can change pagination; external content, macros and active objects are disabled. Format details continue to disclose these limits.

## Execution and limits

- The source is copied without modification into a private, uniquely named job directory. Input is limited to 64 MiB.
- Conversions serialize, time out after 60 seconds, and are cancelled by terminating the dedicated worker. Native crashes leave the main reader alive.
- Before loading documents, the worker installs a kernel filter denying internet sockets, connections and executable launches, then verifies the restriction. The main reader and updater retain their normal network access. This is process hardening, not a claim of complete isolation from every file under the app UID.
- Native output files are limited to 256 MiB; accepted PDFs contain 1–4000 pages. A timed-out, cancelled or failed job is removed.
- The engine profile disables macros, active content and automatic external link updates. Bundled fonts make rendering independent of a network connection.
- Unprotected output is validated before atomic publication in a versioned, content-addressed cache.
- Protected Word documents prompt for the source password. Their derived PDFs are encrypted with a fresh random key, not the source password, and removed with the viewer session. Neither key nor source password is persisted. An interrupted process may leave an encrypted preview in Android's private evictable cache.

## Builds and provenance

Gradle runs `scripts/prepare_word_engine.py`. Python 3.9+, Android build tools, Java, and NDK 29.0.14206865 are required; the task installs the pinned NDK if absent. The first build downloads three verified upstream APKs (roughly 300 MiB total). Subsequent builds reuse `GRADLE_USER_HOME/caches/mydoc-word-engine`, or `MYDOC_WORD_ENGINE_CACHE` if supplied. Generated assets/libraries stay under `app/build/generated/word-engine`; binaries are not committed.

ABI-specific APKs carry one engine architecture; the universal APK carries all three and is substantially larger. See [engine provenance and licenses](../third_party/libreoffice/README.md). Engine updates require changing the version, all package hashes, source reference and cache version, followed by device fidelity checks.

## Verification scope

Device regressions cover headers/footers, merged table text, table-picture and colored-text pixels, all eight registered Word extensions, original-source preservation, concurrent opens, and missing/wrong/correct passwords. A protected preview must stay encrypted on disk and reopening the source must request authentication again. The tests also check that the worker's network restriction does not leak into the main process.

These synthetic and upstream regression fixtures establish particular capabilities, not universal Word compatibility. ARM binaries are prepared for both device ABIs; an x86_64 emulator does not verify their execution. Physical-device checks and a larger collection of independent Microsoft Word reference exports remain valuable before making stronger fidelity claims.

### Verified 2026-11-08

515 JVM tests and 24 API 35 x86_64 device tests passed, including the protected Word viewer's full unlock/text/close lifecycle. Debug lint completed with zero errors; debug, AndroidTest and R8-optimized release builds passed. The release build was signed using a temporary local test certificate, not a production release credential, and opened both the layout fixture and the protected fixture through the actual password dialog on the emulator. All 64-bit engine libraries have 16 KiB ELF LOAD alignment; this is a binary inspection, not execution on a 16 KiB device. ARM execution remains untested.

Measured APK sizes for this build: ARM64 approximately 100 MiB, ARMv7 91 MiB, x86_64 107 MiB, universal 267 MiB. These are whole-app download sizes and may change with subsequent builds.
