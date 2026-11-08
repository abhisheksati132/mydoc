# Document fidelity programme

Approved build order: Word → PowerPoint → Excel → remaining supported formats.

| Module | Responsibility | Depends on |
| --- | --- | --- |
| fidelity-corpus | Synthetic source documents, reference exports, content and visual checks | — |
| word-fidelity | Embedded pictures, paragraph/page layout, styles, tables; legacy and OpenDocument separately | fidelity-corpus |
| presentation-fidelity | Slide geometry, themes, fonts, images, charts and diagrams | fidelity-corpus |
| spreadsheet-fidelity | Cell formatting, dimensions, drawings, charts and print layout | fidelity-corpus |
| remaining-formats | Audit each registered ebook, text, image and media route | fidelity-corpus |

## Contract

Keep source files unchanged and processing local. Never execute macros or fetch external image links. Bound decompression, bitmap allocation and page counts. Preserve cancellation and meaningful failure messages. Rendering caches must change version when conversion behaviour changes.

Success means preserving visible source content and comparing supported layout features against reference exports. Opening successfully or extracting text alone does not prove fidelity. Keep the format registry honest about omissions. Exact matching across every Office file is not currently established and must not be advertised.

## First increment: DOCX pictures

Read embedded DrawingML picture relationships and dimensions, preserve picture order relative to paragraph text, render supported raster images in paginated and reflow readers, and retain image-only paragraphs. Show a visible explanation for missing or undecodable images. Floating pictures are initially placed at their text anchor; wrapping, effects and vector formats require further work. Images in tables need explicit verification rather than being silently flattened.

Verification: parser fixtures for mixed text/pictures, image-only paragraphs, relationship resolution and external-link rejection; Android raster/PDF output checks; existing unit suite, lint and debug build. Office reference exports are required before claiming original-layout fidelity.

## Current baseline

DOCX: headings, emphasis, lists and basic text tables; pictures absent before this programme. DOC/DOT: text only. ODT/OTT/FODT and RTF: partial reflow. PPTX family: custom slide renderer with chart/diagram limits; PPT/PPS/POT: text recovery. XLSX/XLSM: styled grid with cached formula values; XLS and ODS families omit significant styling. PDF/XPS/CBZ use an existing fixed-page engine; EPUB/FB2 reflow. These are separate rendering paths, not one parser fix.

Reference: Microsoft Open XML picture structure: https://learn.microsoft.com/en-us/office/open-xml/word/how-to-insert-a-picture-into-a-word-processing-document
Bitmap bounds/sampling: https://developer.android.com/reference/android/graphics/BitmapFactory.Options

## Progress: first Word increment

Implemented DrawingML embedded raster pictures in body reading order, including image-only paragraphs, internal relationship resolution, display extents, alt descriptions and anchor-state recording. The page converter now paints pictures; the reflow reader also paints direct table-cell pictures. Broken/unsupported pictures retain a visible placeholder. Converted tables retain image markers rather than silently losing those cells. Conversion cache uses a new renderer version.

Remaining Word work: complete paragraph/character styles and document defaults, line height and paginated justification, first-line/hanging indents, section layout, headers/footers, floating positions and wrapping, crop/effects, vector and older VML pictures, converted table pictures and legacy/ODF/RTF image support. The new picture code is not a claim that these layouts match Word.

PowerPoint, Excel and the remaining-format audit are queued; their rendering behaviour is unchanged by this increment. Reference exports from Word/PowerPoint/Excel or LibreOffice and screenshot comparison have not yet been established.

## Opening reliability

A real device check reproduced simultaneous thumbnail/reader conversion requests deleting each other's shared staged Word PDF, forcing reflow fallback. A regression launches eight opens together. Conversions now serialize within each Office family so both the PDF and its outline/note sidecars publish consistently; waiting Word/Excel requests are cancellable. This bounds active conversion memory but can queue distinct files behind earlier conversions. Revisit scheduling with measured large-library latency before claiming a performance improvement.

## Verification for this increment

503 JVM tests passed, including eight new DOCX picture regressions. Four API 35 device tests passed: picture pixels in a generated PDF, invalid-image fallback, bounds-first sampling, and eight simultaneous Word conversions. The concurrent-open regression failed before the fix (seven of eight requests failed) and passed afterward. A synthetic DOCX was opened manually in both the paginated and reflow readers and its picture was visually inspected. These checks do not establish exact layout fidelity across real Office documents.

## Engine direction to investigate

Achieving broad Office layout fidelity may require a mature layout engine instead of extending these simplified readers indefinitely. LibreOfficeKit provides native loading and rendering APIs and has an Android integration; feasibility for MyDoc remains unproven. Evaluate Android ABI builds, package size, startup/memory use, maintenance and licensing before introducing it. Do not upload user files or add an external conversion service as a shortcut.

- https://docs.libreoffice.org/libreofficekit.html
- https://docs.libreoffice.org/android.html

## Second Word increment: paragraph geometry and page breaks

Scope: DOCX/DOTX left/center/right alignment, paragraph spacing before/after, left/right indents, explicit page breaks and page-break-before. Resolve nearest named paragraph styles through bounded, cycle-safe inheritance; direct zero and false values override inherited properties. Carry these properties on headings, list items, paragraphs and pictures. Converted pages overlap adjacent paragraph spacing rather than adding both gaps. Reflow adapts source indents and spacing to a phone and does not reproduce fixed page boundaries.

Preserve manual line breaks separately from page breaks and retain empty paragraphs as vertical space. A list item starting after a page break must keep its marker. Refresh the conversion-cache version so prior layouts are rebuilt.

Still out of scope: full line-height rules, paginated justification and bidi paragraph direction, document defaults/default styles, first-line/hanging indents, section/column breaks, contextual/automatic spacing, table-cell paragraph geometry and floating layout. Inline pictures/text still reflow into separate blocks. Source files remain unchanged.

Reference: https://learn.microsoft.com/en-us/office/open-xml/word/working-with-paragraphs

The shared rendering notice now has a dismiss control, because its text can obscure opening paragraphs in reflow at large text sizes. Dismissal affects only the notice: Document details retains the full limitations, and a different document or changed disclosure shows its notice again.

Manual follow-up: changing the system text size while an externally opened file was active returned to Home; Resume reopened the file. Navigation restoration for this configuration-change path remains unverified/unfixed. Reflow's navigation count follows content blocks rather than original source pages. Viewer footer labels also need further large-text layout work.

Validation: 513 JVM tests passed, including ten paragraph-layout regressions. Seventeen focused API 35 device tests passed: thirteen raster/PDF/opening tests and four notice-interaction tests, including double text size and RTL. Pixel checks cover centered text and pictures, overlapping paragraph spacing, manual line breaks and blank-line height. A two-page synthetic DOCX was inspected manually, including inherited centering and the right-aligned/indented second page. No Office reference export comparison or physical-device performance claim is made.
