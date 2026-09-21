# Segmented Buttons - Universal AI Detection Guide & Elements GM3 Specifications

This reference provides universal visual heuristics for detecting Segmented Buttons (fused multi-segment selection controls) across any design system or platform, followed by Elements GM3 specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Segmented button** is a linear selection control composed of multiple fused segments sharing common dividing borders. They allow users to select options, switch views, or sort elements. Agents must detect segmented buttons based on their fused container grouping, thin internal dividers, and active state contrast, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Fused Grouping & Outer Boundaries**: Look for a linear row of 2 to 5 adjacent button segments fused together into a single cohesive container. The outer container features fully rounded pill-shaped corners (e.g., 20dp corner radius at rest).
-   **Internal Vertical Dividers**: Look for thin vertical dividing lines (e.g., 1dp width strokes using `--md-sys-color-outline-variant` or standard outline-variant) separating the individual choice segments within the shared outer boundary.
-   **Core Anatomy & Content Elements**:
    -   *Segments*: Individual clickable zones housing centered label text, icons, or both.
    -   *Labels*: Short, succinct text formatted in sentence case.
-   **Active State Contrast**: Look for clear visual contrast highlighting the currently selected segment(s). Selected segments feature a prominent background color fill (e.g., primary container or secondary container) and often display a leading checkmark icon.

--------------------------------------------------------------------------------

## Part 2: Elements GM3 Specifications & Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate detected segmented buttons against the following strict standards:

### Elements GM3 Component Specifications & Sizing

-   **Modern Button Group Alternatives**:
    -   **Standard Guidance**: While legacy segmented buttons are supported, modern UIs under Elements GM3 often utilize **Connected button groups** or dedicated chip groupings for selections to provide consistent layout flexibility.
-   **Dimensions & Tokens**:
    -   **Container Height**: 40dp standard height, matching the default Elements GM3 button height.
    -   **Corner Radius**: Outer container has fully rounded corners with a 20dp radius (sys.shape.corner.full).
    -   **Typography**: Label text must use the Elements GM3 `Label Large` style (`--cee3-typescale-label-large-link` or `500 0.9rem/1.3rem 'Google Sans Text'`) written in sentence case.
    -   **Outline & Divider Width**: 1dp border stroke framing the container and dividing segments (mapped to `--md-sys-color-outline` or `--cee3-sys-color-extended-grey-outline`).
    -   **Paddings & Targets**: Minimum 12dp left/right padding per segment; 8dp between internal elements (icons and text). Minimum accessible interaction target is 48x48dp, even if visually narrower (density should not compromise interaction target sizes).
-   **Behavioral Rules**:
    -   **Single-Select**: Only one segment can be active at a time (Radio button mutually exclusive semantics).
    -   **Multi-Select**: Zero or more segments can be active concurrently (Checkbox multi-select semantics).

### Critical Elements GM3 Violations to Flag

-   **Excessive Segments**: Designing a segmented button container with more than 5 segments, overcrowding the layout and violating usability guidelines.
-   **Undersized Interaction Targets**: Shrinking segment touch targets below the mandatory 48x48dp minimum accessible size (violating density rules).
-   **Drop Shadows**: Applying drop shadows or elevation shadows to the segmented button container; segmented buttons must stay flat (elevation 0) without a drop shadow.
-   **Incorrect Typography/Casing**: Using ALL CAPS for segment labels instead of sentence case, or using a non-standard font instead of the Google Sans Text baseline font.
