# Dividers - Universal AI Detection Guide & Android Motion Specifications

This reference provides universal visual heuristics for detecting Dividers (thin
separating lines used to group content or establish hierarchy) across any design
system or platform, followed by Android Motion specifications for
compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Divider** is a thin visual line or rule used to separate unrelated content,
group related items within lists, or establish structural hierarchy across a UI.
Agents must detect dividers based on line thickness, orientation, and layout
margins, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Line Thickness & Geometry**: Look for continuous, unbroken lines with a
    fine visual thickness of approximately 1dp (or 1px).
-   **Orientation & Alignment**: Most dividers are horizontal lines spanning
    across containers or lists. Vertical dividers are less common but appear in
    wide desktop/tablet layouts to separate side-by-side content panes or
    toolbar groupings.
-   **Spacing & Layout Margins**:
    -   *Full-Width*: Spans 100% of the parent container's width, separating
        distinct major sections or interactive vs. non-interactive areas.
    -   *Inset*: Features a left indent (e.g., aligning with text margins while
        bypassing leading avatars/icons), separating related items within a
        continuous list.
    -   *Middle-Inset*: Features indents on both the left and right sides.
-   **Visual Contrast**: Dividers act as subtle decorative boundaries. They
    exhibit low visual contrast relative to the background surface, ensuring
    they do not overpower adjacent text or interactive components.

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion adherence audit, evaluate detected
dividers against the following strict standards derived from Android Motion
guidelines:

### Android Motion Divider Specifications & Measurements

-   **Dimensions & Paddings**:
    -   **Thickness**: Strictly 1dp height (or width for vertical dividers).
    -   **Full-Width Divider**: 100% container width (0dp left/right margins).
    -   **Inset Divider**: 16dp left margin, 0dp right margin (aligning with
        standard 16dp list item text padding).
    -   **Middle-Inset Divider**: 16dp left margin, 16dp right margin.
    -   **Vertical Spacing**: 4dp space between divider and supporting text; 8dp
        bottom margin.
-   **Contextual Usage Rules**:
    -   *Full-Width*: Used to divide larger sections of unrelated content or
        separate sticky headers/footers from scrolling content.
    -   *Inset*: Used within lists to separate related content items (e.g.,
        separating email threads or contact rows).
-   **Elevation & Shadows**:
    -   Must sit completely flat with no drop shadow, mapped to `--droid-sys-elevation-level0`.
-   **Color & Accessibility Tokens**:
    -   Dividers are classified as decorative elements; therefore, they do not have mandatory WCAG contrast minimums.
    -   They utilize dynamic subtle color tokens, specifically `--droid-sys-color-outline` (light: `#747775`, dark: `#8e918f`) or `--droid-sys-color-surface-variant` (light: `#e1e3e1`, dark: `#444746`) to maintain an elegant, low-contrast appearance.

### Critical Android Motion Violations to Flag

-   **Excessive Line Thickness**: Using thick lines (>1dp/1px) for standard
    content dividers, creating heavy, distracting visual barriers.
-   **High Contrast Lines**: Rendering dividers in stark, bold, or high-contrast
    colors (like pure black on white) that disrupt the page's visual hierarchy.
-   **Improper Inset Margins**: Misaligning inset dividers with the established
    text padding of adjacent list items.
-   **Floating Shadow/Elevation**: Applying any elevation level above `--droid-sys-elevation-level0` or rendering drop shadows on dividers.
