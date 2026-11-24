# Quick preview

The original thumbnail preview shipped in v2.0.0. Paged PDF previews are currently unreleased.

In an explorer list or grid, open a file or folder action menu and choose **Quick preview**. The sheet
puts content first, followed by the full selectable name and location, format, size or folder count,
and modification date. It uses MyDoc's existing typography, tonal surfaces and rendering components.

**Open** follows the normal file or folder route. **More actions** returns to the same item’s menu. Closing the sheet leaves the directory and scroll position behind it. Preview state survives configuration changes and clears when the item disappears or browsing context changes.

PDFs now have previous/next page controls and a current/total page label. Only the selected page is
rendered, off the main thread, with its longest edge limited to 1600 pixels; native document resources
close after each render. The selected page survives configuration changes. Protected or unreadable PDFs
show a message directing the user to Open. Search, zoom, annotations and password entry remain in the
full reader. This preview does not change the saved reading position or modify the file.

Images fit completely inside the preview; list and grid thumbnails keep their existing crop.
Other formats remain thumbnail-and-details previews, and formats without an available thumbnail use
file-type artwork. No new permission, dependency or network service is required.

## Validation

- Current unit suite: 665 tests, no failures, three existing skips. Lint, debug and instrumentation APK builds passed.
- 11 focused API 35 preview tests passed: action entry, callbacks, close, full name/location, large text,
  RTL, page navigation and bounds, requested-page pixels and size budget, damaged/protected files,
  and selected-page restoration.
- Additional PDF navigation checks passed in dark mode and a 2400×1080 viewport at 200% system text size;
  captured layouts were visually inspected. Emulator display, text size and night mode were restored.
- Physical-device performance, tablet layout and full TalkBack navigation remain unverified.
