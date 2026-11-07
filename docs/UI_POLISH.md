# MyDoc detail polish

Keep the Ink & Seal identity: ivory or graphite surfaces, teal accents, Fraunces display headings and Inter controls. Reserve glass for floating navigation and controls; file content needs stable contrast.

## Current batch

- Home: place the preview/list switch alongside the recent-files heading with an accessible action name; remove its separate list row.
- Settings: show the category title once in the top bar; retain section labels, settings, explanations and confirmations.
- Tools: replace three rows of category chips with a compact menu exposing all seven choices and the current selection.
- Tools recovery: a search with no results in the selected category must offer Clear filters even when other categories contain matches.

## Next passes, in order

1. File browsing: align filename, metadata, thumbnails and overflow controls across list, grid, search and recents. Check long filenames, absent metadata, selection, clipboard and permission failures.
2. Reading and media: align toolbars and menus, make control visibility predictable, check progress labels and resume feedback, and keep content clear of floating controls.
3. Feedback: unify loading placeholders, progress, cancellation, success, errors and undo. Every empty state should explain the condition and offer a relevant action.
4. Microdetails: inspect icon optical alignment, consistent touch targets, pressed/focused/selected states, divider thickness, corner radii, punctuation and animation timing.
5. Adaptation: inspect narrow and wide screens, light/dark/AMOLED, 2x text, landscape, right-to-left layout, keyboard navigation and TalkBack traversal.

## Acceptance for each screen

- A clear first action and visual hierarchy; no repeated title or decorative panel without a purpose.
- Related controls grouped together; spacing follows shared tokens.
- Essential labels and explanations reflow; truncation is deliberate and recoverable.
- Interactive controls have accessible names, visible state and at least 48 dp touch targets.
- No content hidden beneath navigation, keyboard, clipboard or transient feedback.
- Verify populated, empty, loading, error and selection states where applicable.
- Capture representative rendered screens before considering visual work complete. Record device and theme; emulator checks do not establish physical-phone performance.

## Coverage

This batch covers Home section controls, Settings heading hierarchy and Tools category navigation. It is not an exhaustive review of every reader, media player, dialog or file operation.

## Verification of this batch

- Six focused Compose device tests passed on the API 35 emulator, including the category-search recovery regression.
- Rendered Home and Tools checked with normal and 2x system text; the category menu exposes all seven choices at 2x. Settings Appearance checked in light theme with the duplicated heading removed. Dark/AMOLED Home and Tools also inspected.
- Final debug build, Android test build, unit tests (466 passing) and lint passed with zero lint errors. Logs: `app/build/detail-final-checks.log` and `app/build/detail-device-tests.log` (ignored local artifacts).
- Physical-device frame timing, full TalkBack traversal, keyboard navigation, translated layouts and remaining screens still need their dedicated passes.
