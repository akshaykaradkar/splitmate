---
name: layout-scaffold
description: Canonical Material 3 app shell layout with responsive navigation, content area templates, and positioning rules.
---
# Layout Scaffold

Goal: Provide a tested, canonical app shell structure that the agent drops in as the foundation for every Material 3 application. This eliminates improvised layouts and guarantees correct navigation, scrolling, and responsive behavior.

## Prerequisites
Before using this skill, you MUST have already read:
- **`genui` skill**: Component Registry and decision flowchart.
- **`design` skill**: Window Size Classes, semantic landmarks, elevation.
- **`react-routing` skill**: If the app has multiple pages/routes.

## When to Use This Skill

You MUST use this skill whenever you are:
- **Creating a new application** or page that needs navigation.
- **Adding a NavigationBar, NavigationRail, or TopAppBar** to an existing app.
- **Restructuring the root layout** of an application.
- **Fixing layout issues** such as broken scrolling, mispositioned navigation, or content overflow.

---

## The Canonical App Shell

Every Material 3 application with navigation MUST use this shell structure. Do not improvise an alternative.

### Visual Diagram

```
┌──────────────────────────────────────────┐
│            TopAppBar                     │  64–152px height
├────────┬─────────────────────────────────┤
│        │                                 │
│  Nav   │          <main>                 │
│  Rail  │       (scrollable)              │
│ 80px   │     p-4 sm:p-6                  │
│        │                                 │
│(hidden │   ┌─────────────────────┐       │
│  on    │   │  content pane(s)    │       │
│compact)│   │  bg-surface-container│       │
│        │   └─────────────────────┘       │
│        │                                 │
├────────┴─────────────────────────────────┤
│     NavigationBar (compact only, 80px)   │
└──────────────────────────────────────────┘
```

### Shell Template

```tsx
import { TopAppBar } from '@gbreeze/components/appbar/app-bar';
import { NavigationBar, NavigationBarItem } from '@gbreeze/components/navigationbar/navigation-bar';
import { NavigationRail, NavigationRailItem } from '@gbreeze/components/navigationrail/navigation-rail';

export default function App() {
  return (
    <div className="bg-surface text-on-surface h-screen flex flex-col overflow-hidden">
      {/* ① AppBar — always on top, never scrolls */}
      <TopAppBar variant="small" title="App Name" navigationIcon="menu" />

      {/* ② Body — Rail + Main side by side */}
      <div className="flex flex-1 overflow-hidden">
        {/* ③ NavigationRail — hidden on compact, visible on medium+ */}
        <NavigationRail className="hidden sm:flex shrink-0">
          <NavigationRailItem icon="home" label="Home" active />
          <NavigationRailItem icon="explore" label="Explore" />
          <NavigationRailItem icon="settings" label="Settings" />
        </NavigationRail>

        {/* ④ Main content — fills remaining space, scrolls independently */}
        <main className="flex-1 overflow-y-auto p-4 sm:p-6">
          {/* Page content or <Routes> go here */}
        </main>
      </div>

      {/* ⑤ NavigationBar — visible on compact only, hidden on medium+ */}
      <NavigationBar className="sm:hidden">
        <NavigationBarItem icon="home" label="Home" active />
        <NavigationBarItem icon="explore" label="Explore" />
        <NavigationBarItem icon="settings" label="Settings" />
      </NavigationBar>
    </div>
  );
}
```

### Why Each Class Matters

| Element | Classes | Purpose |
|---------|---------|---------|
| **Root `div`** | `h-screen flex flex-col overflow-hidden` | Full viewport height, vertical stack, prevents body scroll |
| **TopAppBar** | (component handles its own styles) | 64px height, `flex-shrink: 0` built-in — won't collapse |
| **Body row** | `flex flex-1 overflow-hidden` | Horizontal layout for Rail + Main, takes remaining height, clips overflow |
| **NavigationRail** | `hidden sm:flex shrink-0` | Hidden on compact, visible column at 600px+, fixed 80px width (component CSS), won't shrink |
| **`<main>`** | `flex-1 overflow-y-auto p-4 sm:p-6` | Fills remaining width, scrolls independently, responsive padding |
| **NavigationBar** | `sm:hidden` | Visible only on compact, 80px height (component CSS), sits at bottom of flex column |

### Critical Rules

1. **Never skip the root `overflow-hidden`.** Without it, the page will double-scroll — both `<main>` and `<body>` will have scrollbars.
2. **Never use `h-full` instead of `h-screen` on the root.** The root must be viewport-sized to constrain the layout. `h-full` only works if a parent is also sized.
3. **Never use `position: fixed` or `position: sticky` on the AppBar in this layout.** The `h-screen flex flex-col` approach keeps the AppBar in normal flow at the top. The `overflow-hidden` on the root and `overflow-y-auto` on `<main>` handle scroll containment. Fixed/sticky adds z-index complexity and breaks the flex layout.
4. **Never put `overflow-y-auto` on the body row.** Only `<main>` should scroll. The body row clips with `overflow-hidden`.
5. **Both NavigationBar and NavigationRail render in the DOM.** CSS (`hidden`/`sm:flex`/`sm:hidden`) handles visibility. This is simpler and more reliable than JavaScript-based conditional rendering.

---

## Responsive Navigation Switch

Both navigation components render the **same destinations** but are toggled by CSS breakpoints:

| Component | Compact (`< 600px`) | Medium+ (`≥ 600px`) |
|-----------|---------------------|----------------------|
| `NavigationBar` | Visible (default) | Hidden (`sm:hidden`) |
| `NavigationRail` | Hidden (`hidden`) | Visible (`sm:flex`) |

### Rules

1. **Both must have the same items.** Same icons, labels, and active states. They represent the same navigation — just in different form factors.
2. **NavigationBar: 3–5 items maximum** (Material guideline). If more destinations are needed, use the Rail's header menu for overflow.
3. **NavigationRail gets `shrink-0`** to prevent the 80px width from collapsing when content is wide.
4. **The Rail's optional `fab` prop** can host a primary action FAB. On compact, place the FAB as `fixed bottom-24 right-4` to sit above the NavigationBar.

---

## Content Area Patterns

Inside `<main>`, use one of these patterns based on the app's content type.

### Feed Pattern (single-pane scrollable)

For apps with a single scrollable stream of content (social feeds, article lists, dashboards).

```tsx
<main className="flex-1 overflow-y-auto p-4 sm:p-6">
  <div className="max-w-screen-lg mx-auto flex flex-col gap-6">
    {/* Page title */}
    <h1 className="typescale-headline-md">Dashboard</h1>
    {/* Content pane */}
    <section className="bg-surface-container rounded-xl p-4 sm:p-6">
      {/* cards, lists, etc. */}
    </section>
  </div>
</main>
```

Key classes:
- `max-w-screen-lg mx-auto` — constrains width and centers content on wide screens
- `flex flex-col gap-6` — vertical stack with consistent section spacing
- Each `<section>` is a pane: `bg-surface-container rounded-xl p-4 sm:p-6`

### List-Detail Pattern

For apps with a master list and detail view (email, file managers, contacts).

```tsx
<main className="flex-1 overflow-hidden">
  <div className="flex h-full">
    {/* List pane — scrolls independently */}
    <div className="w-full sm:w-[360px] sm:shrink-0 sm:border-r sm:border-outline-variant overflow-y-auto p-4">
      {/* List items */}
    </div>
    {/* Detail pane — scrolls independently */}
    <div className="hidden sm:flex flex-1 overflow-y-auto p-4 sm:p-6">
      {/* Detail content */}
    </div>
  </div>
</main>
```

Responsive behavior:
- **Compact**: List pane is full-width. Detail is hidden. Use routing to switch between list and detail views.
- **Medium+**: Both panes visible side-by-side. List is fixed at 360px, detail fills remaining space. Both scroll independently.

### Supporting Pane Pattern

For apps with a main view and supplementary sidebar (filters, metadata, properties panel).

```tsx
<main className="flex-1 overflow-y-auto p-4 sm:p-6">
  <div className="max-w-screen-xl mx-auto grid grid-cols-1 md:grid-cols-[1fr_320px] gap-6">
    {/* Main content */}
    <section className="bg-surface-container rounded-xl p-4 sm:p-6">
      {/* Primary content */}
    </section>
    {/* Supporting pane */}
    <aside className="bg-surface-container rounded-xl p-4 sm:p-6">
      {/* Filters, metadata, etc. */}
    </aside>
  </div>
</main>
```

Responsive behavior:
- **Compact + Medium**: Single column, supporting pane stacks below main content.
- **Expanded+ (`≥ 840px`)**: Two columns, supporting pane sits to the right at 320px.

---

## Positioning Rules

| Element | Positioning | How It Works |
|---------|-------------|--------------|
| **TopAppBar** | Normal flow | Sits at top of `flex flex-col`. `flex-shrink: 0` (built into component CSS) prevents collapse. |
| **NavigationRail** | Normal flow | Inside `flex` row, inherits full height from `flex-1` parent. `shrink-0` prevents width collapse. |
| **NavigationBar** | Normal flow | Sits at bottom of root `flex flex-col`. Below `<main>`, above nothing. |
| **`<main>`** | Normal flow | `flex-1 overflow-y-auto` — grows to fill remaining space between AppBar/Rail and NavigationBar. Only scrollable element. |
| **FAB (medium+)** | NavigationRail's `fab` slot | Pass as `<NavigationRail fab={<Fab icon="edit" />}>`. Rail positions it internally. |
| **FAB (compact)** | `fixed bottom-24 right-4` | Floats above the NavigationBar (80px bar + 16px gap ≈ `bottom-24`). |

---

## AppBar Scroll Behavior

The `TopAppBar` accepts an `isScrolled` prop that toggles tonal elevation (changes background from `surface` to `surface-container` and adds `shadow-2`). Connect this to the scroll position of `<main>`:

```tsx
const [isScrolled, setIsScrolled] = useState(false);
const mainRef = useRef<HTMLElement>(null);

useEffect(() => {
  const el = mainRef.current;
  if (!el) return;
  const handler = () => setIsScrolled(el.scrollTop > 0);
  el.addEventListener('scroll', handler, { passive: true });
  return () => el.removeEventListener('scroll', handler);
}, []);

// In the shell:
<TopAppBar variant="small" title="App Name" isScrolled={isScrolled} />
// ...
<main ref={mainRef} className="flex-1 overflow-y-auto p-4 sm:p-6">
```

---

## Shell-First Composition

When building any page, ALWAYS follow this order:

1. **Start with the shell** — AppBar + Navigation + `<main>` container.
2. **Add routing** — `<Routes>` go inside `<main>`. Pages render there, not around the shell.
3. **Build page content** — Each page fills the `<main>` slot using a content area pattern above.
4. **Add components** — Cards, tables, forms go inside pages.

### Anti-Patterns

| Anti-Pattern | What Breaks | Fix |
|-------------|-------------|-----|
| Content first, navigation after | Shell structure breaks, nav ends up mispositioned | Shell-first: render scaffold first, pages second |
| `items-center justify-center` on root | All content floats in the middle of the viewport | Content flows from top: use the shell template |
| `min-h-screen` instead of `h-screen` with `overflow-hidden` | Page expands beyond viewport, double scrollbar | Use `h-screen flex flex-col overflow-hidden` on root |
| `position: fixed` on AppBar | Overlaps content, needs padding compensation, z-index fights | Normal flow within the flex column |
| NavigationRail without `shrink-0` | Rail collapses when content is wide | Add `shrink-0` via className |
| JavaScript media queries to toggle Bar/Rail | Unnecessary complexity, flash of wrong nav | CSS `hidden`/`sm:flex`/`sm:hidden` |
| Wrapping NavigationBar inside `<main>` | Bar scrolls with content instead of staying at bottom | Bar goes in root flex column, after the body row |

---

## Tonal Region Map

> **Full reference:** See the **design** skill's "Tonal Region Map" section for the complete hierarchy diagram, all region assignments (including dialogs, menus), and anti-pattern table.

Quick reference for shell regions:

| Region | Tailwind Class | Rationale |
|--------|---------------|-----------|
| Root / page background | `bg-surface` | Lowest tonal layer |
| NavigationRail | `bg-surface` (component default) | Blends with page background per M3 spec |
| NavigationBar | `bg-surface-container` (component default) | Slight tonal lift for bottom bar visibility |
| TopAppBar (resting) | `bg-surface` (component default) | Blends with page at rest |
| TopAppBar (scrolled) | `bg-surface-container` (automatic via `isScrolled`) | Tonal elevation on scroll |
| Content panes | `bg-surface-container` | Tonal separation from page background |
| Cards within panes | `bg-surface-container-high` | One step above the pane |

**Rule:** Never skip levels. Don't place `surface-container-high` content directly on `surface` without an intermediate pane.

---

## Spacing Rhythm

### Shell Spacing

| Relationship | Value | Notes |
|-------------|-------|-------|
| Rail ↔ Content | 0px | Rail and content sit flush; `<main>` padding handles separation |
| AppBar ↔ Content | 0px | Direct contact; tonal shift on scroll provides separation |
| NavBar ↔ Content | 0px | Direct contact; bar's tonal surface provides separation |
| Content inner padding | `p-4 sm:p-6` | 16px compact, 24px medium+ (per WSC table) |

### Content Spacing

| Relationship | Tailwind | When |
|-------------|----------|------|
| Between major sections | `gap-6` | Between distinct content groups |
| Between related items | `gap-4` | Cards in a list, form fields |
| Between tightly related items | `gap-2` | Label + supporting text, icon + text |
| Inside panes | `p-4 sm:p-6` | Consistent with content area padding |

---

## Responsive Patterns

Concrete, copy-ready patterns for common responsive scenarios using the Tailwind breakpoints mapped to Material Window Size Classes.

### Breakpoint Reference

| Tailwind Prefix | Pixel | Material WSC |
|----------------|-------|--------------|
| (default) | `< 600px` | Compact |
| `sm:` | `≥ 600px` | Medium |
| `md:` | `≥ 840px` | Expanded |
| `lg:` | `≥ 1200px` | Large |
| `xl:` | `≥ 1600px` | Extra Large |

### Pattern: Responsive Card Grid

```
Compact:     1 column
Medium:      2 columns
Expanded+:   3 columns
Large+:      4 columns (optional, if content warrants)
```

```tsx
<div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
  {/* Cards */}
</div>
```

Rules:
- Cards in a grid should have **equal height** per row — use `grid` not `flex-wrap`.
- Minimum card width: ~280px. Don't go below this.
- If content doesn't fill 3+ columns well, cap at 2: `grid-cols-1 sm:grid-cols-2 gap-4`.

### Pattern: Responsive Typography

> For the full typescale → semantic role mapping and anti-patterns, see Rule 11 in the `genui` skill.

| Element | Compact | Medium+ | Expanded+ |
|---------|---------|---------|-----------|
| Page title | `typescale-headline-sm` | `typescale-headline-md` | `typescale-headline-lg` |
| Section title | `typescale-title-lg` | `typescale-title-lg` | `typescale-headline-sm` |
| Body text | `typescale-body-md` | `typescale-body-md` | `typescale-body-lg` |
| Captions | `typescale-body-sm` | `typescale-body-sm` | `typescale-body-sm` |

Use Tailwind responsive prefixes on typescale utilities:

```tsx
<h1 className="typescale-headline-sm sm:typescale-headline-md md:typescale-headline-lg">
  Page Title
</h1>
```

### Pattern: List-Detail Responsive

Already covered in the Content Area Patterns section above. Summary:

- **Compact**: Single-pane — show List OR Detail via routing.
- **Medium+**: Two-pane — List (fixed 360px) + Detail (flexible).

```tsx
<div className="flex h-full">
  <div className="w-full sm:w-[360px] sm:shrink-0 sm:border-r sm:border-outline-variant overflow-y-auto p-4">
    {/* List */}
  </div>
  <div className="hidden sm:flex flex-1 overflow-y-auto p-4 sm:p-6">
    {/* Detail */}
  </div>
</div>
```

### Pattern: Supporting Pane / Sidebar

Already covered in the Content Area Patterns section above. Summary:

- **Compact + Medium**: Single column, pane stacks below content.
- **Expanded+**: Side-by-side with fixed-width supporting pane (280–360px).

```tsx
<div className="max-w-screen-xl mx-auto grid grid-cols-1 md:grid-cols-[1fr_320px] gap-6">
  <section className="bg-surface-container rounded-xl p-4 sm:p-6">{/* Main */}</section>
  <aside className="bg-surface-container rounded-xl p-4 sm:p-6">{/* Supporting */}</aside>
</div>
```

### Pattern: Hide vs. Reflow

When content doesn't fit on compact, prefer **reflowing** over **hiding**:

| Scenario | Bad (hiding) | Better (reflowing) |
|----------|-------------|-------------------|
| Navigation actions | `hidden sm:flex` on buttons | Move to overflow menu via `Menu` component |
| Supporting pane | `hidden md:block` | Stack below main: `grid-cols-1 md:grid-cols-[1fr_320px]` |
| Data table columns | Hide columns with `hidden` | Switch to a card-list view on compact |
| Page header actions | `hidden` on compact | Move to AppBar `actionIcons` |

Exceptions where **hiding is acceptable**:
- Switching between NavigationBar/Rail (equivalent components, no information lost)
- Decorative elements that don't carry meaning
- Supplementary labels when icons alone are clear

### Pattern: Responsive Padding

| Element | Classes |
|---------|---------|
| Content area | `p-4 sm:p-6` |
| Pane padding | `p-4 sm:p-6` |
| Card padding | `p-4` |

Step up padding at the medium breakpoint (`sm:`) using responsive prefixes.

### Responsive Anti-Patterns

| Anti-Pattern | Fix |
|-------------|-----|
| Card grid with `flex-wrap` producing unequal heights | Use CSS `grid` with explicit column classes |
| Same typography size at all breakpoints | Step up with responsive prefixes: `typescale-headline-sm sm:typescale-headline-md` |
| `hidden` on content that carries meaning | Reflow into a different layout (stack, menu overflow) instead |
| Fixed pixel widths on content containers | Use `max-w-*` with `mx-auto`, not `w-[800px]` |
| No column count change on card grids at breakpoints | Add `sm:grid-cols-2 md:grid-cols-3` responsive classes |

---

## Complete Example

A full shell with routing, responsive navigation, and scroll-aware AppBar:

```tsx
import { useState, useEffect, useRef } from 'react';
import { Routes, Route, useLocation, useNavigate } from 'react-router-dom';
import { TopAppBar } from '@gbreeze/components/appbar/app-bar';
import { NavigationBar, NavigationBarItem } from '@gbreeze/components/navigationbar/navigation-bar';
import { NavigationRail, NavigationRailItem } from '@gbreeze/components/navigationrail/navigation-rail';
import { Fab } from '@gbreeze/components/fab/fab';

const destinations = [
  { path: '/', icon: 'home', activeIcon: 'home', label: 'Home' },
  { path: '/explore', icon: 'explore', activeIcon: 'explore', label: 'Explore' },
  { path: '/settings', icon: 'settings', activeIcon: 'settings', label: 'Settings' },
];

export default function App() {
  const location = useLocation();
  const navigate = useNavigate();
  const [isScrolled, setIsScrolled] = useState(false);
  const mainRef = useRef<HTMLElement>(null);

  const isActive = (path: string) =>
    path === '/' ? location.pathname === '/' : location.pathname.startsWith(path);

  useEffect(() => {
    const el = mainRef.current;
    if (!el) return;
    const handler = () => setIsScrolled(el.scrollTop > 0);
    el.addEventListener('scroll', handler, { passive: true });
    return () => el.removeEventListener('scroll', handler);
  }, []);

  return (
    <div className="bg-surface text-on-surface h-screen flex flex-col overflow-hidden">
      <TopAppBar variant="small" title="App Name" isScrolled={isScrolled} />

      <div className="flex flex-1 overflow-hidden">
        <NavigationRail
          className="hidden sm:flex shrink-0"
          fab={<Fab icon="edit" />}
        >
          {destinations.map((d) => (
            <NavigationRailItem
              key={d.path}
              icon={d.icon}
              activeIcon={d.activeIcon}
              label={d.label}
              active={isActive(d.path)}
              onClick={() => navigate(d.path)}
            />
          ))}
        </NavigationRail>

        <main ref={mainRef} className="flex-1 overflow-y-auto p-4 sm:p-6">
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/explore" element={<ExplorePage />} />
            <Route path="/settings" element={<SettingsPage />} />
          </Routes>
        </main>
      </div>

      <NavigationBar className="sm:hidden">
        {destinations.map((d) => (
          <NavigationBarItem
            key={d.path}
            icon={d.icon}
            activeIcon={d.activeIcon}
            label={d.label}
            active={isActive(d.path)}
            onClick={() => navigate(d.path)}
          />
        ))}
      </NavigationBar>
    </div>
  );
}
```
