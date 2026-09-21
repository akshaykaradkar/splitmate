# Floating Action Buttons (FABs) - Universal AI Detection Guide & Android Motion Specifications

This reference provides universal visual heuristics for detecting Floating
Action Buttons, Extended FABs, and FAB Menus (prominent floating action
containers) across any design system or platform, followed by Android Motion
design system specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Floating Action Button (FAB)** is a prominent, high-priority interactive
container designed to represent the primary or most common action on a screen
(e.g., Compose, Create, Add). Agents must detect FABs, Extended FABs, and FAB
Menus based on their floating placement, distinctive geometry, and high-contrast
visual layers, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Placement & Floating Anchoring**: FAB components float above all other
    underlying page content, maintaining their position during page scroll.
    -   *Compact / Mobile Screens*: Consistently anchored in the lower trailing
        quadrant (bottom-right for LTR layouts, bottom-left for RTL layouts) or
        bottom-center of the viewport.
    -   *Expanded / Desktop Screens*: Placed in the bottom trailing corner OR
        integrated directly into the upper leading region of an expanded
        navigation rail.
-   **Variants & Visual Signatures**: Look for three primary structural
    patterns:
    -   **Standard FAB (Circular / Rounded Square)**: A compact floating
        container displaying a single prominent icon.
    -   **Extended FAB (Pill / Rounded Rectangle)**: A wider, horizontally
        elongated floating container housing a clear icon followed by a concise
        text label (1-2 words). Tightly hugs its content.
    -   **FAB Menu (Action Stack)**: A vertical cluster of related secondary
        actions (2-6 items) sprouting from a base FAB. When open, the base FAB
        transforms into a distinct "Close" button (e.g., an "X" or chevron).
-   **Core Anatomy & Touch Targets**:
    -   *Container*: A prominent bounding box featuring a high-contrast
        background fill and a distinct drop shadow (elevation).
    -   *Action Media*: A centered icon (Standard FAB) or leading icon plus text
        label (Extended FAB / Menu items).
-   **Adaptive Behavior**: Agents analyzing multi-state UI flows should note
    that Extended FABs frequently collapse into standard circular FABs on
    downward scroll to maximize viewing space, expanding back on upward scroll.
-   **FABs vs. Chips**: Extended FABs (pill-shaped or rounded-rectangular with
    icon and text) can visually resemble Chips. However, FABs are distinguished
    by their **floating behavior** (anchored above content, persistent on
    scroll) and **primary action** status. Standalone floating pill-shaped or
    rounded-rectangular elements must be mapped to **`Extended FAB`** (or
    `FAB`), never to `Chip`.

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion design system adherence audit, evaluate detected FAB
components against the following strict standards derived from Android Motion guidelines:

### Android Motion Component Specifications & Sizing

Android Motion design system establishes specific sizing tiers and behavioral rules for FAB
components:

1.  **Standard FAB Specifications**:
    -   **Sizes**: The system defines three distinct sizes: `FAB` (default, 56x56dp), `Small FAB` (40x40dp), and `Large FAB` (96x96dp).
    -   **Shape**: Utilizes rounded square geometry (squircle-like) rather than pure circles.
        -   *Default FAB (56x56dp)*: Corner radius is 16dp using `--droid-sys-shape-corner-large`.
        -   *Small FAB (40x40dp)*: Corner radius is 8dp using `--droid-sys-shape-corner-small`.
        -   *Large FAB (96x96dp)*: Corner radius is 28dp using `--droid-sys-shape-corner-extra-large`.
    -   **Color Roles**: Uses primary, secondary, or tertiary baseline color palettes.
        -   *Primary Container style*: `--droid-sys-color-primary-container` (light-dark(`#d3e3fd`, `#0842a0`)) background with `--droid-sys-color-on-primary-container` (light-dark(`#041e49`, `#d3e3fd`)) content.
        -   *Secondary Container style*: `--droid-sys-color-secondary-container` (light-dark(`#c2e7ff`, `#004a77`)) background with `--droid-sys-color-on-secondary-container` (light-dark(`#001d35`, `#c2e7ff`)) content.
2.  **Extended FAB Specifications**:
    -   **Sizes**: The system introduces three height tiers: `Small` (56dp height, replacing baseline extended FAB), `Medium` (80dp height), and `Large` (96dp height).
    -   **Shape**: Corner radius is 16dp using `--droid-sys-shape-corner-large` or fully rounded pill-shaped 28dp using `--droid-sys-shape-corner-extra-large` or `--droid-sys-shape-corner-full`.
    -   **Content**: Must contain a clear text label (sentence case, typography `--droid-sys-typescale-label-large`) and an optional but highly recommended leading icon. Both icon and label act as a single focusable element. Start/end internal padding is strictly 16dp.
3.  **FAB Menu Specifications**:
    -   **Origin & Spacing**: Animates from the top trailing edge of the base FAB. Maintains a recommended 4dp gap between the base FAB and the lowest menu item.
    -   **Item Anatomy**: Close button matches the 56dp base FAB. Menu items align with medium button specs and must contain text labels (icons recommended). Replaces legacy speed dial patterns or stacked small FABs.

### Android Motion Behavioral & Exclusivity Rules

-   **Action Exclusivity**: Only **one** primary FAB or Extended FAB should be
    active per screen. Placing multiple competing FABs on a single screen
    disrupts the visual hierarchy.
-   **Elevation Hierarchy**: FABs reside at a high resting elevation (`--droid-sys-elevation-level3` (6px)). Hovering a FAB temporarily increases its elevation by 1 level to `--droid-sys-elevation-level4` (8px).
-   **Navigation Rail Integration**: On large web/desktop screens, placing the
    FAB or Extended FAB at the top of an expanded navigation rail provides an
    ergonomic, accessible primary action.

### Critical Android Motion Violations to Flag

-   **Multiple Active FABs**: Placing two or more permanent FABs on the same
    screen, creating conflicting primary calls to action.
-   **Legacy Small FABs or Speed Dials**: Using deprecated small FAB stacks or
    speed dials instead of the standardized `FAB Menu`.
-   **Missing Text on Extended FAB**: Presenting an Extended FAB container
    without a text label, violating component definitions.
