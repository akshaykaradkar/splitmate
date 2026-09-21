# Codebase Component & Token Audit Guide

This document defines the rules for inspecting HTML markup, CSS stylesheets,
component templates, and design token definitions. Use these rules to audit
component mapping, styling attributes, and Android Motion Design System design token
adherence in code.

--------------------------------------------------------------------------------

## 📐 1. Component Mapping & DOM Structure Detection

To identify and verify clean layout structure, parse the UI markup and DOM
structure:

> [!NOTE] **Android Motion Components & Custom Elements are valid components.**
> UI code may use standard HTML/CSS, Web Components, Android Motion `<droid-gb-*>` or `<droid-*>`
> elements (such as Android Motion, Lit, or custom elements libraries). Expect custom element tags such as
> `<droid-gb-button>`, `<droid-button>`, `<droid-gb-card>`, `<droid-card>`,
> `<droid-gb-list>` / `<droid-gb-list-item>`, `<droid-gb-icon>`, `<droid-gb-fab>`,
> `<droid-gb-checkbox>`, `<droid-gb-switch>`, `<droid-assist-chip>`, etc. Treat these
> as valid first-class components and map each to its canonical Android Motion component
> type (e.g., `<droid-gb-button color="filled">` → `Filled button`).

### Global & Navigation Components

-   **App Bar**: Locate top-bar headers such as `<header>`, `<div class="app-bar">`,
    or `.header` nested inside the main layout. Verify if they are styled with a
    fixed/sticky top position or occupy full width.
-   **Main Navigation**: Check for `<nav>` or `.navigation` containers.
    -   **Navigation Bar (Bottom)**: Check for containers anchored to the bottom
        (e.g., `bottom: 0`, `position: fixed`).
    -   **Navigation Rail**: Check for sidebar panels with narrow width (e.g.,
        `72px` to `80px`) and stacked vertical children.
    -   **Navigation Drawer**: Check for larger side drawers (e.g., `240px` to
        `360px` width) with horizontal items.

### Layout & Action Surfaces

-   **Cards**: Trace `.card` or `.surface` containers. Check that they enclose
    composite text, metrics, or graphics rather than acting as a single action
    element.
-   **Lists and List Items**: Audit list elements (e.g., `<ul>`, `<ol>`, or
    `.list` containers). Confirm that list items (e.g., `<li>` or `.list-row`)
    are correctly nested within their parent list container.
-   **Buttons (Filled, Outlined, Tonal, Text)**: Trace `<button>` tags, `.btn`
    elements, or web components. Confirm their type by inspecting borders, solid
    background containment, and padding declarations.
-   **FAB / Extended FAB**: Look for action buttons with absolute floating
    positions (e.g., `bottom: 24px; right: 24px`) or floating action tags.

--------------------------------------------------------------------------------

## 🎨 2. Styling, Motion & Token Inspection

Audit stylesheets (e.g., `style.css`, theme CSS, or CSS-in-JS) to verify that
style variables correspond properly to Android Motion design tokens:

### Custom Properties and Variables

-   Verify that colors are mapped to CSS variables (e.g., `var(--droid-sys-color-primary)`
    or `var(--color-primary)`) rather than using hardcoded raw hex strings.
-   Verify elements use design variables declared in `:root` or theme scopes.

### Elevation, Spacing, and Motion/Animation

-   Check card container padding limits (e.g., `16px` to `24px` margins).
-   Ensure margins and grid columns respect responsive layout instructions.
-   Check that elevations are implemented using standard box shadows or Android Motion
    elevation tokens (e.g., `var(--droid-sys-elevation-level1)` or `--droid-sys-elevation-*`).
-   Verify that transitions and animations bind to standard Android Motion easing (e.g., `--droid-sys-motion-easing-emphasized`) and duration (e.g., `--droid-sys-motion-duration-250`) tokens instead of hardcoded bezier curves or random timing values.

--------------------------------------------------------------------------------

## 📖 3. Validation Against Design Tokens & Specifications

Verify overall codebase compliance against available design specifications,
design token documents, or theme definitions (e.g., `design_memory.md`,
`DESIGN.md`, `tokens.json`, or theme CSS files):

### Theme and Typography Adherence

-   Check that hex codes in stylesheets match the colors listed in the design
    token specification or theme palette.
-   Verify typography styles match designated values (e.g., Google Sans,
    Google Sans Text, or specified sans-serif typography).
-   Verify type scale values (Display, Headline, Title, Body, Label) match the spec.

### Responsive Grids & Spacing

-   Trace layout styles, responsive media query breakpoints (compact, medium,
    expanded boundaries), and element class associations.

### Color & Palette Extraction Heuristics

To extract the theme palette from CSS for the JSON `theme` object, inspect the
stylesheet variables defined in `:root` or the body container. Match the CSS
variables to the following JSON theme keys:

-   `primaryColor`: Extract from `--droid-sys-color-primary`, `--color-primary`,
    `--primary`, or similar primary theme variables.
-   `secondaryColor`: Extract from `--droid-sys-color-secondary`,
    `--color-secondary`, `--secondary`, or similar secondary theme variables.
-   `backgroundColor`: Extract from `--droid-sys-color-background`,
    `--color-background`, `--background`, or similar background theme variables.
-   `surfaceColor`: Extract from `--droid-sys-color-surface`, `--color-surface`,
    `--surface`, or similar surface theme variables.
-   `onSurface`: Extract from `--droid-sys-color-on-surface`, `--color-on-surface`,
    `--on-surface`, or similar variables.
-   `onSurfaceVariant`: Extract from `--droid-sys-color-on-surface-variant`,
    `--color-on-surface-variant`, `--on-surface-variant`, or similar variables.

If the theme variables are not mapped using `--droid-sys-color-*` or standard
custom property names, trace the custom properties applied to container tags
in the markup files using their inline styles or class selectors in stylesheets
(e.g., background-color values).
