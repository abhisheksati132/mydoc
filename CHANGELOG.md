# Changelog

All notable changes to MyDoc. Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
versions follow the `versionName` in `app/build.gradle.kts`.

## [1.4.4] — 2026-11-01

Range: `v1.4.3..v1.4.4`.

### Changed

- **Design foundation rebuilt.** One token layer now owns colour, radius, spacing, typography and
  motion (`designsystem/tokens`, `designsystem/animation`): warm paper neutrals, ink text, a single
  violet accent, a 4dp spacing grid, a six-step radius scale, and a crisp `AppMotion` timing
  contract. Every interactive surface uses the same `pressScaleEffect` press physics.
- **Brand surface.** Launcher and splash re-cut to the editorial identity; the `Stacks` name is
  gone from metadata and generated artefacts.
- **File manager decomposed.** `FileManagerScreen` is no longer the owner of its own chrome:
  extracted `FileManagerActionSheet`, `FileManagerClipboardBar`, `FileManagerSelectionTopBar`,
  `FileManagerBreadcrumbBar`, `FileManagerTopBar` and `FileManagerOverlays`. The screen is down to
  895 lines from 1,272, then further by the later top-bar/overlay split.
- **Google Files hub decomposed.** `GoogleFilesHubContent` split into `EditorialStorageOverviewCard`,
  `EditorialCategoryGrid` and `StorageVolumeCards` (634 lines removed from the hub).
- **Settings decomposed.** `SettingsScreen` split into `SettingsAppearanceSection`,
  `SettingsReadingSection`, `SettingsStorageSection`, `SettingsMediaSection`, `SettingsAboutSection`
  and `SettingsComponents` (793 lines removed from the screen).
- **Presentation viewer decomposed.** Extracted `PresentationSlideCards`,
  `PresentationEmptyRecoveryCard`, `PresentationBottomBar`, `PresentationTopBar`, and
  `PresentationSpeakerNotesSheet`. `PresentationViewerScreen` is down to 727 lines from 1,871.
- **PDF viewer and engines decomposed.** `PdfViewerScreen` split into `PdfFloatingContextualBar`,
  `PdfPageActionSheet`, `PdfViewerDialogsAndSheets`, `PdfViewerControlsOverlay` and
  `PdfContextualActionsHost`. `PdfToolsEngine` split into `PdfSecurityEngine`,
  `PdfPageOperationsEngine`, `PdfConversionEngine`, `PdfOptimizationEngine`, `PdfEditingEngine` and
  `PdfScanOcrEngine`, with shared helpers in `PdfToolUtils`. The remaining facade is 433 lines from
  1,711; its public API is unchanged.
- `OfficeToPdfConverter.generateCacheKey` now takes a `uriString` instead of a `Uri` so the
  cache-key derivation can be unit tested. Behaviour is identical.

### Fixed

- **Update verification on Android 13+.** `PackageInstallerHelper` now reads archive and app
  package info through `PackageInfoFlags` on API 33+, instead of relying on the deprecated
  int-flag overloads that are supressed today and removed in a future SDK.
- **`Stacks` leftovers.** The APK extraction directory is `Download/MyDoc/ExtractedAPKs` (was
  `Download/Stacks/ExtractedAPKs`), and PDF/A producers plus the conversion report say `MyDoc`.

### Fixed (visual pass)

- **Overlay windows followed the platform bar theme.** Bottom sheets and dialogs own a separate
  window, so the activity's edge-to-edge bar appearance never reached them: on a dark theme the
  sheet drew a light navigation bar. The new `OverlaySystemBars()` in
  `designsystem/components/OverlaySystemBars.kt` resolves the enclosing window through
  `DialogWindowProvider` and applies `AppTheme.isDark` to that window's controller. It is called
  from `AppBottomSheet`, `AppDialog`, and the 21 hosts that build `ModalBottomSheet`/`Dialog`/
  `AlertDialog` directly, so the fix covers the whole class, not one screen.
- **Chip strips clipped the next chip.** Horizontally scrolling rows cut the following chip at the
  viewport edge with no hint that more content existed. `Modifier.horizontalScrollWithFade(state)`
  in `designsystem/components/ScrollEdgeFade.kt` erases a 24dp ramp at each edge with
  `BlendMode.DstOut` over an offscreen layer, driven by `canScrollBackward`/`canScrollForward`, so
  the fade resolves to the real surface underneath on light, dark and AMOLED. Applied to the file
  and format filter chips, home quick actions, file-manager category chips, the Tools category
  strip, tool option rows, scan filter presets, spotlight filter pills and viewer tool pills.
- **Storage legend hid half its data.** The overview card's legend scrolled sideways and showed
  about four of its eight entries. It is now a `FlowRow` that wraps, so files, photos, videos,
  audio, apps, other and free space are all visible at once with no hidden destination.

### Added (tests)

- `PackageInstallerHelperTest` — signature and package-verification paths.
- `OfficeToPdfConverterTest` — deterministic cache keys and temp-file hygiene.
- `ToolModelsTest` — `formatByteSize` and tool savings calculations.
- `PdfToolUtilsTest` — additional coverage alongside the extraction.

### Changed (design-system consolidation)

- **One chip.** `AppChip` is the only chip implementation; `AppFilterChip` is a thin alias for
  callers that still name the filter role. The five hand-rolled chips (format filters, file
  categories, theme pickers, tool sticky filters, preview swatches) now render through it, so they
  share selection colour, border, minimum touch target and press physics.
- **Raw colours tokenized.** 135 hex literals across the UI layer moved into
  `DocumentCanvasColors`, `ToolGroupColors`, `CaptureChromeColors` and `StorageChartColors`, or onto
  the existing semantic roles (`error`, new `warning` and `scrim`). Document-content defaults in
  `PptxParser` stay literal on purpose — they are file colours, not app chrome.
- **Raw radii tokenized.** All 140 `RoundedCornerShape(N.dp)` literals outside the token package now
  reference `AppRadius`; the scale gained `large` (16dp) and `xLarge` (24dp) so 16/18 and 22/24/26/28/32
  literals collapse onto one step each. Thin progress bars and drag handles use `roundShape`. Bottom
  sheets that carried 24dp or 28dp tops now use the documented `sheetShape` (20dp).
- **No per-call text sizes.** Chrome text that overrode `fontSize` (`15`, `12`, `11`, `9`,
  `17`, `14` sp) now uses a typography role. Reading-scale and document-rendering sizes are
  untouched, because they are content, not chrome.
- **No `isDarkTheme` plumbed through screens.** `HomeScreen`, `HomeTabContent`, `ToolsScreen`,
  `AudioPlayerScreen` and `SpotlightSearchSheet` read `AppTheme.isDark` from the composition instead
  of receiving a boolean that duplicated `LocalIsDark`.
- **Redundant OLED branches removed.** `AppTheme` already swaps in `BlackAppColors` for AMOLED, so
  `if (isOled) <literal> else colors.X` and `if (isOled) colors.surface else colors.surfaceContainer`
  were always the same value; they are now just `colors.X`. `EditorialStorageOverviewCard` lost its
  unused `isOled` parameter.
- **Storage legend matches the bar.** The overview card's stacked bar draws six segments
  (docs, photos, videos, audio, apps, other); the legend now labels all of them plus free space
  instead of hiding Apps and Other. Entries with no destination are no longer clickable.
- **Dead colour and API removed.** `StorageCategoryType.color` (zero consumers, ten literals) is
  gone. Unused imports across the UI layer were dropped after the tokenization pass.

### Verification

- `./gradlew :app:testDebugUnitTest` — 125 tests, 0 failures.
- `./gradlew :app:assembleDebug` — debug APK builds.
- `./gradlew :app:lintDebug` — 0 errors, 146 warnings, in issue classes unrelated to this work:
  `GradleDependency` (64), `ModifierParameter` (21), `UnusedResources` (19),
  `IconDuplicatesConfig` (10), `ObsoleteSdkInt` (7), `DefaultLocale` (4),
  `FrequentlyChangedStateReadInComposition` (4), `AndroidGradlePluginVersion` (3),
  `TrustAllX509TrustManager` (3), `StaticFieldLeak` (3), `AutoboxingStateCreation` (3),
  `UseTomlInstead` (3), plus one each of `ScopedStorage`, `SetJavaScriptEnabled`,
  `QueryPermissionsNeeded`, `PictureInPictureIssue` and `SourceLockedOrientationActivity`.
  None of them are in the files added by the visual pass.
- Visual pass on an API 35 emulator (`mydoc_api35`), light, dark and AMOLED: nine screens
  captured to `screenshots/` (`01-launch` … `09-quickactions-scrolled`). Overlay bars were
  confirmed at the window level with `dumpsys window`: the sheet's dialog window no longer
  reports `LIGHT_NAVIGATION_BAR` in dark theme, and still reports it in light theme where a white
  bar is correct. Chip fades were confirmed by sampling the strip edge pixels (ramp, not a hard
  cut) before and after a swipe. The legend was confirmed to render every entry without scrolling.
  Layout and motion at other screen sizes, rotation and non-default font scales remain unverified.
