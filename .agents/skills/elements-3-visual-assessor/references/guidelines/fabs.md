# Floating Action Buttons (FABs) - Universal AI Detection Guide & Elements GM3 Specifications

This reference provides universal visual heuristics for detecting Floating Action Buttons (FABs) and Extended FABs across any UI, followed by Elements GM3 design system specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Floating Action Button (FAB)** represents the primary action in an application on both mobile and desktop screens. It is most commonly used to create or start something new (e.g., Compose, Create, Add). Agents must detect FABs and Extended FABs based on their distinctive geometry, prominent placement, and primary action status, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Placement & Positioning**: In the Elements GM3 design system, FABs are restricted to the left-side navigation elements rather than floating arbitrarily over page content.
    -   *Large Viewports (Desktop)*: Placed in the upper-left region of the expanded navigation drawer, above the primary navigation. It follows a clear DOM focus order.
    -   *Small Viewports (Mobile/Tablet)*: Placed within the navigation overlay/drawer.
-   **Variants & Visual Signatures**: Look for two primary structural patterns:
    -   **Standard FAB (Rounded Square)**: A compact container displaying a single prominent system icon.
    -   **Extended FAB (Horizontally Elongated Rounded Square or Pill)**: A wider container housing a clear leading icon followed by a concise text label (1-2 words).
-   **Core Anatomy & Sizing**:
    -   *Container*: A boxier bounding container with a distinct background fill and rounded corners. In Elements GM3, standard elevation is removed at rest because of its specific navigation drawer placement, meaning it doesn't overlap content and doesn't require drop shadows.
    -   *Action Media*: A centered system icon (Standard FAB) or a leading icon plus a text label (Extended FAB).
-   **FABs vs. Chips or Buttons**: Extended FABs are distinguished by their high prominence, placement at the top of the navigation hierarchy, and primary-action role. They must never be classified as generic Buttons or Chips.

---

## Part 2: Elements GM3 Specifications & Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate detected FAB components against the following strict standards:

### Component Specifications & Sizing

Elements GM3 establishes specific visual and structural standards for FAB components:

1.  **Standard FAB Specifications**:
    -   **Size**: Size must be exactly 56x56dp. *Note*: Elements GM3 only endorses the default FAB size; small, medium, large, and branded FAB sizes are deprecated or archived.
    -   **Shape**: Features a boxier geometry with a strict **16dp** corner radius (`--cee3-shape-corner-large` or equivalent), rather than a circle.
    -   **Color Styles**: Uses the standard **Primary** color style or a **White** fill (to avoid visually competing with the product logo in the App bar).
    -   **Elevation**: Elevation is **removed at rest** (no drop shadow). Standard interaction states (hover, focus, press) follow Google Material standards.
2.  **Extended FAB Specifications**:
    -   **Sizing**: Features a height of **56dp** (matching standard FAB height).
    -   **Shape**: Features a **16dp** corner radius or a fully rounded pill with **28dp** corner radius, with **16dp** start/end internal paddings.
    -   **Content & Text**: Must contain a clear text label (sentence case) and a leading icon. The text label should be short action words (e.g., Create, Add, Compose), aiming for **12 characters** (maximum **20 characters**).
3.  **FAB with Dropdown Menu (Multiple Actions)**:
    -   **Usage**: When multiple primary actions are required, they must be displayed in a dropdown menu positioned **8dp** below the FAB.
    -   **List Limits**: Must contain between **2 to 6 actions**.
    -   **Styling**: The dropdown menu must have **Elevation 2** and a **4dp** corner radius.
    -   **Iconography**: Menu list material icons must be exactly **24dp** in size. Do not mix menu items with and without icons. Cascading submenus are prohibited.
    -   **Interaction**: Follows the ARIA "menu button" pattern.

### Behavioral, Placement & Accessibility Rules

-   **Action Exclusivity**: There should be at most **one** primary FAB or Extended FAB per viewport.
-   **Placement & Magnification**: Fixed placement inside the drawer is **not recommended** because it restricts magnification users from scrolling through navigation items.
-   **No External Links**: The FAB represents an internal primary action and must **never** navigate to an external link, new tab, or new window.
-   **Accessibility Roles**:
    -   ARIA role is set to `button` by default (for same-page actions). It can be configured as `link` if it navigates to a different page.
    -   If an icon-only FAB is used, an explicit accessibility label is mandatory.
    -   The FAB must be contained within its own `region` ARIA landmark inside the `navigation` landmark.

### Critical Elements GM3 Violations to Flag

-   **Deprecated/Non-Endorsed Sizes**: Using small or large standard FAB sizes, or archived branded FAB styles.
-   **Incorrect Shape**: Using pure circular geometry for standard FABs instead of the mandated 16dp corner radius.
-   **Resting Elevation**: Presenting a FAB with a visible drop shadow at rest when integrated into the navigation drawer or rail.
-   **Ambiguous Labels**: Extended FAB text labels exceeding 20 characters or containing passive, non-action words.
-   **External Navigation**: Mapping a FAB action to open an external website or separate tab.
