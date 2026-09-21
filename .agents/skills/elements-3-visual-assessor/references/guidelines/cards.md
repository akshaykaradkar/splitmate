# Cards - Universal AI Detection Guide & Elements GM3 Specifications

This reference provides universal visual heuristics for detecting Cards
(containment components displaying content and actions about a single subject)
across any design system or platform, followed by Elements GM3
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

## Part 2: Elements GM3 Specifications & Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate detected cards against
the following strict standards derived from Elements GM3 guidelines:

### Elements GM3 Visual & Behavioral Rules

-   **Card Styles & Container Colors**:
    -   **Outlined Card**: Features no drop shadow. Background color is `Surface_container_lowest` (using the `--cee3-sys-color-surface-container-lowest` or outline tokens). It has a visible thin border stroke (`outline_variant`) around the container.
    -   **Elevated Card**: Features a resting elevation of **+2** (3dp height, corresponding to `--cee3-sys-elevation-level-2` or equivalent) with a drop shadow over a `Surface_container_lowest` background fill. Used to provide clear separation from background surfaces.
    -   **Filled Card (Default)**: Features no drop shadow (0dp resting elevation). Background color uses `Surface_container_highest` (`--cee3-sys-color-surface-container-highest` or equivalent) by default.
    -   **Extended Filled Cards**: Elements GM3 supports **4 additional filled card styles** using different surface container colors for the background (such as different levels of surface container fills) to create better visual hierarchy and clarity on the page.
-   **Tokens & Measurements**:
    -   **Corner Radius**: Standard **12dp corner radius** (Medium baseline shape style) applied to all four corners of the card container.
    -   **Padding**: Left and right padding must be strictly **16dp**.
    -   **Typography**: Title/headline typography inside the card header should use **Static/Title/Medium** (corresponding to `--cee3-typescale-title-medium-link` or `--cee3-typescale-title-medium` token).
-   **Layout & Collections Spacing**:
    -   Cards grouped in a grid, vertical list, or horizontal row must maintain spacing aligned with the page-level grid system (standard 8x dp spacing: 8dp, 16dp, 24dp).
    -   On small viewports, cards can be rearranged into a vertical list with a **16dp gutter/gutter spacing** for better usability.
-   **Selection Cards**:
    -   For cards acting as selection controls (single or multi-select), a radio button or checkbox is required to indicate the selection status. **No additional styles** (e.g., active background/borders) should be applied to the card container for selected states to ensure consistency, NTC, and accessibility.
    -   Ideally, the entire card should be the interactive target area (directly actionable/focusable) with no other links or buttons inside. If other links/actions are necessary, do not use the entire card as the target area; define distinct, non-overlapping target areas.
-   **Accessibility & Interaction States**:
    -   *Focus Indicator*: Directly actionable cards must display a clear, perceivable focus indicator (focus ring) in addition to container state layers to comply with Non-Text Contrast (NTC) and keyboard focus requirements.
    -   If a card has only one interactive element, the focus ring can encompass the entire card. If there are multiple interactive elements, the focus ring must highlight only the active selection control or button.

### Critical Elements GM3 Violations to Flag

-   **Action Stacking (Nested Buttons in Actionable Cards)**: Placing independent interactive buttons or links inside a card when the entire card surface is already directly actionable. This violates accessibility rules, causing conflicts in the accessibility tree and interaction states.
-   **Unscrimmed Text over Images**: Overlaying text directly on busy background images without a translucent scrim or protective backing shape, failing WCAG contrast requirements.
-   **Improper Corner Radii**: Utilizing sharp 0dp or custom rounded corners instead of the mandatory 12dp corner radius for standard card containers.
-   **Over-styling Selection Cards**: Changing the container border or background color when a selection card is active instead of relying solely on the nested radio button or checkbox selection state.
-   **Missing Focus Indicators**: Failing to render a prominent, visible focus ring on keyboard navigation for actionable card containers.
