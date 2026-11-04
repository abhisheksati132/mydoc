# Changelog

All notable changes to MyDoc. Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
versions follow the `versionName` in `app/build.gradle.kts`.

## [1.7.0] — 2026-11-04

Range: `v1.6.1..v1.7.0`.

The "Ink & Seal" identity is now actually rendered, not just documented. The design-system audit
of v1.6.1 found that the brand type scale existed but never reached the screen; this release closes
that gap and the four other confirmed findings, plus the reader and library work that came with it.

### Fixed

- **Brand typography reaches the UI.** `AppTheme` never passed a `typography` argument to
  `MaterialTheme`, and `appTypographyFromMaterial()` mapped the brand roles onto the framework
  default — so ~130 `MaterialTheme.typography.*` call sites rendered Roboto while Fraunces, Inter
  and JetBrains Mono sat unused in `AppTypography`. Added `brandMaterialTypography()`, which
  expresses the brand tiers in Material 3's own roles, and supplied it to `MaterialTheme`. Screen
  titles and document names are now Fraunces, UI text Inter, and metadata (`p. 1/3 · 1.4 KB`)
  JetBrains Mono. Font scaling is untouched.
- **Bottom navigation touch targets and semantics.** Nav items were ~33dp tall and used
  `clickable(role = Role.Tab)` with no selected state. They now use `selectable` with a 48dp
  minimum applied outside the padding, so the clickable bounds themselves grow rather than just
  the visual pill. Measured on device: 48dp tall, and the active tab reports `selected = true`.
- **Library overflow menu shown where it belongs.** The 3-dot menu (view toggle, hidden files,
  new folder/file, recycle bin, refresh) appeared on the Library landing page, where every item
  targets a folder that is not open. It now appears only while browsing internal storage.
- **Tools and Library top bars align.** The Tools screen applied the status-bar inset twice — once
  on its root column and again by passing zero insets to the shared bar — so its title sat lower
  than Library's. Both are now identical.
- **File picker metadata.** The first-run picker accepted `*/*` but labelled every result
  `application/pdf`, 0 bytes, DOCUMENT. It now resolves the real MIME type, display name, size,
  timestamp and media kind from the picked URI, so icons, badges and viewer routing stay honest.

### Added

- **Office document thumbnails.** Word, Excel and Slide tiles in Recents and Library were flat
  "WORD"/"PPT" plates. They now run through the same converter the viewer uses and show the real
  first page, falling back to the plate when a file cannot be converted. Conversions are capped at
  two in flight so a grid of documents cannot stampede the parsers on first paint. Legacy binary
  `.ppt`/`.xls` keep plates — there is no converter for them.
- **Swipe between documents.** The viewer's pager only ever walked pages inside one file. A fling
  past the first or last page now steps to the neighbouring document in the same folder, across
  PDF, Word, Excel, Slide, EPUB and text. Mid-document swipes still turn pages, and tab swiping
  stays on the outer screens. The gesture observer never consumes events, so it cannot steal a
  page turn.
- **Markdown rendering.** `.md` files opened as raw monospace with visible `#` and `**` markers.
  The text viewer now renders headings with a real type scale, bold/italic/code/strikethrough
  spans, bullets and ordered lists, block quotes and rules, with search highlighting preserved.
- **Home brand lockup.** The header was a text-only wordmark. The Seal — a rounded square whose
  negative space forms an M with a folded-paper corner, as specified in `docs/brand-identity.md`
  §4 — now sits beside it. It is drawn rather than a drawable, because the app's theme is chosen
  independently of system night mode, so it tracks Seal teal on paper and lifted teal on graphite.

### Changed

- **Stale brand language removed.** Comments and the design-system preview still described Stamp
  Vermilion, ink navy and "Paper under glass", and the preview's accent swatch printed `#D94324`
  for a color that is `#0E5C57`. Corrected across seven files.
- **Glass gradients are now an explicit exception.** The brand rule said "no gradients anywhere"
  while `GlassSurface` uses short vertical gradients for the specular rim and inner shadow. The
  rule in `docs/brand-identity.md` now carves out optical edge lighting specifically, instead of
  the docs and the code contradicting each other.

### Known gaps

- The launcher and splash still use the older document-sheet mark; only the in-app lockup uses the
  Seal M. Re-cutting the launcher icon is deliberately out of scope here.
- Not yet verified on device: update-banner overlap with the last list item, PDF reader chrome
  density, photo-viewer contrast over bright images, and glass cost on low-RAM hardware.

## [1.6.1] — 2026-11-04

Range: `v1.6.0..v1.6.1`.

### Fixed

- **Video player controls legible in light mode.** The player's top and bottom control bars used the theme-adaptive `GlassSurface`, which falls back to a warm near-white plate in light mode — so the white icons and text vanished and the bars clashed with the black video. They now use a fixed dark scrim, so controls are always visible and the player looks identical in light and dark.
- **Settings top bar artifact.** The Settings top bar rendered as a glass plate with a specular rim and elevation shadow, which in dark mode read as a stray "box with a line" over the black background. It now uses the same flat, transparent top bar as Library.
- **Tools top bar consistency.** The Tools screen used a bespoke header (different title style and icon tint from the rest of the app). It now uses the shared `AppTopBar`, so Tools, Library and Home share one top-navigation treatment.

## [1.6.0] — 2026-11-04

Range: `v1.5.19..v1.6.0`.

### Added

- **Brand identity — "Ink & Seal".** A documented, ownable design system (`docs/brand-identity.md`): positioning (the private, local-first, every-format suite that never phones home), a quiet-luxury personality, voice rules, and hard anti-slop guardrails (no gradients/aurora/glow, one accent, one typeface pair, color never the only signal).

### Changed

- **New color system.** Replaced the generic Apple system palette with a warm, restrained "Ink & Seal" scheme across `AppColors` and the Material 3 schemes in `AppTheme`: warm bone canvas (`#F5F3EE`) / warm graphite dark, near-black label text, one **Seal-teal** accent (`#0E5C57` / `#4FB0A6`) and a single **brass** micro-accent (`#A67C3D`) reserved for the seal and premium touches. Format/media colors are desaturated to warm letterpress tones so the UI reads calm, not a crayon box.
- **New brand mark + launcher/splash icon.** Replaced the generic blue gradient stacked-paper icon with an ownable vector seal — a Seal-teal rounded square holding a cream folded-corner document with two text rules — wired into the adaptive launcher icon, the cold-start splash, and the window background. Verified in light, dark and on the splash.
- **Wordmark.** Home header is now a clean Fraunces "MyDoc" (the trailing accent-period cliché removed).
- **Brand is the default theme.** Material You wallpaper theming now defaults **off** (it overrides the brand accent); it remains an opt-in toggle in Settings → Appearance.

### Assets

- `assets/brand/mydoc-seal.png` (raster mark), `assets/brand/launch-banner.html` + `launch-banner.png` (1200×630 launch banner in the identity).

## [1.5.19] — 2026-11-04

Range: `v1.5.18..v1.5.19`.

### Changed

- **iOS system color palette.** Replaced the Tailwind/editorial hexes with Apple's actual system colors across the design tokens (`AppColors`, `AppTheme` light/dark/AMOLED schemes, `DocumentFormatColors`, media-kind tokens) and the file-type badge tints: systemBlue accent (`#007AFF` / `#0A84FF`), systemRed/Orange/Green/Indigo/Purple/Pink/Teal/Mint/Brown/Gray for formats, and neutral iOS chrome (grouped `#F2F2F7` canvas, `#1C1C1E`/`#2C2C2E` dark surfaces, label/secondaryLabel text). The result is the restrained "color comes from content, chrome is neutral" Apple liquid-glass look rather than a saturated editorial palette. Verified on the emulator in light and dark.

## [1.5.18] — 2026-11-04

Range: `v1.5.17..v1.5.18`.

Full-codebase bug audit follow-up. Fixes are grouped by the area they affect.

### Fixed

- **Legacy `.ppt`/OLE2 parsing.** The FAT sector filter compared against `0xFFFFFFFD.toInt()` (which is `-3` in Kotlin), so no sector was ever registered and every legacy PowerPoint stream decoded to garbage. Corrected the signed comparison.
- **XLSX shared strings.** Text is now accumulated per `<si>` (joining rich-text runs) instead of per `<t>`, which had shifted every later cell's referenced string and shown wrong text throughout a sheet.
- **PDF "Compress" corrupt output.** A failed MuPDF save no longer leaves a truncated file eligible as the returned "compressed" result.
- **Clear cache no longer wipes offline backups.** "Thumbnail & render cache" stopped deleting `filesDir/saved_docs`, the copies that keep revoked-grant documents readable.
- **Bulk-trash Undo.** The trashed set is captured and restored from that list, so UNDO after a bulk move no longer silently does nothing.
- **Extracted-APK path traversal.** `versionName` (attacker-controlled APK metadata) is now sanitized before being used in the output filename.
- **Backup privacy.** The Room database (`mydoc.db`) and pinned-folders prefs are excluded from cloud backup and device transfer, matching the app's stated privacy intent.
- **PDF dual-page navigation.** Search, jump, bookmark, outline and volume-key navigation now target the dual-page list (by pair index) instead of the never-composed pager.
- **Presentation pinch-zoom.** The gesture detector no longer restarts mid-pinch (was keyed on the value it mutates).
- **Main-thread rendering / IO.** Scrollbar live thumbnails, OCR/TTS page renders, spreadsheet loading and viewer state writes moved off the UI thread; viewer progress now persists under the active (re-picked) document URI.
- **Redact safety.** Redact requires a non-blank query instead of silently redacting the hardcoded default term.
- **Offline-cache correctness.** `saved_docs` lookup matches the full `${docKey}_` prefix (no cross-document hash-prefix collisions), and the backup copy is written via temp+rename so the source is never truncated mid-copy.
- **Recycle Bin safety.** Same-batch trash names are made unique (no overwrite / shared-path loss), the expired-trash purge runs in a cancellable caller scope, and freshly-trashed files are mtime-stamped so reconciliation cannot delete them as old orphans.
- **XLSX writer / renderer robustness.** Malformed worksheet parsing no longer throws `StringIndexOutOfBounds`; empty-range host no longer throws on `lastKey()`; XLSX→PDF page count is capped (OOM guard); Excel number formatting uses `Locale.US` symbols.
- **PPTX zip-bomb guard.** The aggregate decompressed-size limit is now actually enforced.
- **MuPDF zoom tiles.** Region rendering crops the requested tile region instead of squashing the whole page into it.
- **Misc.** Text-viewer input stream is closed (no descriptor leak), search close cancels the in-flight query, the updater only stamps its 24h throttle on a successful fetch, TypeScript `.ts` no longer classifies as video, and the watermark size clamp no longer crashes on sub-100pt pages.

## [1.5.17] — 2026-11-04

Range: `v1.5.16..v1.5.17`.

### Added

- **Skeleton first-paint states.** The Home feed and Library browse now render static tonal placeholder blocks while their data loads, instead of a blank pane. Deliberately shimmer-free: a pulsing skeleton would reintroduce exactly the motion some users asked the system to remove.
- **Reduced-motion respected across glass.** A new `LocalReducedMotion` reads the system "remove animations" setting (transition/animator scale = 0) and observes live changes via a `ContentObserver`. When on, scroll- and touch-driven glass stays at its base thickness (blur pinned to 12dp, tint at rest) and the nav-bar interaction spring is skipped, so chrome never animates against the user's accessibility preference.
- **Design-system preview skeletons.** Section 6 of the debug-only Paper-under-Glass preview renders both skeleton layouts for review; opened by long-pressing the About & updates card in Settings.

## [1.5.16] — 2026-11-04

Range: `v1.5.15..v1.5.16`.

### Changed

- **True Liquid Glass optics.** Rebuilt `GlassSurface` on the real Apple glass anatomy: transparent Haze base so blurred content supplies the color (vibrancy), low-alpha tint (0.42–0.55) instead of an 80% opaque plate, fine grain (noiseFactor 0.04), a directional top-arc specular edge replacing the uniform all-around rim, and the inner shadow moved under content at 8dp. Light mode now casts accent-tinted shadows.
- **Continuous-curvature corners.** New `ContinuousCornerShape` (superellipse, exponent 4.5) for glass chrome and sheets so curvature ramps in like iOS instead of snapping at a circular arc.
- **Live blur on every screen.** Haze sources now feed the navbar from the Home feed, Library browse/directory lists and grid, the Tools list, and the PDF viewer page column (top and bottom viewer bars included); previously only the Home feed could blur.
- **Scroll-reactive glass.** `GlassMotionState` thickens the navbar glass as content scrolls: blur ramps 12→28dp and tint densifies over the first 240px of scroll.
- **Color moved into content.** File-type inks now lift to tonal variants in dark themes; modal sheets use a translucent (94%) plate since cross-window blur is impossible.
- **Vibrancy on live glass.** A luminance-preserving saturation lift (1.18x) is applied to the blurred layer so content color reads louder through the glass instead of muted, approximating Apple's vibrancy pass.
- **Spring-settled nav glass.** Tab changes kick a damped spring through `GlassMotionState.interaction`, thickening the navbar blur momentarily before it relaxes, so glass responds to touch as well as scroll.
- **Tertiary support accent.** Storage capacity bars and the App Manager "Split APK" badge now use `tertiary`/`tertiaryContainer` semantic containers: one primary accent for action, one support accent for resource metrics.

### Removed

- **Rainbow layer deleted.** `VividGradients.kt` (7 unused Tailwind-default gradients) and `VividComponents.kt` removed; section headers fall back to a neutral editorial rule.

## [1.5.15] — 2026-11-03

Range: `v1.5.14..v1.5.15`.

### Added

- **AppDropdownMenu with Apple Liquid Glass.** Replaced standard flat opaque menus with translucent frosted glass dropdown surfaces, 16dp rounded card shapes, and 1dp luminous specular rims across all screens.
- **Home Feed Error State Banner.** Added real-time error banner presentation in `HomeFeedPane` when document loading or recents flow encounters failures.
- **Active TileBitmapCache Global Trim Registry.** Registered all active `TileBitmapCache` instances to automatically purge on system memory trim (`ComponentCallbacks2`).

### Fixed

- **MuPDF Permissions Integer Bitmask.** Computed ISO 32000-1 user permissions bitmask as a proper signed integer string (`permissions=$permMask`), fixing the parse failure in MuPDF's `fz_lookup_option_integer` so print and copy restrictions are genuinely applied.
- **App Manager "Split APK" Badge Squeezing.** Moved the "Split APK" badge to a dedicated row below the version label with `maxLines = 1` and `softWrap = false`, eliminating horizontal compression that caused character-by-character vertical text wrapping.
- **App Manager & Action Sheet Navigation Bar Overlap.** Configured `skipPartiallyExpanded = true`, full-height container filling, 45% scrim, and generous bottom content padding (`96dp`) so sheets cleanly sit above system bars and never show background content below.
- **Single-Item Action Sheet Glass Styling.** Added tactile drag handle, translucent glass surface, IME padding, and consistent scrim overlay to `SingleItemActionBottomSheet`.
- **Page Reorder Touch Targets.** Expanded all reorder buttons (Move Left, Delete, Move Right) to the standard minimum 48dp (`AppSpacing.minTouchTarget`), eliminating fat-finger deletion risks.
- **PrintHelper Toast Elimination.** Wired `onMessage` snackbar callbacks to all `PrintHelper.printDocument` callers in `ReadingControlsBottomSheet` and `PresentationViewerScreen`.

### Removed

- **Pruned 8 Tautological Unit Tests.** Eliminated mock tests performing trivial arithmetic on local variables (`assertEquals(35, 35)`, `255 - 255 == 0`, and simulated loops) from the E2E quality gate suite.

## [1.5.14] — 2026-11-03

Range: `v1.5.13..v1.5.14`.

### Added

- **Material You Dynamic Themed Icon.** Added `<monochrome>` adaptive icon vector layer so MyDoc dynamically tints with system wallpaper colors on Android 13+.
- **User-Facing PDF Security Permissions.** Wired `allowPrinting` and `allowCopying` toggle controls into Protect PDF tool UI and MuPDF permission strings.
- **Watermark Angle Controls.** Added interactive rotation chips (-45° diagonal, 0° horizontal, +45°, 90° vertical) in Add Watermark tool.
- **Storage Failure Recovery.** Added proactive error state cards in Library browse pane when storage analysis or volume inspection fails.
- **Memory Pressure Trimming.** Integrated `onTrimMemory` hooks across `TileBitmapCache` and `MainActivity` to purge and resize bitmap caches under OS memory pressure.

### Fixed

- **Eliminated Fake Redaction & Sanitization.** `redactPdf` and `stripMetadata` now strictly throw on engine failure instead of copying unredacted files or claiming metadata was purged.
- **Edge-to-Edge Keyboard Blindness.** Added `android:windowSoftInputMode="adjustResize"` and `.imePadding()` across `AppBottomSheet`, `ToolRunnerSheet`, and `SpotlightSearchSheet` so input fields and action buttons never hide behind the software keyboard.
- **Card Roof/Floor Disappearance & Liquid Glass Overhaul.** Replaced fading directional gradient borders with continuous, luminous 1dp bounding rims across light, dark, and AMOLED modes.
- **Vibrant Modern Palette.** Replaced muddy ivory/brown tones with radiant slate-white `#F8FAFC`, deep sapphire `#0066FF`, and vivid semantic badge colors.
- **Splash Screen Parity.** Splash screen animated icon now points directly to `@mipmap/ic_launcher` instead of legacy line art.
- **Library Tab Divider Inconsistency.** Removed unwanted horizontal border divider from Library header to match Home and Tools headers.
- **Touch Targets & Text Clipping.** Expanded page reorder buttons and search clear buttons to standard touch targets (≥40-48dp); fixed "Landscap" truncation with `TextOverflow.Ellipsis`.

### Removed

- **Dead Code Sweep.** Pruned 19 unused files and assets, including 10 dead mipmap PNGs, uncalled `AudioExtractor`, legacy splash XMLs, unreferenced `AppChip`, and unused color/typography tokens.

## [1.5.13] — 2026-11-03

Range: `v1.5.12..v1.5.13`.

### Added

- **Adaptive Concept 2 App Launcher Icon.** Complete high-resolution adaptive launcher icon pack installed across all density buckets (`mipmap-mdpi` to `mipmap-xxxhdpi`) and `mipmap-anydpi-v26`.
- **E2E Opaque-Box Test Suite.** Added 64 automated tests covering data safety, storage cleaner protection, office document parsing, PDF tools correctness, viewer lifecycle, and release quality gates.

### Fixed

- **Storage Cleaner safety.** Protected standard user and system directories (`Download`, `DCIM`, `Documents`, `Pictures`, `Music`, `Movies`, `Android`) against deletion; active browser downloads (`.crdownload`, `.part`) are strictly excluded from temp and duplicate scans.
- **Physical Trash purge.** Automatic 30-day purge now atomically unlinks physical files from `.trash` in addition to removing database records.
- **Zip Slip and directory traversal.** Canonical path checks enforced on all zip extractions and storage operations.
- **Spreadsheet memory footprint.** Spreadsheet viewer refactored to use virtualized viewport windowing, preventing OutOfMemoryError on large sheets.
- **Office parsing heap exhaustion.** DOCX, PPTX, and XLSX parsers now use streaming XML parsing instead of contiguous in-memory byte buffers.
- **PDF tools truthfulness & fallback.** "Redact", "Watermark", "Repair", and "Crop" now feature honest UI guidance, dual-engine fallback (MuPDF + PDFBox), and strict keyword/geometry validation.

### Removed

- **Dead assets.** Pruned unused font `bricolage_grotesque.ttf` (408 KB) and legacy duplicate night-specific launcher resources.

## [1.5.12] — 2026-11-03

Range: `v1.5.11..v1.5.12`.

### Fixed

- **CI could not start.** The `gradle/actions/setup-gradle` pin and the
  `softprops/action-gh-release` pin pointed at commits that do not exist, so every workflow failed at
  the Set up job step. Both now resolve to real tagged commits (v4.4.4 and v2.2.1).
- **No instrumented tests in CI.** A second job boots an API 35 emulator with KVM and runs
  `connectedDebugAndroidTest`, uploading the report.
- **A rejected update no longer crashes the app.** A certificate mismatch in the manual Install
  action threw `SecurityException` on the main thread; both Install entry points now surface it as an
  error state.
- **Debug builds offered an update they could not install.** Update checks are skipped in debug
  builds, whose debug signing key can never accept a release APK.
- **Legacy Word formats claimed full support.** `.doc`, `.rtf`, `.wpd` and similar fall back to
  extracted text; the reader now says so instead of presenting it as formatted output.

### Changed

- The zoom-tile cache object was renamed `ZoomTileCache` to end the name collision with the
  page-render cache in `rendering.cache`.

### Verified

- Every bundled native library is 16 KB aligned on both x86_64 and arm64 (ELF `PT_LOAD` alignment
  `0x4000`), and the APKs pass `zipalign -c -P 16`.

## [1.5.11] — 2026-11-03

Range: `v1.5.10..v1.5.11`.

### Removed

- **`:office-engine` module.** 2,042 Java files and ~472k LOC that no app code referenced, kept in the
  release by its own consumer ProGuard rule (~4.5 MB of dex). Its PDF path was disabled and several
  bundled files carried no licence grant. Removed with its Gradle wiring.
- **Dead UI and safety code:** `ZipSafeReader` (never called), `AppTabHeader`, unused Vivid components,
  `DocumentFilterChips`, `EditorialMasthead`, `EditorialRow`, `PaperSurface`, `PaperTheme`,
  `RecentsPane`, the duplicate `ui/common/AppDivider` and the unused `ui/common/EmptyState`.
- **Unused dependencies:** `zxing-core` (no QR feature exists) and `haze-materials`.
- **Placebo settings.** "Hardware Video Acceleration" and "High-Quality Vector PDF" stored a
  preference that no renderer or player read; both controls and their dead state are gone. Eleven
  further preference keys that nothing consumed (granted folder, scan scope, confirm-before-delete,
  photo export quality) were deleted.

### Fixed

- **Tool results are saved with their real type.** Save, Open and Share now use the output file's
  actual extension and MIME instead of the tool's static metadata. A multi-part split is saved as a
  ZIP (not a corrupt `.pdf`) and a stitched long image as JPG/PNG (not a `.zip`).
- **Split honours a typed page range.** The range field previously only applied when the document
  produced a single part; it now exports exactly the selected pages as one PDF.
- **No false success.** The engine rejects empty outputs, image-to-PDF fails when no selected file
  decodes, and Annotate/Redact report an error when the search text is not found instead of returning
  the input unchanged.
- **PDF to Word is labelled honestly.** The tool no longer claims Excel/PowerPoint output it never
  produced, and the file picker offers images for image-to-PDF and text types for text-to-PDF.
- **Glass surfaces are legible.** The no-blur fallback is now opaque instead of 94–96 % alpha, so
  stacked sheets no longer ghost through each other and cards remain visible on AMOLED; the sheet
  scrim and shadows follow the theme.
- **Bottom chrome no longer overlaps.** Snackbars (including the Files UNDO) and the Files FAB clear
  the floating navigation pill; the PDF contextual bar no longer applies the status-bar inset twice.
- **Accessibility and large-font layout:** file action sheets scroll, dialog buttons use the theme
  contrast colour, fixed-height headers grow with font scale, and the remaining sub-48 dp touch
  targets were raised.
- **Release-only hardening:** `DesignSystemPreviewScreen` is reachable only in debug builds; file
  names from external intents are sanitized before writing to cache.
- **CI action pins restored.** The release and build workflows use full commit SHAs again and
  `build.yml` declares read-only permissions.
- **Settings now change what they promise.** PDF page scaling drives the live reader fit preference;
  Auto-Resume and Auto-Loop video are honoured by the player; the haptic toggle silences app and
  viewer feedback through a shared gate.
- **The updater ignores the debug version suffix.** `1.5.11-debug` no longer parses as `1.5`, which
  had made a 1.5.11 debug install show "Update available: v1.5.9" and offer a downgrade. A regression
  test covers suffixed and plain versions.

### Changed

- `SECURITY.md` now documents the ML Kit metrics transport, the app-lock fail-open path, the actual
  backup exclusions and the requested permissions instead of overstating protection.

## [1.5.10] — 2026-11-03

Range: `v1.5.9..v1.5.10`.

### Changed

- **Version catch-up.** Releases 1.5.4 through 1.5.9 shipped without changelog entries; their
  sections are restored below from the commit history.

### Removed

- **Five tools whose behaviour was narrower than their labels.**
  - *Office to PDF* had no working path into the tool engine: `OfficeToPdfConverter` was a standalone
    DOCX/XLSX/PPTX text-and-table re-layout that only its own unit test called, and the bundled
    `:office-engine` renderer was never wired to it.
  - *PDF/A export* stamped a conformance marker into document metadata without validating the output
    against any PDF/A profile.
  - *Fill form* handled AcroForm text, checkbox and choice fields only; XFA forms and field-level
    validation were unsupported.
  - *Compare* diffed extracted text word sets per page, so image-only PDFs compared as empty and
    layout changes were invisible.
  - *OCR to PDF* wrote searchable text, but as invisible lines at approximate positions instead of
    per-word coordinates, so selection and copy drifted from the printed text.

  The five are gone from the tool catalog, engine dispatch and tests. The Tools screen now reports
  24 tools, and non-PDF documents only show tools that accept them.
- **Dead page-rotation controls in the visual page organizer.** The rotate buttons changed preview
  state only; `Apply & Save` sent page indices without rotation, so the rotation was silently
  discarded. Move left, move right, remove and apply now match what the engine executes.

### Fixed

- **Protect PDF requires a password.** An empty user password produced an unopenable file with no
  error, and raw passwords were interpolated into MuPDF's comma-separated option string. Passwords
  are now validated (non-blank, no comma or control characters) in both the UI and the engine.
- **No more `file://` URIs leaving the app.** Share, Open with and Set-as-wallpaper now resolve
  through the app's FileProvider (with a cache copy fallback), closing the
  `FileUriExposedException` crash and the broken share targets on API 24+.
- **Privacy documentation matches the code.** The README, SECURITY and architecture docs now state
  that document processing is on-device while the updater checks GitHub and Play Services may fetch
  the ML Kit model, and that Android backup copies the saved-document store and Room database.
- **Security documentation matches the code.** `SECURITY.md` no longer claims Apache POI conversion,
  a live Office→PDF pipeline, or that `ZipSafeReader` guards every archive. It now names the limits
  each OOXML parser actually applies and states that the shared guard is not yet wired.
- **Integration tests can run again.** Room schemas are exposed to `androidTest` assets, the
  kotlinx-serialization runtime is aligned with Room 2.8.5's schema parser, the migration test passes
  `MIGRATION_3_5`, and two stale viewer-benchmark assertions match the current middle-truncated
  title and 1-based page counter. All 8 instrumented tests pass on `mydoc_api35`.

## [1.5.9] — 2026-11-03

Range: `v1.5.8..v1.5.9`.

### Changed

- **Liquid glass reached the remaining surfaces.** Cards, bottom sheets, sheet headers, toolbars,
  the tools grid, file lists, storage cleaner, app manager, trash bin, photo info and presentation
  chrome now render through `GlassSurface`; `AppBottomSheet` uses a transparent container so the
  sheet content carries the glass.
- Version 1.5.9 (159).

### Known issues

- Sheets receive no `hazeState`, so the glass is a translucent plate rather than a blurred backdrop,
  and stacked sheets show the sheet beneath them.

## [1.5.8] — 2026-11-03

Range: `v1.5.7..v1.5.8`.

### Changed

- **App icon:** Concept 2, "Prismatic Document Prism", adopted as the official icon across all
  densities. Version 1.5.8 (158).

## [1.5.7] — 2026-11-03

Range: `v1.5.6..v1.5.7`.

### Changed

- **New launcher identity:** the "Paper under glass" icon replaces the four-pill launcher icon in
  every density. Version 1.5.7 (157).

## [1.5.6] — 2026-11-03

Range: `v1.5.5..v1.5.6`.

### Added

- Horizontal swipe between the Home and Files tabs.
- Document-type filtering in the library.
- Swipe navigation and reworked zoom in the photo viewer; updated video player controls.

### Changed

- Glass treatment extended to dialogs, the clipboard bar, file panes and spotlight search; the dead
  bottom-nav code path was removed. Version 1.5.6 (156).

## [1.5.5] — 2026-11-03

Range: `v1.5.4..v1.5.5`.

### Fixed

- Startup crash caused by invalid `HazeStyle`/`LocalHazeStyle` background colours; ML Kit kept by
  ProGuard so OCR still works in release builds. Version 1.5.5 (155).

## [1.5.4] — 2026-11-03

Range: `v1.5.3..v1.5.4`.

### Added

- **"Paper under glass" design system:** colour, typography and radius tokens, Fraunces and JetBrains
  Mono, and the `GlassSurface` component.
- Home feed with a hero card and recent grid, plus floating glass bottom navigation.
- Unified library screen with the redundant browse tabs removed.
- Viewer chrome: condensing glass top bar, glass bottom bar, search overlay and thumbnail strip.
- Tools reorganized into six task groups; settings glass toggle and storage-tools relocation.
- Scroll benchmark, `ViewerViewModel`/`UpdateManager` unit tests and a Room migration test skeleton.
- **`:office-engine`** packaged as an internal Gradle module (native Office renderer, 2,042 Java
  files). It is not referenced by any app code and is not wired to any tool.

### Fixed

- Hardening pass across document parsers: bounded reads, ZIP entry limits, typed `Result` for file
  operations, `CancellationException` rethrown, empty catches removed.
- Biometric guard, navigation insets, theme selector and accessibility follow-ups; debug
  `applicationIdSuffix`; CI signing injection and test-report upload.
- Test isolation: ViewModels created and cleared inside their test scopes.

## [1.5.3] — 2026-11-02

Range: `v1.5.2..v1.5.3`.

### Changed

- **Build toolchain migration:** Upgraded Android Gradle Plugin to 9.5.0, Gradle wrapper to 9.6.0, Kotlin Gradle Plugin to 2.3.21, KSP to 2.3.13, and Room to 2.8.5 with built-in Kotlin DSL.
- **compileSdk raised to 37:** Updated to compileSdk 37 to support modern AndroidX 2026 releases while keeping runtime targetSdk at 35.
- **Dependency modernization:** Bumped 20 dependencies across AndroidX, Compose BOM (2026.09.00), CameraX (1.6.2), Exifinterface (1.4.2), Media3 (1.11.1), Coroutines (1.11.0), and Testing libraries.
- **MuPDF updated to 1.28.5:** Upgraded `com.artifex.mupdf:fitz` from 1.24.4 to 1.28.5, ensuring native libraries are 16 KB page-size aligned (`Aligned16KB`) for Android 15+.
- **Hero transition animation:** Migrated deprecated `ResizeMode.ScaleToBounds` to `ResizeMode.RemeasureToBounds` for Compose 1.12.

### Fixed

- **MuPDF search API deprecation:** Updated search invocations in `PdfEditingEngine` and `PdfSecurityEngine` to use `Page.search(...)` resolving Fitz 1.28 symbol removals.
- **Thread and locale lint hygiene:** Resolved 4 Lint 32 checks across `SignPdfSheet` (`ThreadConstraint`), `FileManagerDialogs` (`NonObservableLocale`), and `SettingsScreen` (`LocalContextGetResourceValueCall`).

## [1.5.2] — 2026-11-02

Range: `v1.5.1..v1.5.2`.

### Added

- **`AppTopBar`, one top-bar wrapper.** Files, Settings, spotlight search and the viewer header now
  come from a single component: title and subtitle slots, action slot, container colour and bar
  insets are declared once instead of re-cut per screen.
- **`AppMessages` snackbar bus.** `Context.showMessage(text)` replaced all 54 `Toast.makeText`
  calls across 19 files, rendered by one `AppSnackbarHost` at the activity root. Toasts ignored the
  app theme, landed wherever the system put them and could not be dismissed; the host renders them
  themed, in position and dismissible. Screens that need an action (trash undo) keep a local host.
- **A designed splash.** `androidx.core:core-splashscreen` with a purpose-built document mark on a
  plate matched to the window background, declared in both the compat and the API 31+ platform
  attribute sets. The system splash previously showed the four-pill launcher icon. The mark is held
  until the first Compose frame with a 1200 ms deadline, so a stalled first frame cannot trap the
  user on the splash.
- **`UnlabeledTapTarget` lint rule** (`lint-rules/`, wired through `lintChecks`) reports
  `IconButton`, FAB and clickable surfaces whose `contentDescription` is null or missing, so an
  unlabeled control can no longer be merged.
- **`AppRadius.inset` (12dp) and `DocumentReadingType`.** The radius scale now steps cleanly
  8/12/16/20/24/28 with a named tier for elements nested inside a card, and the reader's document
  text sizes (heading levels, body, table cells) live in the token layer rather than in the views.
- **`LibraryItemTest`** covers the identity key the search list dedupes on.
- **First instrumented UI tests.** Five Compose tests in a new `androidTest` source set, aimed at the
  things that broke silently rather than at easy coverage: the search list dedupes a duplicate
  identity to one row and its count label agrees, a query narrows list and count together, tapping a
  result reports that item, `AppTopBar` renders title, subtitle and a working back action after the
  `modifier` reorder, and the `AppMessages` bus reaches the snackbar host. All five pass on
  `mydoc_api35`.

### Changed

- **Spotlight search is a routed screen.** It was a `Dialog` with `usePlatformDefaultWidth = false`
  plus its own system-bar overlay — a whole screen dressed as a modal. It now shares the activity's
  navigation, so back, insets and the header behave like every other destination. Trash, camera
  capture and the cropper stay dialogs: they are modal tasks, not places.
- **One back handler instead of seven.** The mutually guarded `BackHandler` stack collapsed into a
  single ordered pop over the active-screen state.
- **`ui/filemanager` is `ui/files`.** The file-manager cluster moved in with its owner.
- **Every literal corner radius and font size in the app layer is a token.** 45
  `RoundedCornerShape(<n>.dp)` and 14 `.sp` literals were folded onto `AppRadius` and the type
  scale. Off-scale radii snapped to the nearest step (10/14 → 12, 18 → 16, 22 → 20), the 2dp tab
  indicator's 1dp radius became `roundShape` (clamping makes them identical), and thumbnails take
  one radius from the scale instead of threading a `cornerRadius: Int` through five composables.
- **Dead resources removed.** 20 unused strings, the unreferenced `drawable/ic_launcher_foreground.xml`,
  and ten night-mode icon PNGs that were byte-identical to their day twins. The night
  `ic_launcher.png` and `ic_launcher_round.png` differ from the day copies, so they were checked by
  blob hash and kept.
- **Six version checks below `minSdk 26` deleted** — the M/O branches in haptics, audio playback
  parameters, the installer helper, `StaticLayout` creation and the PiP gate. `mipmap-anydpi-v26`
  became `mipmap-anydpi` for the same reason.
- **Test dependencies moved into the version catalog**, which also aligned `kotlinx-coroutines-test`
  with the runtime it tests (it was pinned to 1.8.1 against a 1.10.1 runtime).
- **`kotlinOptions` became `compilerOptions`** in `app/build.gradle.kts`, and 18 composables reordered
  `modifier` to the first optional parameter.
- **Boxed state replaced with typed state** in the three places that held an `Int`/`Long` in a plain
  `mutableStateOf` (search match index in both readers, cache size in Settings), which was allocating
  a wrapper per update.

### Fixed

- **Search could list one document twice, and its header count disagreed with the list.** Results
  are deduped by identity and the count reads the same collection the list renders, so the two can
  no longer diverge. The list key moved to the identity as well, which keeps Compose state attached
  to the right row.
- **The typed-signature placeholder resized the moment you typed.** The empty state was 20sp over a
  32sp input; placeholder and field now share one `displaySmall` italic style.
- **Archive entry names were locale-dependent.** `String.format` without a locale used the device
  default, so on Arabic, Persian or Thai locales `%03d` wrote non-ASCII digits into zip entries
  (`page_٠٠٧.png`) inside the archives the split and image-export tools produce. Five sites now pin
  `Locale.ROOT` for machine identifiers and `Locale.US` for the one displayed number, which also
  stops "Compression saved only 12,3%" on German and French locales. A JVM probe of `%02x` across
  seven locales came back ASCII everywhere, so the four hex-digest call sites are deliberately
  unchanged.
- **Three ViewModels held an Activity context.** `MainActivity` passed itself straight into
  `StorageViewModel.Factory`, and the viewer and spreadsheet factories forwarded
  `LocalContext.current`. All three ViewModels now take `Application`, resolved through
  `applicationContext` in the factory, so a rotation cannot pin a dead Activity for the life of the
  ViewModel.
- **The text and Word readers recomposed the whole viewer on every scroll frame.** Both read
  `firstVisibleItemIndex` in composition — once as a `LaunchedEffect` key, once as the header's page
  number. The index now comes from a `derivedStateOf`, and reading progress is persisted through
  `snapshotFlow` instead of a composition-time read.
- **PDF text extraction re-parsed the whole document for every page.** `AndroidPdfRenderer` called
  `PDDocument.load(file)` inside `getPageText`, `getDocumentText`, `search` and `getOutline`, so
  reading text across N pages cost N full parses. One document is now opened per renderer and closed
  with it.
- **Edited Office documents reopened the stale PDF.** The conversion cache key was
  `MD5(uri + fileName)`, so saving a document under the same name kept serving the PDF rendered from
  the previous version. The key now carries the source's size and modification time, falling back to
  the old behaviour when a provider reports neither.
- **Picture-in-picture entered without a source rect or auto-enter.** The player's measured bounds
  are now passed to `setSourceRectHint`, and `setAutoEnterEnabled(true)` is set on API 31+ — the two
  calls lint asks for at `targetSdk 31` and above.
- **The signature plate was a stadium, not paper.** A 200dp-tall box clipped with `roundShape` clamps
  to a full pill; it now uses `cardShape`, matching every other surface in the sheet.

## [1.5.1] — 2026-11-02

Range: `v1.5.0..v1.5.1`.

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
