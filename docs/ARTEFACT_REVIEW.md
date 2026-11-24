# Learning from Artefact

Reviewed Artefact commit `d003a6a66cdc7bcb395dfb8e9390f90a119acc4c` against MyDoc `0e940829`.
Evidence: repository source, design documentation and four published screenshots. Artefact was
not installed or benchmarked; screenshots demonstrate its published design, not every current state.

## What is worth learning

| Area | Evidence in Artefact | MyDoc decision |
| --- | --- | --- |
| Content first | `preview/QuickPreview.kt` presents pages with a direct route into the full viewer. | Improve our existing preview, which currently shows a thumbnail and details. |
| Clear action hierarchy | `ui/ToolScreens.kt` shares tool-page structure and a primary action. | Keep a consistent final action and reveal options near the relevant task. Our image-PDF editor already follows this direction. |
| Coherent glass | `ui/glass/AdaptiveGlass.kt` samples the backdrop and changes ink with hysteresis. | Keep our existing adaptive reader ink and reduced-motion support; measure readability and frame cost before adding more glass. |
| Recognisable identity | Published home/tools screens use colourful desk cards and expressive type. | Keep MyDoc's warm paper, graphite and teal. Improve content hierarchy without adopting the desk-card styling. |
| Editing depth | README and the engine guide describe spreadsheet and Word editing. | Treat editing as a separate project requiring save-copy, undo, recovery and round-trip fixtures. Viewing fidelity remains essential. |

Artefact's guide describes a Rust engine boundary; MyDoc already has native PDF rendering and
an optional LibreOffice companion for Office fidelity. A rewrite is not justified by a feature
list or screenshots. Compare real reference files, memory, opening time and output before changing
the engine. Neither repository's README proves superior document fidelity.

## First implementation: useful Quick Preview

Assumptions: improve the existing explorer preview; preserve the full reader as the place for
search, annotations and password entry; add no dependency or permission; use MyDoc's own components.

Acceptance criteria:

- PDF previews let the user move forward and back without opening the reader or changing files.
- Display the complete rendered page and current/total page counts; prevent navigation beyond bounds.
- Render only the selected page, off the main thread, within a 1600-pixel longest-edge budget.
- Close native document resources after each render, including errors and cancellation.
- A protected or damaged PDF shows a clear route to Open instead of an endless loader.
- Image previews fit the entire image; explorer list/grid thumbnails retain their existing crop.
- Present content before file details, including on short screens with large text.
- Open, Share, More actions, selectable file details and closing the sheet continue to work.
- Verify portrait, wide layout, large text and existing RTL behavior.

Implementation: `ui/files/FileQuickPreviewSheet.kt`, a focused PDF-preview component and the existing
thumbnail component. Verify with `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`,
`:app:assembleDebugAndroidTest` and focused `FileQuickPreviewOnDeviceTest` instrumentation.

## Next, in order

1. External-file preview: an optional, short-lived preview entry from other apps. Specify URI grant
   lifetime, sensitive files, Back behavior and full-reader handoff before adding an exported activity.
2. Contextual tools: choose a document and see only applicable actions, preserving discoverability
   of the complete tool catalogue.
3. Editing: start with a recoverable spreadsheet edit-and-save-copy flow, with formula and formatting
   round-trip tests; follow with Word editing after layout fidelity and save behavior are proven.

These are follow-up recommendations, not shipped features. Notes, shopping lists and voice memos
are not part of this pass: they would broaden the product without directly improving file work.

Sources: [Artefact repository](https://github.com/Chethan616/Artefact/tree/d003a6a66cdc7bcb395dfb8e9390f90a119acc4c),
[design guide](https://github.com/Chethan616/Artefact/blob/d003a6a66cdc7bcb395dfb8e9390f90a119acc4c/DESIGN.md),
[engine guide](https://github.com/Chethan616/Artefact/blob/d003a6a66cdc7bcb395dfb8e9390f90a119acc4c/CODE.md).
No Artefact code or assets are incorporated in this implementation.

## Implemented and checked

The first slice above is implemented. Eleven focused API 35 tests pass, including page pixels,
navigation, damaged/protected files and restoration. Extra dark-mode and wide-screen/200%-text
navigation checks pass; screenshots were inspected. The 665-test unit suite has zero failures and
three existing skips. Lint and debug/instrumentation APK builds pass. Physical-phone performance and
spoken TalkBack testing remain unverified.
