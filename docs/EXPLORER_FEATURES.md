# Explorer workspace

## Scope and build order

1. Folder tabs and optional dual-pane browsing: independent navigation/selection, shared copy/cut clipboard, up to six tabs. Side-by-side panes require at least 900 dp. Back applies to the active pane. Closing a busy tab is disabled.
2. Advanced search: current folder or descendants, name, extension, byte-size range and modified-within-days filters. Hidden-file preference applies. Traversal is cancellable, bounded and never follows symbolic links. Partial results are labelled.
3. Batch rename: preview prefix/suffix, literal replacement and optional numbering, preserving extensions by default. Validate the whole plan before writes. Stage names without overwriting, then publish. On failure restore originals where possible and explicitly report any unresolved paths.

All capabilities use the existing Material 3 / Ink & Seal theme and string resources. No new dependency, permission, network integration or database schema is needed. Source-file mutations remain user-triggered.

## Acceptance

- Tabs keep independent folders, searches and selections while clipboard items can be pasted in another tab/pane.
- Search applies all filters together and explains truncated/unreadable traversal. Selection covers only visible results.
- Rename changes nothing until confirmation; collisions, unsafe names, stale sources and duplicate targets are rejected. Failure rollback is tested through injected filesystem faults.
- Focused JVM tests, full unit suite, debug assembly and Android Lint pass. Device UI checks are reported separately and only performed with disposable app data.

## Commands

`./gradlew.bat testDebugUnitTest --tests "*Explorer*Test"`

`./gradlew.bat testDebugUnitTest assembleDebug lintDebug`

## Using the features

Open Library, then use + to add a tab. On screens at least 900 dp wide, the split button shows two folders. Tap a pane to direct Back navigation to it. Copy or cut in one tab and paste in another.

In a folder, choose Advanced search from the menu. Size values are KiB. Clear search returns to the folder. Recursive search stops at 1,000 matches or 50,000 entries and labels incomplete results.

Select multiple files and choose Rename to review every proposed name before applying. Interrupted renames retain a private recovery journal and display original, proposed and hidden staging paths after restart. Recovery is manual; the app never guesses which existing file to overwrite.

## Validation (2026-11-06)

427 JVM tests and both focused Android UI tests passed on the API 35 emulator. Debug assembly and Android Lint passed (existing warnings remain). Phone Library navigation and wide-screen split rendering were checked in disposable emulator data; no device runtime crash was reported. Real-device performance and interrupted-operation recovery UI still need manual release testing.
