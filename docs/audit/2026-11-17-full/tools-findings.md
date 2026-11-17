# Tools and cross-subsystem findings

Audited revision: `6bf34ca6feee75f4fcfb9f1fe5afc189f1abec38`. Source evidence, not device reproduction. Production code is unchanged.

## T01 — P1: Paused read-aloud never resumes

`tools/tts/TtsPlaybackController.kt:253` calls `launchReading` while state remains PAUSED. The job's check at line 178 returns before it can reach `speakAndAwait`, the method that sets PLAYING. Trigger: play a nonblank page, pause, then resume from the viewer or media notification. Expected: restart the paused chunk. Actual: no speech and state remains paused. Starting a new request while paused has the same problem. Fix: establish an active generation and a loading/playing state before launching, while still respecting a subsequent pause. Regression: pause/resume both with cached text and a suspended page loader, plus stop/start from paused.

## T02 — P2: Viewer disposal stops the advertised background audiobook

`tools/tts/DocumentTtsManager.kt:58` forwards shutdown to the process-global controller's stop. PDF, Word and text viewer disposal invokes it (for example `ui/viewer/pdf/PdfViewerScreen.kt:118`). Trigger: start narration and navigate out of the viewer. Actual: playback and foreground service stop. Expected from the controller/service documentation: audio survives leaving the activity/viewer. Define the actual product lifetime, then detach screen observation separately from stopping playback; a longer lifetime also requires an independent page loader rather than a renderer closed with the viewer. Regression: navigate home and back during narration and validate renderer ownership.

## T03 — P2: Late engine errors can fail a newer TTS utterance

`tools/tts/TtsPlaybackController.kt:108–113` calls `complete(ERROR)` in both error callbacks without checking utteranceId. Other callbacks check currentUtteranceId. Trigger: request B supersedes request A, then engine delivers A's delayed error after B creates its waiter. Actual: B's waiter is completed as ERROR and B stops. Fix: filter all callbacks by the active utterance/session generation and marshal mutable controller state onto one dispatcher. Regression: inject a stale onError after starting a newer utterance.

## T04 — P2: PDF-to-DOCX drops valid supplementary Unicode characters

`tools/engine/PdfToolUtils.kt:67` filters UTF-16 Char values, accepting ranges ending D7FF and starting E000. Both surrogate halves of every supplementary character are rejected, although valid pairs are legal XML characters. `PdfConversionEngine.kt:566` uses this function for the DOCX text. Trigger: export PDF text containing emoji or supplementary CJK characters. Actual: those characters disappear. Fix: iterate Unicode code points, retaining valid XML 1.0 supplementary code points and rejecting only isolated surrogates/illegal values. Regression: round-trip a DOCX paragraph containing U+1F600 and U+20000, plus invalid isolated surrogates.

## T05 — P2: Empty PDF text extraction is reported as success

`tools/engine/PdfConversionEngine.kt:409` initializes the builder with a Markdown heading. The no-content check near line 463 tests this whole builder, which can never be empty after that header. OCR errors are converted to empty strings. Trigger: an image-only PDF whose OCR returns no recognized text or fails. Actual: a heading-only Markdown file is returned as a successful extraction. Fix: count actual extracted page characters, fail clearly if none, and report per-page OCR failures when output is partial. Regression: no text/OCR result and mixed success/failure pages.

## T06 — P2: Unbounded rasterization in OCR/compression fallback

`tools/engine/PdfConversionEngine.kt:438` and `:512` render scanned pages at a fixed 1.5 scale before allocating both a native pixmap and Bitmap. `tools/engine/PdfOptimizationEngine.kt:199` similarly renders at 0.85. These paths omit the cappedRenderScale/area limits used elsewhere. Trigger: a PDF with very large page boxes and little or no text, or STRONG compression reaching raster fallback. Memory grows with page area and can exhaust the heap/native allocator before OCR. Fix: apply a shared finite dimension AND pixel-area budget before native rendering, and ensure page/pixmap cleanup when allocations throw. Regression: oversized page boxes for both OCR export formats and compression.

## T07 — P2: Signing valid small PDF pages throws before saving

`tools/engine/PdfEditingEngine.kt:172` clamps signature width with `coerceIn(80f, visW * 0.85f)`. For visW below approximately 94.12 points, the maximum is below the minimum and coerceIn throws. Trigger: sign a narrow receipt/label PDF. Fix: derive minimum and maximum from the actual page dimensions and bound signature height and position too. Regression: 72-point-wide page in all quarter-turn rotations.

## T08 — P3: Signature status checks abandon decoded bitmaps

`tools/security/SignatureStorage.kt:109` calls decode to determine status. Successful decode allocates a Bitmap, but status discards it without explicit recycle. Signing paths invoke status/hasSavedSignature/problemMessage, sometimes repeatedly, even when they only need a yes/no answer. This causes avoidable native/heap allocation pressure until GC, particularly for imported signatures. Fix: explicitly release the successful probe bitmap, or separate integrity validation from bitmap ownership. Regression: repeated status checks with a large saved signature while monitoring allocations. This is allocation/lifetime pressure, not a claim of permanent unreachable-memory retention.

## T09 — P2: Metadata removal also removes document accessibility structure

`tools/engine/PdfSecurityEngine.kt:263` sets structureTreeRoot to null while stripping metadata. The tagged structure tree is used for accessible reading order, not simply author/title metadata. Trigger: run Strip metadata on a tagged accessible PDF. Actual: output loses tags and reading structure despite being presented as metadata cleanup. Fix: preserve the structure tree and sanitize only explicitly identified metadata/authoring properties, or disclose the destructive accessibility change. Regression: tagged PDF with reading order and alternative text.

## Cross-subsystem cache ownership concern

`discovery/DocumentIndexer.kt:102` unconditionally deletes the conversion returned by `NativeOfficeConverter`, whose normal success file is a shared content-addressed cache entry (`NativeOfficeConverter.kt:45–47`). Reopening the same Office file after indexing must regenerate it. An already-open Unix file descriptor may keep working after unlink, so this evidence establishes cache eviction and unnecessary conversion, not a proven immediate viewer crash. Keep ownership explicit and delete only per-call temporary outputs.

## T10 — P1: PDF tools stage provider streams without a size or cancellation limit

`tools/engine/PdfToolsEngine.kt:342` creates a temporary input, then uses `input.copyTo(output)` without a byte limit or cancellation check. The file is added to tempFilesToDelete only after copyUriToTemp returns (near line 99). Trigger: select a very large input or a provider that streams indefinitely, then run a tool. Actual: staging consumes cache storage until EOF/disk failure, Cancel cannot interrupt this loop cooperatively, and a failed staging copy is absent from the finally cleanup list. The partial input remains on disk. Fix: use the existing bounded-copy policy with cancellation, register staging ownership before copying, and delete on every failure. Regression: input exceeding cap, a stream throwing halfway, cancellation while streaming, and aggregate size across multiple inputs.

## Verification and coverage

No production fixes were made. These findings are established by control/data flow. Native/device reproduction remains outstanding. Local Gradle checks failed before executing tests because the Gradle 9.9.1 distribution download timed out. See the inventory for file-level coverage; this report alone does not certify all tools or UI files as fully reviewed.

## Focused execution evidence

Using the cached Kotlin 2.4.21 compiler (without Gradle), compiled the production PdfToolUtils.kt with a small audit probe. Input A + U+1F600 + U+20000 + B produced AB: `Unicode roundtrip preserved=false`. The signing-width arithmetic copied verbatim from the production expression threw IllegalArgumentException for visW=72. The latter is an expression-level reproduction, not a device-level PDF signing test.

Compiled and executed the existing PdfToolUtilsTest, PdfPermissionsTest and StitchedImageLayoutTest with cached JUnit 4.13.2: 22 tests passed. Existing sanitizer tests cover entity escaping and BMP controls but not supplementary code points. These isolated tests do not validate Android integration or native rendering.

## T11 — P2: Cropper recycles a bitmap while background jobs can still read it

`tools/scan/cropper/FreeCornersCropperView.kt:166` immediately recycles the previous rotated image. Auto-fit at line 643 and Apply at line 686 run bitmap reads on Dispatchers.Default; the rotate control remains enabled and no per-bitmap job/ownership lock exists. Trigger: rotate once, begin Auto-fit or Apply, then rotate again while the worker still holds that rotated bitmap. Actual: the worker can access recycled pixel storage; Auto-fit getPixels can throw into its uncaught coroutine, while Apply may return a stale/recycled fallback. Fix: disable conflicting controls during work and cancel/join or otherwise retain source bitmap ownership until readers finish. Regression: suspend worker before pixel read, rotate again, resume worker; also test dismissal and repeated Apply. This is a source-confirmed ownership race; its timing has not been reproduced on a device.
