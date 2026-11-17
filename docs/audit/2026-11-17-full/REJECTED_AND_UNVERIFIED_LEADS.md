# Rejected and unverified leads

## Earlier updater staging-cleanup finding — downgraded

The final APK path protected by UpdateManager differs from ApkDownloader's randomly named .part staging file. That mismatch exists. However, the production cleanUpdatesCache call sites are before download starts and after its success/failure, serialized by the operation mutex. SettingsRepository.clearCache explicitly excludes the entire updates directory. No production concurrent-cleanup caller was established. The existing test directly invoking cleanup during a mock download does not prove a UI-triggered production race. Treat as a latent API-contract weakness, not a confirmed current user-facing P2 defect. Earlier chat reports overstated reachability.

## Earlier updater dismiss-to-install race — not yet reproduced through current UI callers

Generation checking and installer invocation are not one synchronized operation. However, current UI callers launch downloadAndInstall on Main, ApkDownloader returns via withContext, and there is no suspension between the final generation check and installer call. Dismissal from the same main-thread UI cannot interleave in that synchronous section. A cross-thread caller could expose the race; no such current production caller was established. Keep as a concurrency hardening lead; do not advertise a proven user-triggered install-after-dismiss failure from this evidence alone.

## Global zoom cache clear — rejected as a crash finding

The clear path evicts entries without recycling bitmaps that composables may still reference. Eviction alone is insufficient evidence of a use-after-recycle defect.

## PDF volume-key LaunchedEffect(Unit) — rejected as stale-state evidence

The reviewed state uses a local delegated `val state by collectAsState()`. Capturing that delegated property can continue to read the current State.value. An effect with a constant key does not by itself prove the lambda captures an immutable initial snapshot. Require a runtime reproduction or a traced copied snapshot before reporting stale pageCount.

## Shared Office cache deletion — confirmed eviction, current viewer crash unproven

Deleting the content-addressed cache forces later reconversion. Existing native Unix descriptors may remain usable after unlink. A current-session crash must not be claimed without evidence of a later path reopen.

## Repair silently drops damaged pages — rejected for ordinary counted inputs

PdfToolsEngine compares finalPages and initialPages for REPAIR and emits a recovery warning when fewer pages survive. Reviewing the inner skip alone missed that caller-level disclosure.

## Dependency vulnerabilities and binary internals

No fresh advisory/provenance analysis or native-binary reverse engineering was completed in this pass. A successful CodeQL run is not evidence that every dependency or binary is safe. No CVE claim is made without verifying the resolved artifact and authoritative advisory.

## Android string wrapper quotes - rejected

Double quotes delimiting Android string-resource whitespace do not by themselves render as punctuation; the resource compiler interprets them. Source XML text alone was insufficient for the proposed visible-quote bug. Cached APK aapt2 output showed ordinary resource values, but that APK was not asserted to match the audited commit. No current device reproduction supports the claim.

## Release apksigner exit suppression - hardening observation only

The workflow captures apksigner output with `|| true`, then requires the expected signer fingerprint. A locally tampered copy of a cached debug APK failed verification without printing that fingerprint, so this probe did not establish a signature bypass. The artifact was an existing local APK, not an asserted build of the audited SHA. Requiring apksigner exit success explicitly would make the gate easier to reason about, but no exploitable bypass is claimed.
