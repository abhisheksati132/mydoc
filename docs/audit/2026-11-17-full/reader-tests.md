# Reader and viewer test review

**Scope:** The 41 rendering, reader, viewer, and office-engine test files that were not marked `full` in `FILE_COVERAGE.csv`. Each file was read completely. `reader-tests.json` lists these paths with `coverage: full`. This is a source review; no tests were run.

## Regression coverage gaps for confirmed findings

- **ODS repeated-row truncation (SpreadsheetParser.kt:414-424):** `SpreadsheetFormatsTest.kt` covers repeated ODS cells and a two-row repeat, then checks the following row's position. It does not cover a repeated non-empty row count above 500, the silent truncation threshold, or the `SheetData.truncated` indication. Add a case with more than 500 repeats and assert both retained rows and the partial-content signal.
- **PPTX nested group transforms (PptxParser.kt:1583-1690):** `PptxParserTest.kt` covers a simple synthetic PPTX, stream/file route equivalence, ODP, malformed slides, and fallback formats. No test builds nested `<p:grpSp>` groups with distinct transforms and checks the final child bounds. Add nested and single-level geometry assertions so stack order is pinned.
- **PPTX archive enumeration limits (PptxParser.kt:523-539):** `PptxParserTest.kt` has no case that crosses the file-based 2,500-entry or 200 MB decompressed package limits, then checks that returned pages disclose truncation. Add a compressed package with a required slide after a limit boundary and assert a non-null `incompleteReason`.
- **PDF bookmark reuse across URI (PdfViewerScreen.kt:182-185):** The viewer and model tests exercise page, search, settings, lifecycle, and geometry behavior, but no Compose test switches the active URI in the same composition and verifies that the bookmark indicator reloads for that URI. Add a reuse test with different bookmark sets for two documents.

## Coverage observed

The suite includes real-file device checks for Office conversion fidelity, protected document handling and cleanup, PDF search/render concurrency, regional tile rendering, Word semantics, accessibility of page controls, plus unit coverage for parser errors, persistence, progress, formatting, zoom, search, cancellation, and resource lifecycle. These tests were inspected as source only; their presence does not establish that they pass on the current revision.
