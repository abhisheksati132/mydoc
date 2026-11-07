# Explorer tools

## Capability map and build order

| Module | User outcome | Dependencies |
|---|---|---|
| ZIP browser | Navigate archive folders and inspect file sizes before extracting | Existing extraction |
| Folder insights | See total bytes, largest files, and immediate subfolder sizes | Read-only local scan |
| Transfer center | View copy/move progress and history; stop after the current item | Existing copy/move repository |

Build order: ZIP browser, folder insights, transfer center, integration checks.

## Scope and acceptance

- ZIP metadata is loaded off the main thread, without extracting bytes. Implicit folders work; unsafe paths, duplicate entries, ambiguous trees and excessive entry counts are rejected. Browse/back/search stay inside the archive. Extraction requires confirmation and uses existing guarded extraction. Other archive formats keep their existing actions.
- Insights scan the selected folder, count hidden files, skip symlinks, stay within the canonical root and cap traversal at 100,000 entries. Show unreadable/skipped or truncated data as incomplete. Show the largest 20 files and immediate child folder totals. Scans cancel when dismissed.
- Transfers share one center across tabs. Progress counts selected top-level items, with one running transfer at a time. Cancellation finishes the current item before stopping; completed items remain. Failed or unattempted clipboard items remain available. History is in memory (latest 20); operations survive closing a child tab and screen rotation, but are not a background service or process-death queue. Errors and partial completion are visible. No automatic retries of moves.
- Keep Material 3 and existing theme, accessible controls, string resources, no new permission, dependency or database schema. Do not change unrelated tools or publish without a new request.

## Implementation and tests

Kotlin engines in data/local, plain result models in domain/model, Compose sheets in ui/files, integration in FileManagerViewModel/ExplorerWorkspace. Reuse existing repository mutations.

Use suspend IO work and explicit cancellation handling. Tests use JUnit temporary folders, injected transfer behavior and disposable emulator app data. Cover archive path abuse and implicit folders; insight totals, limits and symlinks; transfer success, partial failure, safe cancellation and shared ownership.

Commands: `./gradlew.bat testDebugUnitTest assembleDebug lintDebug`; focused tests use `--tests "*ExplorerTools*Test"`; Android tests are filtered to the new feature class.

## Using the tools

Tap a ZIP in Library to browse it. Tap folders to enter them, use Up or Back to return, and filter names in the current archive folder. Extract all asks for confirmation. The browser reads ZIP metadata. Tap a file to open a temporary copy, or select entries and confirm Extract selected to create a new folder beside the ZIP. Encrypted/RAR/7z browsing is unsupported.

Inside a storage folder, open the menu and choose Folder insights. Tap a subfolder to browse it. The scan is a snapshot: concurrent changes can alter totals. Logical bytes exclude filesystem allocation and linked files; hidden files are included.

Open Tabs and workspace → Transfers. Paste opens it automatically. It shows the latest 20 transfers; at most one runs at a time. Clear finished removes history, never files. Progress counts selected files/folders, not bytes inside a folder. Stop preserves completed items and leaves failed/unattempted entries on the clipboard. Inspect both sides before retrying a partial move. App-session history is not persisted; transfers do not survive process termination.

## Validation (2026-11-06)

- 437 JVM tests passed, including archive safety, folder totals/limits, safe cancellation, partial transfer reporting, and closing the initiating tab.
- Five focused Android tests passed on API 35, including real copy byte preservation and a real filesystem symlink.
- Debug app/test APK assembly and Android Lint passed; pre-existing warnings remain.
- ZIP browsing and Back navigation, folder insights, and the transfer entry point were inspected on a disposable emulator. On a short landscape display, scrolling reached the archive extraction controls. No runtime crash was logged.
- Real-device throughput, app termination during a transfer, and very large directories still need release testing.

## Transfer experience refinement — 2026-11-07

Transfers use individual status cards with explicit operation, status, item counts and destination. The progress bar measures processed top-level items and appears only while running/stopping; failed and stopped terminal results do not display a misleading full bar. Successful results omit the irrelevant zero-failure count. Stopped results explain reviewing source and destination before continuing. Existing detailed failure and partial-move guidance remains visible; retries are still deliberate.

Open destination folder dismisses the sheet and navigates the active phone tab or focused wide-screen pane. The folder availability check runs on IO before replacing the current view. An unavailable folder leaves the current view and selection intact and explains reconnecting storage or choosing another folder. A newer navigation or folder-opening request supersedes the pending check; saved-search and transfer-destination navigation share the same checked-folder mechanism.

The transfer engine, item-boundary cancellation, latest-20 in-memory history, shared clipboard ownership and repository file operations are unchanged. Clear finished removes only history. This is not background or resumable transfer support.

Validation: final app/test APK builds, 495 unit tests and lint passed (zero errors). Nineteen focused API 35 tests passed: terminal progress/status recovery, a narrow 2x-text destination action, stopping behavior, existing archive/insight/real-copy checks, file actions and core UX. A disposable file was copied through the real app, checked byte-for-byte with the original, and reopened from history after navigating away. Removing that test destination produced the recovery message while preserving the current view. Fixtures were removed. Tablet pane routing is implemented but was not run on a tablet; physical-device throughput and exhaustive TalkBack/keyboard traversal remain unverified. Logs/screenshots are ignored artifacts under `app/build/transfers-*`.
