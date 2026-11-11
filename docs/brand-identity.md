# MyDoc — Brand Identity

Single source of truth for how MyDoc looks, sounds, and behaves. Everything in the app
(UI tokens, launcher, launch assets) derives from this file. If a decision isn't here, it
isn't the brand.

---

## 1. Positioning

**What MyDoc is:** the calm, private, do-it-all document suite that never phones home.

**The one-sentence promise:**
> Every document. On your device. Yours.

**The differentiator (this is the whole brand, not a feature list):**
Office / Google Drive / WPS are **cloud-first** — your files live on their servers, telemetry
is everywhere, and the product is a subscription you rent. MyDoc is the opposite: **local-first,
zero telemetry, every format** (PDF, Office, ebook, image, audio, video), plus edit / scan /
organise / convert. That is not "Office with fewer features." It is a genuinely different — and
today, more premium — promise: *you own your work.*

**Against what we define ourselves:**
- Not a copy of Microsoft / Google / WPS. We borrow their *capability*, never their *look*.
- Not a "vibrant AI productivity app." No gradient slop, no aurora, no emoji, no violet-on-white.
- Not a bare file manager. It is a considered suite for people who take their documents seriously.

**Audience:** professionals, students, researchers, and anyone who keeps important documents and
cares about privacy and craft. People who notice when something is well-made.

---

## 2. Personality — Quiet Luxury / Refined Minimal

Confident, understated, expensive. The design language of a good notebook, a letterpress seal,
a well-set book — not a SaaS landing page.

Principles:
- **Restraint over density.** Fewer, better things. Whitespace is a feature, not waste.
- **One accent, used with intent.** Color is a signal, not decoration.
- **Craft in the small things.** Optical alignment, real type, considered hairlines.
- **Calm confidence.** We never shout. No hype, no exclamation marks, no "supercharge your…".
- **Ownable.** The warm paper + graphite + Seal-teal palette and the wax-seal mark are ours.

---

## 3. Name treatment

Keep **MyDoc**. It is generic on its own, so the brand earns it through typography and restraint,
not the word itself.

- Canonical form: **MyDoc** (capital M, capital D, one word). Never "MyDoc." with a trailing period,
  never "My Doc", never "MYDOC" except inside the seal monogram.
- Wordmark: set in the display serif (Fraunces) at a refined weight, tight tracking. The wordmark
  is the brand; the seal is the badge.
- Lockup: `[seal] MyDoc` horizontal; seal alone at small sizes (launcher, favicon).

---

## 4. Logo / mark

**The Seal.** A small, embossed wax-seal / letterpress emblem — a rounded-square containing an
`M` formed from a folded-paper negative space. It says *official, notarized, sealed-and-yours.*
It is the premium, ownable device; the wordmark carries the name.

- Primary: Seal in **Seal teal** on warm paper (light) or lifted Seal teal on graphite (dark).
- Brass variant used **only** for the seal itself or tiny "premium" touches — never as a UI accent.
- Flat, vector, single-tone. No gradients, no gloss, no drop shadows, no bevels beyond a subtle
  letterpress emboss.
- Clear space ≥ the seal's own height on all sides. Minimum size 24 px (below that, use the plain
  `M` glyph).

---

## 5. Color system — "Ink & Seal"

Near-monochrome warm neutrals + **one** accent (Seal teal) + **one** micro-accent (brass, sparingly).
The cool Apple systemBlue and the saturated rainbow format colors from v1.5.19 are retired here on
purpose — they are everyone's palette; this one is ours.

### 5.1 Light — warm paper
| Token | Hex | Role |
|---|---|---|
| Canvas / background | `#F5F3EE` | warm bone app background |
| Surface | `#FCFBF8` | cards, sheets (near-white, warm) |
| Surface muted | `#EDEAE2` | secondary surfaces, wells |
| Line / separator | `#DEDACF` | warm hairline |
| Ink (text primary) | `#1A1712` | warm near-black |
| Ink muted (secondary) | `#6B6558` | warm taupe |
| **Accent — Seal** | `#0E5C57` | primary actions, selection, links |
| On accent | `#FFFFFF` | text on Seal |
| Accent container | `#DCEAE7` | pale Seal fill (chips, active bg) |
| Micro-accent — Brass | `#A67C3D` | seal, premium badges only — never a UI control color |
| Error | `#B4231C` | refined brick |
| Scrim | `#000000 @ 55%` | modal isolation |

### 5.2 Dark — graphite
| Token | Hex |
|---|---|
| Canvas | `#0F0E0C` |
| Surface | `#1A1815` |
| Surface muted | `#24211C` |
| Line | `#332F28` |
| Ink | `#F2EFE8` |
| Ink muted | `#A8A090` |
| **Accent — Seal (lifted)** | `#4FB0A6` |
| On accent | `#06231F` |
| Accent container | `#123A36` |
| Brass | `#C79A55` |
| Error | `#F0685E` |

### 5.3 AMOLED — obsidian
Same as Dark but `Canvas #000000`, `Surface #12100D`, `Surface muted #1C1A16`, `Line #2A2721`.

### 5.4 Format colors — desaturated, letterpress
Format coding stays (it has scanning value) but is **muted to warm, low-chroma** tones so the UI
reads calm and premium, not a crayon box. One hue family, restrained saturation:
| Format | Light | Dark |
|---|---|---|
| PDF | `#8C3B34` | `#C56A5E` |
| Word | `#33506E` | `#7C9CC0` |
| Excel | `#3A5A44` | `#7FA98B` |
| Slides | `#8A5A2E` | `#C0905A` |
| Image | `#5B4E6B` | `#9C8CB0` |
| Video | `#7A4256` | `#BE7E94` |
| Audio | `#2F5A5A` | `#6FA6A6` |
| Archive | `#6B5A3A` | `#A99463` |
| APK | `#3A5A4A` | `#77A48C` |
| Code | `#4A4E52` | `#8A9096` |
| Generic | `#5D5A52` | `#98938A` |

### 5.5 Glass (Liquid Glass chrome)
Keep the translucent Haze glass, but tint **warm neutral**, not cool. Accent never floods the glass;
color comes from content showing through (vibrancy). Rim highlights stay white hairlines; drop
shadows in light mode are warm-ink, not accent-tinted.

---

## 6. Typography

Two typefaces, one mono. No more.

- **Display — Fraunces** (variable serif). Wordmark, screen titles, hero numbers, empty-state
  headlines. Weight 500–600, optical size high, tracking −0.01 to −0.02em. This is what makes the
  plain name feel expensive.
- **Body / UI — Inter.** Everything functional: labels, body, buttons, lists.
- **Mono — JetBrains Mono.** Metadata, file sizes, page counts, paths, timers. Tabular figures.

Scale (dp/sp, existing AppTypography): keep the restrained scale; headings step by weight + size,
not by color. Body 14–16, secondary 13, caption 11–12. Line-height 1.5 body, 1.2–1.3 for display.

---

## 7. Voice

Calm, precise, first-person-plural ownership. Short declaratives. Tell the user what happened and
what's next; never hype.

- Do: "Moved to Recycle Bin." "3 items restored." "Every document stays on your device."
  "Open any PDF, Office doc, or book."
- Don't: "Supercharge your workflow 🚀", "Oops!", exclamation marks, "seamless", "unleash",
  marketing adjectives, emoji.
- Destructive actions: plain, specific, reversible-first ("Move 4 items to Recycle Bin — Undo").

---

## 8. Layout, spacing, elevation

- 4/8 dp spacing rhythm (AppSpacing). Generous gutters; let content breathe.
- Consistent corner radius scale (AppRadius); squircle continuous corners on glass only.
- Elevation is quiet: hairline separators + subtle warm shadows. No glow.
- Respect safe areas; nav reserves content insets; primary actions ≥ 44 dp.

---

## 9. Anti-slop guardrails (hard rules)

1. No gradients, aurora blooms, or glow shadows anywhere. **One exception:** the glass chrome
   (`GlassSurface`) uses short vertical gradients for the 1dp specular rim and the inner shadow.
   Those model light on a physical edge, they are not decorative fills, and they are the only
   gradients permitted. Nothing else — no hero gradients, no tinted washes, no glow.
2. Exactly one UI accent (Seal teal). Brass is a badge/seal micro-accent, never a control color.
3. No emoji as icons. One vector icon family, consistent stroke.
4. Max two typefaces (+ mono for data).
5. Color is never the only signal (pair with icon/text for status).
6. No decorative color used where neutral would do; restraint is the premium.
7. Motion respects reduced-motion; 150–300 ms, spring/ease-out, purposeful only.
8. Every screen shares one vocabulary — if a screen looks like a different app, it's wrong.

---

## 10. Application map (where this lands in code)

- `designsystem/tokens/AppColors.kt` — Light/Dark/Black + `fromMaterialScheme` + `DocumentFormatColors`
  + media-kind helpers → the "Ink & Seal" values above.
- `designsystem/tokens/AppTheme.kt` — `fallbackLight/fallbackDark/amoled` Material 3 schemes.
- `designsystem/tokens/AppTypography.kt` — Fraunces display / Inter / JetBrains Mono (confirm).
- `ui/files/FileThumbnail.kt` — `TypeTint` → desaturated format table.
- Home wordmark + launcher icon + `res/` adaptive icons → the Seal + Fraunces wordmark.
- Launch banner → this palette + wordmark + seal.

---

## 11. Material You on top of Ink & Seal (Hybrid Design Architecture)

MyDoc marries Google's **Material You (Material 3 Expressive)** with the bespoke **Ink & Seal** design craft:

- **Ink & Seal as the Foundation:**
  - Warm bone canvas (`#F5F3EE`), warm near-white cards (`#FCFBF8`), and graphite darks (`#0F0E0C` / `#1A1815`) provide an editorial, book-like substrate that feels physical and timeless.
  - Typographic hierarchy anchored by **Fraunces** display serif, **Inter** functional sans, and **JetBrains Mono** tabular metrics.
  - Desaturated, letterpress format chips with WCAG AAA contrast ratios.

- **Material You as the Expressive Dynamic Layer:**
  - **Dynamic Theming (Monet):** When dynamic color is enabled, system color seeds cleanly tint the `AppColors.fromMaterialScheme` bridge while respecting warm luminance scales.
  - **M3 Expressive Motion:** Fluid spring easing (`Spring.StiffnessMediumLow`) on bottom sheets, search overlays, and page turn gestures.
  - **Pill Indicators & Floating Chrome:** Full support for Material 3 pill indicators, expressive fab morphing, and continuous squircle corners (`AppRadius.roundShape`).
  - **Tactile Accessibility:** High-contrast focus boundaries, explicit touch targets (≥48dp), and complete screen-reader semantics for all interactive swatches and controls.

