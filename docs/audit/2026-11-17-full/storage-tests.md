# Storage and data test review

**Revision:** `6bf34ca6feee75f4fcfb9f1fe5afc189f1abec38`  
**Scope:** All 30 related tests in storage/data/discovery/domain/util/update scope were read completely. Four cleaner tests, the separate PDF tools test, and the data-integrity E2E test remain outside this review per task allocation. `storage-tests.json` records full coverage for each in-scope file. No tests or builds were run.

## Confirmed findings

No new production defect was established from test source alone. Test gaps relevant to findings in `storage.md`:

- `StorageMoveTrashSafetyTest` covers symlink-safe permanent deletion, but has no directory-link case for the copy-then-delete cross-volume move fallback. Its existing symlink case confirms that permanent deletion is separately protected; it does not cover `move()`.
- `StorageOperationSafetyTest` verifies cleanup of newly created ZIP extraction output after a later failure, but does not exercise `ZipCollisionStrategy.REPLACE` with a valid early replacement and a later failing entry. The already-present file overwrite/rollback gap is therefore unguarded.
- No test in the inventoried discovery set constructs a directory tree with a large number of empty directories to check the traversal budget; existing selected-folder indexing tests concentrate on documents/content.
- `SecureLogTest` validates redaction in `SecureLog`, but there is no `UserMessages` test asserting that raw exception details are not logged through its read/storage error helpers.
- The related widget paths have no widget intent identity or progress-refresh test in this test inventory.

The test suite does contain positive coverage for partial move/trash accounting, cancellation during source deletion, symlink-safe permanent deletion, size/copy constraints, ZIP traversal limits, offline-copy sidecars, source preservation during discovery indexing, updater state/dismiss races, and release logging redaction. These are source assertions only; none were executed during this review.

## Room schema and migration review

Read all six exported schema files under `app/schemas/com.abhisheksati.mydoc.data.local.AppDatabase/` (versions 3 through 8) and compared their entity fields, primary keys, indexes, and tables against `AppDatabase.kt` and the migration SQL.

- Versions 3 and 4 have the expected historical table sets; v4 adds `shelves` and `shelf_assignments`.
- `MIGRATION_4_5` removes both shelf tables. The v5 schema matches v3, including the unchanged identity hash, and the direct 3→5 migration is an intentional no-op.
- `MIGRATION_5_6` adds the favourites and media-playback timestamp indexes represented in v6.
- `MIGRATION_6_7` adds the recents and trash timestamp indexes represented in v7.
- `MIGRATION_7_8` creates the highlights table and document URI index represented in v8; the current database declares the same fields and index.
- The current Room database is version 8 and registers the 3→5, 4→5, 5→6, 6→7, and 7→8 paths. No schema/migration mismatch was confirmed.

The Android migration tests exercise the 3→5 path and the 5→6/6→7 index/data-preservation paths. The JVM migration test exercises the 3→5 and 4→5 SQL calls through a proxy. There is no dedicated migration test for 7→8 in the reviewed files; the v7→8 change is additive and matches the exported v8 schema, but the Android migration result itself was not run here.
