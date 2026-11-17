# Latest audit-fix verification

Audited HEAD: `6e7fc776b0325bcfcccfb1a2fc769ec3a8b07b6e` on `main`, MyDoc 2.0.16 / 206. Fetch confirmed HEAD equals origin/main. Compared changes since the original audit revision `6bf34ca6feee75f4fcfb9f1fe5afc189f1abec38`. No production source was changed during verification. The pre-existing untracked `.qoder-gate/` directory was preserved.

**Verdict: most audit fixes are implemented, including the four high-priority source defects. It is not accurate to say every issue and verification gap is closed.** A source-level fix and a passing focused regression are distinguished below from device/integration proof.

## Remaining issues and limitations

1. **P2 — Folder enumeration is only partly bounded.** `DocumentIndexer.kt:36` calls `SelectedDocument.children` before enforcing the per-child file/queue budgets. `SelectedDocument.kt:27-38` still materializes every cursor row into a list without an entry cap or cancellation checks. The new 200-directory queue/visit ceiling prevents unbounded recursive scheduling, but one extremely wide directory still causes unbounded list allocation and delayed cancellation. Bound cursor consumption itself and signal truncation.
2. **P2 — Raw release throwable logs remain outside the fixed helper.** `UserMessages` now uses SecureLog correctly, but `StorageRepositoryImpl` still calls `android.util.Log.w(..., e)` in storage-query/reconciliation failure paths, including lines 1154/1157. Exceptions can carry private paths/URIs into device logs. This is a broader residual redaction gap, not evidence that the specific UserMessages change failed, and not evidence of remote disclosure.
3. **Read-aloud after closing the document is not implemented.** T02 was resolved by documenting viewer-scoped playback in controller/manager/architecture comments. `DocumentTtsManager.shutdown()` still stops narration. Screen-off/background playback while the viewer remains alive is the supported scope. Treat continuing after viewer exit as a deferred capability, not a new behavior delivered by this fix.
4. **Audit documentation is incomplete.** The tracked `docs/audit/2026-11-17-full/AUDIT_REPORT.md` is zero bytes. Other subsystem reports remain available, but the main report link is empty. The historic handoff/remediation documents still describe the old audit snapshot; this new report records current status.
5. **Verification gaps remain.** No newly added targeted integration tests were found for metadata tag preservation through the complete save pipeline, narrow/rotated PDF signature output, OCR Save without all-files access, cleaner failure UI, widget identity/progress, bookmark URI reuse, or the cropper's asynchronous rotation/disposal race. Source changes address these paths, but helper tests alone do not establish all device behavior.

Additional durability observation: ZIP rollback restores originals for ordinary extraction failure, but restoration exceptions are swallowed by `runCatching` around rollback actions. If restoration itself fails, the caller receives the extraction error without recovery details and the original may remain only in `.bak_*`. Preserve and surface recovery information; no injected restore-failure reproduction was performed here. Cleaner empty-folder-only completion also retains generic “Moved to Trash” wording despite moving no files.

## Finding-by-finding status

| Original finding | Latest implementation status | Verification notes |
| --- | --- | --- |
| Directory-link move deletion outside selected tree | Fixed in source | No-follow traversal now covers counting, snapshots, copy and deletion; links are recreated as links. New regression exists, but skipped locally because Windows symlink creation was unavailable. |
| Unbounded PDF-tool input staging / orphan partial files | Fixed in source | Per-file 256 MiB, aggregate 512 MiB acceptance limit; cancellation between reads; temp ownership registered before copy. Blocking provider reads are still not interruptible until they return. |
| Word thumbnail limit checked after readBytes | Fixed in source | Reads are bounded at 50 MiB while streaming. No new thumbnail/provider regression found. |
| TTS pause/resume | Fixed in source | Fake-engine tests cover resumed chunk, new request from paused, and stop/new start. |
| Stale TTS error callbacks | Fixed in source | All callbacks dispatched to main scope and matched against current utterance ID; regression added. |
| TTS survives leaving viewer | Scope clarified; capability deferred | Current shutdown still stops playback; no independent service-owned page loader added. |
| ZIP REPLACE rollback changes originals on failure | Fixed for ordinary failure | Backup/restore and reverse rollback implemented; malicious later-entry regression preserves original bytes. Restore-failure durability caveat above. |
| Supplementary Unicode removed from DOCX | Fixed | Production sanitizer walks code points; real existing tests and mutation check executed locally. |
| Heading-only OCR extraction success | Fixed in source | Blank pages excluded; no-content builder returns null and caller fails. Helper regression added. |
| Uncapped OCR/compression rasterization | Fixed in source | Render paths now call shared cappedRenderScale; helper bound regression added. Native oversized-page integration not newly proved. |
| Narrow-page signing exception | Fixed in source | layoutSignature bounds width/height/origin; helper tests cover narrow/tall visible dimensions. |
| Metadata removal destroys tagged structure | Fixed in source | Destructive structureTreeRoot assignment removed. Complete tagged-output save regression remains missing. |
| Signature status discards decoded bitmap | Fixed in source | Probe bitmap explicitly recycled. AES tests now call production seal/open helpers. |
| Cropper rotates/recycles active worker input | Fixed for reviewed UI actions | Work state disables rotate, apply and dismissal during jobs; no async regression added. |
| Positional widget opens wrong recent document | Fixed in source | Intent carries URI and tap resolves identity with saved-copy fallback. |
| Widget progress remains stale | Fixed in source | Persisted progress triggers refresh. |
| Office indexing deletes shared conversion cache | Fixed in source | Both indexing paths delete only deleteWhenClosed outputs. |
| Selected-folder traversal budget | Partial | Queue and directory count capped; eager child-list allocation remains. |
| UserMessages raw exception logging | Fixed in that helper | Other direct throwable logs remain as described above. |
| Cleaner shows success after total trash failure | Fixed in source | Completion only set if actual work succeeded; partial counts/bytes carried. Empty-folder-only wording remains misleading. |
| PDF bookmark state carries across URI | Fixed in source | remember keyed to document.uri. |
| Spotlight displays old results during debounce | Fixed in source | Results cleared at effect start; cancellation checked before publication. |
| Weak encryption and E2E checks | Improved | Production crypto helpers exercised; PDF text output reparsed; ZIP-bomb rejection asserted; cache tests use production accounting; PDF fixture writer generates real cross-reference data. Known Android graphics stub catches are narrowed, not proof of rendering. |
| Missing 7-to-8 migration regression | Added | Dedicated device migration test added. |
| Stale current-version/toolchain/selection-limit docs | Fixed | SECURITY and ARCHITECTURE say 2.0.16/206; toolchain notices match catalog; discovery limit says 1,000 files/200 folders. |

## Verification evidence

- Fresh local app unit-test XML: 94 suites, 637 tests, zero failures/errors, 3 skipped (634 passed). All result files were rewritten during this run. Directory-link move and permanent-trash symlink cases were among the skipped tests; do not call them locally executed passes.
- Independently compiled unmodified production PdfToolUtils and its existing JUnit tests with cached Kotlin: all 14 passed. An isolated copy with supplementary-code-point acceptance removed failed the new supplementary Unicode test. Production sources remained untouched. Logs under ignored `app/build/audit-verification/`.
- Exact-HEAD [CI Build](https://github.com/abhisheksati132/mydoc/actions/runs/44061357747) succeeded: unit tests, lint, assemble debug, and API 35 instrumented-test job all reported success. [CodeQL](https://github.com/abhisheksati132/mydoc/actions/runs/44061357592) also succeeded. No test count is inferred from the status API.
- Whole-project local `testDebugUnitTest lintDebug assembleDebug` failed before task execution while resolving macrobenchmark runtime dependencies: Moshi 1.13.0 signing key `8671A8DF71296252` was unavailable locally/remotely. The cached jar's SHA-256 equals the committed expected value `bd004dd9ba175e603ad44420bf605250291b9b7bc6b9d9e916f5741ac8663307`; this was a missing-key failure, not a demonstrated checksum mismatch. Verification was not disabled and no trust metadata was changed.
- App-scoped local `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline --console=plain` succeeded in 5m 1s. 58 actionable tasks: 7 executed, 51 up-to-date. The unit-test task executed freshly; APK assembly reused Gradle-validated current outputs. Lint reported **0 errors, 170 warnings, 1 hint**. Logs: ignored `app/build/latest-app-verification.log`; complete-project failure log: `app/build/latest-verification.log`.
- No fresh local instrumentation/device run was performed. An API 35 emulator was available, but the device-test evidence above is the exact-HEAD CI result. Native/device-specific fixes without direct regressions remain explicitly unverified here.

The missing Moshi key blocks the broader all-module local gate, while the app-scoped gate passes. Do not equate the former with a current app compile failure or disable signature verification to make it green. No commit, push, release, branch change or production-code fix was made during this verification.
