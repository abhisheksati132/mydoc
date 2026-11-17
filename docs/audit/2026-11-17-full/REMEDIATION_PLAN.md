# Remediation order

Audit only: no production fix has been applied or pushed.

1. **Contain destructive traversal.** Make move/trash/restore copy fallback, snapshots and deletion consistently avoid following directory links. Verify outside sentinel integrity and forced rename failure. Keep permanent-delete no-follow behavior.
2. **Bound tool input staging.** Enforce per-input/aggregate byte budgets and cancellation; own and clean the staging file before copying starts. Include throwing/never-ending provider streams.
3. **Prevent listing memory crashes.** Apply size limits while reading legacy Word thumbnail input rather than after readBytes. Include misleading length metadata.
4. **Fix ZIP REPLACE failure behavior.** Back up overwritten entries until all selected work commits, then clean backups. A failed archive must not silently leave old files replaced.
5. **Repair TTS state and resource ownership.** Fix paused resume, filter stale engine callbacks, define background lifetime, detach viewers from service ownership, and ensure the retained loader owns a usable renderer.
6. **Correct user-visible content.** Preserve supplementary Unicode in DOCX; fail or disclose zero/partial OCR extraction; bound OCR/compression raster sizes; support narrow-page signatures; preserve tagged PDF structure in metadata stripping. Correct nested PPTX groups and explicitly disclose ODS/PPTX limit truncation.
7. **Stabilize widget and reader identity.** Pass stable document identity from widget; key bookmarks and related per-document state by URI; refresh widget progress after persistence.
8. **Coordinate asynchronous ownership.** Fix cropper bitmap recycling races and respect shared Office conversion cache ownership. Updater concurrency concerns remain hardening leads, not confirmed production races; see REJECTED_AND_UNVERIFIED_LEADS.md.
9. **Finish access and error flows.** Save OCR text through a scoped destination; preserve cleaner failure states; bound directory indexing and redact release exception logs.
10. **Verify and document.** Add meaningful focused regressions, rerun Android gates once Gradle is available, perform device reproductions, and update privacy/feature claims. Do not lower test or lint gates to ship.

See subsystem reports for exact evidence, severity and caveats. Reconcile new findings before applying these as a fixed exhaustive list.
