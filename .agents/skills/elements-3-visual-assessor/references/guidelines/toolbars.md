# Toolbars - Universal AI Detection Guide & Elements GM3 Specifications

This reference provides universal visual heuristics for detecting Toolbars (docked or floating bottom action containers) across any design system or platform, followed by Elements GM3 specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Toolbar** is a dedicated action container positioned at the bottom (or sides) of a screen, designed to display frequently used contextual actions relevant to the current page or active mode (e.g., formatting tools, editing controls, or item filters). Agents must detect toolbars based on bottom docking, floating pill geometry, and clustered action buttons, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Placement & Docking Signatures**: Look for prominent action containers anchored near the bottom of the viewport:
    -   **Docked Toolbar**: Anchored flush against the extreme bottom edge of the window, spanning 100% of the viewport width.
    -   **Floating Toolbar**: Floats directly above body content near the bottom (horizontal) or along the side edges (vertical).
-   **Containment Geometry & Boundaries**:
    -   *Docked*: A clean rectangular container with sharp, straight corners (no rounded corners by default) sitting flush with the bottom edge.
    -   *Floating*: A distinct pill-shaped container featuring fully rounded corners, exhibiting a prominent elevation drop shadow over underlying content.
-   **Clustered Action Elements**: Look for horizontal groupings of icon buttons (filled, tonal, standard) or form controls. Floating toolbars frequently sit immediately adjacent to a primary Floating Action Button (FAB) lockup.
-   **Active Mode Color Cues**: Detect two primary color schemes: *Standard* (low-emphasis surface fill focusing attention on body content) vs. *Vibrant* (high-emphasis primary/secondary fill drawing attention to controls, frequently indicating an active editing mode).
-   **Floating Navigation Toolbar (Non-Standard Usage)**: If a bottom bar is styled as a floating pill (not full-width, rounded corners, floats above content) but houses navigation destinations, it MUST be classified as a **`Toolbar`** based on its visual geometry, prioritizing visual structure over functional intent.

--------------------------------------------------------------------------------

## Part 2: Elements GM3 Specifications & Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate detected toolbars against the following strict standards derived from Elements GM3 guidelines:

### Elements GM3 Component Specifications & Sizing

-   **Dimensions & Sizing Tokens**:
    -   **Container Height**: Default toolbar container height is strictly **64dp**.
    -   **Touch Targets**: Every interactive element (e.g., icon buttons) inside the toolbar must meet the mandatory minimum **48x48dp touch target** requirement. No density should be applied that reduces the touch targets below 48x48dp.
    -   **Paddings & Spacing**: Minimum 16dp outer padding from window edges for docked toolbars. Grouped items maintain 32dp default spacing. Vertical floating toolbars maintain a minimum 24dp margin from window edges.
-   **Geometry & Shape Roles**:
    -   **Docked Toolbar**: Container must feature **straight corners** (0dp border-radius) when spanning 100% width. Rounded corners on a docked toolbar container are a violation.
    -   **Floating Toolbar**: Container must be pill-shaped with **fully rounded corners** (using `--cee3-sys-shape-corner-full` or equivalent) and must utilize a prominent elevation drop shadow to distinguish it from the background content.
-   **Color & Custom Properties**:
    -   Fills utilize surface container roles, such as `--cee3-sys-color-extended-grey-container` (standard, low-emphasis) or `--cee3-sys-color-extended-blue-container` (vibrant, active editing mode).
    -   Icons must be standard **18dp**, utilizing `--md-icon-font` (`'Google Symbols'`).
-   **Layout Conflicts & Mutually Exclusive Navigation**:
    -   **Mandatory Rule**: Products must **never** show a toolbar and a navigation bar at the same time at the bottom of a screen. Show the navigation bar on primary top-level pages, and toolbars on subsequent pages requiring local actions.

### Critical Elements GM3 Violations to Flag

-   **Simultaneous Bottom Toolbar and Navigation Bar**: Presenting both a bottom action toolbar and a bottom navigation bar simultaneously on a single screen layout.
-   **Rounded Corners on Docked Toolbars**: Applying rounded corners to a full-width docked toolbar container (which misleadingly implies the container expands or floats).
-   **Undersized Touch Targets**: Shrinking toolbar icon buttons below the mandatory 48x48dp minimum accessible size.
-   **Incorrect Height**: Constructing a toolbar container with a height other than the required 64dp.
