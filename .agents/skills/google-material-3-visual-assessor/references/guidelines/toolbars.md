# Toolbars - Universal AI Detection Guide & MD3 Specifications

This reference provides universal visual heuristics for detecting Toolbars
(docked or floating bottom action containers) across any design system or
platform, followed by Material Design 3 (MD3) specifications for compliance
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
        "Bottom App Bar" which is no longer recommended in the Expressive update).
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

## Part 2: MD3 Specifications & Compliance Auditing

When conducting a Material Design 3 (MD3) adherence audit, evaluate detected
toolbars against the following strict standards derived from Material
guidelines:

### MD3 Component Specifications & Sizing

-   **Dimensions & Sizing Tokens**:
    -   **Container Height**: Default container height is strictly **64dp**.
    -   **Touch Targets**: Every interactive element inside the toolbar must
        meet the mandatory minimum **48x48dp touch target** requirement (using space token `--md-sys-measurement-space600`).
    -   **Paddings & Spacing**: 
        -   Minimum **16dp** outer padding from window edges for docked toolbars (`--md-sys-measurement-space200`).
        -   Grouped items maintain a **32dp** default spacing (`--md-sys-measurement-space400`).
        -   Vertical floating toolbars maintain a minimum **24dp** margin from window edges (`--md-sys-measurement-space300`) and are positioned opposite navigation rails for layout balance.
-   **Shape Tokens & Geometry**:
    -   **Docked Toolbar Container**: Must use **straight corners** with zero border radius (`--md-sys-shape-corner-none`).
    -   **Floating Toolbar Container**: Must use fully-rounded corners (`--md-sys-shape-corner-full` / `max(50cqw, 50cqh)`).
    -   **Button Shapes within Floating Toolbars**: Avoid using square icon buttons in floating toolbars. Their square shape conflicts with the fully-rounded shape of the floating toolbar container.
-   **Color & Elevation Roles**:
    -   **Standard Color Configuration**: Low-emphasis color scheme to focus attention on body content. Typically uses `--md-sys-color-surface-container` (or `surface-container-high`/`surface-container-highest`) for container background.
    -   **Vibrant Color Configuration**: High-emphasis color scheme that draws attention to controls, often indicating a temporary change in page behavior (such as entering edit mode). Uses primary/secondary container roles (e.g. `--md-sys-color-primary-container` or customized roles).
    -   **Floating Toolbar Elevation**: Floating toolbars must have an elevation shadow by default, utilizing `--md-sys-elevation-level2` (3px) or higher. Elevation can be removed (`--md-sys-elevation-level0`) if the content beneath the toolbar is already highly visually distinct.
-   **Layout Conflicts & Mutually Exclusive Navigation**:
    -   **Mandatory Rule**: Products must **never** show a bottom toolbar and a bottom navigation bar at the same time on the same screen. Show the navigation bar on primary top-level pages, and toolbars on subsequent pages requiring local actions.
-   **Scroll & Adaptive Behaviors**:
    -   On page scroll, toolbars can animate off-screen, remain fixed, or collapse dynamically into a single primary action FAB (fully supported on Jetpack Compose).
    -   *iOS Platform*: iOS docked and floating toolbars receive Expressive liquid glass styling updates (applying glass effects, where the content beneath the toolbar remains partially visible) while maintaining identical 64dp height and padding conventions.

### Critical MD3 Violations to Flag

-   **Simultaneous Toolbar and Nav Bar**: Presenting both a bottom action
    toolbar and a bottom navigation bar simultaneously on a single screen
    layout.
-   **Rounded Corners on Docked Toolbars**: Applying rounded corners to a
    full-width docked toolbar container (which misleadingly implies the
    container expands or floats).
-   **Square Buttons in Floating Toolbars**: Using square icon buttons or sharp-cornered controls inside a fully-rounded floating toolbar container.
-   **Undersized Touch Targets**: Shrinking toolbar icon buttons below the
    mandatory 48x48dp minimum accessible size (`--md-sys-measurement-space600`).
-   **Missing Elevation on Floating Toolbars**: Failing to apply elevation (`--md-sys-elevation-level2` or higher) to a floating toolbar when the underlying content is not visually separated.
