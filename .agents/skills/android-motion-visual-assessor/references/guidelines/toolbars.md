# Toolbars - Universal AI Detection Guide & Android Motion Specifications

This reference provides universal visual heuristics for detecting Toolbars
(docked or floating bottom action containers) across any design system or
platform, followed by Android Motion design system specifications for compliance
auditing.

## Part 1: Universal AI Detection Heuristics

A **Toolbar** is a dedicated action container positioned at the bottom (or
sides) of a screen, designed to display frequently used contextual actions
relevant to the current page or active mode (e.g., formatting tools, editing
controls, or item filters). Agents must detect toolbars based on bottom docking,
floating pill geometry, and clustered action buttons, regardless of design
system adherence.

### Key Visual & Geometric Heuristics

-   **Placement & Docking Signatures**: Look for prominent action containers
    anchored near the bottom of the viewport:
    -   **Docked Toolbar**: Anchored flush against the extreme bottom edge of
        the window, spanning 100% of the viewport width. (Replaces the legacy
        "Bottom App Bar").
    -   **Floating Toolbar**: Floats directly above body content near the bottom
        (horizontal) or along the side edges (vertical).
-   **Containment Geometry & Boundaries**:
    -   *Docked*: A clean rectangular container with sharp, straight corners (no
        rounded corners by default) sitting flush with the bottom edge.
    -   *Floating*: A distinct pill-shaped container featuring fully rounded
        corners, exhibiting a prominent elevation drop shadow over underlying
        content.
-   **Clustered Action Elements**: Look for horizontal groupings of icon buttons
    (filled, tonal, standard) or form controls. Floating toolbars frequently sit
    immediately adjacent to a primary Floating Action Button (FAB) lockup.
-   **Active Mode Color Cues**: Detect two primary color schemes: *Standard*
    (low-emphasis surface fill focusing attention on body content) vs. *Vibrant*
    (high-emphasis primary/secondary fill drawing attention to controls,
    frequently indicating an active editing mode).
-   **Floating Navigation Toolbar (Non-Standard Usage)**: If a bottom bar is
    styled as a floating pill (not full-width, rounded corners, floats above
    content) but houses navigation destinations, it MUST be classified as a
    **`Toolbar`** based on its visual geometry, prioritizing visual structure
    over functional intent.

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion design system adherence audit, evaluate detected
toolbars against the following strict standards derived from Android Motion
guidelines:

### Android Motion Component Specifications & Sizing

-   **Dimensions & Sizing Tokens**:
    -   **Container Height**: Default container height is strictly **64dp**.
    -   **Touch Targets**: Every interactive element inside the toolbar must
        meet the mandatory minimum **48x48dp touch target** requirement.
    -   **Paddings & Spacing**: Minimum 16dp outer padding from window edges for
        docked toolbars. Grouped items maintain 32dp default spacing. Vertical
        floating toolbars maintain a minimum 24dp margin from window edges
        (positioned opposite navigation rails for layout balance).
-   **Shape & Geometries**:
    -   **Docked Toolbar**: Uses straight corners with 0px radius (`--droid-sys-shape-corner-none`).
    -   **Floating Toolbar**: Uses fully rounded corners (`--droid-sys-shape-corner-full`, which is `max(50cqw, 50cqh)`) and must use elevation shadow `--droid-sys-elevation-level3` (6px) or `--droid-sys-elevation-level4` (8px).
-   **Color & Typography**:
    -   Container fill: `--droid-sys-color-surface` (light-dark(`#fdfcfb`, `#1f1f1f`)) or `--droid-sys-color-surface-variant` (light-dark(`#e1e3e1`, `#444746`)).
    -   Interactive icon buttons inside the toolbar must be rendered with `'Google Symbols'` using `--md-icon-font` and `--droid-sys-color-primary` (or `--droid-sys-color-on-surface`).
-   **Layout Conflicts & Mutually Exclusive Navigation**:
    -   **Mandatory Rule**: Products must **never** show a toolbar and a
        navigation bar at the same time at the bottom of a screen. Show the
        navigation bar on primary top-level pages, and toolbars on subsequent
        pages requiring local actions.
-   **Scroll & Adaptive Behaviors**:
    -   On page scroll, toolbars may animate off-screen or collapse dynamically
        into a single primary action FAB.
    -   These animated transitions must utilize the `--droid-sys-motion-duration-250` (250ms) or `--droid-sys-motion-duration-300` (300ms) with the premium `--droid-sys-motion-easing-emphasized` (`cubic-bezier(0.2, 0.0, 0.0, 1.0)`) or standard decelerate easing curves for realistic fluid feedback.

### Critical Android Motion Violations to Flag

-   **Simultaneous Toolbar and Nav Bar**: Presenting both a bottom action
    toolbar and a bottom navigation bar simultaneously on a single screen
    layout.
-   **Rounded Corners on Docked Toolbars**: Applying rounded corners to a
    full-width docked toolbar container (which misleadingly implies the
    container expands or floats and violates `--droid-sys-shape-corner-none`).
-   **Undersized Touch Targets**: Shrinking toolbar icon buttons below the
    mandatory 48x48dp minimum accessible size.
-   **Linear Scroll Animations**: Animating the toolbar entrance or exit with basic linear transitions instead of the required `--droid-sys-motion-easing-emphasized` curve.
