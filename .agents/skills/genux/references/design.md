# Material Design Guidance

## Index

1.  [Window Size Classes](#1-window-size-classes)
2.  [Semantic Landmarks](#2-semantic-landmarks)
3.  [Canonical Layout Patterns](#3-canonical-layout-patterns)
4.  [Layout Implementation Rules](#4-layout-implementation-rules)
5.  [Elevation & Surface Model](#5-elevation-surface-model)
6.  [Tonal Region Map](#6-tonal-region-map)
7.  [Spacing Rhythm](#7-spacing-rhythm)

--------------------------------------------------------------------------------

## 1. Window Size Classes

Layout regions, panes, and navigation adapt based on canonical viewport width
breakpoints:

<!-- mdformat off(prevent table wrapping) -->

| Class | Width | Margin | Padding | Panes | Primary Navigation |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Compact** | `< 600px` | `16px` | `16px` | `1` | Bottom navigation region (`<nav>` after `<main>`) |
| **Medium** | `600-839px` | `24px` | `24px` | `1-2` | Side navigation region, collapsed |
| **Expanded** | `840-1199px` | `24px` | `24px` | `1-2` | Side navigation region, expanded |
| **Large** | `1200-1599px` | `24px` | `24px` | `1-2` | Side navigation region, expanded |
| **Extra Large** | `>= 1600px` | `24px` | `24px` | `1-3` | Side navigation region, expanded |

<!-- mdformat on -->

--------------------------------------------------------------------------------

## 2. Semantic Landmarks

Structure overall application shells using semantic HTML landmarks wrapping
pre-built gBreeze components:

-   **Banner**: `<header role="banner">` containing `<md-gb-app-bar>` (or React
    `<AppBar>`) — Global branding, top app utilities, and search
    (`<md-outlined-text-field slot="search">`). Never hand-roll a top app bar
    using raw `<div>` and `<button>` elements.
-   **Navigation**: `<nav role="navigation">` containing `<md-gb-list>` /
    `<md-gb-list-item>`, `<md-gb-toolbar>`, or `<md-tabs>` — Primary navigation
    region (side rail/drawer, docked/floating toolbar, or tabs).
-   **Main**: `<main role="main">` — The core application viewport and
    scrollable content panes containing `<md-gb-card>` modules.
-   **Content Info**: `<footer role="contentinfo">` — Secondary metadata, status
    indicators, or legal links.

--------------------------------------------------------------------------------

## 3. Canonical Layout Patterns

-   **List-Detail**: Matches global window size classes. On Compact, display
    only List (`<md-gb-list>`) or Detail (1 pane). On Medium+, display both
    side-by-side (2 panes).
-   **Supporting Pane**: Displays supplementary context (e.g., filters using
    `<md-gb-chip>`, inspect panels, metadata) alongside the main content canvas.
-   **Feed**: A single-pane scrollable container of `<md-gb-card>` or
    `<md-gb-list-item>` elements centered or bounded by max-width.

--------------------------------------------------------------------------------

## 4. Layout Implementation Rules

-   **Shell-First Composition**: Render the app shell first — top banner region
    (`<header role="banner"><md-gb-app-bar>...</md-gb-app-bar></header>`),
    primary navigation region (`<nav>`), and the `<main>` container — then fill
    `<main>` with page content, then place `<md-gb-card>`, `<md-gb-list>`, and
    form components inside pages.
-   **Navigation Priority**: Use only one primary navigation region at a time.
    Switch between bottom navigation and side navigation at the 600px
    breakpoint.
-   **Alignment**: Position side navigation on the leading edge (left for LTR,
    right for RTL).
-   **Line Length**:
    -   **Body text**: 45–75 characters (max 80ch). Use `max-w-prose` in
        Tailwind.
    -   **Headlines**: Max 154 characters.
-   **Pane Containment & Card Modules**: Group major layout regions into
    `<section>` panes and place individual content blocks or widgets inside
    `<md-gb-card>` (or React `<Card>`):

    ```html
    <section class="bg-surface-container-low rounded-xl p-4 sm:p-6 flex flex-col gap-4">
      <md-gb-card color="filled">
        <div class="p-s200 flex flex-col gap-s100">
          <!-- card module content -->
        </div>
      </md-gb-card>
    </section>
    ```

    Never use a raw `<div class="bg-surface-container rounded-xl p-4">` as a
    stand-in for a Card; always use `<md-gb-card color="filled">` (or
    `color="outlined"` / `color="elevated"`).
-   **No Viewport Centering for Full Apps**: Never use `min-h-screen flex
    items-center justify-center` on the root container of a multi-component app.
    That pattern is only for single-focus landing pages or empty states. For
    real app layouts:

    -   Content flows from the top, aligned to the start.
    -   Use `items-start` on flex containers.
    -   Use `mx-auto` with a `max-w-*` on the content wrapper for horizontal
        centering of constrained content — not on the outer shell.

    ```html
    <!-- ❌ WRONG: Shrinks the entire application into a centered card -->
    <div class="min-h-screen flex items-center justify-center">
      <div class="w-[800px]">...</div>
    </div>

    <!-- ✅ RIGHT: App fills viewport with pre-built App Bar, Nav List, and Cards -->
    <div class="flex flex-col h-screen overflow-hidden bg-surface text-on-surface">
      <header role="banner">
        <md-gb-app-bar size="sm" variant="standard">Dashboard</md-gb-app-bar>
      </header>
      <div class="flex flex-1 overflow-hidden">
        <nav role="navigation" class="w-60 bg-surface p-2">
          <md-gb-list>
            <md-gb-list-item checked>Overview</md-gb-list-item>
          </md-gb-list>
        </nav>
        <main role="main" class="flex-1 overflow-y-auto p-6">
          <md-gb-card color="filled">
            <div class="p-s200">Content</div>
          </md-gb-card>
        </main>
      </div>
    </div>
    ```
-   **Content Width Constraints**: Content areas should not stretch indefinitely
    on ultra-wide screens:

<!-- mdformat off(prevent table wrapping) -->

| Content Type | Max Width | Tailwind Utility | Notes |
| :--- | :--- | :--- | :--- |
| **Prose / Articles** | ~65ch | `max-w-prose` | Preserves readable line length |
| **Forms / Settings** | 576–672px | `max-w-xl` to `max-w-2xl` | Prevents input fields from stretching excessively |
| **Dashboards** | 1280px | `max-w-7xl` | Standard bounded container |
| **Data Displays** | Full width | `w-full` | Uses fluid multi-column grids |

<!-- mdformat on -->

-   **Layout Anti-Patterns**:

<!-- mdformat off(prevent table wrapping) -->

| Anti-Pattern | Why It Fails | Fix |
| :--- | :--- | :--- |
| `h-screen` on scrollable content | Cuts off content on mobile viewports | Use `min-h-screen` or `flex-1 overflow-y-auto` |
| Nested scroll containers | Double scrollbars confuse users | Only the `<main>` area should scroll |
| Hardcoded widths (`w-[500px]`) | Breaks on smaller screens | Use max-width (`max-w-lg w-full`) |
| Unconstrained full-width text | Lines > 80ch are hard to read | Add `max-w-prose` to text containers |
| Absolute positioning for layout | Breaks responsive flow | Use flexbox or CSS grid |

<!-- mdformat on -->

--------------------------------------------------------------------------------

## 5. Elevation & Surface Model

### Tonal Separation First

Use tonal surface tokens (`surface-container-*`) and `<md-gb-card
color="filled">` as the primary mechanism for separation and visual hierarchy.
Do not rely on drop shadows or un-componentized `<div>` boxes for cards.

```html
<!-- ❌ WRONG: Shadow-based separation on a raw div or padding/background on card host -->
<div class="bg-surface shadow-lg rounded-xl p-6">...</div>
<md-gb-card color="filled" class="p-6 bg-surface-container">...</md-gb-card>

<!-- ✅ RIGHT: Pre-built filled Card component with inner wrapper for padding -->
<md-gb-card color="filled">
  <div class="p-s300">...</div>
</md-gb-card>
```

### Elevation Rules

-   **Shadows for Protection**: Elevation levels 2+ and shadows are reserved for
    floating or transient surfaces that physically overlap content (FABs, menus,
    dialogs).
-   **Interactive Increase**: Interactive elevated surfaces increase elevation
    by +1 level on hover and focus.
-   **Never Skip Levels**: Do not place high-elevation surfaces directly on base
    surfaces without intermediate containers when grouping content.

### Elevation Levels

<!-- mdformat off(prevent table wrapping) -->

| Level | Role / Components in Kit | Typical Styles |
| :--- | :--- | :--- |
| **0** | Base surfaces, filled/outlined buttons, resting cards, top banner region at rest | `bg-surface` or `bg-surface-container`, no shadow |
| **1** | Elevated cards, elevated buttons | Slight tonal lift, minimal shadow |
| **2** | Scrolled top banner region, menus, bottom navigation region | `bg-surface-container`, level 2 shadow |
| **3** | Dialogs, resting FABs | `bg-surface-container-high`, level 3 shadow |
| **4** | Hover/focus state of level 3 surfaces | Extended shadow |
| **5** | Dragged or active manipulation states | Highest shadow elevation |

<!-- mdformat on -->

--------------------------------------------------------------------------------

## 6. Tonal Region Map

The canonical mapping between semantic landmark regions, Tailwind utility
classes, and Material tokens:

<!-- mdformat off(prevent table wrapping) -->

| Region (Landmark) | Tailwind Class | M3 Token | Rationale |
| :--- | :--- | :--- | :--- |
| **Page background** (`html`/`body`) | `bg-surface` | `--md-sys-color-surface` | Lowest tonal layer; everything sits on top of it |
| **Primary navigation region — side** (`<nav>` on leading edge) | `bg-surface` | `--md-sys-color-surface` | Blends with page background; no tonal lift |
| **Primary navigation region — bottom** (`<nav>` after `<main>`) | `bg-surface-container` | `--md-sys-color-surface-container` | Slight tonal lift to separate from content above |
| **Top banner region — at rest** (`<header role="banner">`) | `bg-surface` | `--md-sys-color-surface` | Blends with page background at rest |
| **Top banner region — scrolled** | `bg-surface-container` | `--md-sys-color-surface-container` | Scroll state raises tone; component handles transition |
| **Content pane** (`<section>` inside `<main>`) | `bg-surface-container` or `bg-surface-container-low` | `--md-sys-color-surface-container[-low]` | Tonal separation from page background |
| **Nested card surface** (inside a pane) | `bg-surface-container-high` | `--md-sys-color-surface-container-high` | One step above pane for visual lift |
| **Modal surface** (dialogs) | `bg-surface-container-high` | `--md-sys-color-surface-container-high` | Elevated surface per M3 elevation model |
| **Transient overlay surface** (menus, popovers) | `bg-surface-container` | `--md-sys-color-surface-container` | Elevation level 2: tonal lift plus shadow |

<!-- mdformat on -->

### Visual Hierarchy Ladder

```
surface                            ← page background, side nav region, banner region at rest
  └─ surface-container-low         ← optional subtle pane variant
      └─ surface-container         ← content panes, bottom nav region, scrolled banner region
          └─ surface-container-high        ← nested card surfaces, modals
              └─ surface-container-highest ← extreme emphasis / highlighted containers
```

### Tonal Anti-Patterns

<!-- mdformat off(prevent table wrapping) -->

| Anti-Pattern | Fix |
| :--- | :--- |
| Everything is `bg-surface` — flat, no depth | Use `bg-surface-container` for content panes |
| Side navigation uses `bg-surface-container` — looks detached | Set to `bg-surface` to match page background |
| Card and containing pane use identical surface token | Step the card up one level from the pane (`bg-surface-container-high`) |
| Using `shadow-lg` to separate navigation from content | Use tonal tokens; shadows are only for elevated surfaces |
| Using `bg-white`, `bg-gray-*`, or `bg-[#hex]` for layout regions | Replace with semantic `bg-surface-*` tokens |

<!-- mdformat on -->

--------------------------------------------------------------------------------

## 7. Spacing Rhythm

### 8dp Spacing Grid

All spatial dimensions (margins, padding, gaps) align with the 8dp grid system.
Increments of 4dp (`p-1`, `gap-1`) are reserved for compact internal component
density.

### App Shell Spacing

These shell alignment values are fixed:

<!-- mdformat off(prevent table wrapping) -->

| Relationship | Value | Tailwind | Notes |
| :--- | :--- | :--- | :--- |
| **Side navigation region ↔ content area** | 0px | No gap | Flush alignment; content area padding provides separation |
| **Top banner region ↔ content below** | 0px | No gap | Tonal shift on scroll provides separation |
| **Bottom navigation region ↔ content above** | 0px | No gap | Tonal surface provides separation |

<!-- mdformat on -->

### Content Spacing

<!-- mdformat off(prevent table wrapping) -->

| Context | Value | Tailwind Class | Usage |
| :--- | :--- | :--- | :--- |
| **Pane Internal Padding** | 16px / 24px | `p-4 sm:p-6` | Internal breathing room for content panes |
| **Between Panes (Gutter)** | 16px / 24px | `gap-4 sm:gap-6` | Separation between adjacent panes |
| **Stack Item Gap** | 12px / 16px | `gap-3 sm:gap-4` | Gap between vertical list items or form rows |
| **Inline Elements** | 8px | `gap-2` | Gap between buttons, chips, or inline icons |

<!-- mdformat on -->

### Grid Spacing

<!-- mdformat off(prevent table wrapping) -->

| Class | Columns | Gutter | Margin |
| :--- | :--- | :--- | :--- |
| **Compact** | 4 | 16px (`gap-4`) | 16px (`p-4`) |
| **Medium** | 8 | 24px (`gap-6`) | 24px (`p-6`) |
| **Expanded+** | 12 | 24px (`gap-6`) | 24px (`p-6`) |

<!-- mdformat on -->

### Density Scale

<!-- mdformat off(prevent table wrapping) -->

| Density | Factor | Target Context |
| :--- | :--- | :--- |
| **Default** | 0 (base) | Consumer apps, mobile screens, touch interfaces |
| **Comfortable** | -1 (compact) | Dashboards, master-detail enterprise views |
| **Compact** | -2 (dense) | Data-dense tables, code viewers, analytical tools |

<!-- mdformat on -->

### Spacing Rules

1.  **Never use odd pixel values** for layout spacing (avoid `p-[13px]`,
    `gap-[7px]`).
2.  **Nest smaller gaps inside larger gaps**: Card internal gap (`gap-2`) < Pane
    gap (`gap-4`) < Page margin (`p-6`).
3.  **Responsive padding**: Use `p-4 sm:p-6` on main containers to scale
    gracefully between mobile and desktop.
4.  **Touch targets**: Ensure all interactive elements have at least 48x48px
    touch bounding boxes on Compact viewports.

### Spacing Anti-Patterns

<!-- mdformat off(prevent table wrapping) -->

| Anti-Pattern | Why It Fails | Fix |
| :--- | :--- | :--- |
| `p-0` on page container | Content sticks directly to window edges | Use `p-4 sm:p-6` |
| Inconsistent gutters | Visual jitter across adjacent columns | Align all gutters to 16px or 24px |
| Over-spaced compact rows | Excessive vertical scroll on mobile | Use compact density scale (-1 or -2) |
| Hardcoded margin overrides | Colliding spacing models break layouts | Use standard Tailwind flex/grid gaps |

<!-- mdformat on -->
