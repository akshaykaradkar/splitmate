# Sliders - Universal AI Detection Guide & Elements GM3 Specifications

This reference provides universal visual heuristics for detecting Sliders (analog value selection controls) across any design system or platform, followed by Elements GM3 specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Slider** is an interactive analog control that allows users to make selections from a continuous or discrete range of values (e.g., adjusting volume, brightness, or pricing filters). Agents must detect sliders based on their linear track division, draggable handle anchors, and range behaviors, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Linear Track with State Split**: Look for a prominent elongated track (horizontal or vertical) divided into two contrasting visual segments: an active track fill (representing the selected range or intensity) and an inactive background track (representing the remaining unselected range).
-   **Draggable Handle (Thumb)**: A distinct vertical bar, circle, or pill shape positioned directly on the track at the exact boundary where the active fill ends. Upon interaction, the handle frequently shrinks in width or alters its shape geometry.
-   **Range Selection (Dual Handles)**: Look for the presence of *two* handles on a single track, which defines a **Range Slider** used for selecting minimum and maximum bounding values.
-   **Discrete Stops (Ticks/Dots)**: Look for small dots, ticks, or lines spaced evenly across the track length, indicating predetermined snapping points (Stops/Discrete variant).
-   **Track & Value Media**: Larger slider variants frequently embed a descriptive icon directly inside the active track bar itself (e.g., a speaker icon). Floating value indicators or tooltips frequently appear above the handle during active drag interactions.

--------------------------------------------------------------------------------

## Part 2: Elements GM3 Specifications & Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate detected sliders against the following strict standards:

### Elements GM3 Component Variants & Sizing

Elements GM3 establishes specific sizing tiers and structural configurations:

1.  **Slider Variants**:
    -   **Standard (Continuous)**: Selects a single value starting from zero or the track beginning.
    -   **Centered Slider**: Zero or default value is anchored in the middle of the track, allowing selection across positive and negative ranges.
    -   **Range Slider**: Features two handles to bound minimum and maximum values (primarily horizontal).
    -   **Stops (Discrete)**: Snaps to predetermined step values indicated by track tick marks.
2.  **Dimensions by Size**:
    -   `Extra Small (XS)`: Track Height 16dp; Handle Height 44dp; Handle Width 4dp; Track Corner Radius 8dp (Existing default).
    -   `Small (S)`: Track Height 24dp; Handle Height 44dp; Handle Width 4dp; Track Corner Radius 8dp.
    -   `Medium (M)`: Track Height 40dp; Handle Height 52dp; Handle Width 4dp; Track Corner Radius 12dp; Inset Icon 24dp.
    -   `Large (L)`: Track Height 56dp; Handle Height 68dp; Handle Width 4dp; Track Corner Radius 16dp; Inset Icon 24dp.
    -   `Extra Large (XL)`: Track Height 96dp; Handle Height 108dp; Handle Width 4dp; Track Corner Radius 28dp; Inset Icon 32dp.
3.  **Orientation & Inset Icons**:
    -   Supports both horizontal (default) and vertical orientations.
    -   *Inset Icons*: Optional icons inside the track are supported on M, L, XL standard sliders. If track space compresses, the icon shifts dynamically to the inactive track.
4.  **Color tokens**:
    -   Active track uses the primary color role (mapped via `--cee3-sys-color-extended-blue` or standard primary tokens).
    -   Inactive track uses outline or variant surface colors (mapped via `--cee3-sys-color-extended-grey-outline` or standard surface/outline tokens).

### Elements GM3 Behavioral & Contrast Rules

-   **Immediate Effect**: Slider value adjustments must take effect immediately without requiring a separate "Save" or "Submit" confirmation action.
-   **Contrast Anchoring**: The trailing end of the inactive track must be anchored visually to ensure at least a 3:1 contrast ratio against the background surface, especially over low-contrast surfaces.

### Critical Elements GM3 Violations to Flag

-   **Unanchored Low Contrast Tracks**: Presenting a thin inactive track over a low-contrast background without a visual end anchor, failing WCAG contrast minimums.
-   **Delayed Value Application**: Requiring users to click a separate submit button for slider adjustments to take effect in standalone settings.
