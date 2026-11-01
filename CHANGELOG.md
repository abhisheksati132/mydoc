# Changelog

All notable changes to MyDoc. Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
versions follow the `versionName` in `app/build.gradle.kts`.

## [Unreleased] — since v1.4.3

Range: `v1.4.3..HEAD` (14 commits, 24 files, +4207 / −3047).

Every entry below is internal. **No user-visible behaviour, layout, or copy changed in this
range** — this batch is structural work plus the tests that lock it down.

### Changed

- **File manager decomposed.** `FileManagerScreen` is no longer the owner of its own chrome:
  extracted `FileManagerActionSheet`, `FileManagerClipboardBar`, `FileManagerSelectionTopBar`,
  and `FileManagerBreadcrumbBar`. The screen is down to 895 lines from 1,272.
- **Presentation viewer decomposed.** Extracted `PresentationSlideCards`,
  `PresentationEmptyRecoveryCard`, `PresentationBottomBar`, `PresentationTopBar`, and
  `PresentationSpeakerNotesSheet`. `PresentationViewerScreen` is down to 727 lines from 1,871.
- **PDF engine decomposed.** `PdfToolsEngine` split into `PdfSecurityEngine`,
  `PdfPageOperationsEngine`, `PdfConversionEngine`, `PdfOptimizationEngine`, and
  `PdfEditingEngine`, with shared helpers moved to `PdfToolUtils`. The remaining facade is
  433 lines from 1,711; its public API is unchanged.
- `OfficeToPdfConverter.generateCacheKey` now takes a `uriString` instead of a `Uri` so the
  cache-key derivation can be unit tested. Behaviour is identical.

### Fixed

- **Update verification on Android 13+.** `PackageInstallerHelper` now reads archive and app
  package info through `PackageInfoFlags` on API 33+, instead of relying on the deprecated
  int-flag overloads that are supressed today and removed in a future SDK.

### Added (tests)

- `PackageInstallerHelperTest` — signature and package-verification paths.
- `OfficeToPdfConverterTest` — deterministic cache keys and temp-file hygiene.
- `ToolModelsTest` — `formatByteSize` and tool savings calculations.
- `PdfToolUtilsTest` — additional coverage alongside the extraction.
