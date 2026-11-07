# Ink & Seal: optical glass and layout

Scope: shared GlassSurface and root navigation, Home/Library/Tools footer clearance, Library toolbar and floating controls. Document/page canvases, content lists and reader layouts remain outside this visual pass. Preserve all in-progress feature work.

Direction: keep warm paper/graphite, Fraunces/Inter and Seal teal. Use regular glass for navigation/controls, restrained edge refraction/specular lighting and a stable readable tint; no animated decorative backgrounds. Content surfaces remain solid. Apple native APIs are unavailable on Android; use a verified stable Haze glass renderer, subject to compatibility and rendering checks. Do not describe blur-only fallback as optical refraction.

Build order: prove Haze compatibility; centralize material/motion fallbacks; compact responsive glass dock with a persistent selection cue; measured footer insets and clipboard placement; collapse redundant Library workspace controls into the existing menu; verify phone, dark, opaque and large-text states.

Accessibility/performance: preserve 48 dp targets, visible keyboard focus, semantic tabs, readable text, and reduced-motion handling. React to power-saving/contrast changes and keep an opaque mode. Limit live optics to small surfaces; no repeating idle animation. Measure contrast on actual configured color pairs and report device limits honestly.

Acceptance: no last row or clipboard action remains trapped under floating navigation, navigation labels fit narrow/large-text layouts, no duplicate accessible names, selection visible without animation, glass-off works, light/dark screenshots reviewed, unit/build/lint and focused emulator tests pass. Live material appearance is not proof of real-device frame time.

Sources: Apple Materials HIG https://developer.apple.com/design/human-interface-guidelines/materials ; Haze 2.1.0 source and migration https://github.com/chrisbanes/haze/tree/2.1.0 .

Implementation: Haze 2.1.0 optical glass is used by the rounded dock and create-folder control on Android 13+. Other shapes use typed backdrop blur on Android 12+; absent backdrops, increased contrast, disabled effects and older devices use opaque fill. Automatic mode also honors Battery Saver and low-RAM devices. One theme-level observer set updates the material when system settings change.

The dock has equal-width icon-over-label tabs, an opaque teal selection surface, ripple and keyboard-focus indication. There is no mount-time pulse or idle animation. The Library workspace row appears only with multiple tabs; the ordinary overflow menu exposes saved searches, comparison, transfers and new tabs. The root footer is measured, including the update notice and system navigation; lists, creation control, clipboard and local notices use that measured clearance.

Tint check (simple alpha blend, excluding renderer lighting): unselected Ink text over a light surface at 64% on black gives 6.90:1; dark surface at 78% on white gives 7.60:1. Selected text uses opaque Material primary-container pairs. These calculations do not replace checking rendered text over real content.

Next design work: unify row density and metadata alignment; improve empty/loading/error illustrations and copy; review action-sheet grouping; then test motion and GPU frame time on physical midrange hardware before tuning optics further. Keep reader pages and file thumbnails visually calm.

Validation (2026-11-07): 466 unit tests passed with no failures or skips. Two focused Compose device tests passed on API 35: 320 dp navigation at 2x font scale, selectable tab semantics/targets, and clipboard height reporting after entry. Reviewed light, dark/AMOLED and glass-off states, the collapsed Library menu and the copy notice/clipboard stack. File operations were not executed for this visual check; the clipboard was cancelled after inspecting its actions.

Rendering limit: the SwiftShader software emulator repeatedly stalled on the first graphics-program compilation after an APK install; the trace waited in the emulator OpenGL program creation path. The GPU-backed emulator (NVIDIA host renderer) cold-launched and rendered the same build without that warning. This does not establish physical-phone frame time, thermals or power use; those remain unmeasured.

Message placement: active Library panes queue ordinary notices in their own SnackbarHostState, which shares a bottom column with creation/clipboard controls. The pane's coroutine scope survives consuming the source message, preserving the visible notice. Inactive panes retain app-wide messages. Undo retains its action callback.
