---
name: design
description: Layout, elevation, and Material Design guidelines for the application.
---
# Layout

## Window Size Classes

Adjust layout regions, panes, and navigation based on viewport width.

| Class | Width | Margin | Padding | Panes | Primary Navigation |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Compact** | `< 600px` | `16px` | `16px` | `1` | **Navigation Bar** (Bottom) |
| **Medium** | `600-839px` | `24px` | `24px` | `1-2` | **Navigation Rail** (Collapsed) |
| **Expanded**| `840-1199px`| `24px` | `24px` | `1-2` | **Navigation Rail** (Expanded) |
| **Large** | `1200-1599px`| `24px` | `24px` | `1-2` | **Navigation Rail** (Expanded) |
| **Extra Large**| `>= 1600px`| `24px` | `24px` | `1-3` | **Navigation Rail** (Expanded) |

## Semantic Landmarks
- **Banner:** `<header role="banner">` - Branding and global utilities.
- **Navigation:** `<nav role="navigation">` - Home for Bar or Rail components.
- **Main:** `<main role="main">` - The core application content (panes).
- **Content Info:** `<footer role="contentinfo">` - Metadata and legal links.

## Canonical Patterns
- **List-Detail:** Matches global WSC navigation. On Compact, show only List or Detail (1 pane). On Medium+, show both (2 panes).
- **Supporting Pane:** Displays supplementary content (e.g., filters, metadata) alongside the main content.
- **Feed:** A single-pane scrollable container of cards or list items.

## Layout Implementation Rules
- **Navigation Priority:** Use only one primary navigation component at a time. Switch at the 600px breakpoint.
  ```tsx
  {/* Compact: NavigationBar at bottom */}
  <NavigationBar>...</NavigationBar>
  {/* Medium+: NavigationRail on the side */}
  <NavigationRail>...</NavigationRail>
  ```
- **Alignment:** Position Navigation Rails on the leading edge (Left for LTR, Right for RTL).
- **Line Length:** 
    - **Body:** 45–75 characters (Max 80). Use `max-w-prose` in Tailwind.
    - **Large Headlines:** Max 154 characters.
- **Containment:** Group related content in `surface-container` panes. Use gutters of 24px (or 0px for Compact) between panes.
  ```tsx
  <div className="bg-surface-container rounded-xl p-6">
    {/* pane content */}
  </div>
  ```
- **Adaptive Triggers:** Update and transition layouts when:
    - Device rotates (Landscape/Portrait).
    - App enters/exits split-screen.
    - Foldable device folds/unfolds.
- **Spacing Grid:** Align all spatial relationships (margin, padding, gaps) to an 8dp grid. Use Tailwind's `gap-2` (8px), `gap-4` (16px), `gap-6` (24px), etc.

# Elevation

Elevation is the relative distance between surfaces along the z-axis, creating hierarchy and depth.

## Core Principles
- **Tonal Separation First:** The primary method in Material 3 is using distinct surface color roles rather than relying solely on shadows. Use `bg-surface-container`, `bg-surface-container-high`, etc. instead of `shadow-*`.
  ```tsx
  {/* ✅ RIGHT: tonal separation */}
  <div className="bg-surface-container rounded-xl p-4">...</div>
  
  {/* ❌ WRONG: shadow for layout separation */}
  <div className="bg-white shadow-lg rounded-xl p-4">...</div>
  ```
- **Shadows for Protection:** Use shadows selectively to protect elements against busy backgrounds or to provide interaction cues. Only use `shadow-1` through `shadow-5` for elevated components (FABs, Dialogs, Menus).
- **Interactive Increase:** Hover, focus, or drag typically increases an element's elevation by exactly one level.

## Elevation Levels

| Level | Common Components |
| :--- | :--- |
| **0** | Filled/Outlined Buttons, Cards, App Bars (rest), Lists, Sliders |
| **1** | Elevated Buttons/Cards, Modal Bottom Sheets, Navigation Drawer |
| **2** | Scrolled App Bars, Menus, Navigation Bar, Rich Tooltips |
| **3** | Dialogs, FABs (resting state), Extended FAB |
| **4** | Hover/Focus states for Level 3 elements (e.g., FAB hover) |
| **5** | Dragged states |

## Tonal Region Map

Material 3 uses **tonal elevation** — progressively higher-toned surface colors — to create visual depth and hierarchy without shadows. Every shell region MUST use a specific surface token. Do not pick tokens at random.

### Region Assignments

| Shell Region | Tailwind Class | M3 Token | Rationale |
| :--- | :--- | :--- | :--- |
| **Page background** (`html`/`body`) | `bg-surface` | `--md-sys-color-surface` | Lowest tonal layer; everything sits on top of this |
| **NavigationRail** | `bg-surface` | `--md-sys-color-surface` | Rail blends with the page background — no tonal lift |
| **NavigationBar** | `bg-surface-container` | `--md-sys-color-surface-container` | Bottom bar gets slight tonal lift for visibility |
| **TopAppBar (resting)** | `bg-surface` | `--md-sys-color-surface` | Blends with page at rest; distinguishes on scroll |
| **TopAppBar (scrolled)** | `bg-surface-container` | `--md-sys-color-surface-container` | Component CSS handles this via `isScrolled` prop |
| **Content panes** | `bg-surface-container` or `bg-surface-container-low` | `--md-sys-color-surface-container[-low]` | Tonal separation from the page background |
| **Cards within panes** | `bg-surface-container-high` | `--md-sys-color-surface-container-high` | One step above the pane for visual lift |
| **Dialogs / Modals** | `bg-surface-container-high` | `--md-sys-color-surface-container-high` | Elevated surfaces per M3 elevation model |
| **Menus / Tooltips** | `bg-surface-container` | `--md-sys-color-surface-container` | Elevation level 2 — tonal + shadow |

### Visual Hierarchy (low → high tone)

```
surface                    ← page bg, rail, appbar at rest
  └─ surface-container-low ← optional subtle pane variant
      └─ surface-container ← content panes, nav bar, scrolled appbar
          └─ surface-container-high ← cards, dialogs
              └─ surface-container-highest ← rarely used, extreme emphasis
```

### Rules

1. **Never skip levels.** Don't put `surface-container-high` content directly on `surface`. Use `surface-container` as the intermediate step.
2. **Rail and AppBar should NOT have tonal lift at rest.** They share the page `surface` color to create a seamless background. The NavigationBar is the exception because it needs visual separation from content above it.
3. **Cards inside panes must be one level above the pane.** If the pane is `surface-container`, cards should be `surface-container-high`. If the pane is `surface-container-low`, cards can be `surface-container`.
4. **Never use `bg-white`, `bg-gray-*`, or hardcoded colors** for any shell region. Always use semantic surface tokens.

### Common Anti-Patterns

| Anti-Pattern | Fix |
| :--- | :--- |
| Everything is `bg-surface` — flat, no depth | Use `bg-surface-container` for content panes |
| NavigationRail has `bg-surface-container` — looks disconnected from page | Set rail to `bg-surface` to match page |
| Card and pane use the same surface token — card doesn't pop | Step card up one level from pane |
| Using `shadow-lg` to separate nav from content | Use tonal tokens instead; shadows are for elevated components only |
| Using `bg-white` or `bg-[#hex]` for layout regions | Replace with semantic `bg-surface-*` tokens |

# Spacing Rhythm

Every spatial relationship in the app must follow a consistent rhythm based on Material's 8dp grid. Do not pick arbitrary gap/padding values — use the prescriptive tables below.

## App Shell Spacing

These values are fixed and should never be changed.

| Relationship | Value | Tailwind | Notes |
|-------------|-------|----------|-------|
| **Rail ↔ Content area** | 0px | No gap | Rail and content sit flush; content area padding handles separation |
| **Content area inner padding (compact)** | 16px | `p-4` | Matches the compact margin from the WSC table |
| **Content area inner padding (medium+)** | 24px | `p-6` | Matches the medium/expanded margin from the WSC table |
| **AppBar ↔ Content below** | 0px | No gap | AppBar sits directly above content; tonal shift on scroll provides separation |
| **NavigationBar ↔ Content above** | 0px | No gap | Bar sits directly below content; its own tonal surface provides separation |

## Content Spacing (within `<main>`)

| Relationship | Value | Tailwind | When to Use |
|-------------|-------|----------|-------------|
| **Page title ↔ First content section** | 16px | `mb-4` | After a headline or page title |
| **Between major sections** | 24px | `gap-6` | Between distinct content groups (e.g., "Overview" section and "Details" section) |
| **Between related items in a stack** | 16px | `gap-4` | Between cards in a list, form fields in a group |
| **Between tightly related items** | 8px | `gap-2` | Between label and supporting text, icon and text in a row |
| **Pane internal padding** | 16–24px | `p-4` or `p-6` | Inside `surface-container` panes. Use `p-4` for compact density, `p-6` for comfortable |

## Grid Spacing

| Grid Type | Gap | Tailwind | Notes |
|-----------|-----|----------|-------|
| **Card grid** | 16px | `gap-4` | Consistent gap between cards in all directions |
| **Dashboard widgets** | 16–24px | `gap-4` or `gap-6` | `gap-4` for dense dashboards, `gap-6` for spacious |
| **List-Detail gutter** | 0–16px | `gap-0` or `gap-4` | 0 if using a hard divider, 16px if using spatial separation |

## Density Scale

Three density levels the agent can reference. Default to **comfortable** unless the user requests otherwise.

| Density | Internal Padding | Stack Gap | Grid Gap | Use Case |
|---------|-----------------|-----------|----------|----------|
| **Dense** | `p-3` (12px) | `gap-2` (8px) | `gap-3` (12px) | Data tables, admin tools |
| **Comfortable** (default) | `p-4`–`p-6` | `gap-4` (16px) | `gap-4` (16px) | Most apps |
| **Spacious** | `p-6`–`p-8` | `gap-6` (24px) | `gap-6` (24px) | Marketing, reading-heavy |

## Spacing Rules

1. **Never use `gap-1` (4px) between content items.** It's too tight for Material's visual language. The smallest content gap is `gap-2` (8px).
2. **Never use `gap-10` or larger between standard content.** That's decorative spacing for hero layouts only.
3. **Be consistent within a page.** If card grid A uses `gap-4`, card grid B on the same page must also use `gap-4`.
4. **Use padding on containers, gap on parents.** Don't use margin on children to create spacing — use `gap` on the parent flex/grid and `padding` on containers.

## Spacing Anti-Patterns

| Anti-Pattern | Fix |
|-------------|-----|
| Mix of `gap-2`, `gap-6`, `gap-8` on the same page | Pick one density level and apply it consistently |
| Using `margin-bottom` on every child | Use `gap` on the parent container instead |
| 0px padding inside `surface-container` panes | Always apply at least `p-4` inside panes |
| Different padding on left/right of content | Use symmetric padding (`p-4`, `px-6`) unless deliberately asymmetric |
