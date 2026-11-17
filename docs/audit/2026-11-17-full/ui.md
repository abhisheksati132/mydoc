# UI, viewmodel, and design system audit

Target: MyDoc HEAD `6bf34ca6` (as supplied by the audit coordinator). Read-only review; no production files changed. Build and tests were not run.

## Findings

### [P1] Legacy Word thumbnail reads the entire file before applying its size limit

[OfficePreviewRenderer.kt](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/ui/files/OfficePreviewRenderer.kt:77) calls `InputStream.readBytes()` at line 78, then checks the 50 MiB limit at line 79. The limit therefore does not bound allocation. A large `.doc`/`.dot` selected for a thumbnail can allocate memory proportional to the full file and trigger `OutOfMemoryError`; the outer renderer catches `Exception`, which does not catch this `Error`. [FileThumbnail.kt](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/ui/files/FileThumbnail.kt:208) invokes this renderer as the native-preview fallback for file rows and library items, so displaying a folder/library containing a very large legacy Word file can crash the process. Check known file length before opening when possible and use a bounded stream read so unknown-length/content URIs also cannot bypass the cap.

### [P3] Search briefly shows results from the prior query

[SpotlightSearchSheet.kt](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/ui/search/SpotlightSearchSheet.kt:83) keeps `contentHits` while the query changes and waits 350 ms before replacing them (lines 88–101). During that debounce window, the list still displays the prior query's content hits, so the displayed results temporarily do not match the current query. Clear hits when a new query starts or associate each hit list with the query that produced it and suppress mismatched rows.

### [P2] Cleaner presents a success screen after trash movement fails

[StorageCleanerSheet.kt](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/ui/cleaner/StorageCleanerSheet.kt:751) handles a failed or partial `moveToTrash` by setting `moved` to an empty list (and recording only a partial count). It then always assigns a non-null `cleanedBytes` value, including `0L` on failure (lines 766–768). The UI branches on `cleanedBytes != null` and renders the “Moved to Trash” completion state (around line 223), with a check icon and zero bytes even when no files moved. Track whether items actually moved and show a failure/partial result state with accurate count and bytes.



## Review notes

The non-viewer production source inventory was read end to end, including tools UI and design system. The code-review findings above are based on static source inspection; no runtime verification was performed. The page-organizer cache concern was dropped after tracing its caller: the organizer is conditionally composed and its parent hides it on dismissal, so an input URI change while the same sheet remains open was not established as reachable.

## Coverage status

`ui.json` enumerates the non-viewer UI, tools UI, viewmodels, design system sources, and relevant unit/instrumentation tests. Counts: 107 full-file reads and 47 inventory-only entries. All enumerated non-viewer production sources were read in full; the one viewer production source in the map (`viewmodel/ViewerViewModel.kt`) is inventory-only and outside this audit's scope. Inventory-only entries are uninspected tests, including viewer tests. The map gives the exact per-path status. Builds and tests were not run.
