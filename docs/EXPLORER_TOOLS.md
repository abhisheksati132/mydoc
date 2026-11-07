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

The opposing-arrows button beside the tabs opens Transfers. Paste opens it automatically. It shows the latest 20 transfers; at most one runs at a time. Clear finished removes history, never files. Progress counts selected files/folders, not bytes inside a folder. Stop preserves completed items and leaves failed/unattempted entries on the clipboard. Inspect both sides before retrying a partial move. App-session history is not persisted; transfers do not survive process termination.

## Validation (2026-11-06)

- 437 JVM tests passed, including archive safety, folder totals/limits, safe cancellation, partial transfer reporting, and closing the initiating tab.
- Five focused Android tests passed on API 35, including real copy byte preservation and a real filesystem symlink.
- Debug app/test APK assembly and Android Lint passed; pre-existing warnings remain.
- ZIP browsing and Back navigation, folder insights, and the transfer entry point were inspected on a disposable emulator. On a short landscape display, scrolling reached the archive extraction controls. No runtime crash was logged.
- Real-device throughput, app termination during a transfer, and very large directories still need release testing.
