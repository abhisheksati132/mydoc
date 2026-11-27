# MyDoc archive workspace

Accepted scope: the archive feature table and build order approved in chat, including
multi-format creation/extraction, passwords, split volumes, editing, integrity tests,
batch/background operations and an integrated MyDoc interface. RAR creation and
format-specific repair are not promised by an extraction-only engine.

## Capability map

| Module | Responsibility | Depends on |
| --- | --- | --- |
| archive-engine | Version-pinned engine, capability detection and bounded process I/O | — |
| archive-storage | Validated entries, staging, exact selected-entry streams and atomic publication | archive-engine |
| archive-jobs | Cancellable queued operations, progress and notifications | archive-storage |
| archive-workspace | Browse, search, preview, creation, extraction, edit and password UI | archive-jobs |

Build order: engine → storage → jobs → workspace. Keep existing ZIP flows working
until replacements pass reference and hostile-input tests.

## Acceptance criteria

- Create and reopen ZIP/7z; password round trips and wrong-password recovery.
- List and extract supported RAR, TAR and compressed TAR references.
- Supported split volumes create/reopen; missing parts produce actionable errors.
- Selected extraction, conflict choices, archive editing and integrity checks work
  without overwriting source archives or unrelated destination files.
- Jobs expose real progress and cancellation; passwords are never persisted or logged.
- Engine capabilities govern visible actions; unavailable operations have no active buttons.
- Preserve path, link, byte, entry-count and cache limits. Clean up failed staging.
- Test on the API 35 emulator; do not imply that Windows tools prove Android support.

## Current delivery status

- [x] Prove current official 7-Zip engine on Android; pin source and build inputs.
- [x] Implement archive engine/storage APIs and reference tests.
- [x] Wire multi-format, encrypted and split operations into creation/extraction.
- [x] Add archive editing and integrity verification.
- [x] Add queued background jobs and progress.
- [x] Complete integrated UI and regression verification.

## Implemented behavior

- Creation: ZIP, 7z, TAR, TAR.GZ, TAR.BZ2 and TAR.XZ, presets, dictionary/thread
  controls, solid 7z, AES ZIP, encrypted 7z filenames and numbered split volumes.
- Browse: folder navigation, whole-archive search, name/size sorting, multiple
  selection and nested archive/file previews. Single preview files are limited to
  32 MiB and the preview area to 128 MiB.
- Extraction: a verified new output folder at the chosen location; existing user
  files are never overwritten. Alternatively merge verified files while skipping
  existing names or automatically renaming incoming conflicts. The engine validates paths, links, entry counts,
  declared sizes and checksums before publication.
- Editing: ZIP/7z add, replace, remove and rename operations rebuild a verified copy.
  Originals are retained; adding an existing top-level name requires removing the
  old entry explicitly. Split archives are read-only for editing.
- Tasks: a single FIFO queue, foreground notification, progress, live engine
  pause/resume, cancellation and bounded private history. Credentials stay in
  memory and are wiped after use. Process death is reported as interrupted;
  restarting a task begins again rather than claiming byte-level resume.
- Volumes: numbered 7z, RAR `.partNN.rar`, and classic `.rar`/`.r00` naming are
  normalized to the first part. Missing middle/first parts are rejected before
  processing; native diagnostics handle missing trailing parts.
- Default task limits: 10,000 entries, 512 MiB expanded data, 24 MiB metadata,
  depth 64, decompression memory 256 MiB and one hour per engine invocation.

## Remaining parity work

- Format-specific recovery/salvage. Do not show a generic repair action or claim
  that integrity testing repairs damaged data.

RAR creation remains unavailable. This is an archive workspace upgrade, not a
claim of complete WinRAR/7-Zip/ZArchiver parity.

## Verification commands

Use the repository Gradle wrapper with `:app:testDebugUnitTest :app:lintDebug
:app:assembleDebug :app:assembleDebugAndroidTest`, followed by targeted device
tests and independent archive-tool comparisons. No release or version bump is
part of this implementation request.

## Verification recorded on 2026-11-27

- Built the pinned engine and safe file helper for arm64-v8a, armeabi-v7a, x86
  and x86_64; the executable depends only on Android's libc, libm and libdl.
- Final Gradle gate: `assembleDebug assembleDebugAndroidTest testDebugUnitTest
  lintDebug` succeeded. JVM suite: 679 tests, zero failures/errors, three existing
  skips. Lint completed with zero errors.
- API 35 x86_64 emulator: the broader suite passed 237 tests before the final
  compatibility refinements. All 24 final archive checks passed on the delivered
  implementation, including GNU TAR prefixes, AES ZIP password entry/recovery,
  encrypted 7z, split 7z, independent multipart RAR5, edit copies, conflict
  handling, cancellation, external storage and accessible creation controls.
- The active-history eviction regression was first reproduced by a failing JVM
  test, then fixed. Native output tests reject replaced parent links and existing
  destination files without changing the user's data.
