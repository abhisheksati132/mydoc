# Saved searches and folder views

## Scope and order

1. Persist named searches: folder path, name/extension/size/subfolder filters and a relative modified-within-days window. Reopening recomputes the cutoff. Save from active advanced search; run or delete from a shared Saved searches sheet. Opening a missing folder shows an error without replacing the current view. Maximum 50 saved searches; labels 1-80 characters. No result files are persisted.
2. Persist each folder's sort field/direction and list/grid mode. Apply on entry and restore on restart; tabs viewing the same folder share updates. Categories keep existing global behavior. Unsaved folders use name ascending plus the existing global layout default. Reset removes the folder override. Keep at most 200 folder overrides, discarding least recently updated ones.

Use normalized absolute paths (no filesystem IO when reading preferences). Store a versioned, bounded Properties payload in existing private SharedPreferences, with safe fallback for malformed records. StateFlow broadcasts updates. No new permission, network, schema or dependency. Folder or search removal changes preferences only, never files. Paths are not migrated automatically when a folder is renamed.

## Implementation and acceptance

Plain preference models and store/codec, adapter in SettingsRepository; ViewModel handles save/open/reset and folder-state changes; Compose dialogs/sheet use string resources and the existing theme. Workspace menu groups shared tools to keep the tab bar usable on phones.

Tests: persistence round trip including Unicode, invalid/oversized data, independent views and capacity, relative dates, stale directory-load replies versus saved search navigation, same-folder tab updates, missing folder handling, and Android save/open/delete flow. Existing unit suite, debug build and lint must pass.

Commands: `./gradlew.bat testDebugUnitTest --tests "*ExplorerPreferencesTest"`; `./gradlew.bat testDebugUnitTest assembleDebug lintDebug`. Device tests use unique app-cache fixtures and isolated preference keys.

## Use

Run Advanced search in a folder and choose Save search above its results. Give it a name. Open Explorer tools beside the tab bar and choose Saved searches to run or remove saved entries. A search opens in the visible active tab on a phone, or the focused pane in split view. Removing it deletes only the saved definition. The folder must still be accessible; renamed folders are not followed automatically.

Sort a folder or switch its list/grid layout as usual; the choice is remembered for that folder. Reset folder view in its menu returns to name ascending and the global default layout. Categories keep the existing global layout behavior. Preferences use normalized absolute paths; aliases of a folder may have separate views. The 200-entry view history is ordered by last update rather than visits.

## Validation (2026-11-07)

455 JVM tests and two focused Android tests passed. Tests cover Unicode persistence, relative date preservation, invalid input, capacity and eviction, failed writes, restored/reset views, same-folder tabs, category isolation, missing folders and stale navigation. Android tests save, recreate, run and delete a search using isolated preferences and cache files; view reset preserves fixture bytes. Debug/test APK assembly and Android Lint passed (warnings remain). The full-app Save search banner and Saved searches sheet were visually checked on a disposable API 35 emulator. Production preferences use the app's existing asynchronous SharedPreferences writes; this is not a backup/export feature. Folder aliases and renamed paths are not automatically reconciled.
