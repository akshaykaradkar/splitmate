# Prototyping Workflow

High-level guidance for setting up and developing Material Design prototypes
using the kit.

1.  [Start from a Starter Asset](#1-start-from-a-starter-asset)
2.  [Prototyping Guidance, Tips, and Gotchas](#2-prototyping-guidance-tips-and-gotchas)

--------------------------------------------------------------------------------

## 1. Start from a Starter Asset

The kit provides two first-class starter assets. **Deterministic Default**:
Unless the user explicitly requests a multi-page React/TypeScript application or
an existing React workspace is specified, **ALWAYS default immediately to Option
B (`assets/starter_template.html` Standalone CDN HTML)** and build the prototype
in a single pass without asking for clarification.

### Option A: React Starter Kit

Use when the user explicitly requests a multi-page React + TypeScript + Vite
environment or provides an existing React workspace. Provides pre-configured
gBreeze component wrappers and an integrated Storybook catalog.

**Setup Instructions**:

1.  Copy `assets/react_starter_kit/` out of the workspace into a local cloudtop
    directory:

    ```bash
    cp -r ux/gdp/ai/plugins/material_genux_kit/skills/genux/assets/react_starter_kit/ ~/material_genux_react/
    ```

    *Tip*: Use a per-project folder name (e.g., `~/material_genux_myproject/`).
    If the target directory already exists, do not overwrite without user
    confirmation. Update the `name` field in `package.json` to match your
    project.
2.  Install dependencies and start the dev server:

    ```bash
    cd ~/material_genux_react/
    npm install
    npm run dev
    ```

    Open the local development URL printed by Vite in your terminal (e.g.,
    `http://localhost:5173`). *Note*: The `@material/web: "nightly"` dependency
    is intentional and will be pinned in the generated `package-lock.json`.

**React Starter Kit Gotchas**:

-   **Icons**: Always use the `<Icon />` component (wrapping `<md-gb-icon>`) for
    Google Symbols. Update `--md-icon-font` when a different icon font is
    substituted.
-   **Import Alias**: Always import components and styles using the `@/gbreeze`
    alias:

    ```tsx
    import {Button} from '@/gbreeze/components/button/button';
    import {Icon} from '@/gbreeze/styles/icons/icon';
    ```
-   **Font & Theme Setup**: Font styles load via `@import` in `src/index.css`
    (Google Sans Flex, Google Symbols, and `gm3.css`). Tailwind classes compile
    from `node_modules/@material/web/labs/gb/styles/tailwind.css`.
-   **Anchor Collisions with `useId()`**: When implementing custom popovers or
    menus leveraging the HTML `popover` API and CSS Anchor Positioning, generate
    unique anchor names via React `useId()`. Hardcoded CSS anchor names collide
    and break rendering when multiple components mount.

### Option B: Standalone CDN Template (Default)

**Default path for all single-screen and standalone prototyping requests.**
Provides a single self-contained HTML file with CDN imports for quick standalone
prototypes without npm or a build step.

**Setup Instructions**:

1.  Copy the starter template to your destination:

    ```bash
    cp ux/gdp/ai/plugins/material_genux_kit/skills/genux/assets/starter_template.html ./prototype.html
    ```
2.  Serve `prototype.html` with any local static HTTP server.

**Standalone CDN Template Gotchas**:

-   **Icons**: Always use `<md-gb-icon>add</md-gb-icon>` (import
    `@material/web/labs/gb/styles/icon/md-gb-icon.js`). Update `--md-icon-font`
    when a different icon font is substituted.
-   **Custom Elements**: Use `md-gb-*` custom elements directly:

    ```html
    <md-gb-button>Click me</md-gb-button>
    <md-gb-card>Card content</md-gb-card>
    ```
-   **Import Map Ordering (CRITICAL)**: `<script type="importmap">` **MUST** be
    the very first script in `<head>`, before any `<script type="module">`. If
    any `<script type="module">` (such as `@tailwindcss/browser` or a `fetch`
    script) appears before `<script type="importmap">`, the browser permanently
    ignores the import map and all `@material/web/...` imports fail.
-   **Fault-Tolerant Module Registration**: Load component modules using `await
    Promise.allSettled(modules.map((m) => import(m)))` (as in
    `starter_template.html`). Using static `import "..."` statements causes a
    single missing or newly-submitted `@nightly` path on `unpkg.com` to abort
    the entire `<script type="module">` block and leave all custom elements
    un-upgraded.
-   **Extended Reference Color Palettes (`gm3-color-tokens-full.css`)**: When a
    UI or visual mockup requires tonal reference palettes outside the default
    blue/green primary/tertiary tokens (for example, `--md-ref-palette-purple*`
    for purple/lavender cards), link `gm3-color-tokens-full.css` alongside
    `gm3.css`:

    ```html
    <link rel="stylesheet" href="https://www.gstatic.com/_/boq-sdlc-agents-ui/gbreeze/1.5.0/gm3-color-tokens-full.css">
    ```
-   **Font & Theme Setup**: Font styles load via the `<style>` block in
    `starter_template.html`. Tailwind is loaded from CDN, and its gBreeze
    `@theme` is dynamically loaded.
-   **CDN Imports**: Scripts and styles load via unpkg CDN:

    -   Stylesheet:
        `https://unpkg.com/@material/web@nightly/labs/gb/styles/tailwind.css`
    -   Web components importmap: `@material/web` from unpkg CDN.

--------------------------------------------------------------------------------

## 2. Prototyping Guidance, Tips, and Gotchas

### Decision Flowchart

Before writing any UI element, follow this decision sequence:

```
Need a UI element (button, card, dialog, etc.)
  │
  ├─ Does a gBreeze component exist?
  │    ├─ YES → Use component (React @/gbreeze or md-gb-* custom element).
  │    │
  │    └─ NO  → Does a standard @material/web (MWC) component exist?
  │               ├─ YES → Use @material/web fallback component (e.g.,
  │               │         md-dialog, md-outlined-text-field,
  │               │         md-slider, md-linear-progress).
  │               │
  │               └─ NO  → Is it a standard layout or container element?
  │                          ├─ YES → Build with semantic HTML + Tailwind &
  │                          │         gBreeze utility classes.
  │                          └─ NO  → Compose using low-level primitives
  │                                    (ripple, focus-ring, etc.).
  │
  └─ NEVER hardcode values. Always use tokens (typescale-*, bg-surface-*).
```

### Mandatory Pre-Built Component Lookup Table (React & Standalone HTML)

> **CRITICAL RULE — NEVER ROLL CUSTOM DOM FOR THESE UI ELEMENTS:** If your UI
> includes ANY element in the table below, you **MUST** import and render the
> corresponding `@/gbreeze/components/*` (React) or `<md-gb-*>` / `<md-*>`
> (Standalone HTML) component. Never build buttons, cards, app bars, chips,
> lists, badges, switches, checkboxes, radios, dialogs, tabs, sliders, or text
> fields out of raw `<button>`, `<input>`, `<ul>/<li>`, `<span
> class="rounded-full">`, or `<div class="bg-surface-container rounded-xl">`
> elements.

<!-- mdformat off(prevent table wrapping) -->

| UI Element | React Wrapper (`@/gbreeze/components/...`) | Standalone HTML Tag & CDN Module Import (`@material/web/...`) |
| :--- | :--- | :--- |
| **Top App Bar** | `import {AppBar} from '@/gbreeze/components/appbar/app-bar';`<br>`<AppBar title="Title" variant="standard" leading={...} trailing={...} />` | `<md-gb-app-bar size="sm" variant="standard">`<br>`import '@material/web/labs/gb/components/appbar/md-gb-app-bar.js';` |
| **Toolbar** | Use `<md-gb-toolbar>` custom element in JSX (`@material/web/labs/gb/components/toolbar/md-gb-toolbar.js`) | `<md-gb-toolbar color="standard" layout="floating">...</md-gb-toolbar>`<br>`import '@material/web/labs/gb/components/toolbar/md-gb-toolbar.js';` |
| **Button** | `import {Button} from '@/gbreeze/components/button/button';`<br>`<Button label="Save" icon="add" color="filled" />` (`filled`, `tonal`, `outlined`, `elevated`, `text`) | `<md-gb-button color="filled" size="md" shape="round"><md-gb-icon>add</md-gb-icon>Save</md-gb-button>`<br>`import '@material/web/labs/gb/components/button/md-gb-button.js';` |
| **Icon Button** | `import {IconButton} from '@/gbreeze/components/iconbutton/icon-button';`<br>`<IconButton icon="settings" color="standard" aria-label="Settings" />` | `<md-gb-icon-button color="standard" size="md" shape="round" aria-label="Settings"><md-gb-icon>settings</md-gb-icon></md-gb-icon-button>`<br>`import '@material/web/labs/gb/components/iconbutton/md-gb-icon-button.js';` |
| **FAB** | `import {Fab} from '@/gbreeze/components/fab/fab';`<br>`<Fab icon="edit" label="Compose" color="primary" />` | `<md-gb-fab color="primary" aria-label="Compose"><md-gb-icon>edit</md-gb-icon>Compose</md-gb-fab>`<br>`import '@material/web/labs/gb/components/fab/md-gb-fab.js';` |
| **Split Button** | `import {SplitButton} from '@/gbreeze/components/splitbutton/split-button';`<br>`<SplitButton label="Send" color="filled" />` | `<md-gb-split-button color="filled">...</md-gb-split-button>`<br>`import '@material/web/labs/gb/components/splitbutton/md-gb-split-button.js';` |
| **Card** | `import {Card} from '@/gbreeze/components/card/card';`<br>`<Card color="filled"><div className="p-4">...</div></Card>` (`filled`, `elevated`, `outlined`) | `<md-gb-card color="filled"><div class="p-s200">...</div></md-gb-card>`<br>`import '@material/web/labs/gb/components/card/md-gb-card.js';` |
| **Chip** | `import {ChipSet, AssistChip, FilterChip, InputChip, SuggestionChip} from '@/gbreeze/components/chip/chip';`<br>`import {ExpressiveChip} from '@/gbreeze/components/chip/expressive-chip';`<br>`<ChipSet><FilterChip label="Active" selected /></ChipSet>` | `<md-gb-chip type="filter" selected>Active</md-gb-chip>`<br>`import '@material/web/labs/gb/components/chip/md-gb-chip.js';` |
| **Badge** | `import {Badge} from '@/gbreeze/components/badge/badge';`<br>`<Badge value="3" />` | `<md-gb-badge>3</md-gb-badge>`<br>`import '@material/web/labs/gb/components/badge/md-gb-badge.js';` |
| **List & Item** | `import {List, ListItem} from '@/gbreeze/components/list/list';`<br>`<List><ListItem label="Headline" supportingText="Subtext" leadingContent={<Icon>mail</Icon>} /></List>` | `<md-gb-list><md-gb-list-item>Headline<span slot="supporting-text">Subtext</span></md-gb-list-item></md-gb-list>`<br>`import '@material/web/labs/gb/components/list/md-gb-list.js';`<br>`import '@material/web/labs/gb/components/list/md-gb-list-item.js';` |
| **Menu** | `import {Menu, MenuItem} from '@/gbreeze/components/menu/menu';`<br>`<Menu><MenuItem label="Edit" /></Menu>` | `<md-gb-menu><md-gb-menu-item>Edit</md-gb-menu-item></md-gb-menu>`<br>`import '@material/web/labs/gb/components/menu/md-gb-menu.js';`<br>`import '@material/web/labs/gb/components/menu/md-gb-menu-item.js';` |
| **Divider** | `import {Divider} from '@/gbreeze/components/divider/divider';`<br>`<Divider />` | `<md-gb-divider></md-gb-divider>`<br>`import '@material/web/labs/gb/components/divider/md-gb-divider.js';` |
| **Switch** | `import {Switch} from '@/gbreeze/components/switch/switch';`<br>`<Switch selected />` | `<md-gb-switch selected></md-gb-switch>`<br>`import '@material/web/labs/gb/components/switch/md-gb-switch.js';` |
| **Checkbox** | `import {Checkbox} from '@/gbreeze/components/checkbox/checkbox';`<br>`<Checkbox checked />` | `<md-gb-checkbox checked></md-gb-checkbox>`<br>`import '@material/web/labs/gb/components/checkbox/md-gb-checkbox.js';` |
| **Radio** | `import {Radio} from '@/gbreeze/components/radio/radio';`<br>`<Radio name="group" value="a" checked />` | `<md-gb-radio name="group" value="a" checked></md-gb-radio>`<br>`import '@material/web/labs/gb/components/radio/md-gb-radio.js';` |
| **AI Energy / Aurora** | `import {Energy} from '@/gbreeze/components/energy/energy';`<br>`<Energy active state="processing" />` | `<md-gb-energy active state="processing"></md-gb-energy>`<br>`<script src="https://static.corp.google.com/material-web/gbreeze/latest/components/energy/md-gb-energy.js"></script>` |
| **Focus Ring** | `import '@material/web/labs/gb/components/focus/focus-ring.css';` (`.focus-ring-outer`) or `<md-focus-ring>` | `.focus-ring-outer` (`@material/web/labs/gb/components/focus/focus-ring.js`) or `<md-focus-ring></md-focus-ring>` (`@material/web/focus/md-focus-ring.js`) |
| **Ripple** | `import '@material/web/labs/gb/components/ripple/ripple.css';` (`setupRipple` / `.ripple`) or `<md-ripple>` | `.ripple-host` (`@material/web/labs/gb/components/ripple/ripple.js`) or `<md-ripple></md-ripple>` (`@material/web/ripple/ripple.js`) |
| **Text Field** | `import {TextField} from '@/gbreeze/components/textfield/text-field';`<br>`<TextField variant="outlined" label="Search" />` | `<md-outlined-text-field label="Search"></md-outlined-text-field>` or `<md-filled-text-field label="Search"></md-filled-text-field>`<br>`import '@material/web/textfield/outlined-text-field.js';`<br>`import '@material/web/textfield/filled-text-field.js';` |
| **Select Dropdown** | Use `<md-outlined-select>` or `<md-filled-select>` with `<md-select-option>` | `<md-outlined-select label="Role"><md-select-option value="admin"><div slot="headline">Admin</div></md-select-option></md-outlined-select>`<br>`import '@material/web/select/outlined-select.js';`<br>`import '@material/web/select/filled-select.js';`<br>`import '@material/web/select/select-option.js';` |
| **Tabs** | `import {Tabs, PrimaryTab, SecondaryTab} from '@/gbreeze/components/tabs/tabs';`<br>`<Tabs><PrimaryTab icon="home">Home</PrimaryTab><SecondaryTab>Details</SecondaryTab></Tabs>` | `<md-gb-tabs><md-gb-tab selected><span slot="label">Home</span></md-gb-tab><md-gb-tab><span slot="label">Details</span></md-gb-tab></md-gb-tabs>`<br>`import '@material/web/labs/gb/components/tabs/md-gb-tabs.js';`<br>`import '@material/web/labs/gb/components/tabs/md-gb-tab.js';` |
| **Dialog** | `import {Dialog} from '@/gbreeze/components/dialog/dialog';`<br>`<Dialog open headline="Confirm" content="Are you sure?" />` | `<md-dialog open><div slot="headline">Confirm</div><form slot="content" method="dialog">...</form></md-dialog>`<br>`import '@material/web/dialog/dialog.js';` |
| **Slider** | `import {Slider} from '@/gbreeze/components/slider/slider';`<br>`<Slider min={0} max={100} value={50} />` | `<md-slider min="0" max="100" value="50"></md-slider>`<br>`import '@material/web/slider/slider.js';` |
| **Progress** | `import {LinearProgress} from '@/gbreeze/components/progress/linear-progress';`<br>`import {CircularProgress} from '@/gbreeze/components/progress/circular-progress';`<br>`<LinearProgress value={0.6} />` / `<CircularProgress indeterminate />` | `<md-linear-progress value="0.6"></md-linear-progress>` or `<md-circular-progress indeterminate></md-circular-progress>`<br>`import '@material/web/progress/linear-progress.js';`<br>`import '@material/web/progress/circular-progress.js';` |
| **Elevation** | Use `<md-elevation>` inside custom relative surface containers | `<md-elevation></md-elevation>`<br>`import '@material/web/elevation/elevation.js';` |
| **Snackbar** | `import {Snackbar} from '@/gbreeze/components/snackbar/snackbar';`<br>`<Snackbar open message="Saved" actionLabel="Undo" />` | `<md-gb-card color="filled">` toast surface |
| **Icon** | `import {Icon} from '@/gbreeze/styles/icons/icon';`<br>`<Icon>search</Icon>` | `<md-gb-icon>search</md-gb-icon>`<br>`import '@material/web/labs/gb/styles/icon/md-gb-icon.js';` |

<!-- mdformat on -->

### Build Order & Token Rules

-   **Component → MWC Fallback → Utility Class → Raw HTML**:
    1.  Check the **Mandatory Pre-Built Component Lookup Table** above. If a
        gBreeze (`md-gb-*` / `@/gbreeze/components/*`) or MWC fallback
        (`<md-*>`) component exists, you **MUST** import and use it.
    2.  Never reconstruct buttons, cards, lists, chips, badges, app bars, or
        form inputs out of raw `<button>`, `<input>`, `<ul>/<li>`, or `<div>`
        elements.
    3.  Use `<md-gb-button>` and `<md-gb-icon-button>` strictly for clickable
        user actions—never for non-interactive text containers (`<div>`) or
        static status pills (`<md-gb-badge>`).
-   **No Hardcoded Values**: Never hardcode colors, sizes, or shadows (avoid
    `bg-[#1b1b1f]`, `text-[#ffffff]`, `w-[320px]`). Always use semantic Tailwind
    classes (`bg-surface`, `text-on-surface`) or Material custom properties
    (`--md-sys-color-*`).

    ```html
    <!-- ❌ WRONG: Hardcoded hex colors or raw div card -->
    <div class="bg-[#1b1b1f] text-[#e3e3e3] rounded-xl p-4">

    <!-- ✅ RIGHT: Pre-built Card component with inner wrapper for padding -->
    <md-gb-card color="filled">
      <div class="p-s200 text-on-surface">...</div>
    </md-gb-card>
    ```
-   **Do NOT Add Conflicting Styles to Component Host Tags**:

    -   **Cards & Chips (`b/564629131`, `b/565098453`)**: Never apply `p-*`,
        `bg-*`, `rounded-*`, or `border-*` utility classes directly to
        `<md-gb-card>`, `<md-gb-chip>`, or `<md-gb-button>` host tags (which
        causes double backgrounds and broken borders). Apply inner padding
        (`<div class="p-s200">`) to a child wrapper inside `<md-gb-card>`.
    -   **Button & Icon Button Sizing / Shape (`b/564631678`, `b/565096624`)**:
        Never set `w-*`, `h-*`, `rounded-*`, or inline `width`/`height` on
        `<md-gb-button>` or `<md-gb-icon-button>`. Always use the component
        attributes `size="xs|sm|md|lg|xl"` and `shape="round|square"` (e.g.,
        `<md-gb-icon-button size="md" shape="round">` for `56px` buttons or
        `<md-gb-icon-button size="lg" shape="round">` instead of `class="w-14
        h-14"`).
    -   **Icons (`b/564609925`)**: Never apply `.typescale-*` or `font-*`
        classes to `<md-gb-icon>` or `<span class="md-icon">` (which overwrites
        the `Google Symbols` font-family and breaks icon rendering). Resize
        icons exclusively via `style="--md-icon-size: 20px"` (or
        `[--md-icon-size:20px]`).
    -   **Typescale T-Shirt Suffixes (`b/564612081`)**: Always use abbreviated
        suffixes `-sm`, `-md`, `-lg` (`typescale-title-md`); never spell out
        `-small`, `-medium`, or `-large` (`typescale-title-medium` does not
        exist).
    -   **Semantic Button vs. Container (`b/565096811`)**: Never use
        `<md-gb-button>` for non-interactive containers or static status pills;
        use `<div>` for layout wrappers and `<md-gb-badge>` for status badges.

    ```html
    <!-- ❌ DON'T: Host padding/background on card, w-*/h-* on icon-button, typescale on icon, or -medium suffix -->
    <md-gb-card color="filled" class="p-2 rounded-2xl bg-surface-container-low">
      <h2 class="typescale-title-medium">Title</h2>
      <md-gb-icon-button class="w-14 h-14" style="width: 84px; height: 84px;">
        <span class="md-icon typescale-title-lg">settings</span>
      </md-gb-icon-button>
    </md-gb-card>

    <!-- ✅ DO: Inner wrapper for card padding, size/shape attributes on icon-button, <md-gb-icon> for automatic icon sizing/color, and -md suffix -->
    <md-gb-card color="filled">
      <div class="p-s200 flex items-center justify-between">
        <h2 class="typescale-title-md">Title</h2>
        <md-gb-icon-button size="lg" shape="round" aria-label="Settings">
          <md-gb-icon>settings</md-gb-icon>
        </md-gb-icon-button>
      </div>
    </md-gb-card>
    ```
-   **Container Surfaces**: For distinct content blocks with tonal backgrounds
    and rounded corners, always use `<md-gb-card color="filled">` (or React
    `<Card color="filled">`) rather than a raw styled `<div>`.
-   **Form Controls**: Never use raw `<input>` or `<button>` elements for forms.
    Use `<md-gb-checkbox>`, `<md-outlined-text-field>`, `<md-gb-radio>`,
    `<md-gb-switch>`, and `<md-gb-button>` (or their React wrappers) to
    guarantee accessibility and interaction fidelity. Note that `<md-gb-switch>`
    and `<md-gb-chip>` use `selected` (not `checked`).
-   **Icons Inside Buttons**:

    -   In **React** (`@/gbreeze/components/button/button`), pass the `icon` and
        `label` props: `<Button label="New" icon="add" color="filled" />`.
    -   In **Standalone HTML** (`<md-gb-button>`), place `<md-gb-icon>` directly
        inside the default slot (do **not** add `slot="icon"`): `<md-gb-button
        color="filled"><md-gb-icon>add</md-gb-icon>New</md-gb-button>`.
-   **Shadow DOM Customization & Style Overrides (`<md-gb-card>`,
    `<md-gb-button>`, `<md-gb-icon-button>`)**: All component default styles
    live inside `@layer md` and support layout utility overrides (e.g.,
    `<md-gb-card class="w-full flex-col">`). Note that `--md-gb-*` CSS custom
    properties do **NOT** exist in `labs/gb`. Follow these exact Shadow DOM
    patterns:

    1.  **Custom Card Fill, Corner Shape (`rounded-3xl`), or Dashed Border
        (`border-dashed`) on `<md-gb-card>`**: Because `.card-outlined` and
        `.card-filled` set `--container-color` and `border: 1px solid` on the
        inner `.card` shadow element, host-level `--container-color` is
        overridden. To render dashed borders (`border-dashed`), custom corner
        radii (`rounded-3xl`), or custom tinted fills (e.g., purple/lavender
        note cards using `--md-ref-palette-purple*` from
        `gm3-color-tokens-full.css`), set `style="--container-shape:
        var(--md-sys-shape-corner-xl); --outline-width: 0px;"` on `<md-gb-card
        color="outlined">` and provide a slotted `<div slot="container"
        class="w-full h-full rounded-3xl border border-dashed border-outline
        bg-surface-container pointer-events-none"></div>` (which activates
        `.card:has([name='container'].has-slotted) { background-color:
        transparent; }`):

        ```html
        <md-gb-card
          color="outlined"
          class="relative w-full"
          style="--container-shape: var(--md-sys-shape-corner-xl); --outline-width: 0px;">
          <div
            slot="container"
            class="w-full h-full rounded-3xl border border-dashed border-outline bg-surface-container pointer-events-none">
          </div>
          <div class="relative z-10 p-s200">Custom card content</div>
        </md-gb-card>
        ```
    2.  **Custom Surface/Icon Colors on `<md-gb-button>` and
        `<md-gb-icon-button>`**: Note that `<md-gb-icon-button>` only supports
        `color="filled" | "tonal" | "outlined" | "standard"` (`color="elevated"`
        is **NOT** valid on `<md-gb-icon-button>`). Because Shadow DOM variant
        classes (`.btn-elevated`, `.icon-btn-tonal`) set `--container-color` on
        the inner shadow element, do **NOT** set `--container-color` on `:host`.
        Instead, override the semantic token consumed by that variant on the
        host `style` attribute:

        -   On `<md-gb-icon-button color="tonal">`, set
            `style="--md-sys-color-secondary-container:
            var(--md-sys-color-surface-container-lowest);
            --md-sys-color-on-secondary-container:
            var(--md-sys-color-on-surface);"` for a crisp white surface pill on
            a tinted backdrop.
        -   On `<md-gb-button color="elevated">`, set
            `style="--md-sys-color-surface-container-low:
            var(--md-sys-color-surface-container-lowest);
            --md-sys-color-primary: var(--md-sys-color-error);"` for a white
            elevated pill with semantic error/destructive text.
    3.  **Never Write Unlayered `<style>` Rules on `md-gb-*` Selectors**: You
        **MUST NOT** write unlayered `<style>` rules targeting `md-gb-chip`,
        `md-gb-button`, `md-gb-card`, or `md-gb-icon-button` with `padding`,
        `border`, `background-color`, or `border-radius`. Because unlayered CSS
        beats `@layer md`, styling the custom element host draws a second outer
        box/pill around the component's internal container (causing
        double-border and two-color artifacts).
-   **Clean Imports**: Every `<md-gb-*>` or `<md-*>` tag rendered in HTML must
    have its corresponding `import '@material/web/...';` statement inside
    `<script type="module">`, with no unused imports in React templates.

### Typography, Variable Font Weight & Icon Sizing Rules

-   **Abbreviated Size Suffixes ONLY (`-sm`, `-md`, `-lg`)**: ALWAYS use `-sm`,
    `-md`, or `-lg` suffixes (`typescale-title-md`, `typescale-body-lg`,
    `typescale-label-sm`). Full-word suffixes (`-small`, `-medium`, `-large`,
    such as `typescale-title-medium` or `typescale-body-large`) do **NOT** exist
    in gBreeze and fail silently. IF a full-word suffix is detected, REPLACE it
    immediately with `-sm`, `-md`, or `-lg`.
-   **Use `typescale-emphasized-<role>-<size>` for Medium/Bold Weight**: Because
    `.typescale-*` sets `font-variation-settings: var(--typescale-*-axes)`
    (`'wght' 400`) on the Google Sans Flex variable font, the `'wght'` variation
    axis overrides standard CSS `font-weight` utilities (`font-medium` has zero
    effect and stays at weight `400`, while `font-bold` / `<b>` jumps to `700`).
    Whenever text requires medium, semibold, or bold emphasis, you **MUST** use
    **`typescale-emphasized-<role>-<size>`** (e.g.,
    `typescale-emphasized-title-md`, `typescale-emphasized-body-lg`,
    `typescale-emphasized-label-md`) instead of combining `typescale-*` with
    `font-medium` or `font-bold`. Use `font-brand` or `font-plain` only for
    typeface selection.
-   **NEVER Put `typescale-*` on `<md-gb-icon>` or `<Icon>`**: `.typescale-*`
    compiles to the CSS `font:` shorthand (`font-family: "Google Sans Flex"`),
    which overwrites `font-family: "Google Symbols"` and turns icons into raw
    plain-text ligature strings (e.g., rendering `"delete"` or
    `"signal_cellular_alt"` as text). Instead, ALWAYS size `<md-gb-icon>` or
    `<Icon>` via the `--md-icon-size` custom property (e.g., `<md-gb-icon
    style="--md-icon-size: 20px">search</md-gb-icon>`) or an explicit font-size
    utility without `typescale-*` (`class="text-[20px]"`).
-   **Suppressed Default Typography Utilities**: Tailwind's default font-size,
    line-height, and letter-spacing utilities (`text-xs`..`text-2xl`,
    `leading-*`, `tracking-*`) are reset to `initial` in gBreeze's Tailwind
    theme and compile to nothing. `text-<color>` utilities (`text-primary`,
    `text-on-surface`) belong to `--color-*` and work normally.
-   **No Arbitrary Typography Values**: You **MUST NOT** use arbitrary
    typography utilities (`text-[14px]`, `leading-[20px]`, `tracking-[0.5px]`,
    `font-[600]`) on text elements; use `typescale-<role>-<size>` or
    `typescale-emphasized-<role>-<size>`.

<!-- mdformat off(prevent table wrapping) -->

| Role | Standard Utility (`wght 400`) | Emphasized Utility (`wght 500/600`) | Typical Usage |
| :--- | :--- | :--- | :--- |
| **Display** | `typescale-display-sm/md/lg` | `typescale-emphasized-display-sm/md/lg` | Splash banners, hero screens |
| **Headline** | `typescale-headline-sm/md/lg` | `typescale-emphasized-headline-sm/md/lg` | Modal titles, page section headings |
| **Title** | `typescale-title-sm/md/lg` | `typescale-emphasized-title-sm/md/lg` | Card headers, pane titles |
| **Body** | `typescale-body-sm/md/lg` | `typescale-emphasized-body-sm/md/lg` | Main reading text, descriptions |
| **Label** | `typescale-label-sm/md/lg` | `typescale-emphasized-label-sm/md/lg` | Button text, tags, badges, timestamps |

<!-- mdformat on -->

```html
<!-- ❌ WRONG: Full-word suffix (-medium) and font-medium do nothing; typescale-* breaks icon -->
<h2 class="typescale-title-medium font-medium">Quick Note</h2>
<md-gb-icon class="typescale-body-lg">delete</md-gb-icon>

<!-- ✅ RIGHT: Abbreviated suffix (-md), typescale-emphasized-* for weight, --md-icon-size for icon -->
<h2 class="typescale-emphasized-title-md text-on-surface">Quick Note</h2>
<md-gb-icon style="--md-icon-size: 24px">delete</md-gb-icon>
```

### Component & Button Variant Rules (Zero Raw `<button>` Policy)

-   **Zero Raw `<button>` Policy**: Every interactive button or icon button in a
    prototype **MUST** use `<md-gb-button>` (`<Button>`) or
    `<md-gb-icon-button>` (`<IconButton>`). Never use raw HTML `<button>`
    elements for UI actions.
-   **How to Apply the "One `filled` Button Per Screen" Rule**: Limiting
    `color="filled"` to **one primary CTA per screen** does **NOT** mean other
    buttons on the screen should be raw `<button>` tags! All other buttons on
    the screen **MUST** still be `<md-gb-button>` or `<md-gb-icon-button>` using
    non-filled `color` variants:
    -   `color="filled"` — Primary screen CTA (limit 1 per screen, e.g., "Save
        Note").
    -   `color="elevated"` (on `<md-gb-button>` only) — Surface/white pill
        buttons with subtle elevation (e.g., "Discard", secondary surface
        pills).
    -   `color="tonal"` — Secondary tonal pill/circle actions (e.g., Microphone
        button, secondary tool actions).
    -   `color="outlined"` — Medium-emphasis bordered pill actions (e.g.,
        "Cancel", filter/reset actions).
    -   `color="text"` (on `<md-gb-button>`) / `color="standard"` (on
        `<md-gb-icon-button>`) — Low-emphasis toolbar, header, and inline
        actions.
-   **Button & Icon Button Sizing Scale (`_button-tokens.scss` &
    `_icon-button-tokens.scss`)**: ALWAYS size `<md-gb-button>` and
    `<md-gb-icon-button>` using the `size` attribute according to the exact
    gBreeze height scale:
    -   `size="xs"` = `32px` height (`20px` icon) — compact inline/card/header
        actions
    -   `size="sm"` = `40px` height (`24px` icon, **default**) — standard
        toolbar and secondary buttons
    -   `size="md"` = **`56px` height** (`24px` icon, `title-md` label) — large
        mobile bottom-bar pills, FAB-like actions, and primary CTAs (`56px` icon
        buttons and pill buttons)
    -   `size="lg"` = `96px` height (`32px` icon, `headline-sm` label) —
        extra-large expressive hero controls
    -   `size="xl"` = `136px` height (`40px` icon, `headline-lg` label) — giant
        display/kiosk controls
    -   **CRITICAL WARNING**: Because `size="xl"` is `136px` tall and
        `size="lg"` is `96px` tall, **`size="md"` (`56px`) MUST be used for
        `56px` mobile action buttons and bottom-bar pills**. Never force
        explicit dimensions (`width: 84px`, `height: 84px`, `h-14`, `w-16 h-16`)
        or `rounded-full` onto the outer custom element host.
-   **Button Color Hygiene**: Do not override the icon or text color of standard
    buttons with Tailwind `text-*` classes; use `color` variants or semantic
    `--md-sys-color-*` token overrides on `style`.

```html
<!-- ❌ WRONG: Using raw <button> for secondary actions, size="xl" (136px!) for 56px buttons, or forcing h-14/w-16 on host -->
<md-gb-button color="filled" class="w-full h-14 rounded-full">Save Note</md-gb-button>
<button class="rounded-full bg-surface-container px-4 py-2">Discard</button>
<button class="w-16 h-16 rounded-full bg-secondary-container"><md-gb-icon>mic</md-gb-icon></button>

<!-- ✅ RIGHT: Every 56px mobile action button uses md-gb-button / md-gb-icon-button with size="md" (56px) -->
<md-gb-button color="filled" size="md" class="w-full">Save Note</md-gb-button>
<md-gb-button color="elevated" size="md">Discard</md-gb-button>
<md-gb-icon-button color="tonal" size="md" aria-label="Voice input">
  <md-gb-icon>mic</md-gb-icon>
</md-gb-icon-button>
```
