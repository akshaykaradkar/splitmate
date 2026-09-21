# Segmented Buttons - Universal AI Detection Guide & GM3 Specifications

This reference provides universal visual heuristics for detecting Segmented
Buttons (fused multi-segment selection controls) across any design system or
platform, followed by Google Material 3 (GM3) specifications for compliance
auditing.

## Part 1: Universal AI Detection Heuristics

A **Segmented button** is a linear selection control composed of multiple fused
segments sharing common dividing borders. They allow users to select options,
switch views, or sort elements. Agents must detect segmented buttons based on
their fused container grouping, thin internal dividers, and active state
contrast, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Fused Grouping & Outer Boundaries**: Look for a linear row of 2 to 5
    adjacent button segments fused together into a single cohesive container.
    The outer container features fully rounded corners (forming an outer pill shape).
-   **Internal Vertical Dividers**: Look for thin vertical dividing lines (1dp width strokes) separating the individual choice segments within the shared outer boundary.
-   **Core Anatomy & Content Elements**:
    -   *Segments*: Individual clickable zones housing centered label text,
        icons, or both.
    -   *Labels*: Short, succinct text formatted in sentence case.
-   **Active State Contrast**: Look for clear visual contrast highlighting the
    currently selected segment(s). Selected segments feature a prominent
    background color fill and display a leading checkmark icon (which replaces the default unselected leading icon).

--------------------------------------------------------------------------------

## Part 2: GM3 Specifications & Compliance Auditing

When conducting a Google Material 3 (GM3) adherence audit, evaluate detected
segmented buttons against the following strict standards derived from Material
guidelines:

### GM3 Component Specifications & Sizing

-   **Expressive Deprecation & Replacement**:
    -   **Mandatory Guidance**: Segmented buttons are no longer recommended in the Google Material 3 Expressive update. Audit agents must flag standard segmented buttons and suggest transitioning to the updated **Connected button group** (which separates buttons with consistent 2dp padding while updating the visual design for improved flexibility).
-   **GM3 Legacy Sizing & Tokens**:
    -   **Container Height**: Strictly 40dp standard height. In denser UIs, density modifiers can shrink height, but must only be applied to height.
    -   **Container Width**: Dynamic based on label widths. Segment width is equal to the total container width divided by the number of segments (e.g., 1/3 width each for a 3-segment group).
    -   **Outline & Divider Width**: 1dp border stroke framing the container and dividing segments.
    -   **Paddings & Targets**: Minimum 12dp left/right padding per segment; 8dp padding between internal elements (like icon and label). Minimum accessible touch target is 48x48dp.
    -   **Typography**: Label text must use sentence case (capitalizing only the first letter and proper nouns, e.g., "Distance", not "DISTANCE" or "DISTANCE "). Typically maps to `--md-sys-typescale-label-large` or `--md-sys-typescale-label-medium`.
    -   **Shape**: Outer container uses fully rounded pill corners (`--md-sys-shape-corner-full`).
-   **Color Roles & Design Tokens**:
    -   **Outline & Divider**: Uses `--md-sys-color-outline` or `--md-sys-color-outline-variant`. To maintain legibility, the outline must achieve at least a 3:1 contrast ratio against the background surface.
    -   **Selected Segment Fill**: Uses a prominent container role (like `--md-sys-color-secondary-container` or `--md-sys-color-primary-container`).
    -   **Unselected Segment Fill**: Typically transparent, revealing the underlying surface (e.g., `--md-sys-color-surface`).
    -   **Label & Icons**: Selected text/icons use `--md-sys-color-on-secondary-container` or `--md-sys-color-on-primary-container`. Unselected text/icons use `--md-sys-color-on-surface-variant` or `--md-sys-color-on-surface`.
-   **Variants & Behavioral Rules**:
    -   **Single-Select**: Only one segment can be active at a time, behaving with `Radio button` mutually exclusive semantics (labeled as `Radiogroup` for screen readers).
    -   **Multi-Select**: Zero or more segments can be active concurrently, behaving with `Checkbox` multi-select semantics (labeled as `Checkbox` for screen readers).
    -   **Selection Indicators**: To maximize accessibility, selected segments must use BOTH a visual checkmark icon AND a background color fill to show selection. Color must never be the sole indicator.
-   **iOS Platform Compatibility**:
    -   Segmented buttons are not supported on iOS, and are no longer recommended on Android. On iOS, developers must use Apple's segmented controls, styled for GM3, which support liquid glass effects in the iOS 26 Liquid Glass update. Use button groups for more than 5 items.

### Critical GM3 Violations to Flag

-   **Segmented Button Usage (Adherence Warning)**: Flagging standard segmented buttons during an audit and recommending the GM3 Expressive `Connected button group` instead.
-   **Excessive Segments**: Designing a segmented button container with more than 5 segments, overcrowding the layout. (For more than 5 items, use `Chips` or `Connected button groups`).
-   **Undersized Touch Targets**: Forcing dense segmented button layouts by default that reduce touch targets below the mandatory 48x48dp minimum accessible size.
-   **Missing Checkmark Icon on Selection**: Relying solely on background color changes to indicate selection, violating color contrast accessibility guidelines.
-   **Improper Casing**: Using legacy GM2 ALL CAPS for label text instead of sentence case.
-   **Misaligned Content**: Text labels or icons that are not perfectly centered within their individual segment boundaries.
