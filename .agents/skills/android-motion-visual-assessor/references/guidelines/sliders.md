# Sliders - Android Motion Design System Guidelines & Specifications

This reference provides universal visual heuristics for detecting Sliders
(analog value selection controls) across any application, followed by Android
Motion Design System specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Slider** is an interactive analog control that allows users to make
selections from a continuous or discrete range of values (e.g., adjusting
volume, brightness, or pricing filters). Agents must detect sliders based on
their linear track division, draggable handle anchors, and range behaviors,
regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Linear Track with State Split**: Look for a prominent elongated track
    (horizontal or vertical) divided into two contrasting visual segments: an
    active track fill (representing the selected range or intensity) and an
    inactive background track (representing the remaining unselected range).
-   **Draggable Handle (Thumb)**: A distinct vertical bar, circle, or pill shape
    positioned directly on the track at the exact boundary where the active fill
    ends. Upon interaction, the handle frequently shrinks in width or alters its
    shape geometry.
-   **Range Selection (Dual Handles)**: Look for the presence of *two* handles
    on a single track, which defines a **Range Slider** used for selecting
    minimum and maximum bounding values.
-   **Discrete Stops (Ticks/Dots)**: Look for small dots, ticks, or lines spaced
    evenly across the track length, indicating predetermined snapping points
    (Stops/Discrete variant).
-   **Track & Value Media**: Larger slider variants frequently embed a
    descriptive icon directly inside the active track bar itself (e.g., a
    speaker icon). Floating value indicators or tooltips frequently appear above
    the handle during active drag interactions.

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion Design System adherence audit, evaluate detected
sliders against the following strict standards:

### Android Motion Component Configurations & Sizing

The Android Motion Design System establishes specific sizing tiers, rounded geometries, and structural configurations for sliders:

1.  **Slider Variants**:
    -   **Standard (Continuous)**: Selects a single value starting from zero or
        the track beginning.
    -   **Centered Slider**: Zero or default value is anchored in the middle of
        the track, allowing selection across positive and negative ranges.
    -   **Range Slider**: Features two handles to bound minimum and maximum
        values (primarily horizontal).
    -   **Stops (Discrete)**: Snaps to predetermined step values indicated by
        track tick marks or stops.
2.  **Dimensions by Size (Android Motion Presets)**:
    -   `Extra Small (XS)`: Track Height 16dp; Handle Height 44dp; Handle Width 4dp; Track Corner Radius uses `--droid-sys-shape-corner-small` (8px).
    -   `Small (S)`: Track Height 24dp; Handle Height 44dp; Handle Width 4dp; Track Corner Radius uses `--droid-sys-shape-corner-small` (8px).
    -   `Medium (M)`: Track Height 40dp; Handle Height 52dp; Handle Width 4dp; Track Corner Radius uses `--droid-sys-shape-corner-medium` (12px); Inset Icon 24dp.
    -   `Large (L)`: Track Height 56dp; Handle Height 68dp; Handle Width 4dp; Track Corner Radius uses `--droid-sys-shape-corner-large` (16px); Inset Icon 24dp.
    -   `Extra Large (XL)`: Track Height 96dp; Handle Height 108dp; Handle Width 4dp; Track Corner Radius uses `--droid-sys-shape-corner-extra-large` (28px); Inset Icon 32dp.
3.  **Orientation & Inset Icons**:
    -   Supports both horizontal (default) and vertical orientations.
    -   *Inset Icons*: Optional icons inside the track are supported on M, L, XL
        standard sliders. If track space compresses, the icon shifts dynamically
        to the inactive track.

### Android Motion Visual, Color & Typographic Rules

-   **Color Mappings**:
    -   *Active Track Fill*: Uses `--droid-sys-color-primary` (light-dark(#0b57d0, #a8c7fa)) for standard and positive ranges. For centered slider negative ranges, `--droid-sys-color-tertiary` (light-dark(#146c2e, #6dd58c)) may be used to emphasize the visual contrast.
    -   *Inactive Track Background*: Uses `--droid-sys-color-surface-variant` (light-dark(#e1e3e1, #444746)) to create a subtle boundary.
    -   *Handle (Thumb)*: Uses `--droid-sys-color-on-primary` (light-dark(#ffffff, #062e6f)) or `--droid-sys-color-primary` depending on the active state and focus layers.
    -   *Stops/Ticks*: Must use `--droid-sys-color-outline` (light-dark(#747775, #8e918f)) to remain visible on the inactive segment.
    -   *Floating Value Indicator / Tooltip*: Uses `--droid-sys-color-inverse-surface` (light-dark(#303030, #e3e3e3)) for the background and `--droid-sys-color-inverse-on-surface` (light-dark(#f2f2f2, #303030)) for the label text.
-   **Typography**:
    -   The floating value labels must use the bold brand typeface: `Google Sans`. Specifically, it must map to `--droid-sys-typescale-label-medium` or `--droid-sys-typescale-label-small` (500 0.7rem, 'Google Sans Text') with tracking from `--droid-sys-typescale-label-small-tracking`.

### Android Motion Dynamics & Behavioral Rules

-   **Active Interaction & Drag Springs**:
    -   The handle must shrink in width (e.g., from a vertical pill to a thinner line) and the active track must expand vertically or morph shape when actively pressed and dragged.
    -   This state transition must feel fluid and elastic. It should utilize the **Expressive Fast Effects Spring** (or a conversion of `--droid-sys-motion-easing-standard` over `--droid-sys-motion-duration-150` for web/iOS fallbacks) to react instantly to touch engagement.
-   **Gesture Snapping**:
    -   For stops/discrete configurations, when the user releases their finger, the handle must snap smoothly to the nearest stop indicator utilizing a spring-based physics model (e.g., Expressive Spatial Spring or a curve resembling `--droid-sys-motion-easing-standard` over `--droid-sys-motion-duration-200` to `--droid-sys-motion-duration-300`).
-   **Immediate Effect**:
    -   Slider value adjustments must take effect immediately in the underlying app state without requiring an extra "Save" or "Apply" action button.
-   **Contrast Anchoring**:
    -   The trailing end of the inactive track must be visually anchored (e.g., with a dot stop or bounding container) to guarantee at least a 3:1 contrast ratio against the background surface.

### Critical Android Motion Violations to Flag

-   **Unanchored Low Contrast Tracks**: Presenting a thin inactive track over a low-contrast background without a visual end anchor, violating basic contrast requirements.
-   **Delayed Value Application**: Requiring users to click a separate submit button for slider adjustments to take effect in standalone settings.
-   **Lacking Interaction Feedback**: A slider handle and track that remain completely static in thickness/width during active dragging, failing to provide interactive feedback.
-   **Incorrect Easing Curve**: Programmatic movements or slider snaps that use a stiff, linear transition rather than the natural `--droid-sys-motion-easing-standard` curve.
