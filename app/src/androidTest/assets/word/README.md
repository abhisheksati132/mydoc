# Word regression documents

`fidelity.docx` is a synthetic MyDoc-authored document (python-docx 1.2.0): header, footer, centered blue title, merged table cells and an embedded red raster picture. `template.dotx` changes the OOXML main-part type to a template. `protected.docx` encrypts the same synthetic file using msoffcrypto-tool 6.1.0; its deliberately public test password is `Word-test-42`. No user documents are included.

`sample.rtf`, `sample.fodt`, `sample.odt` and `sample.ott` are minimal MyDoc-authored fixtures containing “Word format fixture”. ODF packages use an uncompressed first mimetype entry and an ODF manifest.

`legacy-image.doc` is LibreOffice's `sw/qa/extras/ww8import/data/image-lazy-read.doc` from tag `libreoffice-26.2.6.3`, under the upstream MPL 2.0 project license. Source: https://github.com/LibreOffice/core/blob/libreoffice-26.2.6.3/sw/qa/extras/ww8import/data/image-lazy-read.doc . `legacy-template.dot` is the same binary under the legacy template extension, testing extension dispatch; it does not establish template-specific behavior. License text is included at `third_party/libreoffice/COPYING.MPL`.
