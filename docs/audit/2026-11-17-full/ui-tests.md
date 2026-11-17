# UI and supporting production/test audit follow-up

Target: MyDoc HEAD `6bf34ca6`. This is a read-only static review. I read the three listed production sources and the test sources marked `full` in [ui-tests.json](/C:/Users/abhis/Downloads/project-base/myapp/docs/audit/2026-11-17-full/ui-tests.json). No build or tests were run. The map marks files that were only inventoried as `inventory-only`.

## Findings

### [P2] OCR save writes to public Documents without a usable modern-storage path

[OcrResultSheet.kt](/C:/Users/abhis/Downloads/project-base/myapp/app/src/main/java/com/abhisheksati/mydoc/ocr/OcrResultSheet.kt:155) writes OCR text directly under `Environment.DIRECTORY_DOCUMENTS` using `File.writeText()`. The app targets SDK 35, while `WRITE_EXTERNAL_STORAGE` is capped at API 29. On Android 11 and newer, a normal app cannot write this public path through the legacy permission. The sheet is opened by the photo and PDF viewer OCR actions without routing through the app's all-files permission helper, so tapping Save can fail for users without all-files access. Use a scoped-storage destination such as MediaStore/SAF, or guard and explain a supported permission path before writing.

### [P2] Encryption tests do not exercise production AES-GCM implementation

[AesGcmEncryptionTest.kt](/C:/Users/abhis/Downloads/project-base/myapp/app/src/test/java/com/abhisheksati/mydoc/security/AesGcmEncryptionTest.kt) reimplements encryption directly with JCA and the shredding case itself writes zeroes to a temporary file. As written, these tests can pass while the production encryption or shredder code is broken. Add tests that call the production implementation and assert round-trip, wrong-key/tamper rejection, and the actual deletion/overwrite behavior.

## Coverage notes

The full test read covers non-viewer UI, explorer and viewmodel tests, selected OCR/TTS/cleaner/security/PDF-tool tests, and the design-system unit and instrumentation tests. The requested OcrResultSheet, StorageCleanerManager, and PdfSecurityEngine production files were read in full. `StorageCleanerManager` and `PdfSecurityEngine` yielded no additional confirmed defect in this pass. Several Android UI tests outside the read set remain inventory-only; see the exact paths in the JSON. Tests and builds were not run, so static review does not establish runtime behavior.
