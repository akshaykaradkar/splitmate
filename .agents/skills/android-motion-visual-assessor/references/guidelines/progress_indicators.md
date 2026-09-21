# Progress & Loading Indicators - Android Motion Design System Guidelines & Specifications

This reference provides universal visual heuristics for detecting Progress
Indicators (linear bars or circular spinning arcs) and Loading Indicators
(shape-morphing sequences) communicating ongoing process states across any
application, followed by Android Motion Design System specifications for
compliance auditing.

## Part 1: Universal AI Detection Heuristics

**Progress indicators** and **Loading indicators** are dynamic visual components
used to communicate the real-time status of an ongoing process (e.g., loading an
app, submitting a form, or fetching data). Unlike static icons, they capture
user attention through motion, active track fills, or shape morphing. Agents
must detect these indicators based on their geometry, track division, morphing
sequences, and contextual placement, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Linear Indicators (Horizontal Bars)**: Look for a distinct horizontal bar
    split into two contrasting visual segments: an active filled track
    (representing progress made) and an inactive background track (representing
    remaining progress).
    -   *Stop Marker*: A tiny circular dot (e.g., 4dp) marking the ending tip of
        the active colored segment in determinate states.
    -   *Expressive/Wavy Style*: An undulating or wavy line rather than a
        straight rectangular bar.
    -   *Discrete/Dotted Status Tracks*: Instead of a continuous solid bar, a
        progress or status indicator can be represented by a horizontal row of
        discrete elements (such as dots, blocks, or segments) styled in a color
        gradient (e.g., blue to red) or solid color, paired with a visual marker
        (like an elongated dot, arrow, or pin) indicating the current value
        along the scale. You MUST map these custom status tracks to **`Progress
        indicator`**.
    -   *Placement*: Typically anchored flush to the extreme top edge of a card,
        dialog, sheet, or page container.
    -   *Media/Playback Integration*: In media players or content cards, a thin
        horizontal line (often blue, red, or brand-colored) is frequently
        positioned at the bottom of a card to show playback progress.
    -   *Button Integration*: Can be embedded directly inside interactive
        buttons (e.g., a thin horizontal progress line at the bottom of a button
        to show associated progress, such as media playback status).
-   **Circular Indicators (Spinning Rings/Arcs)**: Look for a thin circular arc,
    spinning ring, or expanding circular track.
    -   *Button Integration*: Frequently embedded directly inside interactive
        buttons (often replacing the leading icon or text label); typically
        rendered as a monochrome arc matching the text color without an inactive
        background track.
    -   *Placement*: Centered prominently within empty layout spaces, cards, or
        surfaces where content is actively loading.
-   **Loading Indicators (Morphing Shape Sequences)**: Look for a looping,
    dynamic animation sequence displaying morphing geometric shapes (e.g.,
    cycling through distinct polygon or curved bounding contours).
    -   *Pull-to-Refresh*: Commonly positioned at the very top of a scrollable
        list or grid, frequently housed inside an elevated circular or
        pill-shaped background container when overlapping content.
-   **Determinate vs. Indeterminate Signatures**: Look for two distinct
    behavioral states:
    -   *Determinate (Linear/Circular)*: The active colored track grows steadily
        in one direction, reflecting a known, measurable completion percentage.
    -   *Indeterminate (Linear/Circular/Loading)*: The active colored segment
        repeatedly grows, shrinks, oscillates, or morphs shapes, communicating
        an unknown wait time.

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion Design System adherence audit, evaluate detected
progress and loading indicators against the following strict standards:

### Android Motion Component Specifications & Sizing

1.  **Linear Progress Indicator Specs**:
    -   **Minimum Width**: Must be at least **40dp** in width.
    -   **Stop Indicator**: Standard determinate tracks require a **4dp circular end stop indicator** at the trailing tip of the active colored bar to ensure proper non-text contrast (required if track-to-background contrast is under 3:1).
    -   **RTL Mirroring**: The horizontal fill direction must mirror (right-to-left) for RTL languages.
2.  **Circular Progress Indicator Specs**:
    -   **Size Range**: Variable diameter ranging from **24dp** (embedded in buttons) up to **240dp** (for prominent loading states on expanded views).
    -   **RTL Mirroring**: Does not require horizontal mirroring.
3.  **Loading Indicator Specs (Shape Morphing)**:
    -   **Anatomy**: Composed of a looping shape-morphing sequence cycling through unique geometric shapes from the Android Motion shape library.
    -   **Size Range**: Flexible sizing from **24dp** to **240dp** (default is **48dp**). The ratio between the outer container and the active morphing shapes remains fixed.
    -   **Color Roles**: Uncontained indicators (sitting on a surface) use `--droid-sys-color-primary` (light-dark(#0b57d0, #a8c7fa)). Contained indicators (used for pull-to-refresh or overlaying content) use `--droid-sys-color-on-primary-container` (light-dark(#041e49, #d3e3fd)) on a container fill of `--droid-sys-color-primary-container` (light-dark(#d3e3fd, #0842a0)).
4.  **Visual Track Shapes (Expressive Configuration)**:
    -   **Flat (Default)**: Standard linear bar or circular arc with rounded ends mapping to `--droid-sys-shape-corner-full` (max(50cqw, 50cqh)).
    -   **Wavy (Android Motion Expressive)**: The active track renders as a sine wave with configurable amplitude and wavelength, conveying elastic brand dynamics.
5.  **Duration Rules & Process Hierarchy**:
    -   *Instant (<200ms)*: No indicator should be shown.
    -   *Loading Indicator (200ms to 5s)*: Reserved for short processes. Uses the shape-morphing sequence with snappy take-offs to mitigate perceived latency. Must **not** transition directly into a determinate progress bar.
    -   *Progress Indicator (>5s)*: Required for long-running background tasks. Users should be able to navigate away while the task completes.

### Android Motion Accessibility, Contrast & Animation Rules

-   **Color Contrast Minimums**: Active tracks, circular arcs, or morphing loading shapes must maintain at least a **3:1 contrast ratio** against their surrounding container or surface background.
-   **Track Ends & Rounding**:
    -   Linear progress tracks and active filled indicators must use cleanly rounded ends matching `--droid-sys-shape-corner-full` to represent fluid motion.
-   **No Drop Shadows**:
    -   Indicators must remain flat without drop shadows (unless a contained pull-to-refresh container is elevated, in which case it uses `--droid-sys-elevation-level1` or `--droid-sys-elevation-level2`).
-   **Motion Schemes**:
    -   Indeterminate animations must feel organic and fluid, utilizing the Android Motion **Expressive Motion Scheme** (which leverages elastic spring physics) over stiff, linear, or legacy cubic-bezier curves.
-   **Non-Gesture Refresh Alternative**:
    -   Pull-to-refresh gestures must have an accessible non-gesture alternative (like a "Refresh" menu action or standard button).

### Critical Android Motion Violations to Flag

-   **Missing Stop Indicator**: Presenting a linear determinate progress bar with low contrast (<3:1) without the required 4dp end stop marker.
-   **Conflating Loading and Progress Indicators**: Transitioning a short-term shape-morphing `Loading indicator` directly into a long-term `Progress indicator` during a single process flow.
-   **Missing Non-Gesture Refresh**: Implementing pull-to-refresh without providing an accessible text button or menu action alternative.
-   **Inconsistent Process Theming**: Using a circular progress indicator on one screen but a linear indicator on another for the same functional process (e.g. refreshing).
-   **Sharp/Blocky Track Ends**: Rendering a progress track or active indicator with sharp 0dp corners instead of the mandatory rounded ends.
