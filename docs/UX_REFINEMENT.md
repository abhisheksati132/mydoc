# Core UX refinement

Scope: address the 1.9.1 interface review while preserving Ink & Seal, existing file operations, reader routing, search capabilities and user preferences. No new renderer or dependency upgrade.

1. Fix persistent search naming and large-text reflow, with focused device regressions.
2. Make recent files compact by default, retain a preview option, and preserve selection, share, details and removal.
3. Put useful locations before storage statistics; summarize capacity once; keep storage insights and cleaning accessible.
4. Show explicit indexed-library versus folder search scopes and expose advanced filters next to the query. Do not claim an index covers every file.
5. Separate workspace commands from folder/view commands; make per-file overflow names specific.
6. Put settings behind a short category index, preserving all settings, update checks and destructive confirmations.
7. Let tool descriptions wrap and replace misleading taxonomy and clipboard instructions.

Acceptance: search retains its accessible name while typing; the explanation text does not truncate at 2x text; Home headings/actions and metadata reflow at 320 dp; recent-row and preview actions continue to work; every settings category can be entered and backed out of; scope labels match the underlying search; copy actions name the current destination; no loss of transfers, tabs, saved searches, sort/reset or update checking.

Verification: focused Compose device tests, light/dark and 2x text emulator checks, existing unit tests, build and lint. Physical-phone frame timing remains outside this environment's verification.

## Completed behavior

Home defaults to compact recent rows. Preview mode remains available and uses a single column at larger font scales. Reading resumes only from the actual continue list, instead of labeling an arbitrary recent item as in progress. Context menus preserve details, sharing and removing from recents; long press retains selection.

Library shows a search entry, pinned locations, internal/external storage and file categories. Capacity is summarized once, with insights and cleaning still available. At larger text sizes category tiles become a single column.

Indexed-library search and folder search use FileSearchField, with a persistent accessible name, clear action and Search keyboard action. Index coverage is stated explicitly. Folder/category/recursive scopes are named separately, advanced search is next to the query, and empty filtered index results offer a reset. Workspace commands and folder controls are separate menus; Settings remains available in the workspace menu while browsing.

Settings has a six-entry index. Each category retains its existing controls and independent saved scroll position. Back first returns to the index, then leaves Settings. Appearance, reading and startup choices wrap; explanations are not restricted to two lines. All update, cache/history confirmation and app-lock behavior remains wired to the original handlers.

## Verification

- 466 unit tests passed; debug build and lint passed with no lint errors.
- 28 device tests passed on the API 35 GPU-backed emulator: five new core-UX regressions plus existing chrome, dock, search, explorer, preference, comparison, tool and ZIP checks.
- Manually opened all six Settings categories and returned using Back. Confirmed update checking remains visible under About.
- Reviewed light and dark Home, Library, Tools, folder menus, indexed/folder search and clipboard placement. Clipboard was cancelled without pasting during the visual check.
- Reviewed Home, Search and Settings at native 2x text. Focused component tests exercise 320 dp layouts. The final decorative thumbnail fallback was additionally checked at native 2x text after the functional device run; the final build and lint include that change.
- No new dependencies, renderer upgrades or preference migrations. Physical-phone frame time, full TalkBack traversal and a hardware-keyboard walkthrough remain unverified.

## Everyday browsing pass — 2026-11-07

Scope: the folder/category browse → loading → results/empty → clear-filters flow, plus file-row and grid readability. This pass does not establish quality of every reader, media screen or file operation.

- While loading, show a named static placeholder state instead of exposing previous-location rows or claiming the folder is empty. Existing navigation remains available; the loading label uses a polite accessibility live region.
- Empty query, document-type and advanced-search results offer Clear search and filters. Clear ordinary filters in place; clearing an advanced search returns to its folder. Unit tests verify that the query/type filter is cleared and the location remains correct.
- Empty-state content can scroll in a short viewport. Filenames use two lines; size/date metadata can wrap fully, and absent dates do not leave a dangling separator.
- Grid minimum cell width scales with text size. Large-text previews use a wider aspect ratio to avoid enormous square previews in a single column.

Verification: loading and metadata regressions reproduced before fixes. Final debug/test APK builds, 487 unit tests and lint passed (zero lint errors). Sixteen focused API 35 tests passed, including five new browse checks, large-text/RTL layout controls, list/grid restoration and existing core UX. Actual unmatched-search recovery and 2x text list/grid rendering checked on the emulator. Logs and screenshots are ignored local artifacts under `app/build/experience-*`. Physical-phone performance and a full TalkBack/keyboard walkthrough remain unverified.

## File actions pass — 2026-11-07

- Grid cells expose the same file-specific More actions name as list rows. The 48 dp control uses a stable surface over previews and is replaced by the checked-state indicator during selection. Its click does not open the file.
- Selection has direct labelled Copy, Move and Share buttons in a wrapping action group. Close, Select all and overflow remain in the count row; Rename, compression and the destructive bin action remain in the overflow. Existing deletion confirmation is retained.
- Single-item Copy/Move uses an explicit target rather than toggling the selection and then copying/cutting. Shared clipboard preparation retains the existing batch messages and clears selection only after acceptance. A rejected request during transfer leaves the clipboard and selection untouched and reports Transfer in progress.
- No file is moved merely by choosing Move: the existing destination-specific Paste step and transfer handling remain in effect.

Verification: missing grid actions and direct selection actions reproduced before implementation. Final debug/test builds, 490 unit tests and lint passed (zero errors). Twenty focused device tests passed, including large-text direct actions, independent grid overflow clicks, selection checkboxes, Rename access and prior browse/layout/restoration cases. Unit tests cover selected-target copy, unrelated-selection move and clipboard rejection during a gated transfer. The real grid action sheet, one-item copy, destination-labelled clipboard, cancellation and selection controls at normal/2x system text were inspected on the API 35 emulator. Physical-device timing and an exhaustive keyboard/TalkBack walkthrough remain unverified.
