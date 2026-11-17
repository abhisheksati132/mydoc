# Reader fixture review

Reviewed all 24 tracked files under `app/src/androidTest/assets/` and `app/src/test/resources/fixtures/`.

`fixtures.json` records each path. Binary/container files are marked `structural-only`: ZIP based Office/ODF archives were enumerated and CRC checked, and compound files were identified by container signatures/metadata where available. This confirms recognizable container structure only; it does not establish complete parsing, fidelity, interoperability, or intended rendering. XML, FODT, RTF, and README files are marked `full` because their complete textual content was read; the three chart XML fixtures and `sample.fodt` were also checked for well-formed XML.

## Issues and limitations

- The fixture READMEs describe synthetic coverage and explicitly do not claim exhaustive Office compatibility.
- The `legacy-template.dot` fixture is byte-identical to `legacy-image.doc` with a different extension, per its README. It cannot independently establish template-specific legacy parsing behavior.
- `protected.docx`, `protected.pptx`, and `protected.xlsx` are encrypted compound-file containers. Their internal encrypted package contents were not decrypted or semantically inspected. The README documents the test passwords.
- The `real-chart-*.xml` files are well-formed chart parts with cached series/categories, but the review did not validate their relationship targets or rendering in a workbook/package.
- Archive CRC checks detect damaged ZIP members but do not establish schema validity or reader correctness. No fixtures were executed and no builds/tests were run.
- `third_party/libreoffice/COPYING.MPL` is tracked and referenced by the word fixture README for the upstream legacy document source.
