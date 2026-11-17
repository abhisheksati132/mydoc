# Reader and rendering audit (read-only)

**Revision:** `6bf34ca6feee75f4fcfb9f1fe5afc189f1abec38` (`main`)
**Scope:** `app/src/main/java/.../rendering`, `app/src/main/java/.../ui/viewer`, directly connected viewer models, `office-engine/src`, and associated unit/device tests. No production files changed; no build or tests run.

## Findings

### [P3] PDF bookmark indicator carries over when the screen is reused for another document

`PdfViewerScreen.kt:182-185` initializes `bookmarks` with `remember` and does not key it by `document.uri`. `DocumentViewerScreen` and its view model support switching the document in an existing reader session; when Compose reuses this screen for a different URI, the bookmark icon is initially calculated from the previous file's page set. The first bookmark toggle refreshes the set, but until then the displayed state is wrong. Key the remembered value to the URI or observe the settings value for the active URI.

### [P2] ODS repeated rows above 500 are silently omitted

`SpreadsheetParser.kt:414-424` expands a non-empty repeated ODS row only up to `minOf(curRepeatRows, 500)`, but advances `rowIndex` by the full `curRepeatRows`. Rows after the first 500 copies therefore disappear from the grid, and this parser does not set `SheetData.truncated` for that cap. A workbook with a repeated data row count greater than 500 is shown with missing rows and no partial-content warning. Either preserve the full bounded expansion, or mark the sheet as truncated when the expansion cap is reached.

The previously noted global zoom-cache concern does not establish a defect: `ViewerViewModel.releaseSession()` clears the global zoom tile cache, while `ZoomTileCache.clear()` only evicts entries and does not recycle bitmaps still held by a page composable. `RendererSlot` closes displaced renderers, and the view model closes a renderer when a load generation becomes stale.

### [P2] Nested PowerPoint group transforms update the outer group instead of the current group

`PptxParser.kt:1583-1586` pushes each `<p:grpSp>` transform onto `java.util.ArrayDeque` with `push()` (adds at the front), but lines 1655-1690 read or update the transform with `removeLast()` and `addLast()`. For nested groups this changes the outer transform while leaving the inner transform at zero. `resolveElementBounds()` then applies the stack at lines 2212-2224, so shapes in nested groups are positioned/scaled using the wrong transform chain. Use stack-consistent `pop()/push()` or `removeFirst()/addFirst()` for the active transform.

### [P2] File-based PPTX parsing can silently return a partial deck at package limits

`PptxParser.kt:523-539` reads archive entries only while `entryCount < 2_500` and `currentTotal < MAX_TOTAL_BYTES`, then passes the entries collected so far to `parseFromEntryMap()` without recording why the loop stopped. When slide XML entries follow the entry or aggregate-byte limit, the parser returns the slides found so far as a normal presentation (`932-935`) with no `incompleteReason`. Users can therefore see a shortened deck without a partial-content indication. Track whether either limit stopped enumeration and attach the incomplete reason, as the in-memory path does.
## Paths examined closely

- Viewer lifecycle: `DocumentViewerScreen`, `ViewerViewModel`, `SpreadsheetViewModel`, `RendererSlot`, `ZoomTileCache`.
- Native Office path: `NativeOfficeConverter`, `OfficeEngineManager`, `WordEngineService`, `NativeWordCore`, `WordEngineGuard`, plus Office bridge sources.
- Password handling: Office password retry and private preview output; PDF password authentication and encrypted-PDF fallback decision.
- Search and saved geometry: viewer search job cancellation/result publication; page progress restoration; page dimensions and zoom tile cache key construction.

## Coverage limits

This is a source review only; no builds, tests, or device scenarios were run. The coverage JSON marks a path `full` only when all its source lines were read in bounded excerpts during this pass. Follow-up full reads are recorded in readers.json and viewer-extra.json; test reads are recorded in reader-tests.json. FILE_COVERAGE.csv merges these passes. The current defects are confirmed directly from the cited control flow. Associated tests are not represented as proof of runtime behavior. `DocumentIndexer` cache deletion is outside this reader-focused report and was assigned elsewhere.

