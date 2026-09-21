# Dividers - Elements GM3 Design System Guide \& Specifications

This reference provides visual heuristics for detecting and evaluating Dividers under the Elements GM3 design system (including CEE3 system-level and GM3 component specifications) for visual and codebase auditing.

## Part 1: Universal AI Detection Heuristics for Elements GM3

An Elements GM3 **Divider** is a thin visual rule used to separate unrelated content, group related items within lists, or establish structural hierarchy across screen areas.

### Key Visual \& Geometric Heuristics

-   **Fine Line Geometry**: Continuous, flat, single lines with a visual thickness of exactly 1dp (or 1px).
-   **Orientation Alignment**:
    -   *Horizontal Dividers*: Most common; span across containers, tables, dialogs, or list views.
    -   *Vertical Dividers*: Used in desktop/tablet layouts to separate adjacent content panels, columns, or toolbar icon groups.
-   **Layout Margins \& Insets**:
    -   *Full-Width*: Spans 100% of the parent container, separating major sections, headers, or footers.
    -   *Inset*: Features a left indent of 16dp (aligning with standard list item text padding) while bypassing leading icons or avatars.
    -   *Middle-Inset*: Features equal 16dp indents on both left and right edges.
-   **No Drop Shadows**: Dividers must remain flat and completely shadowless.

---

## Part 2: Elements GM3 Component Specifications

When auditing for Elements GM3 compliance, evaluate detected dividers against these specific measurements and rules:

### 1. Dimension Rules

-   **Thickness**: Strictly **1dp (or 1px)** height (or width for vertical dividers).
-   **Full-Width Divider**: 100% container width (0dp left/right margins).
-   **Inset Divider**: Exactly **16dp left margin** and 0dp right margin, perfectly aligned with the text margins of neighboring elements.
-   **Middle-Inset Divider**: Exactly **16dp left margin** and **16dp right margin**.

### 2. Colors and Contrast

-   **Dynamic Color Token**: Dividers utilize the dynamic subtle color token **`outline-variant`** (or standard GM3 outline-variant) to maintain an elegant, low-contrast appearance that does not overpower adjacent text.
-   **No WCAG Contrast Minimums**: Since dividers are classified as purely decorative boundaries, they do not have mandatory WCAG contrast minimums, preventing them from becoming heavy visual barriers.

### 3. Usage \& Behavior Rules

-   **Scrolling Dialogs**: A 1px `outline-variant` divider must appear dynamically when body content in a dialog is scrolled, separating the fixed header/footer from the scrollable area. It may also be used by default to separate tab bars or form headers.
-   **Uncluttered Groups**: Dividers should only be placed between items in a group to separate them. They must not be placed at the very top or bottom of a list container if a boundary is already established by container edges or surrounding spacing.

---

## Part 3: Critical Elements GM3 Violations to Flag

-   **Excessive Thickness**: Using lines thicker than 1dp/1px (e.g., 2dp or 3dp) for standard dividers, creating heavy, visually distracting barriers.
-   **Stark/High-Contrast Lines**: Rendering dividers in stark, bold, or high-contrast colors (like pure black `#000000` on white) that dominate the layout.
-   **Improper Inset Padding**: Misaligning inset dividers with the standard 16dp list padding, leading to sloppy vertical alignment.
-   **Shaded or Shadowed Lines**: Incorporating gradients, bevels, or drop shadows on dividers.
