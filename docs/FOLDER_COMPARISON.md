# Folder comparison

Compare any two open folder tabs on phones or wide screens. This is a read-only snapshot: no sync, overwrite or delete actions. Include hidden files and descendants; skip symbolic links and stay within each canonical root. Stream file contents through SHA-256, not just names/timestamps. Matching folders are structural and omitted from the matching-file count; unmatched folders are listed.

Cap each scan at 5,000 entries and total content reads at 512 MiB. Label unreadable, changing, skipped or unverified entries as Unknown. Missing-counterpart results become Unknown when the opposite scan is incomplete. All work runs on IO and cancels on dismissal. UI offers folder selectors from open tabs, a start button, clear status counts and a differences-only filter. No new dependency, permission or database migration.

Acceptance: same-size changed bytes detected; missing entries correct; unreadable/limited scans never fabricate equality or missing files; hidden/nested files included; read limits and cancellation enforced; comparison writes nothing. Verify focused JUnit tests, full unit suite, build, lint, and a disposable-emulator comparison flow.

Commands: `./gradlew.bat testDebugUnitTest --tests "*ExplorerComparisonTest"`; `./gradlew.bat testDebugUnitTest assembleDebug lintDebug`.

## Use

Open the first folder in Library, add a tab, and navigate that tab to the second folder. Tap Compare folders beside the tab controls. Choose the two folders and select Compare contents. Hide matching files is on by default; turn it off to inspect matches. Cancel stops the scan; Close dismisses it.

Paths are relative to the compared folders. Counts include files and unmatched folders; matching folders themselves are omitted. No synchronization or copying is performed. Results describe a best-effort snapshot, not a transaction: avoid editing either folder during comparison. Unknown results must be checked manually. Saved searches and per-folder preferences are separate follow-up features, outside this slice.

## Validation (2026-11-07)

443 JVM tests passed, including six focused comparison tests. Four Android comparison tests passed on API 35: content comparison and matching-file filter, insufficient-tab guidance, actual symlink handling, and parent redirection after scanning. Debug and test APK builds and Android Lint passed (warnings remain). The comparison entry point was visually inspected in the full app; no runtime crash was logged. Tests used disposable emulator data and app-cache fixtures. Large real-device folders and concurrent edits still need release testing.
