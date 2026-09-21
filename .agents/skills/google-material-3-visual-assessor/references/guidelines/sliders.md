# Sliders - Universal AI Detection Guide & MD3 Specifications

This reference provides universal visual heuristics for detecting Sliders (analog value selection controls) across any design system or platform, followed by Material Design 3 (MD3) specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Slider** is an interactive analog control that allows users to make selections from a continuous or discrete range of values (e.g., adjusting volume, brightness, or pricing filters). Agents must detect sliders based on their linear track division, draggable handle anchors, and range behaviors, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Linear Track with State Split**: Look for a prominent elongated track (horizontal or vertical) divided into two contrasting visual segments: an active track fill (representing the selected range or intensity) and an inactive background track (representing the remaining unselected range).
-   **Draggable Handle (Thumb)**: A distinct vertical bar, circle, or pill shape positioned directly on the track at the exact boundary where the active fill ends. Upon interaction, the handle frequently shrinks in width or alters its shape geometry.
-   **Range Selection (Dual Handles)**: Look for the presence of *two* handles on a single track, which defines a **Range Slider** used for selecting minimum and maximum bounding values.
-   **Discrete Stops (Ticks/Dots)**: Look for small dots, ticks, or lines spaced evenly across the track length, indicating predetermined snapping points (Stops/Discrete variant).
-   **Track & Value Media**: Larger slider variants frequently embed a descriptive icon directly inside the active track bar itself (e.g., a speaker icon). Floating value indicators or tooltips frequently appear above the handle during active drag interactions.

---

## Part 2: MD3 Specifications & Compliance Auditing

When conducting a Material Design 3 (MD3) adherence audit, evaluate detected sliders against the following strict standards derived from Material guidelines and token bindings:

### MD3 Component Variants & Sizing

Material Design 3 (Expressive Update) establishes specific sizing tiers and structural configurations:

1.  **Slider Variants**:
    -   **Standard**: Selects a single value starting from zero or the track beginning. (Renamed from "continuous" in the Expressive update).
    -   **Centered**: Zero or default value is anchored in the middle of the track, allowing selection across positive and negative ranges.
    -   **Range**: Features two handles to bound minimum and maximum values (must only be used horizontally).
    -   **Stops**: Snaps to predetermined step values indicated by track tick marks. (Renamed from "discrete" in the Expressive update).

2.  **Dimensions by Size (GM3 Expressive)**:
    -   **Extra Small (XS)** (Legacy Default):
        -   Track Height: 16dp
        -   Handle Height: 44dp
        -   Handle Width: 4dp
        -   Track Corner Radius: 8dp (`--md-sys-shape-corner-small`)
    -   **Small (S)**:
        -   Track Height: 24dp
        -   Handle Height: 44dp
        -   Handle Width: 4dp
        -   Track Corner Radius: 8dp (`--md-sys-shape-corner-small`)
    -   **Medium (M)**:
        -   Track Height: 40dp
        -   Handle Height: 52dp
        -   Handle Width: 4dp
        -   Track Corner Radius: 12dp (`--md-sys-shape-corner-medium`)
        -   Inset Icon Size: 24dp
    -   **Large (L)**:
        -   Track Height: 56dp
        -   Handle Height: 68dp
        -   Handle Width: 4dp
        -   Track Corner Radius: 16dp (`--md-sys-shape-corner-large`)
        -   Inset Icon Size: 24dp
    -   **Extra Large (XL)**:
        -   Track Height: 96dp
        -   Handle Height: 108dp
        -   Handle Width: 4dp
        -   Track Corner Radius: 28dp (`--md-sys-shape-corner-extra-large`)
        -   Inset Icon Size: 32dp

3.  **Orientation & Inset Icons**:
    -   Supports both horizontal (default) and vertical orientations.
    -   *Inset Icons*: Optional icons inside the track are supported on M, L, and XL standard sliders only. If track space compresses, the icon shifts dynamically to the inactive track. Inset icons are prohibited on centered or range sliders.

### MD3 Color & Token Mappings

-   **Active Track Fill**: `--md-sys-color-primary`
-   **Inactive Track**: `--md-sys-color-surface-variant` or `--md-sys-color-outline-variant` (or `--md-sys-color-surface-container-highest`)
-   **Handle (Thumb)**: `--md-sys-color-primary` (shrinks in width and changes shape when pressed)
-   **Stop Indicators (Ticks)**: `--md-sys-color-on-primary` (active stops) and `--md-sys-color-primary` (inactive stops)
-   **Inset Icon**: `--md-sys-color-on-primary`
-   **Value Indicator**: Features a background container with label text showing the value during active interaction.

### MD3 Behavioral & Contrast Rules

-   **Immediate Effect**: Slider value adjustments must take effect immediately without requiring a separate "Save" or confirmation action.
-   **Contrast Anchoring**: The trailing end of the inactive track must be anchored visually (e.g., via a dot/stop indicator, descriptive icon, or text label) to ensure at least a 3:1 contrast ratio against the background surface (`--md-sys-color-surface`).
-   **Interactions**: Select & drag, select jump (tap track to snap), keyboard arrow keys (Arrow keys increment/decrement by one step, Space + Arrows increment/decrement by larger intervals, Home/End jump to bounds). Focus lands directly on the handle.

### Critical MD3 Violations to Flag

-   **Unanchored Low Contrast Tracks**: Presenting a thin inactive track over a low-contrast background without a visual end anchor (such as a stop indicator), failing WCAG 3:1 non-text contrast minimums.
-   **Delayed Value Application**: Requiring users to click a separate submit button for slider adjustments to take effect.
-   **Prohibited Inset Icons**: Using inset icons on centered or range sliders, which creates visual ambiguity about the slider's starting point.
-   **Vertical Range Sliders**: Implementing range sliders (with two handles) vertically, as it causes high cognitive load.
-   **Improper Size Swapping**: Mismatching the heights of the active and inactive track segments.
