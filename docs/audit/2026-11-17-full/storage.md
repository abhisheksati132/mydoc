# Storage audit — 2026-11-17

**Target:** `6bf34ca6feee75f4fcfb9f1fe5afc189f1abec38` (HEAD; working tree was clean at start)

**Scope:** All 81 production files under `data`, `discovery`, `domain`, `update`, and `util`, plus `MainActivity`, `MyDocApp`, `CrashLog`, `LauncherShortcuts`, widget and tile code. Every scoped production file was read completely; larger files were read in chunks of at most 100 lines. The coverage JSON also inventories 36 related test files, marking only the updater state test as read. This was a read-only review. No tests or builds were run.

## Confirmed findings

### [P1] Cross-volume folder move can delete files outside the selected tree through a directory link

[StorageRepositoryImpl.kt:450](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/data/repository/StorageRepositoryImpl.kt:450), [StorageRepositoryImpl.kt:458](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/data/repository/StorageRepositoryImpl.kt:458), [StorageRepositoryImpl.kt:604](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/data/repository/StorageRepositoryImpl.kt:604)

Trigger: a selected directory contains a symlink to another directory, and `renameTo` fails (as expected across volumes), taking the copy-then-delete fallback. The copy uses Kotlin `File.copyRecursively`, which traverses with `walkTopDown`; `deleteSourceEntries` then enumerates descendants and calls `File.delete()` on each child path. Those paths resolve through the directory link, so the move removes the external target's contents, not just the link. The permanent-delete helper at lines 285–302 instead uses `Files.walkFileTree` without `FOLLOW_LINKS`, demonstrating the safer traversal available in the same class.

**Validation:** Ran Kotlin stdlib 2.4.21 `File.walkTopDown()` in a temporary audit directory containing a Windows directory junction to an outside folder. The walk yielded `external-link\\sentinel.txt`; deleting that child path returned true and the outside sentinel no longer existed. Direct symbolic-link creation was denied by Windows privilege policy, so the local reproduction used a directory junction; Android impact depends on whether the selected volume/provider permits directory symlinks. No project files were involved.

**Fix:** Use no-follow-link traversal for copy, verification, snapshot, and source deletion; treat links as leaf entries and preserve or recreate them rather than traversing targets. Add regression coverage with a directory link to an external sentinel tree and force the rename fallback.

### [P2] Failed ZIP extraction leaves earlier replaced files changed

[StorageRepositoryImpl.kt:877](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/data/repository/StorageRepositoryImpl.kt:877), [StorageRepositoryImpl.kt:915](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/data/repository/StorageRepositoryImpl.kt:915), [StorageRepositoryImpl.kt:933](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/data/repository/StorageRepositoryImpl.kt:933)

When the caller chooses `ZipCollisionStrategy.REPLACE`, an existing file is overwritten as each archive entry is processed. If a later entry fails (for example, an out-of-destination path or extraction limit), failure cleanup deletes only paths in `createdDuringRun`; the replaced file is not added because `targetWasNew` is false. The function returns failure while leaving earlier existing content replaced.

**Fix:** Stage replacements and retain prior files until the entire extraction succeeds, or back up and restore replaced entries during rollback. Add a test with an existing file, REPLACE strategy, a valid first entry, and an invalid later entry; assert the original bytes remain.

### [P2] Widget rows can open a different document after recents change

[ContinueReadingWidget.kt:61](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/widget/ContinueReadingWidget.kt:61), [ContinueReadingWidget.kt:78](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/widget/ContinueReadingWidget.kt:78), [MainActivity.kt:714](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/MainActivity.kt:714), [MainActivity.kt:764](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/MainActivity.kt:764)

The widget renders titles from a recent-documents snapshot, then creates each row's PendingIntent with only its list index. On tap, `MainActivity` queries the current recents list and opens the document at that index. If recents reorder between widget refresh and tap, the row label and opened document diverge; if the list shrinks, the tap becomes a no-op.

**Fix:** Put a stable document identity in the row intent and resolve that identity at tap time, while retaining the saved-copy fallback. A refresh on recents changes helps but cannot eliminate the stale-widget interval.

### [P2] Continue-reading widget does not refresh when page progress changes

[DocumentRepositoryImpl.kt:397](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/data/repository/DocumentRepositoryImpl.kt:397), [ContinueReadingWidget.kt:55](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/widget/ContinueReadingWidget.kt:55), [MainActivity.kt:195](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/MainActivity.kt:195)

The widget displays `lastPage`, but `updatePageProgress` only writes to Room. The only refresh calls are from activity creation and opening/registering a document; progress updates while the reader remains open do not repaint the widget. The displayed page therefore stays stale until a later activity/document refresh.

**Fix:** Refresh the widget after a persisted progress update, or observe the recent-progress flow through a widget update mechanism.

### [P2] Folder indexing has no directory traversal cap

[DocumentIndexer.kt:21](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/discovery/DocumentIndexer.kt:21), [DocumentIndexer.kt:25](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/discovery/DocumentIndexer.kt:25), [DocumentIndexer.kt:27](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/discovery/DocumentIndexer.kt:27)

Selecting a folder caps indexed files at 1,000 and depth at 12, but the limit check only counts files. A selected tree with very many empty directories can therefore enqueue and query all of them; each `children()` call also materializes that directory's full child list. The traversal can consume unbounded time and memory despite the advertised selection cap.

**Fix:** Count visited nodes/directories as well as files and stop at a fixed entry budget; use bounded or paged provider queries where available.

### [P2] User-facing error helpers log raw exception details

[UserMessages.kt:24](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/util/UserMessages.kt:24), [UserMessages.kt:47](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/util/UserMessages.kt:47), [SecureLog.kt:62](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/util/SecureLog.kt:62)

Both read and storage-action failures call `Log.w` with the raw `Throwable`. Android logs its message and stack trace, which can include a document URI or filesystem path. This bypasses `SecureLog`, whose release behavior strips throwable details because they can contain user paths.

**Fix:** Route these through `SecureLog.warn` with a constant summary and private details in the redacted detail lambda.

### [P2, impact limited to cache behavior] Search indexing deletes a shared Office conversion cache entry

[DocumentIndexer.kt:66](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/discovery/DocumentIndexer.kt:66), [DocumentIndexer.kt:102](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/discovery/DocumentIndexer.kt:102), [NativeOfficeConverter.kt:41](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/rendering/engine/NativeOfficeConverter.kt:41)

When indexing a saved Office copy, `NativeOfficeConverter.convert` returns the content-addressed file under `cacheDir/converted_docs` on its normal unprotected-document path. `indexSavedCopy` stores that returned file in `converted`, then unconditionally deletes it in `finally`. This defeats cache reuse and causes the next conversion of the same unchanged document to invoke the native engine again. If a viewer is concurrently using the same cached path, deleting it may affect later path-based reads; that exact device impact is filesystem/session dependent and is not established here.

**Fix:** Only delete conversion outputs whose `OfficeConversion` result explicitly sets `deleteWhenClosed` (private password-render outputs). Leave content-addressed cache files to the cache policy.

## Coverage and verification

`storage-coverage.json` records all 81 scoped production files as fully read and inventories 36 related test files. `UpdateManagerStateTest` was read for the updater dismiss and active-file race paths; other test files are inventoried only. Existing storage tests include permanent-trash symlink safety but do not cover the cross-volume move fallback. No widget-specific test was found. No tests or builds were run.
