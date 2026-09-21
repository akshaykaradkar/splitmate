---
name: genui
description: Core instructions for generating Material Design UI using gBreeze.
---
# UI Generation with gBreeze

Goal: Create a Material Design adherent UI using gBreeze based on the user prompt.

## Prerequisites
Before generating UI, you MUST read and understand the following skills or files if you haven't already in this conversation:
- **`gbreeze` skill (`.agents/skills/gbreeze`)**: Learn about the available gBreeze components, CSS custom properties, and utility classes.
- **`design` skill (`.agents/skills/design`)**: Learn about the Google Material Design layout, window size classes, and elevation principles.
- **`layout-scaffold` skill (`.agents/skills/layout-scaffold`)**: Learn the canonical app shell structure, responsive navigation switch, content area patterns, and spacing rhythm. **Read this before building any full-page layout.**
- **`tailwind.css` (`gbreeze/styles/tailwind.css`)**: Understand the Tailwind theme configuration.
- **Font loading**: Verify that `index.html` loads the fonts required by the active typography tokens (`gm3-typography-tokens.css` → Google Sans Flex, Google Sans Text, Google Sans, Noto Sans). See the Font Loading section in the `gbreeze` skill.

## Decision Flowchart

Before writing **any** UI element, follow this sequence — no exceptions:

```
Need a UI element (button, card, dialog, etc.)
  │
  ├─ Is it in the Component Registry below?
  │    ├─ YES → Import the React component. Read its .tsx file for props.
  │    │         DO NOT copy HTML from the gbreeze skill doc.
  │    │
  │    └─ NO  → Is it a standard HTML element (div, span, img, etc.)?
  │               ├─ YES → Build it with Tailwind + gbreeze utility classes.
  │               └─ NO  → Build it with gbreeze CSS classes from the
  │                         gbreeze skill doc + ripple/focus-ring as needed.
  │
  └─ NEVER hardcode colors, sizes, shadows, or typography. Always use tokens — typescale-*, bg-*, text-on-*.
```

## Component Registry [CRITICAL — read before generating ANY UI]

The following React components already exist. You **MUST** import and use them — never recreate their HTML/CSS by hand.

> **Maintenance note:** If a new component is added to `gbreeze/components/`, add it to this table.

| Component | Import | Key Props |
| :--- | :--- | :--- |
| `TopAppBar` | `gbreeze/components/appbar/app-bar` | `variant`, `title`, `navigationIcon?`, `actionIcons?` |
| `Badge` | `gbreeze/components/badge/badge` | `value?`, `max?`, `showZero?` |
| `Banner` | `gbreeze/components/banner/banner` | `text`, `title?`, `graphic?`, `actions?`, `inset?` |
| `Button` | `gbreeze/components/button/button` | `label`, `icon?`, `color?`, `size?`, `shape?`, `toggle?`, `selected?`, `onChange?` |
| `Card`, `CardButton` | `gbreeze/components/card/card` | `color?` (`elevated`\|`filled`\|`outlined`), `disabled?` |
| `Carousel` | `gbreeze/components/carousel/carousel` | `items: CarouselItem[]`, `variant?` |
| `Checkbox` | `gbreeze/components/checkbox/checkbox` | `indeterminate?`, `error?` (extends `<input>`) |
| `Chip` | `gbreeze/components/chip/chip` | `label`, `icon?`, `avatar?`, `variant?`, `selected?`, `elevated?`, `onDelete?` |
| `DataTable` | `gbreeze/components/datatable/data-table` | `columns: DataTableColumn<T>[]`, `data: T[]`, `selectable?` |
| `DatePicker` | `gbreeze/components/datepicker/date-picker` | `onConfirm?`, `onCancel?` |
| `Dialog` | `gbreeze/components/dialog/dialog` | `isOpen`, `onClose`, `title?`, `icon?`, `actions`, `type?` |
| `Divider` | `gbreeze/components/divider/divider` | `orientation?`, `variant?` (`full-width`\|`inset`\|`middle`) |
| `Fab` | `gbreeze/components/fab/fab` | `icon` (required), `label?`, `color?`, `size?` |
| `FabMenu` | `gbreeze/components/fabmenu/fab-menu` | `actions: FabAction[]`, `icon?`, `activeIcon?`, `variant?` |
| `IconButton` | `gbreeze/components/iconbutton/icon-button` | `icon` (required), `color?`, `size?`, `shape?`, `width?`, `toggle?`, `selected?` |
| `Menu` | `gbreeze/components/menu/menu` | `trigger` (ReactNode), `items: MenuItem[]` |
| `NavigationBar`, `NavigationBarItem` | `gbreeze/components/navigationbar/navigation-bar` | Item: `icon`, `label`, `active?`, `activeIcon?` |
| `NavigationRail`, `NavigationRailItem` | `gbreeze/components/navigationrail/navigation-rail` | Rail: `header?`, `fab?`; Item: `icon`, `label`, `active?` |
| `LinearProgress`, `CircularProgress` | `gbreeze/components/progress/progress` | `value?` (0–100, omit for indeterminate), `variant?` |
| `Radio` | `gbreeze/components/radio/radio` | Extends `<input>` — use `name`, `checked`, `onChange` |
| `SearchBar` | `gbreeze/components/searchbar/search-bar` | `leadingIcon?`, `trailingIcon?`, `onTrailingIconClick?` |
| `SegmentedButton` | `gbreeze/components/segmentedbutton/segmented-button` | `options: SegmentedButtonOption[]`, `value?`, `onChange?`, `multiSelect?` |
| `Slider` | `gbreeze/components/slider/slider` | `min?`, `max?`, `step?`, `value?`, `withLabel?`, `withTicks?` |
| `Snackbar` | `gbreeze/components/snackbar/snackbar` | `isOpen`, `message`, `action?`, `onAction?`, `showClose?`, `duration?` |
| `SplitButton` | `gbreeze/components/splitbutton/split-button` | `options: {label, onClick}[]`, `size?`, `color?` |
| `Switch` | `gbreeze/components/switch/switch` | `selected?`, `defaultSelected?`, `onChange?`, `selectedIcon?`, `unselectedIcon?` |
| `Tabs`, `TabPanel` | `gbreeze/components/tabs/tabs` | Tab: `icon?`, `badge?`, `isStacked?`; `variant?` (`primary`\|`secondary`) |
| `TextField` | `gbreeze/components/textfield/text-field` | `label`, `supportingText?`, `variant?` (`filled`\|`outlined`) |
| `Toolbar`, `ToolbarButton`, `ToolbarSeparator` | `gbreeze/components/toolbar/toolbar` | ToolbarButton: `icon`, `label?`, `active?` |
| `Tooltip` | `gbreeze/components/tooltip/tooltip` | `children`, `position?` (`top`\|`bottom`\|`left`\|`right`) |

## Core Rules

1. **Use React Components First [CRITICAL]**: Before writing **any** UI element, check the Component Registry above.
   - If a matching component exists → import and use it.
   - If you need more detail on a prop → read the `.tsx` source file for the full interface.
   - **If the `.tsx` file delegates props (e.g. uses `ComponentProps`), follow the import chain to the underlying Lit component (e.g., `md-button.ts`) to verify the true `@property` defaults.**
   - If no match exists → you may build raw HTML/CSS using gbreeze utility classes.
   - **Never** copy raw HTML snippets from the `gbreeze` skill documentation when a React wrapper is listed above. Those examples are CSS-level references only.
2. **Do Not Reconstruct**: Only build HTML and CSS classes by hand for a UI element that has **no entry** in the Component Registry, or when you need to customize a component beyond what its React props allow.
3. **No Hardcoded Values**: Do not hardcode colors, sizes, or other design tokens (e.g., avoiding `bg-[#ff0000]` or `w-[200px]`). You must use the connected Tailwind utility classes (e.g. `bg-primary`, `text-on-surface`) or Material tokens.
4. **Icons**: Use the `<Icon />` React component to display Google Symbols font ligature icons. Note: if a `.tsx` gbreeze component explicitly accepts an `icon` prop, pass the string name directly (e.g., `<Button icon="add" />`) rather than manually wrapping it in `<Icon>`.
5. **Popovers & Anchors**: If you implement custom popovers or menus leveraging the HTML `popover` API and CSS Anchor Positioning, you **MUST** dynamically generate unique anchor names (e.g. via React `useId()`). Hardcoded CSS anchor names will collide and break rendering when multiple components mount simultaneously.
6. **Form Controls**: Never use raw `<input>` elements for standard forms without applying the prerequisite gbreeze interaction states (`focus-ring-outer`, `ripple`, etc.). Default strictly to importing `gbreeze/components/` wrappers (like `<Checkbox>` or `<TextField>`) where available to guarantee accessibility and interaction fidelity.
7. **Customization & Layout**: 
   - All component default styles use `@layer` and support being overridden.
   - For layout components (cards, lists), change flex or grid properties (e.g. `className="flex flex-col gap-4"`) to modify default component layout.
   - Prefer using component tokens (custom properties) to customize themable properties before overriding them globally.
8. **No Unused Imports**: After finishing a component, verify every import is actually referenced in the JSX. Remove any gbreeze component imports that are not used. Unused imports signal wrong component choices and bloat the bundle.
9. **Prefer Card Over Styled Divs for Content Containers**: When displaying a distinct content block with `bg-surface-container rounded-xl p-*`, use `<Card color="filled">` instead of a raw `<div>` or `<li>`. Card provides the correct Material surface, border-radius, and optional interaction states (via `CardButton`). Reserve raw `<div>`/`<section>` only for structural layout wrappers that are not visually distinct content containers.
10. **Internal Component Composition [CRITICAL]**: When creating or modifying a gbreeze component (`gbreeze/components/**/*.tsx`), the same "Use React Components First" rule applies **inside** the component. If a gbreeze component needs a button, icon button, icon, FAB, or any other element that already exists in the Component Registry — **import and use the sibling React component**, do not rebuild it from raw HTML/CSS classes. For example:
    - A `TopAppBar` that renders icon buttons → import `IconButton` from `gbreeze/components/iconbutton/icon-button`.
    - A `NavigationRail` that renders a FAB → import `Fab` from `gbreeze/components/fab/fab`.
    - A `Menu` that renders a divider → import `Divider` from `gbreeze/components/divider/divider`.

    The only exceptions are:
    - **Circular dependencies** — if component A imports component B and B would need to import A.
    - **Low-level primitives** that are intentionally standalone (e.g., `Icon`, `ripple`, `focus-ring`).

11. **Typography — Typescale Selection [CRITICAL]**: Every text element MUST use a `typescale-*` Tailwind utility. Never leave text unstyled or use raw Tailwind `text-*`, `font-*`, `leading-*`, or `tracking-*` utilities.

    **Semantic role mapping:**

    | Content Role | Typescale | Responsive Step-Up | Typeface |
    | :--- | :--- | :--- | :--- |
    | Page title / hero | `typescale-headline-sm` | `sm:typescale-headline-md md:typescale-headline-lg` | Brand |
    | Section heading | `typescale-title-lg` | `md:typescale-headline-sm` | Brand |
    | Card title | `typescale-title-md` | — | Plain |
    | Body / paragraph | `typescale-body-md` | `md:typescale-body-lg` | Plain |
    | Supporting text / caption | `typescale-body-sm` | — | Plain |
    | Button / chip / tab label | (set by component CSS — do not override) | — | Plain |
    | Input label | `typescale-body-sm` | — | Plain |
    | Overline / metadata | `typescale-label-sm` | — | Plain |
    | Navigation label | `typescale-label-md` | — | Plain |
    | Large decorative number | `typescale-display-sm` | `sm:typescale-display-md` | Brand |

    **Anti-patterns — never do these:**
    - ❌ `leading-relaxed` or `leading-tight` alongside any `typescale-*` — the typescale already bakes in the M3 line-height via the CSS `font` shorthand. Adding `leading-*` overrides it and breaks the type rhythm.
    - ❌ `tracking-wide` or `tracking-tight` alongside any `typescale-*` — letter-spacing is set by the `--tracking` token inside each typescale. Adding `tracking-*` overrides it.
    - ❌ Raw `text-sm`, `text-lg`, `text-xl`, etc. — Tailwind's default text-size utilities are suppressed (`--text-*: initial`). They compile to nothing and must never be used.
    - ❌ `font-sans`, `font-serif`, `font-mono` — Tailwind's default font-family utilities are suppressed. Use `font-brand` or `font-plain` for the rare case where font-family must be set directly.
    - ❌ Unstyled `<h1>`, `<h2>`, `<p>` without a `typescale-*` class — HTML heading/paragraph elements have no inherent Material styling.

    **Brand vs. Plain typefaces:**
    - **Brand** (`--md-ref-typeface-brand`): Used for `display-*`, `headline-*`, and `title-lg`. Typically Google Sans / Google Sans Flex. Expressive, used for large text.
    - **Plain** (`--md-ref-typeface-plain`): Used for `body-*`, `label-*`, `title-sm`, `title-md`. Typically Google Sans Text. Optimized for readability at small sizes.
    - You do **not** need to set font-family manually — the `typescale-*` tokens already reference the correct typeface.

## Layout Composition

These rules govern how the agent structures pages and arranges content. They prevent the "center everything" and div-soup anti-patterns that arise when the agent starts with content and adds navigation later.

> **Full layout reference:** The **layout-scaffold** skill has the canonical shell template, responsive navigation switch, content area patterns (Feed, List-Detail, Supporting Pane), and positioning rules. Read it before building any full-page layout.

### Rule: Shell-First Thinking

> **Always start by rendering the app shell (AppBar + Navigation + `<main>`), THEN fill in page content.**

Follow this order when building any page — no exceptions:

1. **Render the scaffold** — AppBar, NavigationRail, NavigationBar, `<main>` container (see the layout-scaffold skill's Shell Template).
2. **Define routes** — Pages render inside `<main>`, not alongside the shell.
3. **Build pages** — Each page fills the `<main>` slot with its own content.
4. **Add components** — Cards, tables, forms go inside pages.

Never start with content and wrap navigation around it later.

### Rule: No Viewport Centering for App Layouts

> **Do NOT use `items-center justify-center` on the root layout.** That pattern is only for single-focus landing pages or empty states.

For real app layouts:
- Content flows from the top, aligned to the start.
- Use `items-start` on flex containers.
- Use `mx-auto` with a `max-w-*` on the content wrapper for horizontal centering of constrained content — not on the outer shell.

```tsx
// ❌ WRONG — centers all content in the viewport
<div className="min-h-screen flex flex-col items-center justify-center">

// ✅ RIGHT — content flows from top using the scaffold
<div className="bg-surface text-on-surface h-screen flex flex-col overflow-hidden">
```

### Rule: Content Width Constraints

Every page should constrain its content width to maintain readability. Pair max-width with horizontal centering: `max-w-screen-lg mx-auto`.

| Content Type | Max Width | Tailwind Class |
|-------------|-----------|----------------|
| Prose / text-heavy | 65ch (~45-75 chars) | `max-w-prose` |
| Dashboard / mixed | 1200px | `max-w-screen-lg` or `max-w-7xl` |
| Data tables | Full width of `<main>` | No constraint (table scrolls horizontally) |
| Card grids | 1400px | `max-w-screen-xl` or `max-w-[1400px]` |

### Rule: Pane Containment

> **Every distinct content region should be wrapped in a surface-container pane.**

A "pane" is a `<section>` (or `<div>`) with `bg-surface-container rounded-xl p-4 sm:p-6` that groups related content visually and provides consistent internal padding. For interactive/visually-distinct content blocks, prefer `<Card color="filled">` over a raw styled div (see Core Rule 9).

What is NOT a pane:
- The `<main>` element itself (that's the content area, not a pane).
- Individual components like buttons or chips.

### Rule: Semantic Landmark Structure

The overall page must use semantic HTML landmarks:

```
<header>       → TopAppBar
<nav>          → NavigationRail or NavigationBar
<main>         → Content area (pages render here)
  <section>    → Content panes within a page
<footer>       → Optional, for metadata/legal (contentinfo)
```

Do NOT use `<div>` for everything. Semantic elements provide accessibility and visual hierarchy.

### Layout Anti-Patterns

| Anti-Pattern | What Goes Wrong | Fix |
|-------------|----------------|-----|
| `justify-center items-center min-h-screen` on root | All content floats in the middle of the viewport | Use the scaffold: `flex flex-col h-screen`, content flows from top |
| No `max-w-*` on content | Text spans 1600px+ on wide screens, unreadable | Add `max-w-prose` for text, `max-w-screen-lg` for dashboards |
| Navigation added after content | Shell structure breaks, nav mispositioned | Shell-first: scaffold first, pages second |
| `<div>` for everything, no landmarks | No accessibility, no visual hierarchy | Use `<header>`, `<nav>`, `<main>`, `<section>` |
| Surface tokens not applied to panes | Content floats on raw background, looks flat | Wrap content regions in `bg-surface-container rounded-xl p-6` |

## Common Mistakes — WRONG vs RIGHT

### ❌ WRONG: Recreating a Button from raw HTML
```tsx
// DO NOT do this — the <Button> component already exists
<button className="btn btn-sm btn-filled ripple focus-ring-outer">
  <span className="md-icon btn-icon" aria-hidden="true">add</span>
  Create
</button>
```

### ✅ RIGHT: Using the React component
```tsx
import { Button } from 'gbreeze/components/button/button';
<Button color="filled" size="sm" icon="add" label="Create" />
```

### ❌ WRONG: Wrapping icon in `<Icon>` when a component accepts an `icon` prop
```tsx
import { IconButton } from 'gbreeze/components/iconbutton/icon-button';
import { Icon } from 'gbreeze/styles/icons/icon';
<IconButton icon={<Icon>settings</Icon>} color="standard" size="sm" />
```

### ✅ RIGHT: Passing the string directly
```tsx
import { IconButton } from 'gbreeze/components/iconbutton/icon-button';
<IconButton icon="settings" color="standard" size="sm" />
```

### ❌ WRONG: Hardcoded colors or shadows
```tsx
<div className="bg-[#1b1b1f] shadow-lg rounded-xl p-4">...</div>
```

### ✅ RIGHT: Using token-based classes
```tsx
<div className="bg-surface-container rounded-xl p-4">...</div>
```

### ❌ WRONG: Building a Switch from raw HTML
```tsx
<button role="switch" className="switch ripple focus-ring-outer" aria-checked="false" />
```

### ✅ RIGHT: Using the React component
```tsx
import { Switch } from 'gbreeze/components/switch/switch';
<Switch selected={false} onChange={(v) => setEnabled(v)} />
```

## Design Rules Summary
- **Elevation**: Do not use shadows for layout separation. Use `surface-container` color tokens instead. Shadows are for elevated components (FABs, Dialogs, hover state, etc.).
- **Primary Buttons**: Limit `filled` button and icon button to one per screen. Use text buttons and standard icon buttons if no background color is specified.
- **Cards**: Specify the flex direction of a card's content appropriately with utility classes like `flex-col` or `flex-row`.
- **Card for Content Containers**: Any distinct content region that has a tonal surface background (`bg-surface-container`, `bg-surface-container-high`, etc.) with rounded corners should be a `<Card>` or `<CardButton>`, not a raw `<div>`. This ensures consistent Material styling and makes the surface interactive-ready if needed later.
- **Button Overrides**: Do not override the icon or text color of standard buttons.
