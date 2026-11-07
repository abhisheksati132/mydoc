# Proposed next capabilities

These are recommendations, not shipped features or a commitment to implement all at once. Keep the Ink & Seal interface calm; surface advanced capabilities when relevant.

## Recommended sequence

1. **Tags and smart collections.** Label files across folders (Work, College, Receipts) and gather them into virtual collections without copying. First slice: manual local tags, assign/remove from file actions, browse a collection and open the original. Then add saved rules across selected folders. Handle renamed, moved, missing and permission-denied files explicitly.
2. **Search inside documents.** Find words inside supported PDFs, text and Office documents, with snippets and a useful jump destination. Start with text-based documents; add opt-in OCR for scans later. Keep indexing bounded, cancellable and local, and make coverage, stale results and unsupported files visible.
3. **Organisation rules with preview.** Build a rule such as “PDFs in Downloads, older than 30 days → Documents/Archive”. First slice runs only when requested, shows every proposed move and collision, and uses existing safe transfer handling. Add reversible history where the operation can be reliably undone; never imply all operations are reversible.
4. **Duplicate review workspace.** Extend the existing cleaner summary with a browsable group, paths/metadata/previews, explicit keeper selection and recoverable removal. Existing duplicate detection is a starting point, not a new feature by itself. Prove content equality before calling files identical.
5. **Named workspaces.** Save Work/Study presets of folder tabs, active pane, searches and views. Existing tabs already restore some session state; this adds explicit, reusable named presets. Opening a preset must not discard a running transfer or unsaved user input.

## Later expansion

Local device-to-device transfer and SMB/SFTP browsing are valuable larger projects after local file workflows are reliable. Each needs its own connection, cancellation, credential, conflict and background-transfer design.

## Delivery rule

Choose one capability, specify its first useful slice, implement its entire user flow, then verify populated/empty/error/cancelled and restoration states before expanding. Do not add several unfinished feature buttons.
