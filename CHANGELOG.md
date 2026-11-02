# Changelog

All notable changes to MyDoc. Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
versions follow the `versionName` in `app/build.gradle.kts`.

## [Unreleased]

### Added

- **Dynamic Material You theming.** Colours are seeded from the wallpaper on Android 12+ (fixed
  fallback elsewhere), with the black/AMOLED override preserved. The existing token layer is fed
  from the generated scheme, so legacy screens pick up the same palette.
- **Files-first two-tab navigation.** Home is now just Files and Tools; the Files screen owns the
  storage readout, search and settings entry, and the bottom bar is a Material 3 navigation bar.
- **New Files screen.** Recents and Browse share one header and one list/grid preference. Browse
  drills into storage with breadcrumbs, sorting, hidden-file toggle and storage insights; folders
  and files use real page/photo thumbnails.
- **Selection and bulk actions.** Long-press enters multi-select with share, copy, cut, rename,
  compress and recycle actions; the clipboard bar shows the pending operation and its destination
  hint.
- **Storage insights sheet** breaking usage down by category with a shortcut to the cleaner.
- **Tools tab rebuilt on Material 3.** One searchable list grouped by task, a filter-chip strip,
  search that focuses on open, and tool rows on tonal plates replacing the gradient tiles.
- **Tool runner sheet on Material 3.** Header, input list, progress, result verification and the
  save/open/share actions now use tonal surfaces and standard buttons; the "add more files" action
  appends to the input list instead of replacing it.
- **Tool sheets rebuilt on Material 3.** The page organizer, form filler and signature pad are now
  full-height Material 3 modal sheets with tonal headers, outlined cards, standard text fields,
  switches, dropdowns and filled action buttons that respect the current theme's insets.
- **Viewer chrome rewritten on Material 3.** The document top and bottom bars, PDF floating action
  bar, page action sheet, document tools sheet, reading controls, page grid, thumbnail strip,
  search overlay, jump/password dialogs, outline drawer, fast scrollbar and presentation bars now
  use tonal surfaces, standard shape and type scales, filter chips and standard buttons. The
  vivid gradients, custom chips and bespoke radii are gone from the viewer.
- **Scanner rebuilt on Material 3.** The capture screen drops its gradient and glow treatment for
  a single primary accent, standard type scale and theme-aware controls, while keeping the dark
  scrims that read correctly over a live viewfinder.

### Changed

- **Settings rewritten on Material 3.** Cards, section labels, theme and reading chips, toggles,
  cache actions and the update section all use the generated scheme and M3 components.

- **Search, clipboard bar and folder chrome rewritten on Material 3.** The spotlight sheet is a
  surface-based sheet with filter chips and thumbnails; the clipboard bar is an elevated surface
  with a single-line summary.

### Fixed

- **Paste from a category view wrote into the last folder.** Copying a file and then opening a
  category pasted into the previous directory and dropped the user on the browse hub. The clipboard
  bar now only appears while browsing a real folder, and paste stays in that folder.
- **Form choice fields showed bracketed values.** A combo box value came back as
  `[United States]` from PDFBox, and the editor both displayed and saved the brackets. Choice values
  now come from the selected option list and round-trip cleanly.
- **Page organizer actions sat behind the system taskbar.** The organizer's fixed action bar was
  covered by the gesture-nav taskbar; as a full-height sheet it now stays in reach.
- **New-folder button covered the Paste button.** The FAB steps aside while the clipboard bar is up.
- **Opening a document reset Files to Recents.** The pane is restored from the view model, so
  returning from the viewer lands back in the folder.
- **System bars followed system night mode, not the app theme.** Status and navigation bar icon
  appearance now tracks the app's chosen theme.
- **Document tools sheet opened half-expanded and cut off the list.** It now opens fully expanded,
  so the tool list and its bottom padding stay above the navigation bar.
- **Splash and window background sat on the old paper palette.** The window background now matches
  the Material 3 light surface and AMOLED black, so app launch no longer flashes a different tone.
- **Word documents opened as one blank page.** The offscreen WebView that was supposed to lay the
  document out is never attached to a window, so its measured height was zero and the band slicer
  emitted a single empty page and reported success. Word files now paginate through a native
  renderer built on the existing DocxParser, honouring the document's own page size and margins.
- **Legacy presentations and ebooks were reported as damaged.** `.ppt`, `.pps`, `.pot` and `.odp`
  had no converter, so the raw package was handed to a PDF engine; `.epub`, `.cbz`, `.xps` and
  `.fb2` routed to the text viewer because `DocumentCategory.EPUB` was an alias of `TEXT`. Both
  now reach engines that parse them.
- **Every reopen repaid the full conversion.** Cache keys were the file name alone, or the cached
  PDF was deleted when the viewer closed. Keys now include a content or size+mtime stamp, writes
  are staged and moved into place so a half-written PDF cannot be cached as valid, and failures
  log instead of surfacing as "the file may be damaged".
- **An HTML or CSV table saved as `.xls` opened as an empty grid.** The legacy reader only scraped
  UTF-16 byte pairs, so a single-byte text file yielded no cells at all. Non-OLE2 payloads are now
  read as an HTML table (markup stripped, entities decoded) or as delimited text first.
- **Viewer content clipped the first spreadsheet row.** A fixed 64dp top pad was added under a bar
  that already sits below the status bar.

### Changed

- **Startup and browsing no longer touch disk from the main thread.** Volume enumeration is a
  suspend call, `LibraryItem` size and date strings are computed lazily off-main with a
  thread-safe date formatter, and the recents, continue-reading and document flows `flowOn`
  `Dispatchers.Default`.
- **The landing junk scan is bounded.** The Files pane shows a reclaimable estimate, and the scan
  that feeds it walked the whole volume with no limit; it now stops at 4,000 entries or 600 ms,
  and opening the cleaner still scans everything.
- **Trash and restore cost one MediaStore round trip instead of one per file.** The per-file
  awaited scan capped every delete at 2 s; paths are now batched into a single call.
- **Fewer recompositions and search storms.** Files list and grid rows read selection state
  outside their item lambdas, document text search is debounced by 250 ms, the first screenful of
  page sizes is primed off-main before the viewer publishes its state, and the zoom tile cache is
  cleared when a viewer dies instead of outliving its document.
- **Bitmap caches are bounded by bytes.** The document thumbnail cache moved from 64 entries to a
  6 MB budget, library thumbnails no longer stage a source larger than 40 MB into cache for a
  44 dp tile, and the unused far-page eviction pass was deleted.

## [1.5.0] — 2026-11-02

Range: `v1.4.6..v1.5.0`.

### Changed

- **New visual language: paper.** Home, Files and Settings now render as warm paper sheets with
  ink text, a single blue accent, fibre grain, folded corners, flat icon plates and printed rules.
  The gradient plaques, aurora backdrop, spectrum rules and gradient chips are gone from these
  screens; the storage readout is a paper card with a square accent gauge.
- **Shared chrome follows the host screen.** The tab header rule, section rules and filter chips
  read a `PaperTheme` scope, so Tools and the viewer keep the vivid language while papered
  screens stay on paper. Dark and AMOLED paper tones were lifted off pure black so sheets remain
  visible.
- **Home deck and Index list every recent.** The deck was stuck at 01 because continue-reading
  items were filtered against the recents list they were merged into; all recents now appear once.

## [1.4.6] — 2026-11-02

Range: `v1.4.5..v1.4.6`.

### Fixed

- **Theme toggle left list content on the old palette.** Lazy list items did not invalidate when
  the palette composition local changed, so after flipping the theme the header and nav updated
  while every card, chip and row kept the previous theme's colors until the tab was re-entered.
  The palette is now folded into the lazy item keys on Home, Tools and the Files hub, so items
  rebuild the moment the theme changes.
- **Scanner layout and chrome.** The camera dialog lays Compose out against the display while its
  window decor sits inside the system bars, so inset padding reported zero and the shutter bar was
  pushed under the navigation bar (the sheet's bottom edge landed at 2534px on a 2274px-tall visible
  area). The safe area is now measured against the window with `getWindowVisibleDisplayFrame` and the
  bar bands are dimmed, so the controls sit fully inside the visible window. The page head is a
  masthead (kicker, title, tabular page folio, 2dp rule), the aspect modes and scan filters are
  rule-underlined tabs instead of pill groups, the viewfinder is centred in the camera window between
  the two chrome bands, thumbnails are hairline-framed pages with tabular folios, and the commit
  action is a single accent stamp.
- **Scan output was dropped.** `selectTool()` resets the input list, and the scanner called
  `setInputUris()` before it, so committing a scan opened "Scan to PDF" with no input and a disabled
  run button. The tool is now selected first, then the captured pages are attached.

### Changed

- **Vivid sweep across the tool runner.** The tool sheet opens on a gradient header plaque in the
  tool's section hue and closes its header on the 3dp spectrum rule; the empty input picker is an
  accent-tinted drop card, attached files are soft surface cards with ocean document plaques, and
  progress tracks use the pill gauge. Run and Save are glowing brand-gradient pills with white
  labels, the verification panel, Open / Share and the error retry are soft cards, and the success
  header carries a forest check plaque. Inline option launchers (visual page organizer, form
  editor, signature pad) became secondary framed cards with accent iconography instead of
  competing full-width gradients, so the Run CTA is the only primary action on the sheet. This
  also fixes black labels and icons that sat on accent-filled option buttons in light mode.
- **Vivid sweep across Settings, reader chrome and scanner.** The last editorial surfaces join
  the vivid language. Settings sections are soft cards with gradient icon plaques (grape
  Appearance, ocean Reading, sunset Media, forest Storage, fire Privacy, brand About) on an
  aurora backdrop; folio numbering is gone; toggles, row icons and the update progress bar run
  on the accent and brand gradient; inline actions are soft accent pills. The reader's top and
  bottom bars close on the 3dp spectrum rule, the page scrubber and active sheet tab are accent /
  gradient, and the bookmark fills with accent. The scanner's chrome bands close on spectrum
  rules, mode and filter tabs carry gradient underlines, the page count is a gradient pill, and
  the commit stamp is a glowing brand-gradient tile. `AppSectionHeader` is now title plus a short
  spectrum underline everywhere (Home, Files, Tools, sheets), so section breaks read identically
  across the app.
- **Vivid premium redesign.** The editorial print grammar is replaced by a saturated, glowing
  design language on Home, Files, Tools and the root nav. New tokens in
  `designsystem/tokens/VividGradients.kt` define seven fixed gradient families (brand, sunset,
  ocean, forest, gold, grape, fire) plus colored `glow` shadows; `AppColors` gains pink, cyan,
  amber and green accents and richer light/dark/AMOLED palettes; `AppRadius` softens (cards 16dp,
  sheets 24dp, pill chips). New primitives in `designsystem/components/VividComponents.kt`:
  `AuroraBackdrop` (slow-breathing radial blooms behind every tab), `VividRule` (the 3dp spectrum
  line that closes every header) and `VividIconTile` (gradient plaque with a matching halo).
  Home opens on a glowing gradient scan hero with a frosted camera disc, three gradient action
  tiles and soft cards with colored format badges; Tools and Files list hue-coded cards per group
  and category; the storage readout is a card with a spectrum gauge; selected chips and the active
  nav pill wear the brand gradient inside a floating pill bar.
- **Editorial reading surface.** Home, Files, Tools, Settings and the reader chrome now share one
  printed-page grammar instead of separate card systems. New primitives in
  `designsystem/components/EditorialRules.kt` (`EditorialRule`, `EditorialMasthead`,
  `AppSectionHeader` with folios, `EditorialAction`) and `EditorialRow.kt` replace the surface,
  1dp-border and accent-overline cards. Section breaks are 2dp ink rules, dividers are 0.5dp
  hairlines, lists are ruled rows with no card, and the only filled shapes are ink chips.
- **Home.** The horizontal quick-action scroller is a fixed four-cell action band (Scan, Merge,
  Compress, Sign) that shows every action without scrolling. "Continue reading" carries progress as
  a 2dp accent rule; recent documents are ruled rows with format and size meta.
- **Files.** The storage card, category tile grid and volume cards are replaced by a single index:
  a tappable storage readout with a 2dp gauge, then numbered Collections, Devices, Pinned and
  Utilities rows. Empty categories read "Empty"; app counts are singularised. Removes
  `EditorialStorageOverviewCard`, `EditorialCategoryGrid` and `StorageVolumeCards`; file rows and
  grid tiles drop their cards for paper rows with an ink-stamp selection.
- **Tools.** The duplicated "Featured essentials" cards are gone; tools are a numbered index of
  ruled rows per group, with the category chips as the only filter.
- **Settings.** Sections are numbered folio headings (01–06) with hairline rows instead of cards;
  switches and progress gauges are ink, clearing actions are accent text links, and toggle
  subtitles were shortened so rows hold one or two lines on a phone width.
- **Reader chrome.** Top and bottom bars are paper-coloured ink chrome closed by hairlines; the
  title is `titleSmall` with a tabular `PAGE 01 / 02` folio, glyphs are outlined, the overflow menu
  uses the editorial menu shape, sheet tabs are rule-underlined entries, and the scrubber is ink
  with a floating folio above the thumb.
- **Tokens.** `AppColors.track` (ink at 16% alpha) is the unfilled gauge and scrubber track, because
  the hairline colour disappeared against the dark surface. `AppSpacing` now documents two strokes
  only (`hairline`, `rule`) and sets `listRowHeight` to 56dp.
- **Press physics.** `pressScaleEffect` is critically damped; nothing overshoots 1.0x.

### Verification

- `./gradlew :app:testDebugUnitTest` — 125 tests, 0 failures.
- `./gradlew :app:assembleDebug` — debug APK builds.
- `./gradlew :app:lintDebug` — 0 errors, 146 warnings, unchanged from 1.4.5.
- Visual pass on `mydoc_api35` in light and dark: Home, Files, Tools, Settings and the PDF reader.
  Confirmed the action band shows all four actions, storage gauges and the reader scrubber are
  visible in both themes, and the reader's folio, three-action footer and ink selection read
  correctly. AMOLED, rotation, larger font scales and motion remain unverified.
- Scanner checked on `mydoc_api35` in Document (empty and one page), ID Card and Full View: measured
  the shutter band at 2015–2274px (flush with the navigation bar), the viewfinder centre at 1231px
  against a camera-window centre of 1231px, and the commit stamp opening "Scan to PDF" with the
  captured page attached.

## [1.4.5] — 2026-11-01

Range: `v1.4.4..v1.4.5`.

### Fixed

- **Overlay windows followed the platform bar theme.** Bottom sheets and dialogs own a separate
  window, so the activity's edge-to-edge bar appearance never reached them: on a dark theme the
  sheet drew a light navigation bar. The new `OverlaySystemBars()` in
  `designsystem/components/OverlaySystemBars.kt` resolves the enclosing window through
  `DialogWindowProvider` and applies `AppTheme.isDark` to that window's controller. It is called
  from `AppBottomSheet`, `AppDialog`, and the 22 hosts that build `ModalBottomSheet`/`Dialog`/
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
  None of them are in the files added by this release.
- Visual pass on an API 35 emulator (`mydoc_api35`), light, dark and AMOLED: nine screens captured
  to `screenshots/` (`01-launch` … `09-quickactions-scrolled`). Overlay bars were confirmed at the
  window level with `dumpsys window`: the sheet's dialog window no longer reports
  `LIGHT_NAVIGATION_BAR` in dark theme, and still reports it in light theme where a white bar is
  correct. Chip fades were confirmed by sampling the strip edge pixels (a ramp, not a hard cut)
  before and after a swipe. The legend was confirmed to render every entry without scrolling.
  Layout and motion at other screen sizes, rotation and non-default font scales remain unverified.

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
- `./gradlew :app:lintDebug` — 0 errors, 146 warnings, all in issue classes unrelated to this
  work: `GradleDependency`/`AndroidGradlePluginVersion` (64+3), `UnusedResources` (19),
  `IconDuplicatesConfig` (10), `ObsoleteSdkInt` (7), `DefaultLocale` (4), plus one each of
  `ScopedStorage`, `SetJavaScriptEnabled`, `QueryPermissionsNeeded`, `PictureInPictureIssue` and
  `SourceLockedOrientationActivity`.
- No device or emulator was available: layout, motion and colour changes are unverified visually.
