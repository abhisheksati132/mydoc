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
