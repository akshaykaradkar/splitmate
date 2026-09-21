---
name: react-routing
description: Standardized patterns for implementing navigation and routing using react-router-dom.
---
# React Routing Skill

Goal: Implement a professional, deep-linkable navigation system that removes prop-drilling for navigation handlers and keeps layout logic tied to the URL.

## Core Principles

1.  **Autonomous Navigation**: UI components (Cards, Buttons, Menu Items) should handle their own navigation using hooks (`useNavigate`, `Link`) rather than receiving click handlers from parents.
2.  **URL as State Source**: Dynamic pages (e.g. Video Details, User Profiles) MUST pull their primary data ID from the URL params (`useParams`) rather than from a shared `selectedItem` state.
3.  **Path-Driven Layout**: Global layout visibility (Hiding Sidebar on Watch Page, showing Header on all) should be determined by inspecting the current path via `useLocation`.

## Implementation Workflow

### 1. Setup (main.tsx)
Always wrap the root `<App />` in a `<BrowserRouter>`.
```tsx
import { BrowserRouter } from 'react-router-dom';
// ...
<BrowserRouter>
  <App />
</BrowserRouter>
```

### 2. Route Definition (App.tsx)
Define clear, human-readable paths. Use layout components to wrap `Routes` if needed.
```tsx
import { Routes, Route } from 'react-router-dom';

<Routes>
  <Route path="/" element={<HomePage />} />
  <Route path="/detail/:id" element={<DetailPage />} />
</Routes>
```

### 3. Navigation Patterns

#### Static Links
Use `<Link>` for any standard text or image navigation (Logo, Home button).
```tsx
import { Link } from 'react-router-dom';
<Link to="/">Home</Link>
```

#### Programmatic Navigation
Use `useNavigate` for interactive components like cards or buttons where complex behavior (like ripples or tracking) might occur before navigating.
```tsx
import { useNavigate } from 'react-router-dom';

export function Card({ id }) {
  const navigate = useNavigate();
  return <div onClick={() => navigate(`/detail/${id}`)}>...</div>;
}
```

### 4. Parameter Extraction
In your "Page" or "View" components, extract parameters to fetch data.
```tsx
import { useParams } from 'react-router-dom';

export function DetailPage() {
  const { id } = useParams<{ id: string }>();
  // fetch data based on id
}
```

## Routing & Layout Logic

To hide elements like sidebars on specific routes:
```tsx
const location = useLocation();
const isDetailView = location.pathname.startsWith('/detail/');

return (
  <div className="layout">
    <Header />
    <div className="main">
      {!isDetailView && <Sidebar />}
      <Routes>...</Routes>
    </div>
  </div>
);
```

## Self-Correction Checklist

1. **Prop-Drilling Navigation?** If a parent is passing `onItemClick` just to call `setSelected(id)` and change a view, refactor it to use `useNavigate` inside the child.
2. **Hardcoded IDs?** Are you using local state to track current item? Move it to URL params.
3. **No Go-Back Support?** If the browser's back button doesn't work for navigation you just built, you're not using standard Routing.

## Navigation Wiring

This section explains how to connect `NavigationBar` and `NavigationRail` items to routes so navigation is functional — not just decorative.

> **Prerequisite:** The app shell must already be in place (see the `layout-scaffold` skill). This section covers the **behavior** of navigation, not where it sits in the layout.

### Shared Navigation Destinations

Both NavigationBar (compact) and NavigationRail (medium+) render the **same destinations**. Define them once and render in both components:

```typescript
interface NavDestination {
  path: string;
  icon: string;
  activeIcon: string;
  label: string;
}

const destinations: NavDestination[] = [
  { path: '/', icon: 'home', activeIcon: 'home', label: 'Home' },
  { path: '/explore', icon: 'explore', activeIcon: 'explore', label: 'Explore' },
  { path: '/library', icon: 'video_library', activeIcon: 'video_library', label: 'Library' },
];
```

### Active State from Route

Use `useLocation` to determine which nav item is active. Use prefix matching so nested routes (e.g., `/settings/profile`) highlight the parent tab:

```typescript
import { useLocation } from 'react-router-dom';

const location = useLocation();

function isActive(path: string): boolean {
  if (path === '/') return location.pathname === '/';
  return location.pathname === path || location.pathname.startsWith(path + '/');
}
```

Key rules:
- The root path `/` needs **exact** matching to avoid matching everything.
- All other paths use `startsWith` prefix matching for nested route support.
- Both NavigationBar and NavigationRail items must use the same `isActive` logic.

### Navigation on Click

Nav items use `useNavigate` in their `onClick`:

```typescript
import { useNavigate } from 'react-router-dom';

const navigate = useNavigate();

// In both NavigationBar and NavigationRail:
{destinations.map((dest) => (
  <NavigationBarItem
    key={dest.path}
    icon={dest.icon}
    activeIcon={dest.activeIcon}
    label={dest.label}
    active={isActive(dest.path)}
    onClick={() => navigate(dest.path)}
  />
))}
```

### Extracting a `useNavigation` Hook

For apps with 3+ navigation destinations, extract a shared hook to guarantee sync between Bar and Rail:

```typescript
import { useLocation, useNavigate } from 'react-router-dom';

function useNavigation() {
  const location = useLocation();
  const navigate = useNavigate();

  function isActive(path: string): boolean {
    if (path === '/') return location.pathname === '/';
    return location.pathname === path || location.pathname.startsWith(path + '/');
  }

  return destinations.map((dest) => ({
    ...dest,
    active: isActive(dest.path),
    onClick: () => navigate(dest.path),
  }));
}
```

Both NavigationBar and NavigationRail consume this hook:

```tsx
function App() {
  const navItems = useNavigation();

  return (
    <div className="bg-surface text-on-surface h-screen flex flex-col overflow-hidden">
      <TopAppBar variant="small" title="App Name" />
      <div className="flex flex-1 overflow-hidden">
        <NavigationRail className="hidden sm:flex shrink-0">
          {navItems.map((item) => (
            <NavigationRailItem key={item.path} {...item} />
          ))}
        </NavigationRail>
        <main className="flex-1 overflow-y-auto p-4 sm:p-6">
          <Routes>{/* ... */}</Routes>
        </main>
      </div>
      <NavigationBar className="sm:hidden">
        {navItems.map((item) => (
          <NavigationBarItem key={item.path} {...item} />
        ))}
      </NavigationBar>
    </div>
  );
}
```

### Navigation Wiring Rules

1. **Both navigations must render the same destinations.** If the Rail has 4 items, the Bar must have 4 items with the same labels and icons.
2. **Never hardcode `active={true}` on a nav item.** Always derive it from the current route via `useLocation`.
3. **NavigationBar should have 3–5 items max** (Material guideline). Use the Rail's menu button or a navigation drawer for overflow.
4. **Don't pass navigation handlers as props from App.** Nav items should use `useNavigate` internally (follows the "Autonomous Navigation" principle above).
5. **Browser back/forward must work.** Because `useNavigate` pushes to the history stack, the active state automatically updates when the user navigates with browser controls.

### Navigation Wiring Anti-Patterns

| Anti-Pattern | What Breaks | Fix |
|-------------|-------------|-----|
| `active={true}` hardcoded on a nav item | Active state never changes when route changes | Derive from `useLocation` with `isActive()` |
| NavigationBar has different items than NavigationRail | Users see different nav on resize, confusing | Define destinations once, render in both |
| Using `window.location` instead of `useNavigate` | Full page reload, loses React state | Use `useNavigate` from react-router-dom |
| Exact match only (no prefix matching) | Nested routes like `/settings/profile` don't highlight parent | Use `startsWith(path + '/')` for non-root paths |
| Passing `onClick` handlers from parent as props | Props drilling, violates Autonomous Navigation principle | Use `useNavigate` inside the nav component |
