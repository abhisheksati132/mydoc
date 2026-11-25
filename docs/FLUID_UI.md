# Fluid Ink & Seal

Design contract for the Artefact-inspired UI refinement. Implement with MyDoc's own
components, assets and typography; no reference-app code or assets are imported.

## Screen jobs

- Home: resume one document, then find recent work. A large real cover is the focal point.
- Tools: recognize the main tasks immediately, then browse or search the full catalogue.
- Reader: keep the page dominant, with reachable, labelled controls in floating groups.
- Preview: inspect a file and expand into reading without losing its identity.
- Image to PDF: keep the image large, edits reversible, and export consistently reachable.

## Shared rules

- Clear glass for compact navigation; frosted glass for menus; dense material for panels.
- Warm paper, graphite and seal teal remain the default identity. Category tones are accents.
- Menus grow from their anchor and return there. Touch feedback never delays actions or scroll.
- Expressive motion is local to controls; Calm uses a restrained response. System reduced
  motion overrides both. No idle animation or moving document text.
- Keep 48dp touch targets, focus/back/outside dismissal, opaque contrast/power fallbacks,
  and layouts that scroll or stack with large type and narrow windows.
- Use real covers and functional actions. Preserve file-list density and document geometry.

## Delivery and validation

1. Shared materials, anchored menus, persisted glass/motion preferences.
2. Home and Tools hierarchy, compact reader groups and navigation feedback.
3. Preview handoff and editor controls, then emulator screenshots and interaction checks.

Validate menu edge placement and RTL, preference persistence, reduced motion, large type,
preview navigation and editor actions. Run unit tests, lint and a debug build. Emulator
visual checks do not establish physical-device frame rates or battery performance.

## Implemented

- Anchored menu unfolding and folding, with edge/RTL placement, keyboard traversal,
  back dismissal, and input blocked during the closing animation.
- Persisted glass finishes and Calm/Expressive control motion. Contrast, hardware,
  power and system reduced-motion policy remain authoritative.
- A larger Home cover, visual recents, an anchored Open/Scan menu and illustrated
  action cards shared with featured tools. Large text stacks the Home actions.
- Floating reader groups, bookmark actions in overflow, adaptive text clearance,
  scrollable large-text actions, and shared-element motion disabled under reduced motion.
- Expandable image/PDF previews in the navigation host, with shared content bounds
  into the reader. Selected PDF pages carry through; Back and recreation restore the
  preview. More returns to the same file's actions in the explorer.
- Consistent file badges, selection and paste panels, search result rows and Settings
  categories. Search options stay tucked away until requested; narrow windows and
  large text stack paste actions below their destination.
- A larger image editing preview, grouped controls and a landscape panel, before/after
  rotation/filter comparison, reset and intentional edit haptics. Crop output is preserved.

## Verification — 2026-11-24

- Full JVM suite: 668 tests, no failures or errors.
- Debug build and Android-test APK build passed. Lint: no errors; 187 warnings and one hint.
- API 35 emulator: 39 distinct targeted tests passed, including live glass in both themes,
  persisted preferences, menu selection/back/reopen/reduced motion, Home/tool actions,
  double-size text, RTL, preview expansion/restoration, image edits/export recovery,
  PDF page changes while zoomed, document panning and adaptive reader ink.
- A deliberate RTL-offset mutation failed the placement regression test; source restored.
- Captures use synthetic test content, stay under the ignored build directory, and are
  not replacements for marketing images. Physical-device performance remains unmeasured.

- Full-app launch verification also exposed a host graphics-driver stall in the emulator
  (`glCreateProgram_enc`); the published 2.0.18 APK timed out there too. After stopping
  the build daemon and restarting the emulator process without loading its snapshot,
  three cold launches and native menu interaction succeeded, without another ANR.
  A Home-with-live-backdrop integration test also passed. This does not establish a
  physical-device startup benchmark or attribute the driver stall to the new UI.

## Verification — 2026-11-25

- API 35 emulator: all 54 targeted tests passed on the final UI implementation.
- Full JVM suite: 668 tests passed. Debug and Android-test builds passed.
- Lint: no errors, 187 warnings and two hints; existing warnings remain visible.
- Actual-activity checks cover selected PDF page handoff, Back, recreation and More
  returning to the same file's actions. A forced page-zero mutation failed the
  handoff test as expected; the source was restored and the checks passed again.
- Large-text and RTL checks cover Files, Search, Settings, selection and paste
  actions. Active transfers disable Paste and expose Details. Format metadata has
  paragraph-direction and glyph-position regression checks.
- This pass uses the API 35 emulator as requested. Physical-device frame rate,
  battery use and startup performance remain unmeasured.
