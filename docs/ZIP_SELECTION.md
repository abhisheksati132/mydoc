# Selected ZIP entries

## Scope

Tap a file in the ZIP browser to open a temporary private-cache copy. Check files or folders, keep selections across archive navigation, and confirm Extract selected to create a new uniquely named folder beside the ZIP. Folder selection includes descendants and empty folders. Preserve relative paths, Unicode filenames and extensions. No overwrite or merge into existing folders. Existing Extract all remains available when nothing is selected.

Validate all archive paths/duplicates/tree ambiguity before writes using the existing index. Recheck ZIP metadata while processing; stream selected entries only, with actual-byte limits and CRC/size verification. Reject encrypted/unsupported/damaged archives with an actionable error. Limit extraction to 512 MiB, previews to 128 MiB and retained previews to 256 MiB. A high expansion ratio is rejected after 5 MiB. Check cancellation between buffers. Stage under a private name, then publish without replacing existing entries. On failure remove only created entries; report any remaining recovery folder. No extracted archive link becomes a filesystem symlink.

Preview files live under the existing FileProvider shared cache root. They are temporary and may be cleared by Android or Clear cache. Do not automatically delete active previews; refuse more previews when their cache budget is full and suggest extracting instead. No new permission, dependency, or database schema. Previous saved-search/folder-view work remains in the workspace.

## Acceptance and checks

Tests verify selecting one file avoids others, selecting folders preserves structure, opening preserves bytes/extension, unsafe paths and size limits fail before publication, checksum failures/cancellation clean staging, and name collisions keep existing bytes. Android UI tests cover selecting entries and confirmation, plus opening a real member into private cache. Use isolated cache fixtures only.

Commands: `./gradlew.bat testDebugUnitTest --tests "*ExplorerZipSelectionTest"`; `./gradlew.bat testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest`.

## Use and limits

Open a ZIP from Library. Tap file names to open temporary copies. Check a folder to include its descendants; inherited checkmarks are disabled until that parent selection is cleared. Use Clear selection to start again. Selection survives navigation within the sheet, but is not retained when the sheet is recreated. Confirm Extract selected to write a new folder beside the ZIP; the completion message names it. An empty selection keeps Extract all available.

The ZIP itself is unchanged. Opening nested ZIP files returns them to the archive browser. Cache previews retain filenames/extensions and can be read through the existing content provider. They are not permanent saved documents: Android and Clear cache may remove them. New previews are refused at the cache limit; extraction remains available. ZIP change detection uses file size and modification time, not a whole-archive cryptographic snapshot.

## Validation (2026-11-07)

463 JVM tests passed, including eight selected-ZIP tests for file/folder selection, byte and cache limits, CRC failure, cancellation cleanup, stale selection and preserving previous output. Three focused Android tests passed on API 35: selection confirmation without writes, temporary member opening via a readable content URI, and real selected extraction. Debug and test APK builds and Android Lint passed; warnings remain. Full-app selection and opening a ZIP text member in the normal reader were visually checked in disposable emulator data. Large real-device archives and interrupted-process recovery still need release testing. Previous saved-search/folder-view changes remain uncommitted alongside this feature.
