# Test review findings

Target: 6bf34ca6feee75f4fcfb9f1fe5afc189f1abec38. These findings concern verification strength, not additional proof of a production vulnerability.

## [P2] Security and content tests can pass without the promised result

- `PdfToolsCorrectnessE2ETest.kt:91`: the searchable-text preservation assertion accepts `%PDF` instead of requiring the unique text. A raster-only or text-stripped output satisfies it. Reopen with a PDF text extractor and require the original marker.
- `PdfToolsCorrectnessE2ETest.kt`: Unicode watermark, non-PDF printing and crop checks swallow arbitrary Exceptions. An early unrelated failure can pass without creating/validating output or observing rejection. Restrict expected failures and move Android behavior to device tests.
- `DataIntegrityCleanerE2ETest.kt:388`: the ZIP-bomb test asserts only inside `if (result.isFailure)`; success is accepted with no assertion. Require the documented rejection or explicitly assert bounded extraction behavior under the intended policy.
- The same file's cache-quota test creates 60 files and only asserts that the directory is nonempty; it neither invokes trimming nor asserts a quota.
- `OfficeDocumentParsingE2ETest.kt:228`: the extension test checks a locally declared set, not production format routing. Other search/windowing/rotation tests duplicate calculations rather than invoke the implementation. Keep mathematical examples separate from behavioral regressions.
- `ViewerLifecycleExperienceE2ETest.kt`: the cache-bound test constructs an empty cache and clears it. No entries are inserted and no byte bound or eviction behavior is asserted. The lifecycle scenario changes local variables, so it does not test a real screen/service lifecycle.

These checks may coexist with stronger tests elsewhere. Their individual passing status cannot establish the properties in their names. Prioritize regressions that fail on the source-confirmed defects in this audit.

## [P2] Nominal PDF fixtures are structurally malformed

`SyntheticDocumentFactory.kt:62` writes invented cross-reference offsets (`i * 60`) and a constant `startxref` rather than actual byte positions. `createScannedImagePdf` invokes `/Im1` without defining its image resource. A forgiving PDF parser may repair these; the nominal tests therefore mix repair paths with ordinary feature checks. Generate well-formed fixtures with a PDF writer and validate them independently. Keep deliberately corrupt inputs in separately named negative tests.

No full Android test execution is claimed here. See VERIFICATION_LOG.md for the 22 focused tests actually executed and the blocked local Gradle gate.
