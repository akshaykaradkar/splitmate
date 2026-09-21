# Segmented Buttons - Universal AI Detection Guide & Android Motion Specifications

This reference provides universal visual heuristics for detecting Segmented
Buttons (fused multi-segment selection controls) across any design system or
platform, followed by Android Motion design system specifications for compliance
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
    The outer container typically features fully rounded corners (e.g., forming
    an outer pill shape at rest).
-   **Internal Vertical Dividers**: Look for thin vertical dividing lines (e.g.,
    1dp width strokes) separating the individual choice segments within the
    shared outer boundary.
-   **Core Anatomy & Content Elements**:
    -   *Segments*: Individual clickable zones housing centered label text,
        icons, or both.
    -   *Labels*: Short, succinct text formatted in sentence case.
-   **Active State Contrast**: Look for clear visual contrast highlighting the
    currently selected segment(s). Selected segments feature a prominent
    background color fill and often display a leading checkmark icon (which may
    replace the default unselected icon).

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion design system adherence audit, evaluate detected
segmented buttons against the following strict standards derived from Android Motion
guidelines:

### Android Motion Component Specifications & Sizing

-   **Expressive Deprecation & Replacement**:
    -   **Mandatory Guidance**: Segmented buttons are no longer recommended in
        the Android Motion design system. Products must use the updated
        **Connected button group** instead (which separates buttons with
        consistent 2dp padding while updating the visual design).
-   **Legacy Dimensions & Tokens**:
    -   **Container Height**: 40dp standard height. (In denser UIs, density
        modifiers apply exclusively to height).
    -   **Container Width**: Dynamic based on label widths. Segment width equals
        container width divided by total segments (e.g., 1/3 width each for a
        3-segment group).
    -   **Outer Shape corners**: Outer container corners are fully rounded using `--droid-sys-shape-corner-full`.
    -   **Outline & Divider Width**: 1dp border stroke framing the container and
        dividing segments, utilizing `--droid-sys-color-outline` (light-dark(`#747775`, `#8e918f`)).
    -   **Paddings & Targets**: Minimum 12dp left/right padding per segment; 8dp
        between internal elements. Minimum accessible touch target is 48x48dp.
-   **Colors & Active/Inactive State Styling**:
    -   **Active / Selected Segment**:
        -   *Container Color*: `--droid-sys-color-secondary-container` (light-dark(`#c2e7ff`, `#004a77`)) or `--droid-sys-color-primary-container` (light-dark(`#d3e3fd`, `#0842a0`)).
        -   *Content Color*: `--droid-sys-color-on-secondary-container` (light-dark(`#001d35`, `#c2e7ff`)) or `--droid-sys-color-on-primary-container` (light-dark(`#041e49`, `#d3e3fd`)).
    -   **Inactive Segment**:
        -   *Container Color*: Transparent background.
        -   *Content Color*: `--droid-sys-color-on-surface-variant` (light-dark(`#444746`, `#c4c7c5`)) or `--droid-sys-color-on-surface` (light-dark(`#1f1f1f`, `#e3e3e3`)).
-   **Variants & Behavioral Rules**:
    -   **Single-Select**: Only one segment can be active at a time (acting with
        `Radio button` mutually exclusive semantics).
    -   **Multi-Select**: Zero or more segments can be active concurrently
        (acting with `Checkbox` multi-select semantics).

### Critical Android Motion Violations to Flag

-   **Legacy Segmented Button Usage**: Flagging baseline segmented buttons
    during an audit and recommending the updated `Connected button group` instead.
-   **Excessive Segments**: Designing a segmented button container with more
    than 5 segments, overcrowding the layout.
-   **Undersized Touch Targets**: Shrinking segment touch targets below the
    mandatory 48x48dp minimum accessible size.
