# External audit verification — v1.7.1 (`9b9fba1`) → `main`

Date: 2026-11-04
Auditor: external (ChatGPT-authored static review, two passes)
Verified against: `main` at the commits listed under each finding, before and after the fix.

Every claim in the report was checked against the source before anything was changed. The report
was right about most of it, wrong about four things, and imprecise about the mechanism of one more.
This file is the record so the next pass does not re-litigate settled points.

## Confirmed and fixed

| # | Claim | Evidence before the fix | Fixed in |
|---|---|---|---|
| 1 | URI identity uses 32-bit hashes | `DocumentRepositoryImpl.kt:220`, `DocumentStreamResolver.kt:63,128`, `FileThumbnail.kt:519` all derived cache keys from `hashCode()` | `util/DocumentIdentity.kt`; `c313dc9` + audit batch |
| 2 | File operations lack failure guarantees | `StorageRepositoryImpl.kt:265-347` — one try around the whole batch; `move` incremented `moved` without checking the delete; no free-space check; no cancellation point | same commit |
| 3 | Trash can lose a file | row inserted *after* the physical move (`:851-868`) and `purgeExpiredTrash` deleted DB-less trash files older than 24 h (`:807-811`) | row reserved first; orphans kept and logged |
| 4 | Extraction is not transactional | earlier ZIP entries survived a mid-archive failure (`:495-589`) while the UI reported failure | rollback of created paths, staging-only |
| 5 | Compression leaves a truncated archive | ZIP written straight to `destFile` (`:460`) | staged + renamed |
| 6 | Timestamp temp names collide | `.tmp_${System.currentTimeMillis()}_…` (`:557`) | per-extraction UUID token |
| 7 | PDF native cleanup not on failure paths | `PdfPageOperationsEngine.kt` had five `try` blocks and no `finally` around any destination `PDFDocument`; `MuPdfRenderer` destroyed page/pixmap/structured-text only on the success path | `usePdfDocument`, `withPage`, `withPixmap`, `withStructuredText` |
| 8 | Empty string hides extraction failure | `AndroidPdfRenderer.getPageText/getDocumentText` returned `""` for both "no text" and "could not read" | `String?` through `DocumentRenderer`, both renderers, `ViewerViewModel`, and the seven PDF action call sites |
| 9 | App lock boundary is weak | `BiometricLockHelper.kt:29-33` called `onSuccess()` when `KeyguardManager.isDeviceSecure` was false | fails closed with an explanation |
| 10 | Documents fetched before unlock | `MainActivity.kt:157` ran `handleIncomingIntent` unconditionally; `DocumentStreamResolver.kt:59-66` copied the document into `saved_docs` while the Lock screen was drawn | external intents held until `isAppUnlocked` |
| 11 | Signature store conflates absent and unreadable | `SignatureStorage.kt:74-76,98-100,106-109`; `getOrCreateKey()` minted a new key when the alias was gone | `SignatureStatus` NONE/READY/UNREADABLE; decrypt never creates a key; atomic write |
| 12 | Presentation parser swallows failures | `PptxParser.kt:445` returned the slides parsed so far with no signal | `PptxPresentation.incompleteReason`, shown in the fidelity notice |
| 13 | Media library materialises and over-queries | `MediaRepositoryImpl.kt:33-55` three cursors per request; `HomeViewModel` subscribed twice → six full-library queries per pass | 1.2 s snapshot + explicit invalidation; dead `MediaViewModel` deleted |
| 14 | CI never runs the release artifact | `release.yml` assembled, signed and cert-verified, but installed nothing; instrumented job ran the debug APK only | release APK installed and launched on API 35 x86_64; publish aborts on crash |

## Refuted by the source

| Claim | Why it is wrong |
|---|---|
| "Backup may include the database, offline document copies and pinned folders" | `res/xml/backup_rules.xml:4-27` and `res/xml/data_extraction_rules.xml:3-55` exclude `mydoc.db` (+`-wal`/`-shm`), `saved_docs` in both locations, `.trash`, `user_signature.enc`, `mydoc_settings.xml`, `pinned_folders_prefs.xml` and `mydoc_updater.xml`. The names match what the code actually writes (`AppDatabase.kt:62`, `PinnedFoldersManager.kt:11`, `SignatureStorage.kt`, `DocumentStreamResolver.kt:62`). |
| "Security documentation describes the lack of automatic re-locking when the app leaves the foreground" | Re-lock exists: `MainActivity.onStop` and `onRestart` set `isAppUnlocked = false` and re-add `FLAG_SECURE` whenever app lock is enabled. State is in-memory and never persisted, so process death also re-locks. |
| "The media projection selects large columns / thumbnails" | No thumbnail or blob column is selected. `DATA` is in the projection, which is a path string, not image data. |
| "The old audit's `QUERY_ALL_PACKAGES` claim" | The report itself corrected this; the manifest does not declare it. |
| "CI does not prove release assembly" | Corrected in the report's own second pass: `assembleRelease` runs and the signing certificate is verified against a pinned fingerprint. The real gap was runtime, which is now covered. |

## Mechanism corrected

The report said the lock bypass came from `BiometricManager.canAuthenticate` returning "not
capable". There is no `canAuthenticate` call anywhere in the app; the gate reads
`KeyguardManager.isDeviceSecure`. The effect was as described — it failed open — but the cause was
different, and the fix follows the real one.

Also narrower than reported: app lock cannot be *enabled* on an insecure device
(`SettingsRepository.kt:173-179`, surfaced in `SettingsScreen.kt:198-200`), so the bypass was only
reachable when a user disabled the device lock after turning app lock on.

## Not done, and why

- **Media paging / incremental loading.** The snapshot removes the duplicate query storm; it does
  not make a 20,000-item library cheap to materialise. Cursor-to-LazyList is a real change to
  `MediaRepository`, `LibraryRepository` and the Home/Library feeds and was not attempted here.
- **Document fidelity corpus.** The audit asks for reference renderings from established office
  suites with automated comparison. Nothing like it exists; building it is its own project.
- **`MANAGE_EXTERNAL_STORAGE` eligibility / SAF-only mode.** A distribution-policy and product
  decision, not a code fix.
- **Large-class splits** (`MainActivity`, `StorageRepositoryImpl`, `PptxParser`,
  `VideoPlayerScreen`, `PdfViewerScreen`). Deferred deliberately: splitting them for line count
  would churn the exact files this batch just changed and make the fixes harder to review.
- **Cleaner revalidation before trash.** The audit's point that a scan result is a suggestion, not
  an assurance, still stands: files can change between scan and move.
- **Room upgrade testing with populated databases**, OCR cancellation semantics, and the
  `FileManagerViewModel` directory-load race are unverified or unfixed.

## Found while verifying, not in the report

Re-installing the debug build reset `MANAGE_EXTERNAL_STORAGE` to default, which exposed two things:

- The offline cache migration is a one-time re-read: copies saved under the old 32-bit key are no
  longer matched by the new identity, so the next open goes back to the source. That is the
  correct cost of a collision-resistant key, but it means a document that used to open from cache
  now depends on still having permission to read the original.
- When that re-read is denied, the resolver returned a bare `FileNotFoundException` and the viewer
  reported "The file may be damaged or unavailable" - blaming the document for an access problem.
  `DocumentStreamResolver` now returns `DocumentLoadException(PERMISSION_EXPIRED)` and the viewer
  says "Document inaccessible / MyDoc cannot open this file right now", verified on
  `emulator-5554` with the appop denied and again with it allowed.

## Gates run for this batch

- `:app:testDebugUnitTest` — 231 tests, 0 failures (13 new: 9 cache-identity, 4 storage-safety).
- `:app:lintDebug` — 0 errors.
- `:app:connectedDebugAndroidTest` on `emulator-5554` (API 35 x86_64) — see the run output.
- `:app:compileReleaseKotlin` — green. `assembleRelease` still fails locally by design: release
  signing credentials are not present on this machine, and the build refuses the debug keystore.
