# Changelog

All notable changes to MyDoc. Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
versions follow the `versionName` in `app/build.gradle.kts`.

## [Unreleased]

### Added

- Word DOCX/DOTX embedded DrawingML raster pictures render in paginated and reflow views; image-only paragraphs are preserved, unsupported DrawingML pictures show a placeholder, and direct table-cell pictures appear in reflow. Floating wrapping, picture effects, older VML pictures and converted table pictures remain limited.
- Quick preview in file and folder action menus, with a large thumbnail, full name and location, format and metadata, plus Open and More actions. Available in list and grid views.

### Fixed

- Office thumbnail and viewer conversions no longer run concurrently within the same format family, preventing shared temporary PDFs from interfering with document opening.

## [1.9.5] — 2026-11-07

### Changed

- Transfer history uses status cards, shows progress only while an operation is active, and explains the next step after stopping.
- Transfers can open their destination folder in the active tab or focused pane.
- Grid previews expose named per-file actions, and the selection bar offers direct Copy, Move and Share controls that wrap with larger text.
- Single-file actions consistently use Move instead of Move (Cut).
- File rows show two-line names and wrapping metadata; grid cells grow with text size and use wider preview proportions at larger text.
- Home groups Browse files and preview controls together and lets them wrap below the recent-files heading.
- File browsing shows the active sort field and direction in a labelled control with its menu anchored beside it.
- Tools section headings align with cards and the first section has tighter spacing below the category selector.

### Fixed

- Missing transfer destinations report how to recover while preserving the current view; stale destination checks cannot override newer navigation.
- Single-file Copy/Move targets exactly the requested file without toggling it out of an existing selection or including unrelated files.
- A busy transfer keeps its clipboard and the current selection intact and reports that a transfer is in progress.
- Folder loading no longer exposes stale rows or presents an empty folder before loading finishes.
- Empty searches offer a scrollable Clear search and filters action that restores the current folder or category.
- Folder and category list/grid positions survive recreation while newly opened sources start at the top.
- Delayed Up navigation no longer replaces a newer folder or category request.

## [1.9.4] — 2026-11-07

### Fixed

- Recent files no longer keep selections for files that have left the list, and switching Home tabs settles cleanly on a page instead of freezing between pages — including after rotation mid-scroll.
- Pull-to-refresh keeps the current file category; opening a folder starts at the top; filter chip counts survive refreshes.
- Moving or renaming within the same storage volume no longer fails a free-space check, and case-only renames are accepted without false name conflicts.
- Folder details no longer show stale replies from a previous folder, and folder statistics no longer jank the list while computing.
- Settings and Search states, open dialogs and the text typed into them survive rotation and other recreation.
- Spotlight's Media filter covers every supported media extension, and Tools search only takes keyboard focus on the visible tab.
- Thumbnails refresh when a file's content changes.

## [1.9.3] — 2026-11-07

### Changed

- Home groups the recent-files view switch with its section controls.
- Settings category pages display their heading once.
- Tools category navigation uses a compact menu with an explicit selected state.

### Fixed

- Searching within a tool category shows a recovery action when matches exist only in other categories.

## [1.9.2] — 2026-11-07

### Changed

- Recent files use compact rows by default, with optional previews and preserved selection, share and details actions.
- Library prioritizes search and pinned locations, with a concise storage summary.
- Indexed and folder searches share an accessible field and show their scope explicitly; advanced search is available beside the folder query.
- Workspace commands have their own menu, and clipboard instructions name the destination folder.
- Settings is organized into six categories with Back navigation and saved scroll positions; update checks remain available in About.
- Tool categories are fully visible and descriptions can wrap to two lines.

### Fixed

- File search keeps its accessible name while typing, and file-row option buttons identify the target file.
- Home headings, Settings explanations and choice controls reflow with larger text.
- Compact thumbnail badges use file-type icons at larger text sizes to prevent clipped lettering.

## [1.9.1] — 2026-11-07

### Added

- Open individual ZIP files as temporary copies and extract selected entries into a new folder, with checksum, size and stale-archive checks.
- Named saved searches reopen folders and filters, with relative date windows and local persistence.
- Folders remember their sort order and list/grid layout across tabs and restarts, with a reset option.
- Read-only folder comparison across open tabs, with streamed content checks, missing-file detection, and explicit unknown results for incomplete scans.

### Changed

- Refined Ink & Seal navigation with restrained optical glass, equal-width tabs, visible focus, and opaque contrast fallbacks.
- Simplified the single-tab Library header and measured floating-navigation clearance for lists, clipboard actions and snackbars.
- Glass responds to power/contrast changes; reduced motion also applies to root-tab transitions.

## [1.9.0] — 2026-11-06

### Added

- Browse ZIP folders and file sizes before choosing to extract the archive.
- Folder insights show total content size, immediate subfolder sizes, and the 20 largest files, including hidden files and marking incomplete scans.
- A shared transfer center tracks copy/move progress, session history and actionable errors across tabs. Stop finishes the current item safely; remaining items stay on the clipboard.

## [1.8.0] — 2026-11-06

### Added

- Up to six independent folder tabs and side-by-side browsing on wide screens, with a shared copy/move clipboard.
- Advanced folder search combining name, extension, size, modification date, and optional subfolders.
- Batch rename preview with extension preservation, numbering, conflict checks, rollback, and interrupted-operation recovery details.

## [1.7.6] — 2026-11-06

### Fixed

- **Reader sessions no longer publish a document the reader already left.** Every load carries a
  generation stamp; a slower load's renderer, page count and error are dropped instead of replacing
  the newer one, and password prompts reset per document.
- **`xls` rich and phonetic strings no longer shift the string table.** `cRun` is a 16-bit count and
  `cbExtRst` a 32-bit byte count; reading them as single bytes made one bold cell corrupt every
  later string in the sheet.
- **A scanned page among selectable ones is no longer dropped.** PDF text and Word export OCR each
  page with no text layer instead of OCR-ing only when no page anywhere has text.
- **RTF toggles mean on when they carry no parameter.** Reading the missing `b`/`i`/`ul` parameter
  as zero removed every bold, italic and underline in the document.
- **A stale folder reply can no longer replace the folder on screen.** Directory and category
  requests carry a generation stamp.
- **Bytes that are not a presentation do not become invented slides.** The byte-scrape fallback now
  runs only for a CFBF container, and the viewer marks that result as recovered.
- **Cancelled archive extraction rolls back before it returns**, and a partly failed permanent
  delete reports its completed and failed counts instead of announcing the whole batch as deleted.
- **The Library no longer hides one of two files that share a name and a size.** Deduplication uses
  the URI and real path, not name plus byte count.
- **`.xlsx` fallback paths resolve once** (a relationship that already names `xl/...` no longer
  becomes `xl/xl/...`), and a sheet whose columns run past the grid's limit says so instead of
  looking complete.
- **Sign PDF's "Last Page" is the last page.** It was a fixed sentinel of 999, which signed page 1000
  of a longer document; the bottom presets were also flipped because PDF y grows upwards.
- **A page with an oversized box is rendered smaller instead of failing.** Both PDF engines cap a
  single page bitmap at 24 Mpx, so a pathological MediaBox no longer asks for a multi-gigabyte
  allocation and then leaves a spinner behind.
- **Saving a tool's output no longer copies on the main thread**, so the app stays responsive while
  a large result is written to the destination the reader picked.
- **A damaged presentation says so instead of opening as an empty deck.** The native parser's
  failure used to be logged and discarded, leaving a blank slide counter; it now reaches the error
  state, which clears again if the `.ppt` recovery produces slides.
- **Every Office viewer reads under the same 64 MB budget.** The presentation viewer staged its copy
  with an unbounded `copyTo`, so a stream that lied about its size could fill the cache directory;
  the copy now stops at the budget and reports why.
- **PDF page geometry is resolved off the main thread.** Each scrolled-in page read the cached size
  or opened the page while composing, behind a render holding the renderer lock, so the first frame
  of a new page could stall.
- **Settings measures and clears the cache off the main thread**, and says "history cleared" only
  after the database work has returned rather than when its coroutine starts.
- **A Quick Scan launched from a cold start opens the scanner.** The launch intent was handled before
  the event flow had a subscriber, so the request was dropped.
- **Read-aloud splits over-long sentences and reports engine refusals.** A sentence past the chunk
  limit (a URL, a run of CJK) went to the speech engine whole, an empty chunk could be queued, and a
  synchronous `ERROR` left the UI showing "playing".
- **The storage cleaner no longer claims space back it has not freed.** Its files go to the Bin on the
  same volume, so the completion message now says how much is reclaimable once the Bin is emptied.
- **Image to PDF accepts several images at once**, and an image it cannot decode is named in the
  result instead of being silently dropped from an otherwise "completed" PDF.
- **Exported APKs no longer collide or leave partial files.** The name now includes the package, and
  the export is staged and then published under a free name.
- **A favourite past the 500th is still shown as a favourite.** Membership is read from its own query
  instead of the paged favourites list.
- **Reader chrome stops offering controls that do nothing.** Pages is disabled when a viewer has no
  page grid, and a bookmark tapped without a per-viewer handler now lights the icon.
- **The page organizer renders a thumbnail whenever its tile is on screen.** It pre-rendered every
  page and kept only the last 35, so earlier tiles stayed blank for the rest of the session.
- **Markdown search lands on the match it counted.** Markdown drops blank lines, so the search and
  scrubber worked in line numbers while the list scrolled in item numbers; the two are mapped now.
- **A renamed archive entry is rolled back with the rest.** Under the rename collision strategy the
  new file was written beside an existing one, and checking the original name left it out of the
  rollback list.

- **A partial conversion says what it left out.** A deck past the 500-slide layout cap and a Word
  table past 24 columns now produce a note in the reader's fidelity strip instead of arriving as a
  complete-looking document; the note is cached beside the PDF so it survives a reopen.
- **The storage breakdown describes the volume it belongs to.** Category totals are scoped to the
  selected volume's `VOLUME_NAME`, the app footprint is only counted on internal storage, and the
  cleaner names the root it will scan.

- **Screen titles and Settings groups are TalkBack headings.** The bar title and every settings card
  title carry heading semantics, so a screen reader can jump between them.

- **The UI copy now comes from string resources.** Every screen's titles, action labels, content
  descriptions, empty states, dialogs and progress text read from `strings.xml` (5 entries at the
  start of this work, 779 strings and 6 plural sets now), including the 24 PDF tool names and
  descriptions, storage category and volume names, and plural-aware counts. Still English by design
  or for a later pass: the `DocumentFormats` limitation notes, the relative-date formatter in the
  domain models, the "Slide N" titles the PPTX parser writes into a converted document, and the
  debug-only design-system preview screen. Failure and status messages travel from the ViewModel as
  `UiText` (a resource id plus arguments) and are resolved where a configuration exists, so error
  copy is localisable without giving a ViewModel a Context.

### Known remaining (not addressed in this release)

- Word page furniture: `.docx` headers, footers and sections after the first are still not drawn,
  and a page break is still our re-pagination rather than the file's.
- Plain text and Markdown past the reading budget are truncated with a notice and no way to load
  the rest of the file in the same session.
- The localisation residuals named above, and accessibility beyond the added headings (roles,
  content descriptions on custom gestures, contrast and target sizes), are not part of this pass.
- A `.doc` whose piece table cannot be read falls back to a byte scan; the disclosure headline
  says "Recovered text", but the format's limitation note still describes the piece-table path.

## [1.7.5] — 2026-11-06

### Fixed

- **PDF search returned nothing.** `ViewerViewModel` passed `isActive` where the renderer's
  parameter asks "should I stop?", so the first page check cancelled every ordinary search.
- **Select All could act on files a format chip had hidden.** The chip filtered only inside the
  composable; selection, the item count and the operations read the unfiltered list. The filter now
  lives in the ViewModel, so the rows drawn, the count and Select All are the same projection.
- **Two same-named files shared together arrived as one.** Both sharing paths staged every
  attachment at `cache/shared/<basename>` with overwrite on; each attachment now gets its own
  directory, and an earlier grant cannot read a replacement through the same URI.
- **Open in Office App could send the previous presentation.** The handoff reused any non-empty
  file with the same name and never refreshed it; it now stages per handoff and always writes the
  bytes being viewed.
- **"Move to Recycle Bin" could permanently delete a photo.** A provider-backed photo with no
  filesystem path fell through to `ContentResolver.delete()`. MyDoc now refuses the operation and
  says nothing was deleted.
- **Compressing a folder could overwrite a hidden file.** Staging used a predictable
  `.name.tmp` opened in truncation mode; it is a unique file per run now.
- **`.xls` integer cells decoded wrongly.** The RK flags were swapped: bit 0 is fX100 (divide by
  100) and bit 1 is fInt (signed 30-bit integer). Both RK and MULRK use the corrected decoder, and
  the test now encodes the spec's flags instead of repeating the old mistake.
- **`.ods`/`.fods` repetition changed values and coordinates.** A repeated cell is that value in
  every column it spans, and a repeated row is that row N times; the reader stored one value,
  clamped column runs to 100 and dropped row runs. Both are reproduced now, bounded to the grid
  the reader can show.
- **A Word token wider than the line lost its tail.** A long URL, identifier or CJK run was trimmed
  to a prefix with the remainder discarded while the conversion still reported success. It wraps
  across lines now.
- **Untrusted headers could request disproportionate allocations.** The OLE2 FAT array is sized
  from a header field and is now capped by the file's own size; the cached PPTX route applies the
  same entry-count and aggregate budgets as the in-memory route.
- **A 180-degree image rotation produced a blank image.** The matrix was never translated for that
  turn, so the whole image was drawn at negative coordinates.
- **Smaller honesty fixes.** The delete dialog no longer promises permanent deletion for a move to
  the Bin, the page-jump field waits for submission instead of jumping on the first digit, Crop can
  run with a blank range as its copy says, and the PDF password fields are masked.

### Known remaining (1.7.4 audit, not fixed in this release)

- Word: table columns beyond 24 are dropped, rows are not split across pages, and (`.docx`) page
  furniture is still not rendered.
- `.xls` rich/phonetic shared strings are still parsed with the old field widths; the ODS limit
  above is bounded rather than complete; `.xlsx` `date1904` workbooks still display as 1900-system.
- Export/to-PDF: mixed searchable/scanned pages are still OCR'd all-or-nothing, the long-image
  pixel budget is not enforced, and skipped inputs are not reported.
- Reader sessions: two loads can still race (no generation guard), and cancellation is not checked
  inside the long parse loops.
- URI fallback in `DocumentStreamResolver` still reinterprets opaque `document:`/`primary:` ids.
- Accessibility (headings, roles, sub-48dp targets) and localisation (437 hardcoded strings) are
  unchanged; `context.md` and `improvement.md` are marked historical instead of rewritten.

## [1.7.4] — 2026-11-06

### Changed

- **The app's own text is no longer bold.** Every chrome role in `AppTypography` - the Fraunces
  display/title/heading/document-name/screen-title/wordmark tier and the Inter `*Strong` and overline
  roles - now renders at the regular weight, and the three remaining hardcoded weights in the tools
  panel and the spreadsheet column header went with them. Hierarchy is carried by size, colour and
  spacing instead. `Medium` (500) is kept where a role already used it, which is not a bold. What a
  *document* says is untouched: bold runs in Word, PowerPoint and Markdown still render bold, because
  that weight belongs to the file and not to MyDoc.
- **Video playback is fullscreen.** The status and navigation bars are hidden while the controls are
  hidden or the screen is locked, come back with the controls, and are restored when the player is
  left; a swipe still brings them transiently. The rotation button no longer announces
  "Landscape"/"Portrait"/"Auto Rotate" as a popup, and both control bars now pad for the display
  cutout, which in landscape sits on the side rather than the top.
- **The playback speed menu fits landscape.** It opened as a half-expanded sheet, which in landscape
  is about a fifth of the height it gets in portrait, so the lower speeds sat below the fold with no
  obvious way to reach them, and the side navigation bar overlapped the list. The sheet now opens at
  its full content height, scrolls if the content is taller than the space left to it, and pads for
  the navigation bar and the cutout. Verified on `mydoc_api35` in landscape: all six speeds from
  0.5x to 2.0x are on screen and clear of the system bars.

### Added

- **A binary `.doc` is read through its piece table.** Word 97 stores a document's characters in
  pieces described by a table in `0Table`/`1Table`, pointed at by the FIB in `WordDocument`; each
  piece says where it lives and whether it is Windows-1252 or UTF-16. `Word97Text` follows that now,
  so a legacy document arrives in document order with accented and non-Latin text intact, its
  paragraph and cell marks turned into structure and its field marks gone. The byte-pair scan stays
  as the last resort for a container that is not a Word document at all, and still flags itself as a
  scrape. Tested against a document Word itself saved as ".doc".

- **`.xls` is decoded as BIFF8 instead of being scavenged.** A legacy workbook is a CFBF container
  with a `Workbook` stream of records inside it, and MyDoc had never opened it: the engine scanned
  the whole file for UTF-16 word pairs and dropped whatever it found into column 0 of a sheet named
  "Extracted Data", which lost every number and every cell address. `Biff8Reader` walks the records
  now - shared string table (with CONTINUE, compressed and 16-bit text, rich and phonetic runs),
  the sheet directory, LABELSST / LABEL / NUMBER / RK / MULRK / FORMULA+STRING / BOOLERR, merged
  regions - and the grid shows cells at their real row and column. The test fixture is a workbook
  Excel itself saved as "Excel 97-2003", not records assembled by hand.

- **`.fb2` opens instead of being refused.** FictionBook2 was in the list of containers MyDoc
  deliberately refuses, which was wrong: the MuPDF we already ship contains an FB2 refiner
  (`fb2_document_handler`, the `application/x-fictionbook` registration and the FictionBook stylesheet
  are all in the bundled native library). `.fb2` is now routed the same way `.epub` is — MuPDF lays
  the book out, we do not — and it appears in the library, the folder browse, the category chips, the
  recents badges and the `ACTION_VIEW` filter. Verified on `mydoc_api35` with a two-chapter FB2: the
  prose, the centred chapter titles, the quotation block, the poem and the outline all come through.
- **`.docx` footnotes and endnotes are read.** `word/footnotes.xml` and `word/endnotes.xml` are now
  among the parts the reader opens, a `w:footnoteReference` puts the number Word shows into the text,
  and the note's own sentences are collected at the end of the document. They used to be dropped
  silently: a document's citations survived as a number with nothing behind it.

- **Charts on a slide are drawn instead of skipped.** A chart in a `.pptx` is a part of its own that
  carries its own values (`c:numCache` / `c:strCache`), so a viewer needs neither the workbook behind
  it nor a calculation engine. `ChartModel` reads vertical bar groups - clustered, stacked and
  percentage-stacked - line groups and pie groups, including the 3-D pie, whose tilt is dropped but
  whose slices are the same data, and `ChartPainter` draws them into the rectangle the author gave
  the frame: gridlines with their values, bars, polylines with markers, slices with their share, and a
  colour key. What cannot be laid out truthfully stays blank rather than being improvised: a
  horizontal bar chart, a stacked line, a combo plot area with more than one group, and every other
  kind return nothing, because half a chart is the wrong chart. The colours are the painter's own
  sequence rather than the deck's theme, and the `pptx`/`pptm`/`ppsx`/`potx` capability notes now say
  exactly that. Verified on `mydoc_api35` against a deck PowerPoint itself wrote (bar, pie and line on
  three slides) with instrumented tests that count the series colours in the rasterised page and a
  control deck whose chart part is missing; the reader is also tested against the three chart parts
  taken out of that deck.

- **SmartArt gives up its text.** A diagram on a slide keeps its words in `ppt/diagrams/dataN.xml`,
  one `dgm:pt` per node, each with its text in a `dgm:txVar` - and MyDoc drew nothing at all, because
  the shapes come from a layout gallery the package does not carry. The nodes are read now and their
  text is set out in bands inside the frame the author gave the graphic, in the order the model lists
  them: the `def` variant rather than the shorter one written for a narrow layout, `proc` nodes
  included, the document node and the transition nodes left out, and a node with nothing in it taking
  up no line. **The fixture is written to ECMA-376 Part 1 rather than taken from a deck**: this
  machine's Office has no SmartArt gallery installed (`SmartArtLayouts.Count` is 0, and
  `AddSmartArt` fails), so no Office-authored diagram exists to check against here, and the shapes
  side of a real graphic remains unverified.

- **A binary `.ppt` is read as the record tree it is.** PowerPoint 97-2003 writes one stream of
  records whose headers say which records are containers, and the slides are `RT_Slide` containers
  holding their own text. MyDoc scanned the stream for bytes that look like a header instead, which
  cannot tell a slide from the master it was made from: the master's "Click to edit Master title
  style" and its bullet levels arrived as slide text, a three-slide deck came out as two, and the
  record numbers the scanner compared were not the ones PowerPoint writes. `Ppt97Records` walks the
  tree now, and takes each block from the bytes atom in Windows-1252 or the wide atom in UTF-16,
  depending on which of the two PowerPoint wrote for that text. Verified against two decks PowerPoint
  itself saved as ".ppt": the three-slide one, and one whose title needs `é`, `ï` and an em dash and
  whose body needs Japanese - the case that makes PowerPoint switch encodings.

### Fixed

- **The Word reader stops losing structure it was never able to represent.** `parseDocumentXml` was a
  flat loop over one shared state, so an inner `w:tbl` called `currentTableRows.clear()` and destroyed
  the table around it, every numbered list rendered as bullets, and a `w:hyperlink` kept its text but
  threw away its target. It is now a recursive-descent reader over a model that can hold the
  differences: table cells contain blocks (and therefore tables), list items carry the marker Word
  computed from `word/numbering.xml` at their level, and runs carry the link they came from.
- **Headings are found through the style chain, not through English style ids.** `w:pStyle` names a
  style *id*, and a localised template spells it however it likes (`berschrift1`, `a5`); the level is
  in `word/styles.xml` as `w:outlineLvl`, inherited through `w:basedOn`. Heading 4-9 no longer fall
  through to body text, and `<w:b w:val="0"/>` now switches bold *off* instead of on.
- **A Symbol-font bullet is drawn as a bullet.** Word writes bullet levels as a private-use glyph
  (`U+F0B7` for Symbol), which is a font instruction rather than a character: with the document's own
  typeface the reader saw an empty box. Undrawable markers fall back to `•`, and a bullet the author
  really typed - `-`, `o`, `▪` - is left alone.
- **The converted page no longer invents spaces.** The PDF wrapper joined every run with a space of
  its own, so `…endnote` + `[1]` + `.` became `…endnote [1] .` on the page. It now inserts a space only
  where the document wrote one, and a linked run is drawn underlined and tinted like a link.
- **A chart part no longer has to be beside its slide to be found.** `ppt/charts/` was not among the
  entries the presentation package reader kept, so even a correct chart reference resolved to nothing
  and the frame stayed empty. The reader's own cache lookups were one level too shallow as well:
  Office writes `c:val` → `c:numRef` → `c:numCache`, and a direct child never reached the values.
- **The format table said `.fb2` was refused after it had started opening.** The capability registry,
  the library and the viewer were changed for `.fb2`; the README's "Refused rather than scraped"
  sentence was not, so the documentation claimed a limitation the app no longer had.

### Notes

- The Word viewer's fidelity notice covers the first blocks of a reflowed document while the viewer
  chrome is up, because that surface never hides its chrome. Longer disclosure text makes it cover
  more. Pre-existing, unchanged by this batch, and filed as its own fix.
- The same overlay hides the first lines of a converted slide now that SmartArt text is drawn: on
  `mydoc_api35` the notice sits over the top band of the diagram, and only the lower nodes are
  visible while it is up. The instrumented tests check the rasterised page rather than the screen for
  that reason, and the fix is the same one (#175).

## [1.7.3] — 2026-11-05

### Fixed

- **CI could not build at all on Linux: the dependency pins were Windows-only.** The
  `gradle/verification-metadata.xml` added in this cycle was generated on a Windows machine, so the
  one artifact in the graph that carries an operating-system classifier -
  `com.android.tools.build:aapt2:9.5.0-16666295` - was pinned only as
  `aapt2-9.5.0-16666295-windows.jar`. On the GitHub runners Gradle resolves the `-linux` variant,
  found no checksum for it, and failed the first task that touches the Android tooling classpath
  (`Dependency verification failed for configuration ':app:detachedConfiguration1'`), which took down
  both CI jobs before any test ran. The Linux artifact is now pinned with its own SHA-256, measured
  from Google's repository (2,451,278 bytes, `fa1014cb1431622d292ef55ba870e75882ea19987bec541698b912f446a2d08f`).
  Nothing was relaxed: metadata verification and signature checking stay on, and `aapt2` is the only
  OS-classified component in the resolved graph (checked against the whole pin file).
- **The dependency pin file only looked complete.** `gradle/verification-metadata.xml` had been generated
  on a machine with a warm Gradle cache and a populated PGP keyring, and CI's build job reuses a cache that
  predates the pins, so neither environment ever demanded an entry for what a *cold* build needs: with an
  empty Gradle home and no keyring, 48 artifacts failed verification. The release runner hit the first
  uncached one (`org.json:json:20260814`), and pinning that jar by hand only exposed fifteen more - all of
  them components the file *trusts a PGP key for* but which no runner can resolve a key for, because the
  keyserver is unreachable from CI and a checksum does not discharge an expected signature. The fix is the
  one Gradle provides for exactly this: `./gradlew --export-keys` now ships `gradle/verification-keyring.keys`
  and `.gpg` (63 keys) in the repository, so signatures are verified offline against pinned keys, and the
  fifteen jars the cold run still had no checksum for were measured from `repo1.maven.org` and cross-checked
  against Maven Central's own published SHA-1 before recording. The pin file went from 234 to 341 components
  and 393 to 500 artifact entries, and the release job now points every Gradle call at an empty
  `GRADLE_USER_HOME`, so a missing pin fails the release build instead of hiding behind a cache.
  Nothing was weakened to get green: `verify-signatures` stays on, `ignoredDependencies` is still empty, and
  no checksum was removed.

- **The release workflow's own per-ABI verification step failed the v1.7.3 tag run.** The step
  added to prove that arm64-v8a and armeabi-v7a are no longer published unchecked classified each
  artifact by matching its *filename* against an ABI name, with a catch-all that assumed anything
  unrecognised must have no native libraries. `app-universal-release.apk` is not named after an ABI,
  so the one artifact most users download - and the one that does carry all three - was rejected with
  "carries no native libs at all" before the release was published. The classifier now checks a split
  against the ABI in its name and the universal APK against all three, fails an empty library list on
  its own message, refuses an unknown artifact name, and no longer relies on `unzip`'s internal
  `lib/*` pattern (which matches nothing on some Info-ZIP builds: measured here, the entry list came
  back empty for an APK that does contain `lib/arm64-v8a/libmupdf_java.so`). The rewritten step was
  run against the four real APK layouts this project produces - three splits and the universal, which
  also carries x86 - and against six name/ABI combinations including each way it can fail.
- **"Timeout waiting for emulator to boot" was not flaky infrastructure - it was `-accel off`.** Measured
  from the runners' own logs: the release job launched
  `emulator -port 5554 -avd test ... -accel off` and printed `ProbeKVM: This user doesn't have
  permissions to use KVM (/dev/kvm)` - the device node is `crw-rw---- root kvm` and the CI user is not in
  that group - so an API 35 x86_64 image was booting in software emulation and never came online (the
  collected diagnostics show `adb: device offline` for the whole window). The build workflow's
  instrumented job had the udev rule that grants KVM all along, which is why it boots in about 40 seconds
  while the release job timed out on every attempt, including the v1.7.2 run that started this. The
  emulator action's own `enable-kvm` input does not do it - with that input set the launch line still came
  out `-accel off` - so release.yml now carries the same `Enable KVM` step, and the judge step's advice
  tells you to read the launch line for `-accel off` before retrying, because a retry cannot fix a missing
  permission.
- **Three tests failed on faster hardware because they measured the clock, not the behaviour.**
  `PdfSearchCancellationOnDeviceTest.renderingIsNotHeldBehindTheWholeDocumentSearch` demanded that a
  1,200-page scan take more than 1,000 ms before it would judge locking strategy; CI measured
  502/694/853/873 ms across runs, so the guard fired and the job went red while the lock discipline was
  correct. `DocumentSiblingsOnDeviceTest.theScanLeavesTheMainThreadFree` had the same shape
  (`scan took 239/255/390 ms, too fast to judge` against a 400 ms floor). Both now prove the property by
  ordering: the search test asks for a page *from inside the cancellation predicate*, which runs outside
  the rendering lock, so it completes only if the lock is taken page by page; the sibling test posts work
  to the main looper once the scan is under way and requires that work to run while
  `readableDocumentsIn` is still suspended. The third was a JVM test,
  `TextViewerModelTest.aLongReadStopsWhenTheCoroutineIsCancelled`: it slept 60 ms, cancelled a
  300,000-line read and asserted the loader had noticed - on CI's faster runner the document was already
  fully loaded inside that sleep. It now cancels the loader's own job from inside the stream's third
  chunk read, so the cancellation provably lands while the document is mid-read, and it asserts both that
  the loader noticed and that it did not run to completion. Each of the three was checked against the
  defect it exists to catch - holding the render lock for the whole scan, running the sibling enumeration
  inline on the main thread, and deleting the read loop's `ensureActive()` checkpoint - and failed in each
  case, so none of them passes by being weak.
- **Moving a folder could destroy the only complete copy (H-1).** The cross-volume
  fallback in `StorageRepositoryImpl.move()` copied with `copyRecursively()`, then deleted
  the source tree with `deleteRecursively()`. A copy that threw part-way through left a
  truncated destination, and a delete that failed part-way through left a source that no
  longer matched it — the two branches then discarded the destination and reported plain
  failure, so the user's files existed in neither place intact. Move now copies to a
  staged destination, verifies it entry by entry against a snapshot of the source, and
  only then deletes the source one entry at a time, deepest first. The destination is
  discarded *only* while the source is provably byte-for-byte untouched; anything else
  keeps both copies and reports `PartialStorageOperationException` with the names of the
  entries still in their original location.
- **Trash and restore had the same defect (H-1).** `moveToTrash()` staged the same way.
  `emptyTrash()` cleared every `TrashEntity` row whether or not the corresponding content
  was deleted, so a refused deletion left an invisible file on storage with no record to
  retry it; rows now go only with content confirmed gone. `restoreFromTrash()` returned
  success for a bin entry that no longer exists and kept going after the bin volume
  became unreadable; the two cases are now distinct — an unreadable bin fails early and
  keeps every record, a missing entry is counted as a failure.
- **"Moved N items" counted the request, not the result (H-2).** The message used
  `files.size` while the repository returned the number actually trashed, so a partially
  failed batch announced more success than had happened. It reports `trashed.size`.
- **Partial failures were reported as unqualified success (H-3).** `UserMessages` appends
  which items were retained where they were, so a partial move says what is still in the
  original location rather than implying the operation completed.
- **Release smoke test failed with an unlabeled infrastructure error.** The emulator boot
  timeout was the action's 600 s default while an API 35 system image may need downloading
  mid-step; it is now an explicit 2 400 s. A failed smoke no longer looks like a product
  defect: a judge step distinguishes "Timeout waiting for emulator to boot" (re-run the
  job) from a `FATAL EXCEPTION` in logcat (this build is broken), and a failed smoke
  still blocks publication.
- **Protect PDF, Unlock PDF and two Compress levels never worked.** MuPDF's writer rejects option
  tokens it does not know, and three strings this app sent were never valid options:
  `encryption=aes-256,…` (the key is `encrypt=`), `encryption=none` and `deflate`. Every call threw
  `Unused pdf arguments found` in the native layer. Protect and Unlock reported that as a failed
  operation; BALANCED and STRONG compression caught it, set `cleanSucceeded = false` and carried on
  with a different strategy while still reporting a compaction.
- **PDF permissions were written but not denied (N-8).** The `/P` mask started from `-64` and OR-ed
  in what the user allowed, so unchecking *Allow Printing* left bit 12 (full-resolution print)
  granted and unchecking *Allow Copying* left bit 10 (extraction for accessibility) granted. The
  mask is now built in `PdfPermissions` from the unrestricted base `-4` by clearing the bits the
  user denied, with the bit table documented against ISO 32000-1 Table 25.
- **A permission that was written was not a permission that applied.** Opening a PDF with the owner
  password grants every permission whatever `/P` says, and the tool copied the user password into
  the owner slot whenever the user left it blank — so both switches were decorative. An unset owner
  password now gets a random one, and the saved file is reopened and asked what it allows
  (`Document.hasPermission`) before the tool claims success; if the answer disagrees with the
  switches, the operation fails instead of reporting a protected document.
- Unlock removes encryption by writing the opened document with `encrypt=none` instead of rebuilding
  it page by page, which also keeps the outline, form fields and annotations the graft loop dropped.
- The Protect sheet states that permissions are a request other readers honour and do not stop
  someone printing a picture of a page.

- **A damaged DOCX spun forever (N-24).** In `WordViewerScreen`, the branch catching a typed
  `DocumentLoadException` set the message and the failure kind but never cleared `isLoading`, so a
  half-written archive left a spinner over an empty page and the problem screen was unreachable.
  Loading now goes through `WordDocumentLoader`, which always returns a terminal outcome and never
  turns a cancellation into a failure; the viewer clears the spinner for every outcome.
- **A locked PDF could be reported as a broken one (H-4).** The viewer's fallback engine, Android's
  `PdfRenderer`, cannot open an encrypted file under any password and reports zero pages, which the
  page-count gate read as corruption. The order is now stated in `PdfOpenDecision`: protection is
  established before page counting is trusted, and a file whose trailer carries `/Encrypt` but that
  no available engine can unlock is reported as password protected, with the reason, not as damaged.
  Every early return closes the engine it is abandoning.
- **Unlocking a protected PDF erased the reading position.** `authenticatePassword` wrote page 0 as
  the progress of the document it had just opened. It restores the saved page, clamped to the real
  page count, and prefetches around it like any other open.
- **Search next and previous did not move the reader (N-26).** Both the text and Word viewers
  advanced an index and re-rendered the counter while the document stayed exactly where it was. The
  controls now scroll to the matched line or block, and the block or line holding the current match
  is tinted so "3 of 7" points at something.
- **Word search renumbered the document and corrupted what was saved (N-28).** Searching filtered
  the block list, so the reading position persisted while searching was a different block's index
  once the search closed. The block list is no longer filtered: matches are occurrences counted across
  runs and tables, and progress is written against the whole document.
- **The text viewer read files of any size into memory (N-27).** `readLines()` on an unbounded stream
  copied a whole log into the heap and then copied it again for read-aloud. There is now a stated
  budget — 8 MB, 50 000 lines, 10 000 characters per line — read in chunks, cancellable, decoding
  malformed UTF-8 rather than failing on it, and a reader who hits the limit is told they are looking
  at a prefix instead of being shown a partial file as the whole thing.
- **Opening fifteen PDFs kept fifteen native documents alive (N-25).** Both `ViewerViewModel` and
  `SpreadsheetViewModel` were keyed by document URI, so each opened file got its own instance and the
  store kept them, with their native MuPDF handles, until the Activity was destroyed. One view model
  now serves the viewer and `RendererSlot` owns at most one engine, closing what it replaces.

- **Audio and video resume positions were written from a scope that was being cancelled (N-3).**
  Both players called `rememberCoroutineScope().launch { … }` inside `onDispose`. Tested on device:
  the composition scope is already cancelled by then, so the write never reached Room — the player
  did stop and release, but the position was lost. Saving is now checkpointed while playing (about
  every five seconds of movement, not every 250 ms frame), flushed when playback stops, and flushed
  again on `ON_STOP`, none of which depends on a dying scope. The cost is stated rather than hidden:
  leaving a player can lose up to one checkpoint interval. Audio and video share the same rule,
  including resetting a track played to the end back to zero instead of resuming past the last bar.
- **Cancel was reported as a failure (N-9).** `ToolsViewModel` caught `CancellationException` in the
  same `catch (e: Exception)` as real errors, so pressing Cancel — which sets the panel to Idle
  first — was then overwritten with a red error panel naming the cancellation. Cancellation is now
  handled separately, returns the panel to Idle, and is re-thrown.
- **Clearing history had no confirmation (N-10).** One tap erased every recent document, every
  reading position and every media resume point, under a button labelled "Clear" with no way back.
  It now asks, and the message says what goes (records) and what does not (the files themselves).
  The row's subtitle was also wrong: it claimed only documents and reading progress were cleared
  while media resume points were erased too.
- **The scanner kept the camera after it closed (N-11).** The CameraX use cases were bound to the
  surrounding lifecycle owner, which stays resumed when a sheet is dismissed, so nothing unbound
  them: the preview pipeline kept the camera in use after the reader had gone. Disposal now
  unbinds, turns the torch off, and drops the capture references.
- **Rotating a scan cropped it and leaked a bitmap per tap (N-12).** `createBitmap(src, 0, 0,
  src.width, src.height, matrix, true)` returns a bitmap of the size asked for, so a 90 degree turn
  drew the rotated image into the *original* box and clipped it. Rotation now swaps the axes and
  draws through a canvas. Each tap used to leave the previous full-size camera bitmap alive with no
  reference to it; the copies this screen makes are now recycled when a rotation replaces them and
  when the cropper closes, while the caller's bitmap is never touched.

- **The offline copy could be served after the document had changed (H-9).** A cached copy was
  judged current on file size alone, so an edit that kept the length — a word replaced in place, a
  re-export that happens to match — left MyDoc holding an outdated copy of the user's document with
  no way to notice. Each copy now carries a sidecar recording the size *and* modification time it was
  taken from, and is rewritten when either differs. When a provider reports neither, the copy is kept
  rather than refused, because the copy exists so a document stays readable after its grant dies.
  Sidecars are excluded from the cache's 50-file / 100 MB budget and are deleted with their copy.

- **A spreadsheet that could not be read was shown as an empty one (H-8).** `XlsxReader` wrapped every
  worksheet parse in `runCatching` and discarded the result, so a workbook whose second sheet had
  malformed XML came back as a workbook with a blank second sheet — and a sheet the workbook named
  but whose part was missing was dropped from the list entirely. Failures are now recorded on the
  sheet, surfaced as `failedSheets`/`isPartial` on both the OOXML model and the grid model, and
  stated under the grid: which sheet could not be read, and that what is shown is not the whole
  workbook.
- **Opening a document enumerated its folder on the UI thread (H-10).** The swipe-between-neighbouring-
  files list was built by a private `documentSiblingsFor()` called straight from a `LaunchedEffect` in
  `MainActivity`: `listFiles()` plus a `length()` and `lastModified()` per entry, on the main thread,
  with no way to abandon it. Measured on the emulator, a folder of 5,001 files took 1,143 ms — a
  visible stall on every document open, and worse on a slow device or a big Downloads folder. The scan
  moved to `DocumentSiblings.readableDocumentsIn()`, which runs on `Dispatchers.IO`, checks
  cancellation every 128 entries, and leaves hidden files, subfolders and unsupported types out. A
  scan whose reader has already moved on is now discarded rather than published, so it cannot
  overwrite the swipe chain of the document the user actually opened. A failed enumeration yields an
  empty chain instead of throwing through the effect.
- **Every PDF image export path was dead: MuPDF will not give back pixels without an alpha
  channel.** Five call sites rendered `page.toPixmap(matrix, DeviceRGB, false)` and then read
  `pixmap.pixels`, which the native layer rejects with `invalid colorspace for getPixels (must be
  RGB/BGR with alpha)`. "Export as images", long-image export and the OCR fallbacks therefore threw
  on the first page, on every device. All five now request `DeviceRGB` with alpha, and a device test
  asserts the rejection itself so the flag cannot be flipped back silently.
- **Long-image export held every page in memory at once (N-5).** `pdfToImagesZip` rendered all pages
  into a list of bitmaps, then allocated one enormous bitmap on top of them and drew the list in. A
  20-page A4 document at 300 dpi is 1,785 × 50,520 ≈ 90 million pixels (~360 MB) before compression.
  The geometry is now measured first from the page bounds without rendering, the scale is reduced
  until the stitched image fits a 24-megapixel budget (with the reduction announced in the progress
  text), and pages are rendered one at a time straight onto the stitched canvas, each released in
  `finally`. A cancellation or an I/O failure releases the stitched bitmap too.
- **Image-to-PDF decoded at full camera resolution and ignored orientation (N-6).**
  `BitmapFactory.decodeFile` was called with no options, so a 12 MP photo was buffered in full to
  draw onto an A4 page, and EXIF orientation was never applied — a portrait photo landed sideways.
  Decoding now samples down to the resolution the chosen page can actually show (FIT_IMAGE keeps
  full resolution because the page is the image), the EXIF rotation is applied through the shared
  `BitmapRotation` so the axes swap correctly, and the `PdfDocument` is closed in a `finally`: the
  old code closed it only on the two paths it thought of, leaking the native document when drawing
  or writing threw.
- **PDF compression leaked every image it touched (N-7).** `PdfOptimizationEngine` decoded each
  `PDImageXObject` with `xobj.image`, recycled only the scaled copy it had made itself, and never
  released the original — which PDFBox decodes afresh per call. It also wrapped the decode in
  `runCatching { … }.getOrNull() ?: continue`, so an undecodable image vanished without a trace, and
  the whole strategy sat inside `catch (_: Exception)`. Ownership is now explicit: the decoded bitmap
  is recycled in a `finally`, the strategy failure is logged, `CancellationException` is rethrown,
  and an image larger than the decode budget is skipped *before* decoding (its dimensions come from
  the stream dictionary, not the bitmap) with the number left untouched logged.
- **Office conversions buffered the whole document before checking its size (H-7).** `DocxConverter`
  called `readBytes()` first and compared against its documented 200 MB limit afterwards, so the
  limit could only ever be reached by dying; `PptxConverter` had no limit at all and additionally
  round-tripped the document through a temp file and back (`bytes` + temp + re-read) before rendering.
  Both now read through `OfficeSourceReader`, which consults the reported length before allocating,
  sizes one buffer from it, caps growth at the budget instead of doubling past it (a provider that
  understates the length cannot make the reader allocate past the budget), and keeps the deck's
  placeholder-recovered copy inside the same budget.
- **Zoom tiles showed the whole page instead of the region asked for (H-5).** `renderRegion` built
  the matrix with `Matrix(s).translate(-region.left * pageWidthPx, …)`: MuPDF applies a Matrix
  translation in *pre-scale* (page) units, so the offsets were out by the scale factor, and
  `AndroidDrawDevice.drawPage` allocated the full page anyway before `createScaledBitmap` shrank it.
  Measured on the emulator with a fixture whose quadrants are different greys: every tile — top-left
  and bottom-right alike — averaged 114, which is the whole page's average (115). The region is now
  mapped in page units and drawn straight into a tile-sized bitmap through `AndroidDrawDevice`, so no
  full-page bitmap is allocated per tile and no post-hoc crop is involved. The same measurement now
  gives 160 for the top-left tile and 200 for the bottom-right, i.e. the region that was asked for.
- **A document search stopped page rendering until it had finished (H-6).** `MuPdfRenderer.search`
  took the rendering lock for the entire scan, so scrolling a large PDF waited on every keystroke,
  and `ViewerViewModel` published the result even when the job had been cancelled while the scan ran
  (cancellation cannot interrupt a blocking call, and there was no suspension point before the
  publish). The scan now takes the lock one page at a time — native calls stay serialized, which is
  what the renderer requires — and consults an `isCancelled` predicate between pages; the viewer
  drops a result whose job is no longer active. `AndroidPdfRenderer` had the same whole-scan lock
  and a `catch (_: Throwable) {}` that swallowed every page failure silently; both changed the same
  way, with the failure logged. Proven in both directions: with the old single-lock scan the new
  device test fails ("render only completed after the whole 1,106 ms scan"), with per-page locking it
  passes.
 Both converters
- **A document that was too large was announced as an unsupported one (H-7).** Both converters
  returned `null` for every failure and the viewer turned that into "could not lay out its pages …
  features our converter does not implement" — a memory limit dressed up as a missing feature, and a
  `catch (_: Throwable)` that swallowed `CancellationException` along with everything else. The
  converters now return `OfficeConversion`: over budget or out of memory → `EXCEEDS_LIMITS` naming the
  budget or the device, unreadable → `CONVERSION_FAILED`, and only a genuine layout failure keeps the
  reflow wording. Cancellation is rethrown, not reported.

- **A transient message could overwrite another one (N-16).** `AppMessages` held a single "latest"
  value while the snackbar host was suspended inside `showSnackbar`, so a second message — e.g. the
  "what was left behind" half of a partial-move result — replaced the first and it was never seen.
  The channel is a queue now, delivered in order, with the identifier counter made atomic.
- **Settings rows were two controls, not one (N-17).** The shared row wrapped a live `Switch` in a
  `clickable` Row: two focus nodes, two switch states, and TalkBack announcing each preference twice
  with two different ways to change it. The row is now the single `toggleable` with `Role.Switch` and
  the Switch is display-only.
- **The Quick Scan tile did nothing unless Home was on screen (N-18).** The tile posted into a
  replay-zero shared flow whose only subscriber was `HomeScreen`, so tapping it from the viewer,
  settings or search dropped the request. The request is state in `MainActivity` now, and a pending
  request resolves the derived screen to Home, which opens the scanner and clears it.
- **The hidden-files toggle did not persist (N-19).** `SettingsRepository.setShowHiddenFiles()`
  existed, wrote to preferences and had no callers: the browser flipped its in-memory state and the
  next open was back to hidden-files-off. The toggle now goes through the repository.
- **`formatMiddleTruncated` could ask for a negative number of characters (N-21).** With a long
  extension or a limit below the extension's length, `availableForBase` went negative and
  `name.take(maxChars - ext.length - 1)`/`substring(0, n)` threw while a title bar was being
  composed. Extensions longer than eight characters are now treated as name text, the head of the
  name is kept when nothing else fits, and every limit from 1 up is bounded. Titles containing the
  field separator no longer have to be rewritten to survive a cache round trip either.
- **Favourites and playback lists were unindexed and unbounded (N-29).** Both entities keyed only on
  their `uri` primary key while every list query ordered by a timestamp column, and neither query had
  a `LIMIT`. Room is at version 6 with `MIGRATION_5_6` creating the two indexes the planner needs —
  verified on device, including that existing rows survive the migration — and both flows are capped
  at 500 rows.
- **An outline sidecar could outlive the document it described (N-30).** The cache was keyed by file
  name only and validated nothing, so a document re-converted under the same name read the previous
  outline; saving an empty outline returned early, leaving the old sidecar in place for a table of
  contents that had been deleted. The sidecar now carries the PDF's size and modification time, an
  unstampable (older-format) sidecar is distrusted and dropped, and clearing the outlines clears the
  file. Nested levels were already preserved and still are.
- **`BlackAppColors` was dead code (N-31).** The obsidian palette it defines is never selected;
  `AppTheme` builds the AMOLED variant itself. Removed rather than left to imply a second path.
- **The storage cleaner counted bytes on the main thread during composition (R6).**
  `selectedFiles.sumOf { it.length() }` ran on every recomposition of the sheet: one filesystem stat
  per selected file, on the UI thread, in reaction to a checkbox. It is produced off-thread now and
  only when the selection changes.
- **A storage-provider document id could name a file outside external storage (Batch 6).** The
  resolver's last tier took the `primary%3A<relative path>` part of an external-storage document URI
  and passed it straight to `File(root, relativePath)`, so an id such as
  `primary%3A%2E%2E%2F%2E%2E%2Fdata%2Fdata%2F%3Cpackage%3E%2Fx` resolved to the app's own private
  directory — which is readable, unlike most of what a traversal can reach. The tier now resolves the
  id against a canonical external-storage root through `DocumentStreamResolver.externalStorageChild()`,
  which refuses absolute paths, UNC paths, an empty id, the storage root itself, and anything whose
  canonical form does not stay inside the root. Tested on the JVM and on the emulator, where the
  refusal is checked against a target that really exists and really resolves outside the root.
- **Document names and paths were written to logcat in release builds (Batch 6).** Eighteen logging
  sites interpolated `displayName`, `file.name` or a worksheet name into a `Log.w`/`Log.e` message,
  so a bug report or a connected debugger could recover which documents the user had opened and
  failed to open. They now go through `SecureLog`, whose detail argument is a lambda: with
  `BuildConfig.DEBUG` false the detail is never invoked, so no name is ever put into a message handed
  to logcat, while a debug build still logs it for whoever is fixing the bug. The guarantee is
  behavioural — that field is mutable so a test can drive both paths, and R8 does not delete the branch.

- **A cancelled or failed zip left its staging file on disk (R3).** `StorageRepositoryImpl.zip()`
  builds the archive under a private `.<name>.zip.tmp` name and renames it into place, but nothing
  ever removed that name: a compression that was cancelled, or that failed because a source vanished
  between the folder walk and the read, left the half-written `.tmp` behind — and when the rename fell
  back to a copy and the copy failed, a truncated `.zip` beside it. Both are now taken back on every
  exit from the write, including cancellation, and only if this run created them. The same path also
  skipped the free-space pre-flight that copy and move perform, so a full disk was discovered
  mid-write instead of refused up front.
- **The cleaner's delete re-checks containment (R2).** `scanDirectory` refuses anything that resolves
  outside its root, but `deleteEmptyDirectories` deleted whatever list it was handed — a folder that
  was renamed or replaced by a link after the scan, or one that was never in the scan, or the root
  itself. It now takes the scanned root, skips any entry whose canonical path is not strictly inside
  it, and reports a folder it could not remove instead of swallowing the exception.
- **Two viewer leftovers (N-20, N-22).** `SettingsRepository.setDynamicColor()` wrote a preference no
  screen could reach, and `AppDesignSystemTheme` defaulted its `dynamicColor` parameter to the opposite
  of what the app ships — so every preview (`ViewerTopBar`, `ViewerBottomBar`, `ReadingControlsBottomSheet`,
  the design-system preview) rendered a Material You palette the user never sees. The dead writer, its
  unread flow and the `dynamic_color_enabled` key are gone, and the default is now the shipped value:
  the brand palette, off for wallpaper colours. `OcrResultSheet` split the entire OCR result to count
  words during composition — a regex compile plus one substring per word, on every scroll and every
  button press, for text that cannot change while the sheet is open; the count is now produced once per
  text and the counting itself is a named function under test. `PhotoViewerScreen`'s `isOcrRunning` was
  written nowhere and read nowhere; deleted.

- **A file row could not be heard as selected (R7).** Selection was only ever drawn — a filled container and a tick icon — so a screen reader could not tell which items were in the batch. The first attempt, `role = Role.Checkbox` plus `selected = isSelected` on every row, was measured and discarded: all 11 visible rows reported `checkable="true"` while the selected row still reported `selected="false"`, i.e. a checkbox that never shows itself as checked. What ships is `toggleable(value = isSelected, role = Role.Checkbox)` applied **only while selection mode is on**, with the original `combinedClickable` otherwise. Re-measured with `uiautomator dump` on `mydoc_api35`: in selection mode 11 rows are checkable and exactly the long-pressed one is `checked="true"`; after closing selection 0 rows are checkable and the list is unchanged.

- **Two dead viewer controls that were still dead (N-1, N-4).** The Definition-of-Done pass found
  the audit's "already shipped fixed" note to be wrong against this tree. `ViewerScaffold` passed
  `onOpen = { isDocumentDetailsOpen = false }` and `onRemove = { isDocumentDetailsOpen = false }` to
  the Document Details dialog, so both buttons only dismissed it — "Open with…" and "Remove from
  recents" promised an action and delivered nothing; they now route through `openFile` (FileProvider
  `content://`, never `file://`) and `documentRepository.removeFromRecents(uri)`, and report the
  outcome, including the case where no other app can handle the file. `ViewerBottomBar` accepted
  `hasOutline` — the PDF viewer computes `capabilities.supportsOutline && sheetItems.isNotEmpty()` —
  and still rendered Contents with `enabled = true`, so an outline-less PDF offered an empty sheet;
  it is gated on the flag now, and `ViewerContentsButtonOnDeviceTest` (3, on `mydoc_api35`) pins that
  the other two controls stay usable either way. Reverting the gate makes the no-outline case fail on
  device, so the test is known to catch the defect rather than merely pass beside it. N-1's two
  handlers are wiring at a call site with no seam: they need a screen pass (tap Details → Remove,
  then confirm the entry is gone from Home recents), and are recorded as not covered by an automated
  test rather than counted as tested.

- **Gradle dependency verification is now enforced** (`gradle/verification-metadata.xml`, 233
  components and 381 SHA-256 entries, signature verification with the upstream key trust list). The
  first enforcing build failed on 32 buildscript POM/module files the generation pass never recorded,
  so metadata verification is off and artifact verification is on: the jars, AARs and `.so` files that
  get executed are checksum-pinned, and `assembleDebug` + `testDebugUnitTest` + `lintDebug` all pass
  under it. Documented at SECURITY.md §10 rather than left implied.
### Documentation and release integrity (Batch 6)
- **Third-party notices now describe the APK that exists.** ZXing, `docx-preview`, `JSZip` and a
  design-attribution credit to an unrelated PDF app are removed: no artifact, WebView, JavaScript
  payload or asset of theirs is in the build. Added: AndroidX Camera/Compose/Core/Lifecycle/SplashScreen,
  Room, Haze, Kotlin and coroutines, each with the licence its own POM declares, and the transitively
  packaged Google DataTransport/Firebase/GMS components. The three bundled fonts get their upstream SIL
  OFL 1.1 texts, copied from the projects that publish them, now shipped at
  `app/src/main/assets/licences/` so the licence travels with the font inside the APK.
- **"Zero telemetry" replaced with what is measured.** The app-side claim (no analytics, crash or
  advertising SDK; no collection by MyDoc's own code) is separated from the fact that ML Kit's metrics
  transport is packaged *and registered* in the merged manifest — `TransportBackendDiscovery` with a
  `cct.CctBackendFactory`, `JobInfoSchedulerService`, `AlarmManagerSchedulerBroadcastReceiver` — which
  nothing observed transmitting. The old "the model is downloaded by Play Services on first use" claim
  is corrected: `text-recognition:16.0.1` is the bundled-model artifact, so `assets/mlkit-google-ocr-models/**`
  and `lib<abi>/libmlkit_google_ocr_pipeline.so` are already in the APK.
- **SECURITY.md matched to the source.** Supported versions went from 1.5.x/1.5.13 to 1.7.x/1.7.2; the
  app-lock section no longer describes a fail-open gate with no re-lock, because the code now fails
  closed, clears the unlocked state in `onStop()`/`onRestart()`, re-arms `FLAG_SECURE` and defers
  pending intents; the backup section no longer says the OS may copy the document index and cached
  documents, because both `backup_rules.xml` and `data_extraction_rules.xml` exclude `mydoc.db*`,
  `saved_docs/`, `.trash/` and `pinned_folders_prefs.xml` from cloud backup *and* device transfer. Added:
  the storage-permission assessment, the checked-in debug keystore assessment, and an explicit list of
  what is not yet in place.
- **CI verifies every published APK, not only the universal one** (H-13). Each artifact in
  `app/build/outputs/apk/release/` is now signature-checked against the pinned certificate, its packaged
  ABIs are read out of the archive and matched against the file name, and its package name, versionName
  and versionCode are checked with `aapt2`. The smoke test launches both the x86_64 split and the
  universal APK, and the release body states in plain words that the arm splits were verified but never
  executed. What CI measured — fingerprints, ABI lists, run evidence — is published as
  `verification.txt` and quoted in the release notes, which is what the README's
  "compare the fingerprint with the published value" instruction has been waiting for (H-11).

### Tests

- `StorageCleanerDeleteTest` (7 JVM) — an empty folder inside the scanned root goes, a folder outside
  it stays, the root itself is never a victim, a `../` id that resolves outside is refused, a folder
  that gained content after the scan is refused, a protected name is refused, nested empties go
  deepest first. `StorageOperationSafetyTest` gained 3 more (10 in the class): a cancelled zip
  propagates cancellation and leaves no staging file, a zip whose source vanished mid-write fails and
  takes its output back, and a successful zip still produces a readable archive with no `.`-prefixed
  leftovers. Both new behaviours were proved non-vacuous by re-running the classes with the fix
  removed — 3 cleaner tests and 2 zip tests fail against the previous code, and pass again once it is
  restored.

- `OcrWordCountTest` (5 JVM) — running text, runs of whitespace, an empty result, an unbroken token
  with no spaces (counts 1, not 0), and a 2 MB result counted inside a stated time bound.
  `SettingsRepositorySurfaceTest` (2 JVM) — reflection over the class: no `setDynamicColor` and no
  unread `_dynamicColor` field can come back, while the toggles that are genuinely on screen
  (`setShowHiddenFiles`, `setGlassEffectMode`, `setCheckUpdatesOnLaunch`) must stay. The other half of
  N-20 — a Composable's default parameter — is not reachable from a JVM test; the previews rendering
  the brand palette is a visual property and needs a screen pass to confirm, not a unit test.

- `ExternalStoragePathContainmentTest` (5 JVM) and `ExternalStorageContainmentOnDeviceTest` (3 on
  `mydoc_api35`) — an ordinary relative id resolves to the file inside external storage, a `..` climb
  is refused even when its target exists, an absolute or empty id is refused, naming the root is not
  a document. `SecureLogTest` (4 JVM) — a release message carries the summary only, the detail lambda
  is never invoked, a debug message still names the file.

- `StorageMoveTrashSafetyTest` — 13 JVM tests over an injected `EntryMutator` seam: a
  folder move keeps the copy the source can no longer hold, an untouched source takes the
  copy back, nothing is deleted before the complete copy exists, nested and empty folder
  moves, a vanished source, bin-emptying that keeps a record for content it could not
  delete, restore that reports failures, an unavailable bin volume, and a cancelled move
  that never destroys the only complete copy.
- `StorageMoveTrashOnDeviceTest` — 4 instrumented tests on API 35 that move, trash,
  restore and empty real directories and assert the resulting bytes and database rows.
- `DataIntegrityCleanerE2ETest.tier2_feature6` asserted the silent-skip behaviour H-3
  removes (`isSuccess` with zero restored); it now asserts the reported failure.
- `PdfPermissionsTest` — 8 JVM tests over the mask: all four switch combinations, reserved bits 1-2
  staying zero in every case, the two denied-bit regressions above, and what the switches do not
  claim to control (editing, annotating, forms and assembly keep the value the tool always wrote).
- `PdfProtectPermissionsOnDeviceTest` — 8 instrumented tests that run the real native writer, read
  `/P` back out of the produced bytes, reopen the file with the right password, the wrong password
  and no password, and assert the library's own verdict on printing and copying for all four
  combinations. One test requires that an owner password equal to the user password is *refused*
  rather than reported as protection, because that configuration silently grants everything.
- `PdfWriterOptionsOnDeviceTest` — 3 instrumented tests pinning the writer vocabulary: every option
  string the tools send is accepted by the packaged MuPDF, and the three strings that used to be in
  the code are still rejected.
- Measured and now asserted: MuPDF reports the real page count of an encrypted PDF *before* any
  password is offered, so "zero pages" is a damage signal rather than an encryption one, and
  `needsPassword()` is what decides whether to ask.

- `PdfOpenDecisionTest` — 6 tests over the encrypted-versus-damaged decision.
- `WordViewerModelTest` — 13 tests over the loader's terminal outcomes (typed failure, permission,
  unreadable file, missing stream, cancellation, retry, stream closed) and over occurrence counting.
- `TextViewerModelTest` — 13 tests over the budget: small file, empty file, byte and line caps, a
  single very long line, multibyte content, invalid encoding, cancellation and stream closure.
- `RendererSlotTest` — 6 tests that open fifteen engines in a row and assert one is alive and
  fourteen were closed, plus re-release, same-engine replace and a close that throws.
- `WordViewerOnDeviceTest` — 6 instrumented tests that build real DOCX archives (whole, truncated,
  wrong format), load them through the same path the viewer uses, and assert the match sequence the
  search controls navigate.

Not device-verified: the Compose interaction itself — that tapping next moves the list, and that the
truncation notice is legible in both themes. Those are wired to the functions above but were not
driven on screen in this batch.

- `PlaybackCheckpointsTest` — 10 JVM tests over the checkpoint rule: the first-second and unknown
  duration refusals, the near-end reset, one write per five seconds across 480 polls, the stop and
  background flushes, no rewrite of a position that was just resumed, and a seek backwards.
- `PlaybackPersistenceOnDeviceTest` — 4 instrumented tests against the real Room database. One of
  them reproduces the old pattern through an actual Compose composition and asserts the write does
  **not** land, which is the evidence N-3 needed; the others drive the checkpoint writer the way the
  poll loop does and assert the position survives, resumes on reopen, and resets at the end.
- `BitmapRotationTest` — 5 JVM tests on the axis swap, including four quarter turns returning to the
  original box and a half turn not swapping.

- `OfflineCopyPolicyTest` — 10 JVM tests: the same-size edit is now detected, an unchanged document
  keeps its copy, a length mismatch, a missing/empty/stampless copy, a document whose metadata is
  entirely unknown still reading offline, sidecar format round-trip, and malformed stamps treated as
  absent rather than trusted.
- `OfflineCopySidecarOnDeviceTest` — 2 instrumented tests against the real `saved_docs` directory:
  the offline fallback returns the document and not its sidecar even when the sidecar is the newer
  file, and 51 copies plus their sidecars survive trimming as 51 documents rather than 102 files.

- `XlsxWorksheetFailureTest` — 3 JVM tests over hand-built workbook archives: a well-formed two-sheet
  workbook reports nothing failed, a sheet whose elements do not nest is recorded as failed while its
  neighbour still reads (and the readable sheet is not marked), and a sheet the workbook names whose
  part is absent stays in the list with a reason instead of vanishing.

Known limitation, measured rather than assumed: a worksheet part that is simply cut off mid-file
still parses without error, so it is not reported as failed — the parser reaches end-of-document
quietly. Detecting that needs a terminator check in the worksheet loop, not just an exception.

- `DocumentSiblingsOnDeviceTest` — 5 instrumented tests on the real filesystem: 5,001 files enumerate
  in name order with their real sizes inside a 2.5 s bound (measured ~1.1 s on `mydoc_api35`), hidden
  files / unsupported types / subfolders stay out, an already-gone caller receives nothing
  (`withTimeoutOrNull(0)` returns null), a missing folder yields an empty chain instead of throwing,
  and a scan dispatched from Main keeps the main looper ticking (the test posts a 20 ms ticker and
  fails if the looper did not get at least one tick per 40 ms of a scan measured to take >400 ms —
  a scan that held the main thread could not satisfy it).
- `AppMessagesTest` — 4 JVM tests: two messages arriving while one is on screen are both delivered
  in order, ids are distinct and increasing, and an empty queue yields nothing.
- `FilenameFormatterTest` — 9 JVM tests: a name that fits, a long extension against a 16-character
  limit, every limit from 1 to the name length, a leading dot not treated as an extension, zero and
  negative limits, and non-ASCII names measured in characters.
- `OutlineCacheTest` — 6 JVM tests on real files: nested levels survive, a pipe in a title survives
  unmangled, a re-converted document under the same name does not reuse the old outline, an empty
  outline clears the sidecar, a sidecar without a stamp is distrusted, no cache means no result.
- `MigrationTest` — 3 instrumented tests now, including `migrate5to6AddsIndexesAndKeepsEveryRecord`
  (both indexes created and the user's rows intact) and a bounded-favourites check against the real
  Room database.

- `OfficeSourceReaderTest` — 8 JVM tests on the bounded reader: a claim that matches the stream, a
  claim that is too long (only the bytes that exist are returned), a claim that is too short (it
  grows and still reads in full), a stream past the budget refused rather than buffered, an absurd
  10 GB claim against a 1 KB budget that must not drive the allocation, empty distinguished from
  unreadable, and the failure mapping (neither a size limit nor an out-of-memory may be announced as
  unimplemented features).
- `PdfTileRenderingOnDeviceTest` — 3 instrumented tests on a fixture whose four quadrants are four
  different greys: a tile is the size it was asked for, and the top-left and bottom-right tiles
  average the grey of the quadrant they name. Both quadrant tests failed before the fix, when every
  tile averaged the whole page (measured 114 against a page average of 115).
- `PdfSearchCancellationOnDeviceTest` — 2 instrumented tests over a 1,200 page, text-dense fixture: a
  cancelled search yields nothing while the same query yields matches when not cancelled, and a
  `renderPage` issued while a scan is running completes before the scan does. The second test was
  checked against the old whole-document locking and fails there, so it measures the lock discipline
  rather than just passing.

- `OfficeConversionBudgetOnDeviceTest` — 3 instrumented tests on real files: a sparse 250 MB `.docx`
  refused in under a second without its bytes ever being buffered, a real DOCX still converting to a
  cached PDF that the second open reuses rather than re-rendering, and an empty document refused
  rather than announced as a valid one.
- `ImageConversionMemoryOnDeviceTest` — 5 instrumented tests: a JPEG tagged `ORIENTATION_ROTATE_90`
  ends up taller-than-wide on the page, a 20-page A4 export at 300 dpi stays inside the 24-megapixel
  budget with the scale visibly reduced, "Export as images" yields one decodable PNG per page (it
  produced none before the alpha fix), the alpha-less pixmap read is asserted to *throw* so the fix
  cannot be reverted, and a batch mixing a real PNG with a text file still places the image that
  decoded while reporting the rest as undecodable.

Known gaps, measured rather than assumed: a 0-byte `.docx` is reported by the reader as unreadable
rather than empty, because the stream resolver returns no stream for it — the message is still
truthful ("could not read"), just not as specific as the JVM path. And a 37-byte plain-text file
named `.docx` produced a *successful* PDF: `DocxParser` accepts bytes it cannot parse as a Word
archive and the renderer lays out an empty page. That is a format-validation gap, not a memory one,
and is left for the correctness batch.

## [1.7.2] — 2026-11-04

Range: `v1.7.1..v1.7.2`. Four commits: one consistency pass, the two engineering-audit
follow-ups, and a device-report pass over browsing, selection and dark-mode chrome.

Screens disclose and describe failures the same way, cached document content is keyed on
what it actually is rather than a 32-bit hash, file operations report partial results
instead of lying, and the browsing state that had to survive opening a document moved to
the view models that own it. The Ink & Seal identity is untouched; the visual changes are
corrections to it.

### Added

- **Fidelity disclosure on screen.** `DocumentFormats` declared a render fidelity and per-format
  limitations, but nothing read them — `fidelityNote()` had zero callers — so a byte-scraped
  `.doc` or a text-only `.ppt` was presented as a faithful rendering. One `FidelityNotice` strip
  now sits under the chrome in every viewer, driven by the registry and downgraded at runtime when
  an engine falls back (`usedTextFallback`, `recovered`, sheet truncation). Document details lists
  the complete set of gaps. Seven tests pin the two ways it can fail: staying silent when it
  should speak, and letting a declared capability cover a runtime fallback.
- `UserMessages` — the one place an exception becomes a sentence a reader can act on, with the
  cause sent to the log instead of the screen.

### Fixed

- **Exception text no longer reaches the UI.** More than 30 sites interpolated `e.message` or
  `localizedMessage` into visible strings: file-open failures in `MainActivity`, the viewer and
  spreadsheet view models, every file-manager snackbar (`Failed: …`, `Delete failed: …`), OCR,
  camera, share, biometric and update paths.
- `calculateHash()` returned `"Error: <exception>"` and `"Unavailable"` *as checksums*, so two
  unrelated failures compared equal. It returns `null` now and the properties sheet hides the row.
- Text viewer failures printed a raw red string while every other viewer used
  `DocumentProblemScreen`; they now share one error surface and one `LoadFailure`.
- Status bar insets were applied twice in `TrashBinSheet` (a padded `CenterAlignedTopAppBar`) and
  in the spreadsheet error state (nested `statusBarsPadding`). `DocumentProblemScreen` no longer
  pads itself; each host applies the inset once.
- One chrome metric: `ViewerChrome.topBarHeight` / `bottomBarHeight` replace the `64.dp` and
  `56.dp` literals each viewer re-typed to clear its own bars.

### Fixed — engineering audit follow-up

An external source audit of v1.7.1 (`9b9fba1`). Each claim was checked against the code first;
the confirmed ones are below, the refuted ones are recorded in
[`docs/audit/2026-11-04-external-audit-verification.md`](docs/audit/2026-11-04-external-audit-verification.md)
so they are not re-reported as open work.

- **Document cache identity is no longer a 32-bit hash.** Four layers keyed cached content on
  `hashCode()`: the offline `saved_docs` backup (`DocumentRepositoryImpl`), the tier-2 stream
  fallback (`DocumentStreamResolver`, twice) and thumbnail lookup (`FileThumbnail`). A collision
  hands one document another document's pages or thumbnail with no error anywhere.
  `DocumentIdentity` derives a truncated SHA-256 from the URI alone. A display name is never part
  of the identity, so `openStream()` lost the parameter and the decoy test asserts a name cannot
  steer a lookup. The conversion caches v1.7.1 already hashed were folded into the same helper.
- **A move that could not delete the source is no longer reported as a move.** The cross-volume
  fallback incremented its count unconditionally, so "Pasted 1 item" could mean the file was in
  two places. The deletion is verified; if the source survives, the copy is withdrawn and the
  item counts as failed.
- **Trashing can no longer destroy a file.** The physical move happened first and the database row
  after, and `purgeExpiredTrash` deleted trash files with no row once they were 24 hours old — a
  crash between the two made the document unrecoverable. The row is reserved before the move, and
  orphans are logged and kept rather than deleted.
- **Partial batches report themselves.** A copy or move stopped at the first failing item and said
  nothing about what had already been written. Every item is now attempted, and a split surfaces
  as "Paste completed for 2 of 3 items — 1 could not be completed." with the listing refreshed.
- **Half-finished output is taken back.** A failed extraction left earlier entries in the
  destination while the UI reported failure; a failed compression left a truncated `.zip` that
  opens like a real archive. Extraction rolls back only what it created; compression stages and
  renames.
- **Long file operations are cancellable.** No loop checked for cancellation and `onProgress` was
  a non-suspend lambda, so a cancelled scope ran to completion. Every loop calls `ensureActive()`,
  and copy/move/zip/unzip check free space before writing anything.
- **Native PDF documents are destroyed on every path.** `PdfPageOperationsEngine` had no `finally`
  around any destination document, so a throw during page grafting or saving leaked native memory
  and a file handle — most likely on the damaged files people open. Merge, split (all three
  shapes), remove, reorder and extract go through one helper now, and `MuPdfRenderer` page,
  pixmap and structured-text objects got the same treatment.
- **"No text on this page" and "we could not read this page" are different answers.**
  `getPageText()` / `getDocumentText()` returned `""` for both, so copy, share, read-aloud, search
  and OCR fallback all gave advice for the wrong problem. They return null when the text layer
  could not be read.
- **A saved signature that cannot be decrypted is no longer invisible.** `hasSavedSignature` only
  checked that the file existed, so an undecryptable signature showed "Edit / Redraw" and then
  failed with "No signature found" — and `getOrCreateKey()` silently minted a new key when the
  alias was gone, which is what made that state reachable. `SignatureStatus` distinguishes
  NONE / READY / UNREADABLE, decryption never creates a key, and writes are staged and renamed.
- **App lock fails closed.** With no device credential the gate called `onSuccess()`, so disabling
  the screen lock after enabling app lock removed the protection. An incoming VIEW/SEND intent was
  also resolved — its document read and copied into the offline cache — before anyone
  authenticated, with the lock screen drawn over content that had already been fetched; external
  intents are now held until unlock.
- **A partially parsed presentation says so.** The ODP/flat-XML parse swallowed any throwable and
  returned the slides it had managed — an incomplete deck presented as complete. The reason now
  rides along as `incompleteReason` and appears in the fidelity notice.
- **Home stopped querying the whole media library twice.** `getAllItems()` and `continueItems()`
  each ran three MediaStore cursors, so one subscription cost six full-library queries. A
  1.2-second snapshot lets the second subscriber ride the first, deletions invalidate it, and the
  dead `MediaViewModel` — referenced only from its own file — is gone.


### Changed

- Headers: `TrashBinSheet` moved onto `AppTopBar`. The back affordance is the outlined arrow
  everywhere (AppTopBar, media players, cropper). Bars naming a document use the brand
  `documentName` role; bars naming a screen use `titleLarge`.
- Sheets: five divergent sheet titles (`title`, `titleSmall`, `bodyLargeStrong`, `labelLarge`)
  now share the role `AppBottomSheet` uses.
- Loading: the four document viewers, app manager, cleaner, page grid and page organizer use
  `AppProgressIndicator`; the PDF viewer was previously the only one that did.
- Empty states: the app manager's bare `Text`, the cleaner's centred thumbs-up billboard and the
  spotlight's 64.dp glyph circle are `AppEmptyState` paragraphs.
- Dialogs: `DocumentDetailsDialog`, `PasswordDialog` and `PageJumpDialog` titles are on the same
  role as `AppDialog`.
- Dropped the `AppSpacing as Spacing` alias from the four files that used it.
- Deleting a photo that cannot be moved to the Recycle Bin says it was deleted, instead of promising a recovery that does not exist.

### Fixed — browsing, selection and dark chrome

Seven reports from using the app on a phone, each verified on `mydoc_api35` in light, dark and
pure AMOLED.

- **A folder inside a Library category opens.** Tapping `Download/Knowledge` from the Downloads
  category bounced back to the Library tab: `loadDirectory()` cleared `activeCategory` without
  recording where the browse had come from, so the pane fell back to the category surface. The
  originating category now goes on the back stack and the browse flag is derived from it, so the
  folder opens and Back returns to the category that contained it.
- **The category and filter survive opening a document.** `activeCategory` already lived in the
  view model; the document-type chips did not. `docTypeFilter` moved out of a `remember` in
  `DirectoryPane` — a pane that leaves composition the moment a viewer opens — into
  `FileManagerUiState`. Documents → PDF → open a file → back still shows four PDFs with the chip lit.
- **Swiping past the first or last page changes document in every viewer.** Word reflow, the
  spreadsheet grid and the slide deck had no sibling-swipe wiring at all, and the shared detector
  restarted mid-gesture because `pointerInput` was keyed on the callback lambdas that continuous
  recomposition keeps replacing — so even where it was wired, the fling was dropped. It keys on
  `Unit` and reads the latest callbacks through `rememberUpdatedState`. The sibling list was also
  built from the *grandparent* folder (`documentSiblingsFor()` took a directory and called
  `parentFile` on it again), so a document opened from a folder found no neighbours. Both
  directions confirmed on device: `sample.pdf → sample.ppt → sample.txt` and back.
- **Half-boxes in dark and AMOLED are gone.** One rule now decides the glass rim: `glassRimBrush()`
  keeps the directional specular on ivory and uses one even hairline (`colors.line`) on dark. That
  removes the lit top edge that read as a leaking box boundary in the search field, under the
  viewer chrome and across every card, and `GlassSurface`, `AppDialog`, `DocumentDetailsDialog` and
  `AppDropdownMenu` share it instead of each re-typing the gradient.
- **The storage cleaner reads.** The disabled "Select a category to clean" button painted its own
  label and icon with `colors.onAccent` whatever its state, so a disabled button kept accent text
  on a muted container; the explicit colors are gone and the button's content color decides. The
  reclaimable figure, scan count and truncation warning moved to the roles they should use, the
  category rows got a real hairline, and the sheet has bottom padding so the action no longer sits
  against the navigation bar.
- **Multi-select in Home recents.** Long-pressing a recent card enters selection, taps toggle, and
  a floating bar offers Select all / Share / Remove from recents. The selection lives in
  `HomeViewModel` because the Home pane leaves composition when you change tabs, and a selection
  that evaporates on return is not a selection. Sharing several documents needed `shareUris()` —
  `ACTION_SEND_MULTIPLE` with every URI in the clip data, so a receiver gets a read grant for all
  of them, not just the first.
- **The photo viewer says it once.** Info was in the top bar and in the bottom pill row; the top
  one is gone. Flip horizontal and Slideshow are removed completely: both menu items, the
  `isFlippedHorizontal` / `isSlideshowActive` / `slideshowIntervalSec` state, the auto-advance
  timer, the slideshow branch of the back handler and the `scaleX` mirror in `ZoomableImage`.
- **Bold is a signal again.** One rule: bold marks a name, an active or selected state, or a
  primary action. Audio and video time counters, the read-aloud status line, update copy, the
  cleaner's truncation warning and unselected filter/tool chips went back to regular; the selected
  chip, the non-default playback speed and the selected viewer tool now take the emphasis they
  lacked. Three dialog labels and the video speed list used a raw `FontWeight.Bold` instead of the
  type scale.

### Not done, deliberately

- 13 raw `AlertDialog`s still live outside `AppDialog`; their titles match, porting them is a
  separate sweep. The 11 list-row metric variants (leading wells from 36 to 72.dp, icons 18-24.dp)
  and the off-scale spacing/radius literals are untouched. The fidelity strip floats over the
  first rows of a dense grid instead of reserving space — tap to hide the chrome, or scroll.
- Swipe-to-neighbouring-document needs a folder of readable files. A document opened from a
  MediaStore category or from recents arrives as a `content://` URI with no sibling list to walk,
  so there the gesture stays a page turn. Wiring it means carrying the visible list through the
  open path, not just scanning a directory.
- CI now installs and launches the signed release APK and fails the publish if it crashes, but does not exercise its screens. The reference-rendering fidelity corpus the audit asks for does not exist yet; `MANAGE_EXTERNAL_STORAGE` policy eligibility and a SAF-only mode are product decisions; the large-class splits (`MainActivity`, `StorageRepositoryImpl`, `PptxParser`, `VideoPlayerScreen`) are deferred.

## [1.7.1] — 2026-11-04

Range: `v1.7.0..v1.7.1`. Merged from `fix/office-document-rendering`.

Office document loading, parsing, routing and failure reporting. No UI redesign; the Ink & Seal
identity is untouched.

### Fixed

- **One format registry.** Format knowledge lived in six lists that disagreed (viewer routing,
  the conversion switch, category classification, filter chips, spotlight filters, thumbnails).
  The conversion switch still named formats the router no longer sent there, so `.xls`, `.ods`,
  `.csv` and `.tsv` were handed to the OOXML-only converter, got `null` back, and fell through to
  MuPDF as raw bytes reported damaged. `DocumentFormats` now declares family, viewer, converter,
  thumbnail strategy, render fidelity and limitations per extension, and everything reads it.
- **No more fabricated content.** An unreadable spreadsheet returned a blank "Sheet 1"; an
  unreadable Word file returned a paragraph whose text was the sentence "Document is empty." or
  "File too large or unreadable: …", indistinguishable from the document's own words; a deck that
  parsed to zero slides was byte-scraped into six-strings-per-slide slides and shown as parsed.
  Parsers now throw `DocumentLoadException` with a specific `LoadFailure`, and the viewer names
  the cause.
- **Emptiness is a property, not a string.** `PptxPresentation` used a slide titled
  "Empty Presentation" as its empty sentinel, which a genuine slide with that title would defeat.
  It has `isEmpty`, `emptyReason` and `recovered` now.
- **Excel grid uses the real engine.** The grid was fed by a values-only reader that invented
  sheet names, sorted sheets lexicographically (so `Sheet10` preceded `Sheet2`), showed date
  serials as numbers, dropped formulas without cached values, and ignored hidden sheets, column
  widths and merged ranges. It now projects `XlsxReader`'s model, verified on device: real sheet
  names (`By Subject`, `Faculty`, `Summary`) and workbook column widths.
- **Grouped PowerPoint shapes are placed correctly.** A group pushed an all-zero transform and
  its own `a:off`/`a:ext` were written to the shape variables, so the transform was skipped and
  children were laid out in raw child space.
- **ODP decks are accepted.** The sniff looked for `draw:page` in the first 2048 bytes of
  `content.xml`; real decks place it after `office:automatic-styles`, so valid files were
  rejected as "No slides found".
- **RTF and flat ODF are parsed, not scraped.** Both fell to the byte scanner and displayed
  control words and `<text:p>` tags as document text. `RtfReader` walks the control stream (group
  state, hex escapes in the declared code page, unicode escapes with their fallback skip, binary
  payloads dropped by length, destination groups ignored); `.fodt` and `.fods` are parsed as the
  XML they are.
- **CSV to RFC 4180.** Quoted fields kept their padding (a flag tested after the closing quote
  had cleared it), a stray quote swallowed the rest of the line, delimiter detection counted
  separators inside quoted fields, only the UTF-8 BOM was stripped so UTF-16 decoded to
  NUL-interleaved text, and cp1252 files turned accents into replacement characters.
- **Conversion cache is content-addressed.** Two converters keyed cached PDFs on a 32-bit
  `contentHashCode()` plus file length, so two same-sized documents could be served each other's
  cached PDF. Keys are now truncated SHA-256.
- **Refusal beats fallback routing.** `.xlsb` is refused with an explanation instead of being
  handed to a PDF renderer that reports it damaged. Found on device, not in review.
- **Shared XML parser creation.** Five engines repeated a platform-then-JVM try/catch that only
  worked by accident: under `unitTests.isReturnDefaultValues` the platform parser returns null
  rather than throwing, so the fallback was never reached.

### Changed

- `.odt` and `.ott` now take the paginated path they already qualified for, because the DOCX
  parser genuinely reads `content.xml`.
- Legacy `.doc`, `.dot` and `.xls` still recover text, but say so: the first scraped string is no
  longer promoted to a heading, and the fallback flag drives an in-app disclosure.
- `.xlsb`, `.wps`, `.wpd` and `.fb2` are reported as unsupported rather than partially rendered.
- The README format matrix is expressed in the registry's vocabulary and lists known gaps.

### Tests

`SpreadsheetFormatsTest` (CSV quoting, encoding, delimiter detection, FODS, XLSB refusal) and
`DocumentFormatsTest` (routing contract: refusals, converter requirements, disclosure
invariants, category and filter mapping). Five existing tests asserted the defective behaviour —
the fabricated empty slide, the "Document is empty." prose block, the invented heading, the blank
xlsx sheet — and were rewritten to assert the honest contract. 211 unit tests pass; lint, debug
and release builds are green.

### Not done

Deep cancellation inside long parse loops, streaming instead of whole-file reads, one zip-bomb
budget across the three engines, and cache-directory sweeps. Charts, SmartArt, embedded objects
and animations remain unrendered, and legacy binary `.ppt`, `.xls` and `.doc` stay text-only.

## [1.7.0] — 2026-11-04

Range: `v1.6.1..v1.7.0`.

The "Ink & Seal" identity is now actually rendered, not just documented. The design-system audit
of v1.6.1 found that the brand type scale existed but never reached the screen; this release closes
that gap and the four other confirmed findings, plus the reader and library work that came with it.

### Fixed

- **Brand typography reaches the UI.** `AppTheme` never passed a `typography` argument to
  `MaterialTheme`, and `appTypographyFromMaterial()` mapped the brand roles onto the framework
  default — so ~130 `MaterialTheme.typography.*` call sites rendered Roboto while Fraunces, Inter
  and JetBrains Mono sat unused in `AppTypography`. Added `brandMaterialTypography()`, which
  expresses the brand tiers in Material 3's own roles, and supplied it to `MaterialTheme`. Screen
  titles and document names are now Fraunces, UI text Inter, and metadata (`p. 1/3 · 1.4 KB`)
  JetBrains Mono. Font scaling is untouched.
- **Bottom navigation touch targets and semantics.** Nav items were ~33dp tall and used
  `clickable(role = Role.Tab)` with no selected state. They now use `selectable` with a 48dp
  minimum applied outside the padding, so the clickable bounds themselves grow rather than just
  the visual pill. Measured on device: 48dp tall, and the active tab reports `selected = true`.
- **Library overflow menu shown where it belongs.** The 3-dot menu (view toggle, hidden files,
  new folder/file, recycle bin, refresh) appeared on the Library landing page, where every item
  targets a folder that is not open. It now appears only while browsing internal storage.
- **Tools and Library top bars align.** The Tools screen applied the status-bar inset twice — once
  on its root column and again by passing zero insets to the shared bar — so its title sat lower
  than Library's. Both are now identical.
- **File picker metadata.** The first-run picker accepted `*/*` but labelled every result
  `application/pdf`, 0 bytes, DOCUMENT. It now resolves the real MIME type, display name, size,
  timestamp and media kind from the picked URI, so icons, badges and viewer routing stay honest.

### Added

- **Office document thumbnails.** Word, Excel and Slide tiles in Recents and Library were flat
  "WORD"/"PPT" plates. They now run through the same converter the viewer uses and show the real
  first page, falling back to the plate when a file cannot be converted. Conversions are capped at
  two in flight so a grid of documents cannot stampede the parsers on first paint. Legacy binary
  `.ppt`/`.xls` keep plates — there is no converter for them.
- **Swipe between documents.** The viewer's pager only ever walked pages inside one file. A fling
  past the first or last page now steps to the neighbouring document in the same folder, across
  PDF, Word, Excel, Slide, EPUB and text. Mid-document swipes still turn pages, and tab swiping
  stays on the outer screens. The gesture observer never consumes events, so it cannot steal a
  page turn.
- **Markdown rendering.** `.md` files opened as raw monospace with visible `#` and `**` markers.
  The text viewer now renders headings with a real type scale, bold/italic/code/strikethrough
  spans, bullets and ordered lists, block quotes and rules, with search highlighting preserved.
- **Home brand lockup.** The header was a text-only wordmark. The Seal — a rounded square whose
  negative space forms an M with a folded-paper corner, as specified in `docs/brand-identity.md`
  §4 — now sits beside it. It is drawn rather than a drawable, because the app's theme is chosen
  independently of system night mode, so it tracks Seal teal on paper and lifted teal on graphite.

### Changed

- **Stale brand language removed.** Comments and the design-system preview still described Stamp
  Vermilion, ink navy and "Paper under glass", and the preview's accent swatch printed `#D94324`
  for a color that is `#0E5C57`. Corrected across seven files.
- **Glass gradients are now an explicit exception.** The brand rule said "no gradients anywhere"
  while `GlassSurface` uses short vertical gradients for the specular rim and inner shadow. The
  rule in `docs/brand-identity.md` now carves out optical edge lighting specifically, instead of
  the docs and the code contradicting each other.

### Known gaps

- The launcher and splash still use the older document-sheet mark; only the in-app lockup uses the
  Seal M. Re-cutting the launcher icon is deliberately out of scope here.
- Not yet verified on device: update-banner overlap with the last list item, PDF reader chrome
  density, photo-viewer contrast over bright images, and glass cost on low-RAM hardware.

## [1.6.1] — 2026-11-04

Range: `v1.6.0..v1.6.1`.

### Fixed

- **Video player controls legible in light mode.** The player's top and bottom control bars used the theme-adaptive `GlassSurface`, which falls back to a warm near-white plate in light mode — so the white icons and text vanished and the bars clashed with the black video. They now use a fixed dark scrim, so controls are always visible and the player looks identical in light and dark.
- **Settings top bar artifact.** The Settings top bar rendered as a glass plate with a specular rim and elevation shadow, which in dark mode read as a stray "box with a line" over the black background. It now uses the same flat, transparent top bar as Library.
- **Tools top bar consistency.** The Tools screen used a bespoke header (different title style and icon tint from the rest of the app). It now uses the shared `AppTopBar`, so Tools, Library and Home share one top-navigation treatment.

## [1.6.0] — 2026-11-04

Range: `v1.5.19..v1.6.0`.

### Added

- **Brand identity — "Ink & Seal".** A documented, ownable design system (`docs/brand-identity.md`): positioning (the private, local-first, every-format suite that never phones home), a quiet-luxury personality, voice rules, and hard anti-slop guardrails (no gradients/aurora/glow, one accent, one typeface pair, color never the only signal).

### Changed

- **New color system.** Replaced the generic Apple system palette with a warm, restrained "Ink & Seal" scheme across `AppColors` and the Material 3 schemes in `AppTheme`: warm bone canvas (`#F5F3EE`) / warm graphite dark, near-black label text, one **Seal-teal** accent (`#0E5C57` / `#4FB0A6`) and a single **brass** micro-accent (`#A67C3D`) reserved for the seal and premium touches. Format/media colors are desaturated to warm letterpress tones so the UI reads calm, not a crayon box.
- **New brand mark + launcher/splash icon.** Replaced the generic blue gradient stacked-paper icon with an ownable vector seal — a Seal-teal rounded square holding a cream folded-corner document with two text rules — wired into the adaptive launcher icon, the cold-start splash, and the window background. Verified in light, dark and on the splash.
- **Wordmark.** Home header is now a clean Fraunces "MyDoc" (the trailing accent-period cliché removed).
- **Brand is the default theme.** Material You wallpaper theming now defaults **off** (it overrides the brand accent); it remains an opt-in toggle in Settings → Appearance.

### Assets

- `assets/brand/mydoc-seal.png` (raster mark), `assets/brand/launch-banner.html` + `launch-banner.png` (1200×630 launch banner in the identity).

## [1.5.19] — 2026-11-04

Range: `v1.5.18..v1.5.19`.

### Changed

- **iOS system color palette.** Replaced the Tailwind/editorial hexes with Apple's actual system colors across the design tokens (`AppColors`, `AppTheme` light/dark/AMOLED schemes, `DocumentFormatColors`, media-kind tokens) and the file-type badge tints: systemBlue accent (`#007AFF` / `#0A84FF`), systemRed/Orange/Green/Indigo/Purple/Pink/Teal/Mint/Brown/Gray for formats, and neutral iOS chrome (grouped `#F2F2F7` canvas, `#1C1C1E`/`#2C2C2E` dark surfaces, label/secondaryLabel text). The result is the restrained "color comes from content, chrome is neutral" Apple liquid-glass look rather than a saturated editorial palette. Verified on the emulator in light and dark.

## [1.5.18] — 2026-11-04

Range: `v1.5.17..v1.5.18`.

Full-codebase bug audit follow-up. Fixes are grouped by the area they affect.

### Fixed

- **Legacy `.ppt`/OLE2 parsing.** The FAT sector filter compared against `0xFFFFFFFD.toInt()` (which is `-3` in Kotlin), so no sector was ever registered and every legacy PowerPoint stream decoded to garbage. Corrected the signed comparison.
- **XLSX shared strings.** Text is now accumulated per `<si>` (joining rich-text runs) instead of per `<t>`, which had shifted every later cell's referenced string and shown wrong text throughout a sheet.
- **PDF "Compress" corrupt output.** A failed MuPDF save no longer leaves a truncated file eligible as the returned "compressed" result.
- **Clear cache no longer wipes offline backups.** "Thumbnail & render cache" stopped deleting `filesDir/saved_docs`, the copies that keep revoked-grant documents readable.
- **Bulk-trash Undo.** The trashed set is captured and restored from that list, so UNDO after a bulk move no longer silently does nothing.
- **Extracted-APK path traversal.** `versionName` (attacker-controlled APK metadata) is now sanitized before being used in the output filename.
- **Backup privacy.** The Room database (`mydoc.db`) and pinned-folders prefs are excluded from cloud backup and device transfer, matching the app's stated privacy intent.
- **PDF dual-page navigation.** Search, jump, bookmark, outline and volume-key navigation now target the dual-page list (by pair index) instead of the never-composed pager.
- **Presentation pinch-zoom.** The gesture detector no longer restarts mid-pinch (was keyed on the value it mutates).
- **Main-thread rendering / IO.** Scrollbar live thumbnails, OCR/TTS page renders, spreadsheet loading and viewer state writes moved off the UI thread; viewer progress now persists under the active (re-picked) document URI.
- **Redact safety.** Redact requires a non-blank query instead of silently redacting the hardcoded default term.
- **Offline-cache correctness.** `saved_docs` lookup matches the full `${docKey}_` prefix (no cross-document hash-prefix collisions), and the backup copy is written via temp+rename so the source is never truncated mid-copy.
- **Recycle Bin safety.** Same-batch trash names are made unique (no overwrite / shared-path loss), the expired-trash purge runs in a cancellable caller scope, and freshly-trashed files are mtime-stamped so reconciliation cannot delete them as old orphans.
- **XLSX writer / renderer robustness.** Malformed worksheet parsing no longer throws `StringIndexOutOfBounds`; empty-range host no longer throws on `lastKey()`; XLSX→PDF page count is capped (OOM guard); Excel number formatting uses `Locale.US` symbols.
- **PPTX zip-bomb guard.** The aggregate decompressed-size limit is now actually enforced.
- **MuPDF zoom tiles.** Region rendering crops the requested tile region instead of squashing the whole page into it.
- **Misc.** Text-viewer input stream is closed (no descriptor leak), search close cancels the in-flight query, the updater only stamps its 24h throttle on a successful fetch, TypeScript `.ts` no longer classifies as video, and the watermark size clamp no longer crashes on sub-100pt pages.

## [1.5.17] — 2026-11-04

Range: `v1.5.16..v1.5.17`.

### Added

- **Skeleton first-paint states.** The Home feed and Library browse now render static tonal placeholder blocks while their data loads, instead of a blank pane. Deliberately shimmer-free: a pulsing skeleton would reintroduce exactly the motion some users asked the system to remove.
- **Reduced-motion respected across glass.** A new `LocalReducedMotion` reads the system "remove animations" setting (transition/animator scale = 0) and observes live changes via a `ContentObserver`. When on, scroll- and touch-driven glass stays at its base thickness (blur pinned to 12dp, tint at rest) and the nav-bar interaction spring is skipped, so chrome never animates against the user's accessibility preference.
- **Design-system preview skeletons.** Section 6 of the debug-only Paper-under-Glass preview renders both skeleton layouts for review; opened by long-pressing the About & updates card in Settings.

## [1.5.16] — 2026-11-04

Range: `v1.5.15..v1.5.16`.

### Changed

- **True Liquid Glass optics.** Rebuilt `GlassSurface` on the real Apple glass anatomy: transparent Haze base so blurred content supplies the color (vibrancy), low-alpha tint (0.42–0.55) instead of an 80% opaque plate, fine grain (noiseFactor 0.04), a directional top-arc specular edge replacing the uniform all-around rim, and the inner shadow moved under content at 8dp. Light mode now casts accent-tinted shadows.
- **Continuous-curvature corners.** New `ContinuousCornerShape` (superellipse, exponent 4.5) for glass chrome and sheets so curvature ramps in like iOS instead of snapping at a circular arc.
- **Live blur on every screen.** Haze sources now feed the navbar from the Home feed, Library browse/directory lists and grid, the Tools list, and the PDF viewer page column (top and bottom viewer bars included); previously only the Home feed could blur.
- **Scroll-reactive glass.** `GlassMotionState` thickens the navbar glass as content scrolls: blur ramps 12→28dp and tint densifies over the first 240px of scroll.
- **Color moved into content.** File-type inks now lift to tonal variants in dark themes; modal sheets use a translucent (94%) plate since cross-window blur is impossible.
- **Vibrancy on live glass.** A luminance-preserving saturation lift (1.18x) is applied to the blurred layer so content color reads louder through the glass instead of muted, approximating Apple's vibrancy pass.
- **Spring-settled nav glass.** Tab changes kick a damped spring through `GlassMotionState.interaction`, thickening the navbar blur momentarily before it relaxes, so glass responds to touch as well as scroll.
- **Tertiary support accent.** Storage capacity bars and the App Manager "Split APK" badge now use `tertiary`/`tertiaryContainer` semantic containers: one primary accent for action, one support accent for resource metrics.

### Removed

- **Rainbow layer deleted.** `VividGradients.kt` (7 unused Tailwind-default gradients) and `VividComponents.kt` removed; section headers fall back to a neutral editorial rule.

## [1.5.15] — 2026-11-03

Range: `v1.5.14..v1.5.15`.

### Added

- **AppDropdownMenu with Apple Liquid Glass.** Replaced standard flat opaque menus with translucent frosted glass dropdown surfaces, 16dp rounded card shapes, and 1dp luminous specular rims across all screens.
- **Home Feed Error State Banner.** Added real-time error banner presentation in `HomeFeedPane` when document loading or recents flow encounters failures.
- **Active TileBitmapCache Global Trim Registry.** Registered all active `TileBitmapCache` instances to automatically purge on system memory trim (`ComponentCallbacks2`).

### Fixed

- **MuPDF Permissions Integer Bitmask.** Computed ISO 32000-1 user permissions bitmask as a proper signed integer string (`permissions=$permMask`), fixing the parse failure in MuPDF's `fz_lookup_option_integer` so print and copy restrictions are genuinely applied.
- **App Manager "Split APK" Badge Squeezing.** Moved the "Split APK" badge to a dedicated row below the version label with `maxLines = 1` and `softWrap = false`, eliminating horizontal compression that caused character-by-character vertical text wrapping.
- **App Manager & Action Sheet Navigation Bar Overlap.** Configured `skipPartiallyExpanded = true`, full-height container filling, 45% scrim, and generous bottom content padding (`96dp`) so sheets cleanly sit above system bars and never show background content below.
- **Single-Item Action Sheet Glass Styling.** Added tactile drag handle, translucent glass surface, IME padding, and consistent scrim overlay to `SingleItemActionBottomSheet`.
- **Page Reorder Touch Targets.** Expanded all reorder buttons (Move Left, Delete, Move Right) to the standard minimum 48dp (`AppSpacing.minTouchTarget`), eliminating fat-finger deletion risks.
- **PrintHelper Toast Elimination.** Wired `onMessage` snackbar callbacks to all `PrintHelper.printDocument` callers in `ReadingControlsBottomSheet` and `PresentationViewerScreen`.

### Removed

- **Pruned 8 Tautological Unit Tests.** Eliminated mock tests performing trivial arithmetic on local variables (`assertEquals(35, 35)`, `255 - 255 == 0`, and simulated loops) from the E2E quality gate suite.

## [1.5.14] — 2026-11-03

Range: `v1.5.13..v1.5.14`.

### Added

- **Material You Dynamic Themed Icon.** Added `<monochrome>` adaptive icon vector layer so MyDoc dynamically tints with system wallpaper colors on Android 13+.
- **User-Facing PDF Security Permissions.** Wired `allowPrinting` and `allowCopying` toggle controls into Protect PDF tool UI and MuPDF permission strings.
- **Watermark Angle Controls.** Added interactive rotation chips (-45° diagonal, 0° horizontal, +45°, 90° vertical) in Add Watermark tool.
- **Storage Failure Recovery.** Added proactive error state cards in Library browse pane when storage analysis or volume inspection fails.
- **Memory Pressure Trimming.** Integrated `onTrimMemory` hooks across `TileBitmapCache` and `MainActivity` to purge and resize bitmap caches under OS memory pressure.

### Fixed

- **Eliminated Fake Redaction & Sanitization.** `redactPdf` and `stripMetadata` now strictly throw on engine failure instead of copying unredacted files or claiming metadata was purged.
- **Edge-to-Edge Keyboard Blindness.** Added `android:windowSoftInputMode="adjustResize"` and `.imePadding()` across `AppBottomSheet`, `ToolRunnerSheet`, and `SpotlightSearchSheet` so input fields and action buttons never hide behind the software keyboard.
- **Card Roof/Floor Disappearance & Liquid Glass Overhaul.** Replaced fading directional gradient borders with continuous, luminous 1dp bounding rims across light, dark, and AMOLED modes.
- **Vibrant Modern Palette.** Replaced muddy ivory/brown tones with radiant slate-white `#F8FAFC`, deep sapphire `#0066FF`, and vivid semantic badge colors.
- **Splash Screen Parity.** Splash screen animated icon now points directly to `@mipmap/ic_launcher` instead of legacy line art.
- **Library Tab Divider Inconsistency.** Removed unwanted horizontal border divider from Library header to match Home and Tools headers.
- **Touch Targets & Text Clipping.** Expanded page reorder buttons and search clear buttons to standard touch targets (≥40-48dp); fixed "Landscap" truncation with `TextOverflow.Ellipsis`.

### Removed

- **Dead Code Sweep.** Pruned 19 unused files and assets, including 10 dead mipmap PNGs, uncalled `AudioExtractor`, legacy splash XMLs, unreferenced `AppChip`, and unused color/typography tokens.

## [1.5.13] — 2026-11-03

Range: `v1.5.12..v1.5.13`.

### Added

- **Adaptive Concept 2 App Launcher Icon.** Complete high-resolution adaptive launcher icon pack installed across all density buckets (`mipmap-mdpi` to `mipmap-xxxhdpi`) and `mipmap-anydpi-v26`.
- **E2E Opaque-Box Test Suite.** Added 64 automated tests covering data safety, storage cleaner protection, office document parsing, PDF tools correctness, viewer lifecycle, and release quality gates.

### Fixed

- **Storage Cleaner safety.** Protected standard user and system directories (`Download`, `DCIM`, `Documents`, `Pictures`, `Music`, `Movies`, `Android`) against deletion; active browser downloads (`.crdownload`, `.part`) are strictly excluded from temp and duplicate scans.
- **Physical Trash purge.** Automatic 30-day purge now atomically unlinks physical files from `.trash` in addition to removing database records.
- **Zip Slip and directory traversal.** Canonical path checks enforced on all zip extractions and storage operations.
- **Spreadsheet memory footprint.** Spreadsheet viewer refactored to use virtualized viewport windowing, preventing OutOfMemoryError on large sheets.
- **Office parsing heap exhaustion.** DOCX, PPTX, and XLSX parsers now use streaming XML parsing instead of contiguous in-memory byte buffers.
- **PDF tools truthfulness & fallback.** "Redact", "Watermark", "Repair", and "Crop" now feature honest UI guidance, dual-engine fallback (MuPDF + PDFBox), and strict keyword/geometry validation.

### Removed

- **Dead assets.** Pruned unused font `bricolage_grotesque.ttf` (408 KB) and legacy duplicate night-specific launcher resources.

## [1.5.12] — 2026-11-03

Range: `v1.5.11..v1.5.12`.

### Fixed

- **CI could not start.** The `gradle/actions/setup-gradle` pin and the
  `softprops/action-gh-release` pin pointed at commits that do not exist, so every workflow failed at
  the Set up job step. Both now resolve to real tagged commits (v4.4.4 and v2.2.1).
- **No instrumented tests in CI.** A second job boots an API 35 emulator with KVM and runs
  `connectedDebugAndroidTest`, uploading the report.
- **A rejected update no longer crashes the app.** A certificate mismatch in the manual Install
  action threw `SecurityException` on the main thread; both Install entry points now surface it as an
  error state.
- **Debug builds offered an update they could not install.** Update checks are skipped in debug
  builds, whose debug signing key can never accept a release APK.
- **Legacy Word formats claimed full support.** `.doc`, `.rtf`, `.wpd` and similar fall back to
  extracted text; the reader now says so instead of presenting it as formatted output.

### Changed

- The zoom-tile cache object was renamed `ZoomTileCache` to end the name collision with the
  page-render cache in `rendering.cache`.

### Verified

- Every bundled native library is 16 KB aligned on both x86_64 and arm64 (ELF `PT_LOAD` alignment
  `0x4000`), and the APKs pass `zipalign -c -P 16`.

## [1.5.11] — 2026-11-03

Range: `v1.5.10..v1.5.11`.

### Removed

- **`:office-engine` module.** 2,042 Java files and ~472k LOC that no app code referenced, kept in the
  release by its own consumer ProGuard rule (~4.5 MB of dex). Its PDF path was disabled and several
  bundled files carried no licence grant. Removed with its Gradle wiring.
- **Dead UI and safety code:** `ZipSafeReader` (never called), `AppTabHeader`, unused Vivid components,
  `DocumentFilterChips`, `EditorialMasthead`, `EditorialRow`, `PaperSurface`, `PaperTheme`,
  `RecentsPane`, the duplicate `ui/common/AppDivider` and the unused `ui/common/EmptyState`.
- **Unused dependencies:** `zxing-core` (no QR feature exists) and `haze-materials`.
- **Placebo settings.** "Hardware Video Acceleration" and "High-Quality Vector PDF" stored a
  preference that no renderer or player read; both controls and their dead state are gone. Eleven
  further preference keys that nothing consumed (granted folder, scan scope, confirm-before-delete,
  photo export quality) were deleted.

### Fixed

- **Tool results are saved with their real type.** Save, Open and Share now use the output file's
  actual extension and MIME instead of the tool's static metadata. A multi-part split is saved as a
  ZIP (not a corrupt `.pdf`) and a stitched long image as JPG/PNG (not a `.zip`).
- **Split honours a typed page range.** The range field previously only applied when the document
  produced a single part; it now exports exactly the selected pages as one PDF.
- **No false success.** The engine rejects empty outputs, image-to-PDF fails when no selected file
  decodes, and Annotate/Redact report an error when the search text is not found instead of returning
  the input unchanged.
- **PDF to Word is labelled honestly.** The tool no longer claims Excel/PowerPoint output it never
  produced, and the file picker offers images for image-to-PDF and text types for text-to-PDF.
- **Glass surfaces are legible.** The no-blur fallback is now opaque instead of 94–96 % alpha, so
  stacked sheets no longer ghost through each other and cards remain visible on AMOLED; the sheet
  scrim and shadows follow the theme.
- **Bottom chrome no longer overlaps.** Snackbars (including the Files UNDO) and the Files FAB clear
  the floating navigation pill; the PDF contextual bar no longer applies the status-bar inset twice.
- **Accessibility and large-font layout:** file action sheets scroll, dialog buttons use the theme
  contrast colour, fixed-height headers grow with font scale, and the remaining sub-48 dp touch
  targets were raised.
- **Release-only hardening:** `DesignSystemPreviewScreen` is reachable only in debug builds; file
  names from external intents are sanitized before writing to cache.
- **CI action pins restored.** The release and build workflows use full commit SHAs again and
  `build.yml` declares read-only permissions.
- **Settings now change what they promise.** PDF page scaling drives the live reader fit preference;
  Auto-Resume and Auto-Loop video are honoured by the player; the haptic toggle silences app and
  viewer feedback through a shared gate.
- **The updater ignores the debug version suffix.** `1.5.11-debug` no longer parses as `1.5`, which
  had made a 1.5.11 debug install show "Update available: v1.5.9" and offer a downgrade. A regression
  test covers suffixed and plain versions.

### Changed

- `SECURITY.md` now documents the ML Kit metrics transport, the app-lock fail-open path, the actual
  backup exclusions and the requested permissions instead of overstating protection.

## [1.5.10] — 2026-11-03

Range: `v1.5.9..v1.5.10`.

### Changed

- **Version catch-up.** Releases 1.5.4 through 1.5.9 shipped without changelog entries; their
  sections are restored below from the commit history.

### Removed

- **Five tools whose behaviour was narrower than their labels.**
  - *Office to PDF* had no working path into the tool engine: `OfficeToPdfConverter` was a standalone
    DOCX/XLSX/PPTX text-and-table re-layout that only its own unit test called, and the bundled
    `:office-engine` renderer was never wired to it.
  - *PDF/A export* stamped a conformance marker into document metadata without validating the output
    against any PDF/A profile.
  - *Fill form* handled AcroForm text, checkbox and choice fields only; XFA forms and field-level
    validation were unsupported.
  - *Compare* diffed extracted text word sets per page, so image-only PDFs compared as empty and
    layout changes were invisible.
  - *OCR to PDF* wrote searchable text, but as invisible lines at approximate positions instead of
    per-word coordinates, so selection and copy drifted from the printed text.

  The five are gone from the tool catalog, engine dispatch and tests. The Tools screen now reports
  24 tools, and non-PDF documents only show tools that accept them.
- **Dead page-rotation controls in the visual page organizer.** The rotate buttons changed preview
  state only; `Apply & Save` sent page indices without rotation, so the rotation was silently
  discarded. Move left, move right, remove and apply now match what the engine executes.

### Fixed

- **Protect PDF requires a password.** An empty user password produced an unopenable file with no
  error, and raw passwords were interpolated into MuPDF's comma-separated option string. Passwords
  are now validated (non-blank, no comma or control characters) in both the UI and the engine.
- **No more `file://` URIs leaving the app.** Share, Open with and Set-as-wallpaper now resolve
  through the app's FileProvider (with a cache copy fallback), closing the
  `FileUriExposedException` crash and the broken share targets on API 24+.
- **Privacy documentation matches the code.** The README, SECURITY and architecture docs now state
  that document processing is on-device while the updater checks GitHub and Play Services may fetch
  the ML Kit model, and that Android backup copies the saved-document store and Room database.
- **Security documentation matches the code.** `SECURITY.md` no longer claims Apache POI conversion,
  a live Office→PDF pipeline, or that `ZipSafeReader` guards every archive. It now names the limits
  each OOXML parser actually applies and states that the shared guard is not yet wired.
- **Integration tests can run again.** Room schemas are exposed to `androidTest` assets, the
  kotlinx-serialization runtime is aligned with Room 2.8.5's schema parser, the migration test passes
  `MIGRATION_3_5`, and two stale viewer-benchmark assertions match the current middle-truncated
  title and 1-based page counter. All 8 instrumented tests pass on `mydoc_api35`.

## [1.5.9] — 2026-11-03

Range: `v1.5.8..v1.5.9`.

### Changed

- **Liquid glass reached the remaining surfaces.** Cards, bottom sheets, sheet headers, toolbars,
  the tools grid, file lists, storage cleaner, app manager, trash bin, photo info and presentation
  chrome now render through `GlassSurface`; `AppBottomSheet` uses a transparent container so the
  sheet content carries the glass.
- Version 1.5.9 (159).

### Known issues

- Sheets receive no `hazeState`, so the glass is a translucent plate rather than a blurred backdrop,
  and stacked sheets show the sheet beneath them.

## [1.5.8] — 2026-11-03

Range: `v1.5.7..v1.5.8`.

### Changed

- **App icon:** Concept 2, "Prismatic Document Prism", adopted as the official icon across all
  densities. Version 1.5.8 (158).

## [1.5.7] — 2026-11-03

Range: `v1.5.6..v1.5.7`.

### Changed

- **New launcher identity:** the "Paper under glass" icon replaces the four-pill launcher icon in
  every density. Version 1.5.7 (157).

## [1.5.6] — 2026-11-03

Range: `v1.5.5..v1.5.6`.

### Added

- Horizontal swipe between the Home and Files tabs.
- Document-type filtering in the library.
- Swipe navigation and reworked zoom in the photo viewer; updated video player controls.

### Changed

- Glass treatment extended to dialogs, the clipboard bar, file panes and spotlight search; the dead
  bottom-nav code path was removed. Version 1.5.6 (156).

## [1.5.5] — 2026-11-03

Range: `v1.5.4..v1.5.5`.

### Fixed

- Startup crash caused by invalid `HazeStyle`/`LocalHazeStyle` background colours; ML Kit kept by
  ProGuard so OCR still works in release builds. Version 1.5.5 (155).

## [1.5.4] — 2026-11-03

Range: `v1.5.3..v1.5.4`.

### Added

- **"Paper under glass" design system:** colour, typography and radius tokens, Fraunces and JetBrains
  Mono, and the `GlassSurface` component.
- Home feed with a hero card and recent grid, plus floating glass bottom navigation.
- Unified library screen with the redundant browse tabs removed.
- Viewer chrome: condensing glass top bar, glass bottom bar, search overlay and thumbnail strip.
- Tools reorganized into six task groups; settings glass toggle and storage-tools relocation.
- Scroll benchmark, `ViewerViewModel`/`UpdateManager` unit tests and a Room migration test skeleton.
- **`:office-engine`** packaged as an internal Gradle module (native Office renderer, 2,042 Java
  files). It is not referenced by any app code and is not wired to any tool.

### Fixed

- Hardening pass across document parsers: bounded reads, ZIP entry limits, typed `Result` for file
  operations, `CancellationException` rethrown, empty catches removed.
- Biometric guard, navigation insets, theme selector and accessibility follow-ups; debug
  `applicationIdSuffix`; CI signing injection and test-report upload.
- Test isolation: ViewModels created and cleared inside their test scopes.

## [1.5.3] — 2026-11-02

Range: `v1.5.2..v1.5.3`.

### Changed

- **Build toolchain migration:** Upgraded Android Gradle Plugin to 9.5.0, Gradle wrapper to 9.6.0, Kotlin Gradle Plugin to 2.3.21, KSP to 2.3.13, and Room to 2.8.5 with built-in Kotlin DSL.
- **compileSdk raised to 37:** Updated to compileSdk 37 to support modern AndroidX 2026 releases while keeping runtime targetSdk at 35.
- **Dependency modernization:** Bumped 20 dependencies across AndroidX, Compose BOM (2026.09.00), CameraX (1.6.2), Exifinterface (1.4.2), Media3 (1.11.1), Coroutines (1.11.0), and Testing libraries.
- **MuPDF updated to 1.28.5:** Upgraded `com.artifex.mupdf:fitz` from 1.24.4 to 1.28.5, ensuring native libraries are 16 KB page-size aligned (`Aligned16KB`) for Android 15+.
- **Hero transition animation:** Migrated deprecated `ResizeMode.ScaleToBounds` to `ResizeMode.RemeasureToBounds` for Compose 1.12.

### Fixed

- **MuPDF search API deprecation:** Updated search invocations in `PdfEditingEngine` and `PdfSecurityEngine` to use `Page.search(...)` resolving Fitz 1.28 symbol removals.
- **Thread and locale lint hygiene:** Resolved 4 Lint 32 checks across `SignPdfSheet` (`ThreadConstraint`), `FileManagerDialogs` (`NonObservableLocale`), and `SettingsScreen` (`LocalContextGetResourceValueCall`).

## [1.5.2] — 2026-11-02

Range: `v1.5.1..v1.5.2`.

### Added

- **`AppTopBar`, one top-bar wrapper.** Files, Settings, spotlight search and the viewer header now
  come from a single component: title and subtitle slots, action slot, container colour and bar
  insets are declared once instead of re-cut per screen.
- **`AppMessages` snackbar bus.** `Context.showMessage(text)` replaced all 54 `Toast.makeText`
  calls across 19 files, rendered by one `AppSnackbarHost` at the activity root. Toasts ignored the
  app theme, landed wherever the system put them and could not be dismissed; the host renders them
  themed, in position and dismissible. Screens that need an action (trash undo) keep a local host.
- **A designed splash.** `androidx.core:core-splashscreen` with a purpose-built document mark on a
  plate matched to the window background, declared in both the compat and the API 31+ platform
  attribute sets. The system splash previously showed the four-pill launcher icon. The mark is held
  until the first Compose frame with a 1200 ms deadline, so a stalled first frame cannot trap the
  user on the splash.
- **`UnlabeledTapTarget` lint rule** (`lint-rules/`, wired through `lintChecks`) reports
  `IconButton`, FAB and clickable surfaces whose `contentDescription` is null or missing, so an
  unlabeled control can no longer be merged.
- **`AppRadius.inset` (12dp) and `DocumentReadingType`.** The radius scale now steps cleanly
  8/12/16/20/24/28 with a named tier for elements nested inside a card, and the reader's document
  text sizes (heading levels, body, table cells) live in the token layer rather than in the views.
- **`LibraryItemTest`** covers the identity key the search list dedupes on.
- **First instrumented UI tests.** Five Compose tests in a new `androidTest` source set, aimed at the
  things that broke silently rather than at easy coverage: the search list dedupes a duplicate
  identity to one row and its count label agrees, a query narrows list and count together, tapping a
  result reports that item, `AppTopBar` renders title, subtitle and a working back action after the
  `modifier` reorder, and the `AppMessages` bus reaches the snackbar host. All five pass on
  `mydoc_api35`.

### Changed

- **Spotlight search is a routed screen.** It was a `Dialog` with `usePlatformDefaultWidth = false`
  plus its own system-bar overlay — a whole screen dressed as a modal. It now shares the activity's
  navigation, so back, insets and the header behave like every other destination. Trash, camera
  capture and the cropper stay dialogs: they are modal tasks, not places.
- **One back handler instead of seven.** The mutually guarded `BackHandler` stack collapsed into a
  single ordered pop over the active-screen state.
- **`ui/filemanager` is `ui/files`.** The file-manager cluster moved in with its owner.
- **Every literal corner radius and font size in the app layer is a token.** 45
  `RoundedCornerShape(<n>.dp)` and 14 `.sp` literals were folded onto `AppRadius` and the type
  scale. Off-scale radii snapped to the nearest step (10/14 → 12, 18 → 16, 22 → 20), the 2dp tab
  indicator's 1dp radius became `roundShape` (clamping makes them identical), and thumbnails take
  one radius from the scale instead of threading a `cornerRadius: Int` through five composables.
- **Dead resources removed.** 20 unused strings, the unreferenced `drawable/ic_launcher_foreground.xml`,
  and ten night-mode icon PNGs that were byte-identical to their day twins. The night
  `ic_launcher.png` and `ic_launcher_round.png` differ from the day copies, so they were checked by
  blob hash and kept.
- **Six version checks below `minSdk 26` deleted** — the M/O branches in haptics, audio playback
  parameters, the installer helper, `StaticLayout` creation and the PiP gate. `mipmap-anydpi-v26`
  became `mipmap-anydpi` for the same reason.
- **Test dependencies moved into the version catalog**, which also aligned `kotlinx-coroutines-test`
  with the runtime it tests (it was pinned to 1.8.1 against a 1.10.1 runtime).
- **`kotlinOptions` became `compilerOptions`** in `app/build.gradle.kts`, and 18 composables reordered
  `modifier` to the first optional parameter.
- **Boxed state replaced with typed state** in the three places that held an `Int`/`Long` in a plain
  `mutableStateOf` (search match index in both readers, cache size in Settings), which was allocating
  a wrapper per update.

### Fixed

- **Search could list one document twice, and its header count disagreed with the list.** Results
  are deduped by identity and the count reads the same collection the list renders, so the two can
  no longer diverge. The list key moved to the identity as well, which keeps Compose state attached
  to the right row.
- **The typed-signature placeholder resized the moment you typed.** The empty state was 20sp over a
  32sp input; placeholder and field now share one `displaySmall` italic style.
- **Archive entry names were locale-dependent.** `String.format` without a locale used the device
  default, so on Arabic, Persian or Thai locales `%03d` wrote non-ASCII digits into zip entries
  (`page_٠٠٧.png`) inside the archives the split and image-export tools produce. Five sites now pin
  `Locale.ROOT` for machine identifiers and `Locale.US` for the one displayed number, which also
  stops "Compression saved only 12,3%" on German and French locales. A JVM probe of `%02x` across
  seven locales came back ASCII everywhere, so the four hex-digest call sites are deliberately
  unchanged.
- **Three ViewModels held an Activity context.** `MainActivity` passed itself straight into
  `StorageViewModel.Factory`, and the viewer and spreadsheet factories forwarded
  `LocalContext.current`. All three ViewModels now take `Application`, resolved through
  `applicationContext` in the factory, so a rotation cannot pin a dead Activity for the life of the
  ViewModel.
- **The text and Word readers recomposed the whole viewer on every scroll frame.** Both read
  `firstVisibleItemIndex` in composition — once as a `LaunchedEffect` key, once as the header's page
  number. The index now comes from a `derivedStateOf`, and reading progress is persisted through
  `snapshotFlow` instead of a composition-time read.
- **PDF text extraction re-parsed the whole document for every page.** `AndroidPdfRenderer` called
  `PDDocument.load(file)` inside `getPageText`, `getDocumentText`, `search` and `getOutline`, so
  reading text across N pages cost N full parses. One document is now opened per renderer and closed
  with it.
- **Edited Office documents reopened the stale PDF.** The conversion cache key was
  `MD5(uri + fileName)`, so saving a document under the same name kept serving the PDF rendered from
  the previous version. The key now carries the source's size and modification time, falling back to
  the old behaviour when a provider reports neither.
- **Picture-in-picture entered without a source rect or auto-enter.** The player's measured bounds
  are now passed to `setSourceRectHint`, and `setAutoEnterEnabled(true)` is set on API 31+ — the two
  calls lint asks for at `targetSdk 31` and above.
- **The signature plate was a stadium, not paper.** A 200dp-tall box clipped with `roundShape` clamps
  to a full pill; it now uses `cardShape`, matching every other surface in the sheet.

## [1.5.1] — 2026-11-02

Range: `v1.5.0..v1.5.1`.

### Added

- **Dynamic Material You theming.** Colours are seeded from the wallpaper on Android 12+ (fixed
  fallback elsewhere), with the black/AMOLED override preserved. The existing token layer is fed
  from the generated scheme, so legacy screens pick up the same palette.
- **Files-first two-tab navigation.** Home is now just Files and Tools; the Files screen owns the
  storage readout, search and settings entry, and the bottom bar is a Material 3 navigation bar.
- **New Files screen.** Recents and Browse share one header and one list/grid preference. Browse
  drills into storage with breadcrumbs, sorting, hidden-file toggle and storage insights; folders
  and files use real page/photo thumbnails.
- **Selection and bulk actions.** Long-press enters multi-select with share, copy, cut, rename,
  compress and recycle actions; the clipboard bar shows the pending operation and its destination
  hint.
- **Storage insights sheet** breaking usage down by category with a shortcut to the cleaner.
- **Tools tab rebuilt on Material 3.** One searchable list grouped by task, a filter-chip strip,
  search that focuses on open, and tool rows on tonal plates replacing the gradient tiles.
- **Tool runner sheet on Material 3.** Header, input list, progress, result verification and the
  save/open/share actions now use tonal surfaces and standard buttons; the "add more files" action
  appends to the input list instead of replacing it.
- **Tool sheets rebuilt on Material 3.** The page organizer, form filler and signature pad are now
  full-height Material 3 modal sheets with tonal headers, outlined cards, standard text fields,
  switches, dropdowns and filled action buttons that respect the current theme's insets.
- **Viewer chrome rewritten on Material 3.** The document top and bottom bars, PDF floating action
  bar, page action sheet, document tools sheet, reading controls, page grid, thumbnail strip,
  search overlay, jump/password dialogs, outline drawer, fast scrollbar and presentation bars now
  use tonal surfaces, standard shape and type scales, filter chips and standard buttons. The
  vivid gradients, custom chips and bespoke radii are gone from the viewer.
- **Scanner rebuilt on Material 3.** The capture screen drops its gradient and glow treatment for
  a single primary accent, standard type scale and theme-aware controls, while keeping the dark
  scrims that read correctly over a live viewfinder.

### Changed

- **Settings rewritten on Material 3.** Cards, section labels, theme and reading chips, toggles,
  cache actions and the update section all use the generated scheme and M3 components.

- **Search, clipboard bar and folder chrome rewritten on Material 3.** The spotlight sheet is a
  surface-based sheet with filter chips and thumbnails; the clipboard bar is an elevated surface
  with a single-line summary.

### Fixed

- **Paste from a category view wrote into the last folder.** Copying a file and then opening a
  category pasted into the previous directory and dropped the user on the browse hub. The clipboard
  bar now only appears while browsing a real folder, and paste stays in that folder.
- **Form choice fields showed bracketed values.** A combo box value came back as
  `[United States]` from PDFBox, and the editor both displayed and saved the brackets. Choice values
  now come from the selected option list and round-trip cleanly.
- **Page organizer actions sat behind the system taskbar.** The organizer's fixed action bar was
  covered by the gesture-nav taskbar; as a full-height sheet it now stays in reach.
- **New-folder button covered the Paste button.** The FAB steps aside while the clipboard bar is up.
- **Opening a document reset Files to Recents.** The pane is restored from the view model, so
  returning from the viewer lands back in the folder.
- **System bars followed system night mode, not the app theme.** Status and navigation bar icon
  appearance now tracks the app's chosen theme.
- **Document tools sheet opened half-expanded and cut off the list.** It now opens fully expanded,
  so the tool list and its bottom padding stay above the navigation bar.
- **Splash and window background sat on the old paper palette.** The window background now matches
  the Material 3 light surface and AMOLED black, so app launch no longer flashes a different tone.
- **Word documents opened as one blank page.** The offscreen WebView that was supposed to lay the
  document out is never attached to a window, so its measured height was zero and the band slicer
  emitted a single empty page and reported success. Word files now paginate through a native
  renderer built on the existing DocxParser, honouring the document's own page size and margins.
- **Legacy presentations and ebooks were reported as damaged.** `.ppt`, `.pps`, `.pot` and `.odp`
  had no converter, so the raw package was handed to a PDF engine; `.epub`, `.cbz`, `.xps` and
  `.fb2` routed to the text viewer because `DocumentCategory.EPUB` was an alias of `TEXT`. Both
  now reach engines that parse them.
- **Every reopen repaid the full conversion.** Cache keys were the file name alone, or the cached
  PDF was deleted when the viewer closed. Keys now include a content or size+mtime stamp, writes
  are staged and moved into place so a half-written PDF cannot be cached as valid, and failures
  log instead of surfacing as "the file may be damaged".
- **An HTML or CSV table saved as `.xls` opened as an empty grid.** The legacy reader only scraped
  UTF-16 byte pairs, so a single-byte text file yielded no cells at all. Non-OLE2 payloads are now
  read as an HTML table (markup stripped, entities decoded) or as delimited text first.
- **Viewer content clipped the first spreadsheet row.** A fixed 64dp top pad was added under a bar
  that already sits below the status bar.

### Changed

- **Startup and browsing no longer touch disk from the main thread.** Volume enumeration is a
  suspend call, `LibraryItem` size and date strings are computed lazily off-main with a
  thread-safe date formatter, and the recents, continue-reading and document flows `flowOn`
  `Dispatchers.Default`.
- **The landing junk scan is bounded.** The Files pane shows a reclaimable estimate, and the scan
  that feeds it walked the whole volume with no limit; it now stops at 4,000 entries or 600 ms,
  and opening the cleaner still scans everything.
- **Trash and restore cost one MediaStore round trip instead of one per file.** The per-file
  awaited scan capped every delete at 2 s; paths are now batched into a single call.
- **Fewer recompositions and search storms.** Files list and grid rows read selection state
  outside their item lambdas, document text search is debounced by 250 ms, the first screenful of
  page sizes is primed off-main before the viewer publishes its state, and the zoom tile cache is
  cleared when a viewer dies instead of outliving its document.
- **Bitmap caches are bounded by bytes.** The document thumbnail cache moved from 64 entries to a
  6 MB budget, library thumbnails no longer stage a source larger than 40 MB into cache for a
  44 dp tile, and the unused far-page eviction pass was deleted.

## [1.5.0] — 2026-11-02

Range: `v1.4.6..v1.5.0`.

### Changed

- **New visual language: paper.** Home, Files and Settings now render as warm paper sheets with
  ink text, a single blue accent, fibre grain, folded corners, flat icon plates and printed rules.
  The gradient plaques, aurora backdrop, spectrum rules and gradient chips are gone from these
  screens; the storage readout is a paper card with a square accent gauge.
- **Shared chrome follows the host screen.** The tab header rule, section rules and filter chips
  read a `PaperTheme` scope, so Tools and the viewer keep the vivid language while papered
  screens stay on paper. Dark and AMOLED paper tones were lifted off pure black so sheets remain
  visible.
- **Home deck and Index list every recent.** The deck was stuck at 01 because continue-reading
  items were filtered against the recents list they were merged into; all recents now appear once.

## [1.4.6] — 2026-11-02

Range: `v1.4.5..v1.4.6`.

### Fixed

- **Theme toggle left list content on the old palette.** Lazy list items did not invalidate when
  the palette composition local changed, so after flipping the theme the header and nav updated
  while every card, chip and row kept the previous theme's colors until the tab was re-entered.
  The palette is now folded into the lazy item keys on Home, Tools and the Files hub, so items
  rebuild the moment the theme changes.
- **Scanner layout and chrome.** The camera dialog lays Compose out against the display while its
  window decor sits inside the system bars, so inset padding reported zero and the shutter bar was
  pushed under the navigation bar (the sheet's bottom edge landed at 2534px on a 2274px-tall visible
  area). The safe area is now measured against the window with `getWindowVisibleDisplayFrame` and the
  bar bands are dimmed, so the controls sit fully inside the visible window. The page head is a
  masthead (kicker, title, tabular page folio, 2dp rule), the aspect modes and scan filters are
  rule-underlined tabs instead of pill groups, the viewfinder is centred in the camera window between
  the two chrome bands, thumbnails are hairline-framed pages with tabular folios, and the commit
  action is a single accent stamp.
- **Scan output was dropped.** `selectTool()` resets the input list, and the scanner called
  `setInputUris()` before it, so committing a scan opened "Scan to PDF" with no input and a disabled
  run button. The tool is now selected first, then the captured pages are attached.

### Changed

- **Vivid sweep across the tool runner.** The tool sheet opens on a gradient header plaque in the
  tool's section hue and closes its header on the 3dp spectrum rule; the empty input picker is an
  accent-tinted drop card, attached files are soft surface cards with ocean document plaques, and
  progress tracks use the pill gauge. Run and Save are glowing brand-gradient pills with white
  labels, the verification panel, Open / Share and the error retry are soft cards, and the success
  header carries a forest check plaque. Inline option launchers (visual page organizer, form
  editor, signature pad) became secondary framed cards with accent iconography instead of
  competing full-width gradients, so the Run CTA is the only primary action on the sheet. This
  also fixes black labels and icons that sat on accent-filled option buttons in light mode.
- **Vivid sweep across Settings, reader chrome and scanner.** The last editorial surfaces join
  the vivid language. Settings sections are soft cards with gradient icon plaques (grape
  Appearance, ocean Reading, sunset Media, forest Storage, fire Privacy, brand About) on an
  aurora backdrop; folio numbering is gone; toggles, row icons and the update progress bar run
  on the accent and brand gradient; inline actions are soft accent pills. The reader's top and
  bottom bars close on the 3dp spectrum rule, the page scrubber and active sheet tab are accent /
  gradient, and the bookmark fills with accent. The scanner's chrome bands close on spectrum
  rules, mode and filter tabs carry gradient underlines, the page count is a gradient pill, and
  the commit stamp is a glowing brand-gradient tile. `AppSectionHeader` is now title plus a short
  spectrum underline everywhere (Home, Files, Tools, sheets), so section breaks read identically
  across the app.
- **Vivid premium redesign.** The editorial print grammar is replaced by a saturated, glowing
  design language on Home, Files, Tools and the root nav. New tokens in
  `designsystem/tokens/VividGradients.kt` define seven fixed gradient families (brand, sunset,
  ocean, forest, gold, grape, fire) plus colored `glow` shadows; `AppColors` gains pink, cyan,
  amber and green accents and richer light/dark/AMOLED palettes; `AppRadius` softens (cards 16dp,
  sheets 24dp, pill chips). New primitives in `designsystem/components/VividComponents.kt`:
  `AuroraBackdrop` (slow-breathing radial blooms behind every tab), `VividRule` (the 3dp spectrum
  line that closes every header) and `VividIconTile` (gradient plaque with a matching halo).
  Home opens on a glowing gradient scan hero with a frosted camera disc, three gradient action
  tiles and soft cards with colored format badges; Tools and Files list hue-coded cards per group
  and category; the storage readout is a card with a spectrum gauge; selected chips and the active
  nav pill wear the brand gradient inside a floating pill bar.
- **Editorial reading surface.** Home, Files, Tools, Settings and the reader chrome now share one
  printed-page grammar instead of separate card systems. New primitives in
  `designsystem/components/EditorialRules.kt` (`EditorialRule`, `EditorialMasthead`,
  `AppSectionHeader` with folios, `EditorialAction`) and `EditorialRow.kt` replace the surface,
  1dp-border and accent-overline cards. Section breaks are 2dp ink rules, dividers are 0.5dp
  hairlines, lists are ruled rows with no card, and the only filled shapes are ink chips.
- **Home.** The horizontal quick-action scroller is a fixed four-cell action band (Scan, Merge,
  Compress, Sign) that shows every action without scrolling. "Continue reading" carries progress as
  a 2dp accent rule; recent documents are ruled rows with format and size meta.
- **Files.** The storage card, category tile grid and volume cards are replaced by a single index:
  a tappable storage readout with a 2dp gauge, then numbered Collections, Devices, Pinned and
  Utilities rows. Empty categories read "Empty"; app counts are singularised. Removes
  `EditorialStorageOverviewCard`, `EditorialCategoryGrid` and `StorageVolumeCards`; file rows and
  grid tiles drop their cards for paper rows with an ink-stamp selection.
- **Tools.** The duplicated "Featured essentials" cards are gone; tools are a numbered index of
  ruled rows per group, with the category chips as the only filter.
- **Settings.** Sections are numbered folio headings (01–06) with hairline rows instead of cards;
  switches and progress gauges are ink, clearing actions are accent text links, and toggle
  subtitles were shortened so rows hold one or two lines on a phone width.
- **Reader chrome.** Top and bottom bars are paper-coloured ink chrome closed by hairlines; the
  title is `titleSmall` with a tabular `PAGE 01 / 02` folio, glyphs are outlined, the overflow menu
  uses the editorial menu shape, sheet tabs are rule-underlined entries, and the scrubber is ink
  with a floating folio above the thumb.
- **Tokens.** `AppColors.track` (ink at 16% alpha) is the unfilled gauge and scrubber track, because
  the hairline colour disappeared against the dark surface. `AppSpacing` now documents two strokes
  only (`hairline`, `rule`) and sets `listRowHeight` to 56dp.
- **Press physics.** `pressScaleEffect` is critically damped; nothing overshoots 1.0x.

### Verification

- `./gradlew :app:testDebugUnitTest` — 125 tests, 0 failures.
- `./gradlew :app:assembleDebug` — debug APK builds.
- `./gradlew :app:lintDebug` — 0 errors, 146 warnings, unchanged from 1.4.5.
- Visual pass on `mydoc_api35` in light and dark: Home, Files, Tools, Settings and the PDF reader.
  Confirmed the action band shows all four actions, storage gauges and the reader scrubber are
  visible in both themes, and the reader's folio, three-action footer and ink selection read
  correctly. AMOLED, rotation, larger font scales and motion remain unverified.
- Scanner checked on `mydoc_api35` in Document (empty and one page), ID Card and Full View: measured
  the shutter band at 2015–2274px (flush with the navigation bar), the viewfinder centre at 1231px
  against a camera-window centre of 1231px, and the commit stamp opening "Scan to PDF" with the
  captured page attached.

## [1.4.5] — 2026-11-01

Range: `v1.4.4..v1.4.5`.

### Fixed

- **Overlay windows followed the platform bar theme.** Bottom sheets and dialogs own a separate
  window, so the activity's edge-to-edge bar appearance never reached them: on a dark theme the
  sheet drew a light navigation bar. The new `OverlaySystemBars()` in
  `designsystem/components/OverlaySystemBars.kt` resolves the enclosing window through
  `DialogWindowProvider` and applies `AppTheme.isDark` to that window's controller. It is called
  from `AppBottomSheet`, `AppDialog`, and the 22 hosts that build `ModalBottomSheet`/`Dialog`/
  `AlertDialog` directly, so the fix covers the whole class, not one screen.
- **Chip strips clipped the next chip.** Horizontally scrolling rows cut the following chip at the
  viewport edge with no hint that more content existed. `Modifier.horizontalScrollWithFade(state)`
  in `designsystem/components/ScrollEdgeFade.kt` erases a 24dp ramp at each edge with
  `BlendMode.DstOut` over an offscreen layer, driven by `canScrollBackward`/`canScrollForward`, so
  the fade resolves to the real surface underneath on light, dark and AMOLED. Applied to the file
  and format filter chips, home quick actions, file-manager category chips, the Tools category
  strip, tool option rows, scan filter presets, spotlight filter pills and viewer tool pills.
- **Storage legend hid half its data.** The overview card's legend scrolled sideways and showed
  about four of its eight entries. It is now a `FlowRow` that wraps, so files, photos, videos,
  audio, apps, other and free space are all visible at once with no hidden destination.

### Verification

- `./gradlew :app:testDebugUnitTest` — 125 tests, 0 failures.
- `./gradlew :app:assembleDebug` — debug APK builds.
- `./gradlew :app:lintDebug` — 0 errors, 146 warnings, in issue classes unrelated to this work:
  `GradleDependency` (64), `ModifierParameter` (21), `UnusedResources` (19),
  `IconDuplicatesConfig` (10), `ObsoleteSdkInt` (7), `DefaultLocale` (4),
  `FrequentlyChangedStateReadInComposition` (4), `AndroidGradlePluginVersion` (3),
  `TrustAllX509TrustManager` (3), `StaticFieldLeak` (3), `AutoboxingStateCreation` (3),
  `UseTomlInstead` (3), plus one each of `ScopedStorage`, `SetJavaScriptEnabled`,
  `QueryPermissionsNeeded`, `PictureInPictureIssue` and `SourceLockedOrientationActivity`.
  None of them are in the files added by this release.
- Visual pass on an API 35 emulator (`mydoc_api35`), light, dark and AMOLED: nine screens captured
  to `screenshots/` (`01-launch` … `09-quickactions-scrolled`). Overlay bars were confirmed at the
  window level with `dumpsys window`: the sheet's dialog window no longer reports
  `LIGHT_NAVIGATION_BAR` in dark theme, and still reports it in light theme where a white bar is
  correct. Chip fades were confirmed by sampling the strip edge pixels (a ramp, not a hard cut)
  before and after a swipe. The legend was confirmed to render every entry without scrolling.
  Layout and motion at other screen sizes, rotation and non-default font scales remain unverified.

## [1.4.4] — 2026-11-01

Range: `v1.4.3..v1.4.4`.

### Changed

- **Design foundation rebuilt.** One token layer now owns colour, radius, spacing, typography and
  motion (`designsystem/tokens`, `designsystem/animation`): warm paper neutrals, ink text, a single
  violet accent, a 4dp spacing grid, a six-step radius scale, and a crisp `AppMotion` timing
  contract. Every interactive surface uses the same `pressScaleEffect` press physics.
- **Brand surface.** Launcher and splash re-cut to the editorial identity; the `Stacks` name is
  gone from metadata and generated artefacts.
- **File manager decomposed.** `FileManagerScreen` is no longer the owner of its own chrome:
  extracted `FileManagerActionSheet`, `FileManagerClipboardBar`, `FileManagerSelectionTopBar`,
  `FileManagerBreadcrumbBar`, `FileManagerTopBar` and `FileManagerOverlays`. The screen is down to
  895 lines from 1,272, then further by the later top-bar/overlay split.
- **Google Files hub decomposed.** `GoogleFilesHubContent` split into `EditorialStorageOverviewCard`,
  `EditorialCategoryGrid` and `StorageVolumeCards` (634 lines removed from the hub).
- **Settings decomposed.** `SettingsScreen` split into `SettingsAppearanceSection`,
  `SettingsReadingSection`, `SettingsStorageSection`, `SettingsMediaSection`, `SettingsAboutSection`
  and `SettingsComponents` (793 lines removed from the screen).
- **Presentation viewer decomposed.** Extracted `PresentationSlideCards`,
  `PresentationEmptyRecoveryCard`, `PresentationBottomBar`, `PresentationTopBar`, and
  `PresentationSpeakerNotesSheet`. `PresentationViewerScreen` is down to 727 lines from 1,871.
- **PDF viewer and engines decomposed.** `PdfViewerScreen` split into `PdfFloatingContextualBar`,
  `PdfPageActionSheet`, `PdfViewerDialogsAndSheets`, `PdfViewerControlsOverlay` and
  `PdfContextualActionsHost`. `PdfToolsEngine` split into `PdfSecurityEngine`,
  `PdfPageOperationsEngine`, `PdfConversionEngine`, `PdfOptimizationEngine`, `PdfEditingEngine` and
  `PdfScanOcrEngine`, with shared helpers in `PdfToolUtils`. The remaining facade is 433 lines from
  1,711; its public API is unchanged.
- `OfficeToPdfConverter.generateCacheKey` now takes a `uriString` instead of a `Uri` so the
  cache-key derivation can be unit tested. Behaviour is identical.

### Fixed

- **Update verification on Android 13+.** `PackageInstallerHelper` now reads archive and app
  package info through `PackageInfoFlags` on API 33+, instead of relying on the deprecated
  int-flag overloads that are supressed today and removed in a future SDK.
- **`Stacks` leftovers.** The APK extraction directory is `Download/MyDoc/ExtractedAPKs` (was
  `Download/Stacks/ExtractedAPKs`), and PDF/A producers plus the conversion report say `MyDoc`.

### Added (tests)

- `PackageInstallerHelperTest` — signature and package-verification paths.
- `OfficeToPdfConverterTest` — deterministic cache keys and temp-file hygiene.
- `ToolModelsTest` — `formatByteSize` and tool savings calculations.
- `PdfToolUtilsTest` — additional coverage alongside the extraction.

### Changed (design-system consolidation)

- **One chip.** `AppChip` is the only chip implementation; `AppFilterChip` is a thin alias for
  callers that still name the filter role. The five hand-rolled chips (format filters, file
  categories, theme pickers, tool sticky filters, preview swatches) now render through it, so they
  share selection colour, border, minimum touch target and press physics.
- **Raw colours tokenized.** 135 hex literals across the UI layer moved into
  `DocumentCanvasColors`, `ToolGroupColors`, `CaptureChromeColors` and `StorageChartColors`, or onto
  the existing semantic roles (`error`, new `warning` and `scrim`). Document-content defaults in
  `PptxParser` stay literal on purpose — they are file colours, not app chrome.
- **Raw radii tokenized.** All 140 `RoundedCornerShape(N.dp)` literals outside the token package now
  reference `AppRadius`; the scale gained `large` (16dp) and `xLarge` (24dp) so 16/18 and 22/24/26/28/32
  literals collapse onto one step each. Thin progress bars and drag handles use `roundShape`. Bottom
  sheets that carried 24dp or 28dp tops now use the documented `sheetShape` (20dp).
- **No per-call text sizes.** Chrome text that overrode `fontSize` (`15`, `12`, `11`, `9`,
  `17`, `14` sp) now uses a typography role. Reading-scale and document-rendering sizes are
  untouched, because they are content, not chrome.
- **No `isDarkTheme` plumbed through screens.** `HomeScreen`, `HomeTabContent`, `ToolsScreen`,
  `AudioPlayerScreen` and `SpotlightSearchSheet` read `AppTheme.isDark` from the composition instead
  of receiving a boolean that duplicated `LocalIsDark`.
- **Redundant OLED branches removed.** `AppTheme` already swaps in `BlackAppColors` for AMOLED, so
  `if (isOled) <literal> else colors.X` and `if (isOled) colors.surface else colors.surfaceContainer`
  were always the same value; they are now just `colors.X`. `EditorialStorageOverviewCard` lost its
  unused `isOled` parameter.
- **Storage legend matches the bar.** The overview card's stacked bar draws six segments
  (docs, photos, videos, audio, apps, other); the legend now labels all of them plus free space
  instead of hiding Apps and Other. Entries with no destination are no longer clickable.
- **Dead colour and API removed.** `StorageCategoryType.color` (zero consumers, ten literals) is
  gone. Unused imports across the UI layer were dropped after the tokenization pass.

### Verification

- `./gradlew :app:testDebugUnitTest` — 125 tests, 0 failures.
- `./gradlew :app:assembleDebug` — debug APK builds.
- `./gradlew :app:lintDebug` — 0 errors, 146 warnings, all in issue classes unrelated to this
  work: `GradleDependency`/`AndroidGradlePluginVersion` (64+3), `UnusedResources` (19),
  `IconDuplicatesConfig` (10), `ObsoleteSdkInt` (7), `DefaultLocale` (4), plus one each of
  `ScopedStorage`, `SetJavaScriptEnabled`, `QueryPermissionsNeeded`, `PictureInPictureIssue` and
  `SourceLockedOrientationActivity`.
- No device or emulator was available: layout, motion and colour changes are unverified visually.
