# Search, collections and review (1.9.7)

Open **Search → Contents, tags & smart collections**. Choose files or a folder using Android's picker. MyDoc does not crawl storage without this choice. SAF read grants are retained where supported; a revoked grant, missing file or unavailable provider is shown during refresh/open. **Relink** reconnects a file chosen at a new location while preserving its tags. **Remove from index** never deletes its original.

The private SQLite FTS4 index stores page text and metadata on device, excluding URI/page-number fields from token indexing. Results include a snippet and open the original at the matching rendered page. Queries use quoted prefix tokens, not raw FTS operators. Filenames are also searched. Each document contributes one matching page before the 200-document content-result cap, so a long book cannot hide every other document.

## Scope and limits

- Up to 1,000 selected files. Folder traversal stops at 200 files or 12 directory levels; the completion message reports the limit. Add more folders/batches explicitly.
- Source documents are limited to 16 MiB, including providers with unknown/misreported sizes. Text reads stop at 200,000 characters. Page-based formats stop at 200 pages / 200,000 characters and report partial coverage.
- PDF, EPUB, FB2 and XPS use the existing reader; supported Word/Excel/PowerPoint formats use offline LibreOffice page conversion. For Excel, only rendered print areas and visible sheets are indexed, not hidden cells or cells outside those print areas. Plain text, Markdown, CSV/TSV, logs, JSON/XML and HTML use bounded text reads. HTML is indexed as source text, not executed.
- Password-protected documents are not text-indexed. Scanned pages without text require the separate OCR workflow. Unsupported formats remain available for filename search and tagging with an explicit name-only status.
- Results are **snapshots**, with an indexing timestamp. **Refresh** rebuilds selected file snapshots after edits. **Stop**, leaving the screen or closing the app cancels active work; already completed files persist. No background WorkManager crawl is scheduled.
- Tags are normalized lowercase, up to 20 per file and 40 characters each. Smart collections persist a name, word query, tag and extension. A saved rule updates its membership as the index changes, without copying/moving files.
- **Clear local library** removes indexed text, references and tags, retaining saved collection rules. Android backup/transfer exclusions cover the new database and its journal files. Android app-private storage protects the index; this is not an independently password-encrypted database.

## Duplicate review

Storage Cleaner → Duplicates → **Review all duplicate groups** shows thumbnails, full paths, previews and a keeper choice for every group. No extra copies are selected until a keeper is chosen. Removing a group's keeper choice leaves it unchanged. A keeper is excluded from all selected cleaner categories. Before Trash, full SHA-256 hashes and sizes are rechecked against the scan; modified/unreadable copies or missing/changed keepers are skipped and reported as incomplete cleanup. This does not lock files against concurrent changes by other apps. Existing Trash recovery applies; no new permanent-delete fallback was added.

## Reader and introduction

Reader toolbar buttons share one foreground-only, dirty-frame PixelCopy sampler at 1/6 resolution and no more than 5 Hz. Each button samples its surrounding surface, chooses light/dark ink with hysteresis and falls back to theme ink if capture fails. Sampling stops when controls are hidden or the reader is not foregrounded. This introduces no screenshot storage or network upload.

The introduction appears once for the 1.9.7 guide on Home, can be skipped, does not intercept direct document intents, and requests no permission by itself. Replay it from Settings → About → **Tour & new features**.
