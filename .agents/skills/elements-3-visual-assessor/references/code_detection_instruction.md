# Codebase Component & Token Audit Guide for Elements GM3

This document defines the rules for inspecting HTML markup, CSS stylesheets,
component templates, and design token definitions. Use these rules to audit
component mapping, styling attributes, and Elements GM3 design token
adherence in code.

--------------------------------------------------------------------------------

## 📐 1. Component Mapping & DOM Structure Detection

To identify and verify clean layout structure, parse the UI markup and DOM
structure:

> [!NOTE] **Elements GM3 / gBreeze Web Components & Custom Elements are valid components.**
> UI code may use standard HTML/CSS, Web Components, gBreeze (`<md-gb-*>`),
> `@material/web` (`<md-*>`), or frontend framework libraries (such as
> Angular Material, or custom elements in CEE3). Expect custom element tags such as
> `<md-gb-button>`, `<md-button>`, `<md-gb-card>`, `<md-card>`,
> `<md-gb-list>` / `<md-gb-list-item>`, `<md-gb-icon>`, `<md-gb-fab>`,
> `<md-gb-checkbox>`, `<md-gb-switch>`, `<md-assist-chip>`, etc. Treat these
> as valid first-class components and map each to its canonical Elements GM3 component
> type (e.g., `<md-gb-button type="filled">` → `Filled button`).

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

## 🎨 2. Styling & Token Inspection

Audit stylesheets (e.g., `style.css`, theme CSS, or CSS-in-JS) to verify that
style variables correspond properly to Elements GM3 design tokens:

### Custom Properties and Variables

-   Verify that colors are mapped to CSS variables (e.g., `var(--cee3-sys-color-extended-blue)`,
    `var(--md-sys-color-primary)`, or `var(--color-primary)`) rather than using hardcoded raw hex strings.
-   Verify elements use design variables declared in `:root` or theme scopes.

### Elevation, Density, and Spacing

-   Check card container padding limits (e.g., `16px` to `24px` margins).
-   Ensure margins and grid columns respect responsive layout instructions.
-   Check that elevations are implemented using standard box shadows or Elements GM3
    elevation tokens (e.g., `var(--elevation-1)` or `--md-sys-elevation-*`).

--------------------------------------------------------------------------------

## 📖 3. Validation Against Design Tokens & Specifications

Verify overall codebase compliance against available design specifications,
design token documents, or theme definitions (e.g., `design_memory.md`,
`DESIGN.md`, `tokens.json`, or `design-system-bindings.css` files):

### Theme and Typography Adherence

-   Check that hex codes in stylesheets match the colors listed in the design
    token specification or theme palette.
-   Verify typography styles match designated values (e.g., Google Sans,
    Google Sans Text, Google Symbols, or specified sans-serif typography).
-   Verify type scale values (Display, Title, Body, Label) match the spec.

### Responsive Grids & Spacing

-   Trace layout styles, responsive media query breakpoints (compact, medium,
    expanded boundaries), and element class associations.

### Color & Palette Extraction Heuristics

To extract the theme palette from CSS for the JSON `theme` object, inspect the
stylesheet variables defined in `:root` or the body container. Match the CSS
variables to the following JSON theme keys:

-   `primaryColor`: Extract from `--cee3-sys-color-extended-blue`, `--md-sys-color-primary`,
    `--color-primary`, `--primary`, or similar primary theme variables.
-   `secondaryColor`: Extract from `--md-sys-color-secondary`,
    `--color-secondary`, `--secondary`, or similar secondary theme variables.
-   `backgroundColor`: Extract from `--md-sys-color-background`,
    `--color-background`, `--background`, or similar background theme variables.
-   `surfaceColor`: Extract from `--md-sys-color-surface`, `--color-surface`,
    `--surface`, or similar surface theme variables.
-   `onSurface`: Extract from `--md-sys-color-on-surface`, `--color-on-surface`,
    `--on-surface`, or similar variables.
-   `onSurfaceVariant`: Extract from `--md-sys-color-on-surface-variant`,
    `--color-on-surface-variant`, `--on-surface-variant`, or similar variables.

If the theme variables are not mapped using `--cee3-sys-color-*` or standard
custom property names, trace the custom properties applied to container tags
in the markup files using their inline styles or class selectors in stylesheets
(e.g., background-color values).
