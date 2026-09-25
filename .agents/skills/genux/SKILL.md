---
name: genux
description: >-
  Guides building and prototyping Generative UX (GenUX) user interfaces with the
  Material GenUX Kit. Entrypoint and routing guide for creating Google Material
  UIs, coordinating gBreeze components, and using Aurora energy effects and
  shaders. Use when starting a GenUX prototype or building Material Design web
  UIs.
---

# Material GenUX Kit

The Material GenUX Kit is a prototyping toolkit built on Tailwind CSS and web
components, designed to build Google Material 3 (GM3) UIs in a structured,
maintainable way.

## Thinking Path (3-Phase Prototyping Algorithm)

Execute every UI prototyping task using this 3-phase sequence:

1.  **Phase 1 — Route & Scaffold**: Read
    [references/genui.md](references/genui.md) and
    [references/design.md](references/design.md). **Deterministic Default**:
    Unless the user explicitly requests a multi-page React/TypeScript
    application or an existing React workspace is specified, **ALWAYS default
    immediately to Option B (`assets/starter_template.html` Standalone CDN
    HTML)** and build the prototype in a single pass without asking for
    clarification. Link `gm3-color-tokens-full.css`
    (`https://www.gstatic.com/_/boq-sdlc-agents-ui/gbreeze/1.5.0/gm3-color-tokens-full.css`)
    alongside `gm3.css` whenever a design requires tonal reference palettes
    outside the default blue/green primary/tertiary tokens (e.g.,
    `--md-ref-palette-purple*` for purple/lavender cards). Load
    [`gbreeze`](//depot/google3/ux/gdp/ai/skills/gbreeze/SKILL.md) (plus
    [`gbreeze-energy`](//depot/google3/ux/gdp/ai/plugins/material_genux_kit/skills/gbreeze_energy/SKILL.md)
    when AI/Aurora energy states are requested or shown in a visual mockup).
2.  **Phase 2 — Compose Shell & Components**: Consult the **Mandatory Pre-Built
    Component Lookup Table** in [references/genui.md](references/genui.md),
    build the semantic landmark shell (`<header>`, `<nav>`, `<main>`), and
    populate it using `<md-gb-*>` components (`@/gbreeze` in React),
    `@material/web` fallbacks, and GM3 tokens.
3.  **Phase 3 — Execute the Pre-Completion Self-Audit Checklist**: Verify all 6
    checks below using native file inspection tools (`view_file` or
    `grep_search`) before presenting or saving final code.

## Fast-Path Execution Guardrail (CRITICAL)

-   **No Evaluation or Workspace Snooping**: Do **NOT** search `EVAL.txtpb`
    files, do **NOT** run `ps aux`, and do **NOT** inspect
    `~/.gemini/jetski/brain/` or other home directory folders.
-   **No Upstream `labs/gb` Source Inspection Needed**: Do **NOT** read upstream
    `third_party/javascript/material/web/labs/gb/` SCSS/TS source files
    (`_button-tokens.scss`, `_card-tokens.scss`, `card-element.ts`, etc.). All
    component token scales (`xs=32px, sm=40px, md=56px, lg=96px, xl=136px`) and
    Shadow DOM customization patterns are completely and accurately documented
    in [references/genui.md](references/genui.md).

## Reference Index

When creating or modifying a user interface, consult the kit's references:

-   **Prototyping Workflow**: [references/genui.md](references/genui.md) — Setup
    instructions, starter kits for new projects, mandatory pre-built component
    lookup table, typography/variable-font rules, button size scale & Shadow DOM
    customization rules, and general web prototyping gotchas.
-   **Design System Specifications**:
    [references/design.md](references/design.md) — Material Design guidance,
    including window sizes, semantic landmarks, canonical layout patterns,
    elevation model, tonal region map, and spacing rhythm.

> **Guidance**: When building or prototyping a UI from scratch, read both
> [references/genui.md](references/genui.md) for the workflow and complete
> component lookup table, and [references/design.md](references/design.md) for
> design system specifications.
>
> **Mandatory Pre-Built Component Rule**: Never roll custom HTML/Tailwind DOM
> elements (`<button>`, `<input>`, `<select>`, `<ul>/<li>`, `<div
> class="bg-surface-container rounded-xl">`, `<span class="rounded-full">`) for
> UI controls or containers when a pre-built component exists in the kit
> (`<md-gb-app-bar>`, `<md-gb-toolbar>`, `<md-gb-button>`,
> `<md-gb-icon-button>`, `<md-gb-fab>`, `<md-gb-split-button>`, `<md-gb-card>`,
> `<md-gb-chip>`, `<md-gb-badge>`, `<md-gb-list>`, `<md-gb-list-item>`,
> `<md-gb-menu>`, `<md-gb-menu-item>`, `<md-gb-divider>`, `<md-gb-checkbox>`,
> `<md-gb-radio>`, `<md-gb-switch>`, `<md-gb-tabs>`, `<md-gb-tab>`,
> `<md-gb-energy>`, `focus-ring` / `<md-focus-ring>`, `ripple` / `<md-ripple>`,
> `<md-gb-icon>`, `<md-outlined-text-field>`, `<md-filled-text-field>`,
> `<md-outlined-select>`, `<md-filled-select>`, `<md-select-option>`,
> `<md-dialog>`, `<md-slider>`, `<md-linear-progress>`,
> `<md-circular-progress>`, `<md-elevation>`, or their React
> `@/gbreeze/components/*` equivalents). Check the **Mandatory Pre-Built
> Component Lookup Table** in [references/genui.md](references/genui.md) before
> writing any markup.
>
> **Component & Styling Guardrails (DO / DON'T)**:
>
> -   **No Host Padding/Backgrounds on Cards & Chips**: Never apply `p-*`,
>     `bg-*`, `rounded-*`, or `border-*` classes directly to `<md-gb-card>`,
>     `<md-gb-chip>`, or `<md-gb-button>` host elements (causes double
>     backgrounds and broken borders). Put inner padding on a child `<div
>     class="p-s200">` inside `<md-gb-card>`.
> -   **Button & Icon Button Sizing/Shape**: Never size `<md-gb-button>` or
>     `<md-gb-icon-button>` with `w-*`, `h-*`, `rounded-*`, or inline
>     `width`/`height`. Always use `size="xs|sm|md|lg|xl"` and
>     `shape="round|square"`.
> -   **Semantic Button Usage**: Use `<md-gb-button>` / `<md-gb-icon-button>`
>     strictly for clickable user actions—never for non-interactive text
>     containers (`<div>`) or static status pills (`<md-gb-badge>`).
> -   **Icon Font Preservation**: Never apply `.typescale-*` or `font-*` classes
>     to `<md-gb-icon>` or `<span class="md-icon">` (overwrites the `Google
>     Symbols` font and breaks icon rendering). Resize icons only with
>     `[--md-icon-size:20px]`.
> -   **Typescale T-Shirt Suffixes**: Always use abbreviated suffixes (`-sm`,
>     `-md`, `-lg`) for `.typescale-*` classes (`typescale-title-md`, never
>     `typescale-title-medium`).

## Skills Toolbox

Coordinate with these specialized skills across the prototyping lifecycle:

1.  **UI Components & API Details**:
    [`gbreeze`](//depot/google3/ux/gdp/ai/skills/gbreeze/SKILL.md) — Web
    components (`<md-gb-app-bar>`, `<md-gb-toolbar>`, `<md-gb-button>`,
    `<md-gb-icon-button>`, `<md-gb-fab>`, `<md-gb-split-button>`,
    `<md-gb-card>`, `<md-gb-chip>`, `<md-gb-badge>`, `<md-gb-list>`,
    `<md-gb-menu>`, `<md-gb-divider>`, `<md-gb-checkbox>`, `<md-gb-radio>`,
    `<md-gb-switch>`, `<md-gb-tabs>`, `<md-gb-tab>`, `focus-ring` /
    `<md-focus-ring>`, `ripple` / `<md-ripple>`, plus `@material/web` fallbacks
    like `<md-outlined-text-field>`, `<md-filled-text-field>`,
    `<md-outlined-select>`, `<md-filled-select>`, `<md-dialog>`, `<md-slider>`,
    `<md-linear-progress>`, `<md-circular-progress>`, `<md-elevation>`),
    component properties, color tokens (`--md-sys-color-*`), and Tailwind
    classes. To inspect detailed component slots or styles, route here.
2.  **Aurora Energy & Shader Effects**:
    [`gbreeze-energy`](//depot/google3/ux/gdp/ai/plugins/material_genux_kit/skills/gbreeze_energy/SKILL.md)
    — Animated shader for Aurora, Gemini glow, and surface energy effects
    (`<md-gb-energy>`).
3.  **Prototype Sharing & Deployment**:
    [`zipline`](//depot/google3/ux/gdp/ai/plugins/material_genux_kit/skills/zipline/SKILL.md)
    — Deploy and share built prototypes via Zipline.
4.  **Bug Reporting & Feedback**:
    [`bugaboo`](//depot/google3/prototypes/projects/gdp/bugaboo/SKILL.md) —
    Report bugs, file feedback, and capture workspace snapshots.

## Phase 3: Pre-Completion Self-Audit Checklist (MANDATORY)

Before completing any prototype, inspect your generated HTML/TSX against these 6
rules using native file inspection tools (`view_file` or `grep_search` on the
generated HTML file)—**NEVER** run shell `grep` or `python3 -c` via
`run_command` (which are blocked by Jetski pre-tool hooks):

1.  **Pre-Built Component Adoption, Zero Raw `<button>` Tags & Accurate Button
    Sizing**: Every UI control or container in the **Mandatory Pre-Built
    Component Lookup Table** (`<md-gb-app-bar>`, `<md-gb-card>`, `<md-gb-list>`,
    `<md-gb-chip>`, `<md-gb-badge>`, `<md-gb-switch>`, `<md-gb-fab>`,
    `<md-outlined-text-field>`, etc.) **MUST** use its pre-built component
    rather than raw HTML/Tailwind elements, and every interactive button or icon
    button **MUST** use `<md-gb-button>` (`<Button>`) or `<md-gb-icon-button>`
    (`<IconButton>`). Note that `<md-gb-icon-button>` only supports
    `color="filled" | "tonal" | "outlined" | "standard"` (`color="elevated"` is
    NOT valid on `<md-gb-icon-button>`). Limit `color="filled"` to 1 primary CTA
    per screen and use `color="elevated"`, `color="tonal"`, `color="outlined"`,
    `color="text"`, or `color="standard"` for all other buttons. Size buttons
    via `size="xs"` (`32px`), `size="sm"` (`40px`, default), `size="md"`
    (**`56px`** — use for `56px` mobile bottom-bar pills, FAB-like actions, and
    primary CTAs), `size="lg"` (`96px`), or `size="xl"` (`136px`). Never use
    `size="xl"` (`136px`) or `size="lg"` (`96px`) for `56px` mobile action
    buttons, and never force `width`/`height`/`h-14` on the custom element host.
2.  **Zero `typescale-*` Classes on `<md-gb-icon>` / `<Icon>`**: `.typescale-*`
    sets `font-family: "Google Sans Flex"`, overwriting `"Google Symbols"` and
    turning icons into plain text. ALWAYS size icons via `style="--md-icon-size:
    20px"` (or `text-[20px]` without `typescale-*`).
3.  **Abbreviated `-sm`, `-md`, `-lg` Suffixes ONLY**: Verify zero occurrences
    of `-small`, `-medium`, or `-large` (`typescale-title-medium` does not exist
    in gBreeze and fails silently; use `typescale-title-md`).
4.  **`typescale-emphasized-*` for Medium/Bold Weight**: Because `.typescale-*`
    sets the Google Sans Flex `'wght'` variation axis (`400`), CSS `font-medium`
    does nothing and `font-bold` jumps to `700`. ALWAYS use
    `typescale-emphasized-<role>-<size>` (e.g., `typescale-emphasized-title-md`,
    `typescale-emphasized-body-lg`) for medium/semibold/bold emphasis.
5.  **Zero Unlayered `<style>` Overrides & Valid Shadow DOM Customization**:
    Verify no unlayered `<style>` rules target `md-gb-chip`, `md-gb-button`,
    `md-gb-card`, or `md-gb-icon-button` with `padding`, `border`,
    `background-color`, or `border-radius` (which override `@layer md` and cause
    double borders). Note that `--md-gb-*` CSS variables do **NOT** exist in
    `labs/gb`. For custom card fills, `rounded-3xl`, or `border-dashed` on
    `<md-gb-card color="outlined">`, set `style="--container-shape:
    var(--md-sys-shape-corner-xl); --outline-width: 0px;"` on the card and
    provide a slotted `<div slot="container" class="w-full h-full rounded-3xl
    border border-dashed border-outline bg-surface-container
    pointer-events-none"></div>`. To customize `<md-gb-button>` or
    `<md-gb-icon-button>` surface/icon colors, override the semantic
    `--md-sys-color-*` tokens consumed by the variant on the host `style`
    attribute (e.g., `--md-sys-color-secondary-container` on `color="tonal"`).
6.  **Energy Host, Full-Bleed Backdrop & Lifecycle Hygiene**: Render at most
    **1** `<md-gb-energy>` per screen. When `<md-gb-energy>` is used as an
    ambient screen backdrop inside a mobile frame (`rounded-[40px]`), its
    wrapper **MUST** be full-bleed across the screen container (`class="absolute
    inset-0 w-full h-full pointer-events-none"`) rather than a fixed-height
    bottom box (`h-80`), avoiding horizontal shader clipping seams. Use
    `<md-gb-button>` with `slot="container"` **only** for clickable action
    buttons—for non-interactive AI status pills (`[🚀 Processing]`, `[✨ Gathering
    info]`), use `<md-gb-card>` or a non-interactive `role="status"` pill
    container. When starting in `state="anticipating"`, auto-transition to
    `state="idle"` after `1800ms` via `setTimeout`.
