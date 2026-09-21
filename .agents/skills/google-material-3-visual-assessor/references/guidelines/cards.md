# Cards - Universal AI Detection Guide & MD3 Specifications

This reference provides universal visual heuristics for detecting Cards
(containment components displaying content and actions about a single subject)
across any design system or platform, followed by Material Design 3 (MD3)
specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Card** is a distinct structural container used to group related information,
media, and actions about a single subject or entity. They frequently serve as
entry points into deeper levels of detail or navigation. Agents must detect
cards based on their geometric containment and layout flow, regardless of design
system adherence.

### Key Visual & Geometric Heuristics

-   **Placement & Collections**: Cards appear as modular building blocks within
    page layouts. They are typically arranged in structured groupings such as
    multi-column grids, vertical lists, or horizontal scrolling carousels.
-   **Visual Boundaries & Containment**: Defined by a prominent outer bounding
    box that groups a cohesive set of content.
    -   *Container Boundaries*: Visually established by a solid background color
        contrasting with the underlying page surface, an outer border stroke, or
        an elevation drop shadow.
    -   *Corner Geometry*: Features distinct rounded corners on all four edges
        (e.g., medium rounded rectangles).
-   **Core Anatomy & Layout Flow**: Cards utilize a standard internal hierarchy,
    typically flowing vertically from top to bottom:
    -   *Media / Header (Optional)*: A leading full-bleed image, video preview,
        or author thumbnail/avatar lockup at the top.
    -   *Text Content*: A prominent title/headline, accompanied by an optional
        subhead (byline/location) and supporting body text summary.
    -   *Actions (Optional)*: An action bar positioned at the bottom containing
        interactive controls (e.g., text buttons, icon buttons, or an overflow
        menu).
-   **Mandatory Container Mapping**: Agents MUST bound and classify the outer
    bounding box as `Card`. Agents must not merely identify the internal child
    text, images, or buttons while omitting the parent card container.

-   **Card vs. Button Distinction**: Do not classify a component as a `Card` if
    it only serves as a single action trigger, even if it is large, rectangular,
    or has rounded corners. A Card must contain composite content (e.g. text
    descriptions, metrics, images) or represent a distinct content entity,
    whereas a Button is a single interactive target with a simple label and/or
    icon representing a single action.

--------------------------------------------------------------------------------

## Part 2: MD3 Specifications & Compliance Auditing

When conducting a Material Design 3 (MD3) adherence audit, evaluate detected
cards against the following strict standards derived from Material guidelines:

### MD3 Visual & Behavioral Rules

-   **Card Variants**:
    -   **Elevated Card**: Features a subtle drop shadow (`--md-sys-color-shadow` / `--md-sys-elevation-level1` or higher) over a surface-derived background color (`--md-sys-color-surface-container-low`). Used to provide clear separation from similar background surfaces.
    -   **Filled Card**: Features no drop shadow (elevation `--md-sys-elevation-level0`), utilizing a solid background fill color (`--md-sys-color-surface-container`) that contrasts with the surrounding surface. Used for lower visual emphasis.
    -   **Outlined Card**: Features no drop shadow (elevation `--md-sys-elevation-level0`), utilizing a visible thin border stroke (`--md-sys-color-outline-variant` or `--md-sys-color-outline`) around a transparent or surface-matching fill (`--md-sys-color-surface`). Used for clean, distinct boundary separation.
-   **Tokens & Measurements**:
    -   **Corner Radius**: MD3 specifies a standard **12px corner radius** (`--md-sys-shape-corner-medium`) for all card container variants.
    -   **Internal Padding**: Standard internal padding is 16px (`--md-sys-measurement-space200`) on the left and right.
    -   **Spacing in Collections**: Cards grouped in grids, vertical lists, or carousels must maintain a maximum spacing of 8px (`--md-sys-measurement-space100`) between adjacent cards.
-   **Anatomy & Hierarchy**: The card container is the only required element; all content blocks (text, media, dividers, actions) are optional slots.
-   **Accessibility & DOM Heuristics**:
    -   *Directly Actionable Cards*: Entire card acts as a single large touch target (`button` or `link` role). Must show focus rings on keyboard focus and ripples on click.
    -   *Non-Actionable Cards*: Act as pure structural containers (no a11y role). Can contain nested actionable elements (buttons, links, chips) which serve as distinct tab stops.

### Critical MD3 Violations to Flag

-   **Action Stacking (Nested Buttons in Actionable Cards)**: Placing independent interactive buttons or switches inside a card where the entire card surface is already directly actionable, violating DOM/accessibility interaction patterns.
-   **Unscrimmed Text over Images**: Layering text directly over background images without a translucent scrim (`--md-sys-color-scrim`) or bounding shape beneath the text, failing WCAG contrast requirements.
-   **Improper Corner Radii or Spacing**: Utilizing sharp 0px corners instead of `--md-sys-shape-corner-medium` (12px) or excessive spacing (>8px / `--md-sys-measurement-space100`) between cards within a unified collection.
