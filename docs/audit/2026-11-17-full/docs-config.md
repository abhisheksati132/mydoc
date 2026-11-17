# Documentation and repository configuration review

**Revision:** `6bf34ca6feee75f4fcfb9f1fe5afc189f1abec38`  
**Scope:** Markdown under `docs/` and `docs/audit/` except `docs/audit/2026-11-17-full/`; changelog, security and third-party notices; issue templates; Git ignore/attribute files; exported Room schema JSON. Coverage is recorded per file in `docs-config.json`. Large historical or partially sampled items are explicitly not represented as full reads. No code or config was changed.

## Confirmed documentation inconsistencies

### [P2] Published current version is stale in security and architecture docs

`SECURITY.md:12` says current version `2.0.12` / code `202`; `docs/ARCHITECTURE.md:1` is also headed `v2.0.12`. The current Gradle version is `2.0.14` / code `204` (`app/build.gradle.kts:15-16`), and the changelog begins with `2.0.14` (`CHANGELOG.md:6`). Update the current-version references so readers can identify the reviewed release accurately.

### [P2] Discovery documentation understates folder-indexing file limit

`docs/DISCOVERY.md:11` says selected-folder traversal stops at 200 files. `DocumentIndexer.kt:27` checks `files.size >= 1000` and clears pending directories only at that threshold; the 12-level depth bound is present. The user-facing documentation promises a lower cap than the implementation applies. Align the doc with the actual 1,000-file cap or change the implementation if 200 is intended.

### [P2] Third-party dependency inventory names superseded toolchain versions

`THIRD_PARTY_NOTICES.md:5` describes its release dependency snapshot as AGP 9.5.0 / Kotlin 2.3.21. The checked-in version catalog declares AGP 9.5.1 and Kotlin 2.4.21 (`gradle/libs.versions.toml:2-3`); the changelog also records the upgrade in v2.0.10. Regenerate or relabel this dependency snapshot before readers use it to identify the shipped dependency set.

## Other observations

- Historical audit reports are explicitly marked as snapshots in `docs/audit/README.md` and in the individual report banners. Their obsolete findings and old dependency versions are therefore not treated as current claims.
- `WORD_ENGINE.md` contains both current routing guidance and clearly dated historical verification/size results. Its older verification results are explicitly separated from later extension work and are not presented as fresh runtime proof.
- The issue templates warn against attaching private documents and route security reports to private vulnerability reporting. `.gitignore` excludes local keys/config/build outputs and agent artifacts; `.gitattributes` normalizes text line endings.
- All six exported Room schemas (v3–v8) were parsed and compared with current database version 8 and migration SQL. No schema/migration mismatch was found.

## Coverage limits

`docs-config.json` marks `CHANGELOG.md`, `docs/ARCHITECTURE.md`, `docs/UX_REFINEMENT.md`, `docs/audit/DEPENDENCIES.md`, and `docs/audit/FILES_REVIEWED.md` partial because not every line was reviewed in a complete standalone read. `LICENSE` is marked `third_party_inventory`: it is a large bundled AGPL license text and was inventoried without legal interpretation. The requested `docs/audit/2026-11-17-full/` folder was excluded from this inventory as directed; this report and JSON are saved there as task artifacts.

## Follow-up coverage

Completed full reads for `docs/ARCHITECTURE.md`, `docs/audit/DEPENDENCIES.md`, `docs/audit/FILES_REVIEWED.md`, and `docs/UX_REFINEMENT.md`; their coverage entries are now `full`. The full changelog is 2,444 lines / about 194 KB. Its current 2.0.14 section and release headings were checked, but a complete line-by-line review is not reasonable within this audit pass, so it remains `partial` in the JSON. Historical docs and the changelog contain archived state; their age is stated in the documents or entries.
