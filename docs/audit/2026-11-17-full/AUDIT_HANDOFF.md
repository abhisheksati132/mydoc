# Audit handoff

Target: main at `6bf34ca6feee75f4fcfb9f1fe5afc189f1abec38`, MyDoc 2.0.14 / 204. Source-review pass finished; runtime verification remains incomplete. No production changes, commits, pushes, branches or releases were made.

## Coverage

610 tracked files inventoried and hashed. Final aggregate: 556 full, 6 partial, 31 inventory-only, 17 binary fixture structural-only. All 308 files classified as production source and all 143 test-source files are full reads. These figures describe review coverage, not test coverage or proof of correctness. Remaining partial reads are the historical changelog and five bundled Office license/notice files. Other inventory-only paths include generated baseline/trust data, vendor license collections, fonts, screenshots, and binary tooling. See FILE_COVERAGE.csv and REMAINING_FILES.txt for exact paths. The new audit documents themselves were untracked and are outside the 610-file baseline.

## Start here

- AUDIT_REPORT.md: prioritized findings and verification limits.
- REMEDIATION_PLAN.md: proposed fix order; not implemented.
- tools-findings.md, storage.md, readers.md, ui.md, ui-final.md: triggers, source evidence, fixes and caveats.
- TEST_QUALITY.md, storage-tests.md, reader-tests.md, ui-tests.md: weak checks and missing regressions.
- docs-config.md and fixtures.md: documentation drift and fixture limitations.
- REJECTED_AND_UNVERIFIED_LEADS.md: withdrawn claims; do not reintroduce them without new evidence.
- VERIFICATION_LOG.md: exact local execution and remote CI evidence.

## Verification to carry forward

The local Android gate failed before Gradle initialization because its distribution download timed out. Fresh direct compilation of three unmodified production helpers and their existing JUnit tests passed 22 tests. Production sanitizer and signing-width expression probes reproduced failures. A Kotlin directory-junction probe reproduced outside-sentinel deletion, with Android filesystem reachability explicitly conditional. The exact audited SHA's remote CI and CodeQL runs succeeded; this does not clear the source findings. No new app-device or native-runtime test execution occurred in this audit.

Before claiming a release is ready, implement focused fixes with regressions, rerun Android unit/lint/build gates, and reproduce high-risk cases on devices. Complete dependency-advisory/provenance and licensing review separately; this report does not establish those. Do not weaken gates or treat old build artifacts as the audited SHA.

## Rebuilding the ledger

Run `python docs/audit/2026-11-17-full/inventory.py`. It merges subsystem JSON maps by strongest coverage status, checks XML well-formedness, and rewrites FILE_COVERAGE.csv, coverage-summary.json and REMAINING_FILES.txt. Verify HEAD remains the stated SHA before interpreting its results as this audit's baseline.
