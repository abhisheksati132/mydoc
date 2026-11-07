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
