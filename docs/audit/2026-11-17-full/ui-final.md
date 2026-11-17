# Final UI follow-up audit

Target: MyDoc HEAD `6bf34ca6`. This was a read-only static review. [ui-final.json](/C:/Users/abhis/Downloads/project-base/myapp/docs/audit/2026-11-17-full/ui-final.json) records the exact path coverage. The six previously uninspected instrumented tests are now read in full, along with all 1,184 lines of `strings.xml`. The three requested supporting production sources and the `full` tests from the earlier follow-up remain covered. No builds or tests were run.

## Findings

### [P2] OCR save can fail without all-files access on Android 11+

[OcrResultSheet.kt](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/ocr/OcrResultSheet.kt:155) writes directly to the public Documents directory with `File.writeText()`. For Android 11 and newer, this path is not writable through the legacy external-storage permission, which is capped at API 29. The save action does not check all-files access or use a scoped-storage destination. The sheet is reachable from both the photo viewer ([PhotoViewerScreen.kt](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/ui/media/PhotoViewerScreen.kt:422)) and PDF viewer ([PdfViewerDialogsAndSheets.kt](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/ui/viewer/pdf/PdfViewerDialogsAndSheets.kt:148)); neither caller routes the save action through `StoragePermissionHelper`. Therefore the failure is conditional: users who lack all-files access and use Save can get an error. Users with that access may succeed. A scoped-storage destination or an explicit supported access flow would make saving work consistently.

### [P2] AES-GCM tests do not call production encryption or shredding

[AesGcmEncryptionTest.kt](/C:/Users/abhis/Downloads/project-base/myapp/app/src/test/java/com/abhisheksati/mydoc/security/AesGcmEncryptionTest.kt) reimplements encryption with JCA, and its shredding test writes zeroes directly to its own temporary file. These tests can pass without exercising the production encryption and shredder. Tests should call the production API and verify round-trip, tamper/wrong-key rejection, and actual deletion or overwrite behavior.

## Rejected lead

The apparent quote wrappers in `cleaner_confirm_folders` and `cleaner_confirm_restore_hint` are Android string-resource delimiters, not visible punctuation. Compiled resource output was checked with `aapt2`; no user-visible quote issue is established.

## Strings review and coverage

The XML parsed successfully during static inspection; no duplicate resource names were found. Singular/plural pairs reviewed include file/app counts, OCR counts, scan pages, page counts, search results, transfers, and duplicate review. Placeholder-bearing formats were checked for consistent argument positions within each resource. The inventory reports 66 fully read paths; no inventory-only entries remain in this follow-up map. This is source review only, not a substitute for resource compilation, instrumentation, or locale testing.


