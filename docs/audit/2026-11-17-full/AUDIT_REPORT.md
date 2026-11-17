# Full audit report — 2026-11-17

This is the entry point for the full review of MyDoc that began at `6bf34ca6feee75f4fcfb9f1fe5afc189f1abec38` (2.0.14 / 204) and was remediated through 2.0.16 / 206 plus follow-up fixes on `main`. It consolidates the subsystem reports in this directory; it does not replace them.

## How to read this directory

| File | Contents |
| --- | --- |
| AUDIT_REPORT.md | This file: scope, finding summary, remediation status, evidence, remaining gaps. |
| AUDIT_HANDOFF.md | Historical handoff written at the start of the original pass; describes the pre-fix snapshot only. |
| REMEDIATION_PLAN.md | Historical fix order proposed against that snapshot; every item has since been implemented. |
| tools-findings.md, storage.md, readers.md, ui.md, ui-final.md | Per-subsystem findings with source evidence and proposed fixes. |
| TEST_QUALITY.md, storage-tests.md, reader-tests.md, ui-tests.md | Weak or missing test cases identified in the review. |
| docs-config.md, fixtures.md | Documentation drift and fixture limitations. |
| REJECTED_AND_UNVERIFIED_LEADS.md | Claims withdrawn after verification; do not reintroduce without new evidence. |
| VERIFICATION_LOG.md | Local and remote execution evidence for the original pass. |
| FILE_COVERAGE.csv, coverage-summary.json, REMAINING_FILES.txt | Rebuildable coverage ledger (`python inventory.py`). |
| ../2026-11-17-latest-fix-verification.md | External verification of the fixed tree at `6e7fc776`. |

Coverage figures describe review coverage of the 610-file baseline: 556 full reads, 6 partial, 31 inventory-only, 17 binary fixtures reviewed structurally only. All 308 production source files and all 143 test-source files were fully read. Coverage is not test coverage and not proof of correctness.

## Finding summary and remediation status

Status abbreviations: **Fixed** = source change plus a focused regression where one is feasible; **Gated** = also exercised by the local gate and/or device test below; **Open** = verification gap that no source change can close by itself.

| Area | Finding | Status |
| --- | --- | --- |
| Storage | Directory-link moves/trash followed links and could delete outside the selected tree | Fixed; no-follow traversal in count/snapshot/copy/delete, links recreated as links; regression exists (`StorageMoveTrashSafetyTest`), shell cases skip on Windows without symlink privilege |
| Storage | ZIP REPLACE rollback restored originals only sometimes; rollback failures swallowed | Fixed; reverse-order rollback, `.bak_*` kept until commit, failures collected and surfaced as an `IOException` naming recovery files (see `ZipRollbackRecoveryTest`) |
| Storage | Trash reconcile could delete user files on a crash between move and row insert | Fixed; rows reserved before move, orphan files logged via SecureLog and left in place |
| Discovery | Selected-folder traversal unbounded | Fixed; 1,000-file / 200-directory budgets, and cursor consumption itself is capped with an exact truncation flag (`ChildrenPage`, `SelectedDocumentChildrenTest`) |
| Tools | Unbounded PDF input staging and orphan partial files | Fixed; 256 MiB per file / 512 MiB aggregate, cancellation between reads, temp ownership registered before copy. Provider reads still block until they return (platform limitation, noted below) |
| Tools | ZIP extraction could write outside the destination; ZIP bombs accepted | Fixed; canonical prefix checks, 1 GB / 10,000-file / 100:1 limit, bomb rejection asserted in `DataIntegrityCleanerE2ETest` |
| PDF | Protect/unlock sent invalid MuPDF writer options, yet reported success | Fixed; `encrypt=` options, reopened-and-verified before success is reported |
| PDF | Metadata stripping destroyed tagged-PDF structure | Fixed; PDFBox pass clears only XMP and authored /Info fields; StructTreeRoot and MarkInfo survive (`PdfMetadataStripPreservesTagsTest`) |
| PDF | Redaction reported success without checking the output | Fixed; redactions burned in and output re-searched before returning |
| Readers | Supplementary Unicode dropped from DOCX; blank OCR pages counted as success | Fixed; code-point walk plus mutation-tested regression; empty extraction fails with a typed error |
| Readers | Oversized Word/OCR/compression raster paths could OOM | Fixed; streamed 50 MiB thumbnail bound and shared cappedRenderScale; native huge-page integration not re-proved |
| TTS | Paused state lost; stale engine callbacks overwrote newer state; lifetime tied to viewer process | Fixed for the supported scope: resume, utterance-ID filtering, off-main dispatch. Narration still stops when the viewer is destroyed — deferred capability, documented in the TTS controller/manager comments |
| UI | Widget opened the wrong recent document; progress stale; bookmark state leaked between URIs | Fixed; identity-carrying intents, persisted progress refresh, per-URI remember keys |
| UI | Cleaner could show a success screen after total trash failure; empty-folder-only cleans claimed "Moved to Trash" | Fixed; completion requires actual work, and empty-folder-only cleans now show a dedicated heading and plural |
| Security | Release logs carried paths/URIs through throwable messages | Fixed; all direct `android.util.Log` calls in `main` now go through `SecureLog` (summary-only in release). `SecureLog` itself is the only remaining `android.util.Log` importer |
| Docs | `SECURITY.md`/`ARCHITECTURE.md` version and toolchain drift; discovery limits stale | Fixed; docs match 2.0.16 / 206, the catalog and the 1,000-file/200-folder limits |
| Tests | Weak PDF/ZIP/cache assertions, missing 7→8 migration regression | Improved; production crypto/PDF helpers exercised, real cross-reference fixture writer, device migration test added |

## Verification evidence

**Local app-scoped gate** (`:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --console=plain`): BUILD SUCCESSFUL; 96 suites, 644 tests, 0 failures, 0 errors, 3 skipped (641 passed); lint 0 errors, 170 warnings, 1 hint; debug APKs and the androidTest APK produced. The three skips are the directory-symlink cases that need a privilege this Windows host does not grant (`a folder move never follows a directory link out of the tree`, `permanent trash deletion keeps a symlink target intact`, `copyAndDeleteTreatDirectoryLinksAsLeaves`). The whole-project root gate remains blocked before task execution by the Moshi signing-key limitation in gap 4, not by app code.

**Device** (mydoc_api35, API 35 x86_64, `emulator-5554`): `SelectedDocumentChildrenTest` OK (4 tests) — 1,200-row cap with truncation flag, exact-fit boundary, empty listing, field mapping.

**Non-vacuous by construction:** the first gate runs of the new tests failed and exposed two real defects, both fixed before the green run above — rollback ran `deleteTree` on a target the failed extraction had never created and threw `NoSuchFileException`, which masked the zip-bomb `SecurityException` as a bogus rollback failure; and `PDDocument.load` throws `InvalidPasswordException` before the encrypted-document guard could run, so the PDFBox pass had to refuse password-protected input explicitly.

The local gate was re-run from scratch here; earlier app-scoped runs referenced in the verification logs were executed by the external reviewer, not reused as evidence for this revision.

## Remaining gaps (named, not implied)

1. **Device-behavior integration tests still missing** for: metadata-tag preservation through the full tool save pipeline (helper is covered), narrow/rotated signature output, OCR Save without all-files access, cleaner failure UI states, widget identity/progress end-to-end, bookmark URI reuse across documents, and the cropper's async rotation/disposal race. Source changes address these paths; unit/helper tests alone do not establish device behavior.
2. **Blocking provider reads** in tool staging cannot be cancelled until the provider returns; cancellation is checked between reads.
3. **Read-aloud after closing the viewer** is not implemented; viewer-scoped narration (including screen-off while the viewer lives) is the supported scope.
4. **Whole-project local gate**: `testDebugUnitTest lintDebug assembleDebug` at the root fails before task execution while resolving macrobenchmark runtime dependencies because the Moshi 1.13.0 signing key `8671A8DF71296252` is unavailable locally. The cached jar hash matches the committed expectation; verification was not disabled and trust metadata was not changed. The app-scoped gate covers all app code.
5. **Anonymous CI verification** cannot prove which individual tests executed (GitHub REST quota is exhausted from this machine and job artifacts require auth); the run status for the exact commit is the evidence used above.

Do not treat this report as a release sign-off for device behavior; it records source-level remediation and the gates that actually ran.
