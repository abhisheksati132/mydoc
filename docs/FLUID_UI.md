# Fluid Ink & Seal

Design contract for the Artefact-inspired UI refinement. Implement with MyDoc's own
components, assets and typography; no reference-app code or assets are imported.

## Screen jobs

- Home: resume one document, then find recent work. A large real cover is the focal point.
- Tools: recognize the main tasks immediately, then browse or search the full catalogue.
- Reader: keep the page dominant, with reachable, labelled controls in floating groups.
- Preview: inspect a file and expand into reading without losing its identity.
- Image to PDF: keep the image large, edits reversible, and export consistently reachable.

## Shared rules

- Clear glass for compact navigation; frosted glass for menus; dense material for panels.
- Warm paper, graphite and seal teal remain the default identity. Category tones are accents.
- Menus grow from their anchor and return there. Touch feedback never delays actions or scroll.
- Expressive motion is local to controls; Calm uses a restrained response. System reduced
  motion overrides both. No idle animation or moving document text.
- Keep 48dp touch targets, focus/back/outside dismissal, opaque contrast/power fallbacks,
  and layouts that scroll or stack with large type and narrow windows.
- Use real covers and functional actions. Preserve file-list density and document geometry.

## Delivery and validation

1. Shared materials, anchored menus, persisted glass/motion preferences.
2. Home and Tools hierarchy, compact reader groups and navigation feedback.
3. Preview handoff and editor controls, then emulator screenshots and interaction checks.

Validate menu edge placement and RTL, preference persistence, reduced motion, large type,
preview navigation and editor actions. Run unit tests, lint and a debug build. Emulator
visual checks do not establish physical-device frame rates or battery performance.
