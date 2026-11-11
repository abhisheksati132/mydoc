# Quick preview

Shipped in v2.0.0.

In an explorer list or grid, open a file or folder action menu and choose **Quick preview**. The sheet shows a large thumbnail, full selectable name and location, format, size or folder count, and modification date. It uses MyDoc’s existing typography, tonal surfaces and thumbnail renderer.

**Open** follows the normal file or folder route. **More actions** returns to the same item’s menu. Closing the sheet leaves the directory and scroll position behind it. Preview state survives configuration changes and clears when the item disappears or browsing context changes.

This is a thumbnail and details preview; reading the full document still requires Open. Formats without an available thumbnail use the existing file-type artwork. No new permission, dependency or network service is required.

## Validation

- Debug unit tests, lint, debug APK and instrumentation APK builds passed.
- 23 focused on-device tests passed, including six preview tests covering entry, callbacks, close, full text, double text size and RTL.
- Manual API 35 checks: real PowerPoint thumbnail, list and grid entry, Open and Back, More actions, night-mode recreation and double text size.
- Physical-device performance, tablet layout and full TalkBack navigation remain unverified.
