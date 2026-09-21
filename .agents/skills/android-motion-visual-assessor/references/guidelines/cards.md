# Cards - Universal AI Detection Guide & Android Motion Specifications

This reference provides universal visual heuristics for detecting Cards
(containment components displaying content and actions about a single subject)
across any design system or platform, followed by Android Motion design system
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

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion adherence audit, evaluate detected cards against
the following strict standards derived from Android Motion design system guidelines:

### Android Motion Visual & Behavioral Rules

-   **Card Variants**:
    -   **Elevated Card**: Features a drop shadow (`--droid-sys-elevation-level1` as resting) over a surface background. Used to provide clear separation from similar background surfaces.
    -   **Filled Card**: Features no drop shadow (`--droid-sys-elevation-level0`), utilizing a solid surface-variant background fill color (`--droid-sys-color-surface-variant`) that contrasts with the surrounding surface. Used for lower visual emphasis.
    -   **Outlined Card**: Features no drop shadow (`--droid-sys-elevation-level0`), utilizing a visible thin border stroke (`--droid-sys-color-outline`) around a transparent or surface-matching fill (`--droid-sys-color-surface`). Used for clean, distinct boundary separation.
-   **Tokens & Measurements**:
    -   **Corner Radius**: Android Motion specifies a standard **12px corner radius** (`--droid-sys-shape-corner-medium`) for all card container variants.
    -   **Elevation**:
        -   Elevated Card: Resting `--droid-sys-elevation-level1` (1px drop shadow), Hover/Focus `--droid-sys-elevation-level2` (3px drop shadow).
        -   Filled/Outlined Card: Resting `--droid-sys-elevation-level0` (0px drop shadow), Hover/Focus `--droid-sys-elevation-level1` (1px drop shadow).
    -   **Internal Padding**: Standard internal padding is 16px on the left, right, top, and bottom.
    -   **Spacing in Collections**: Cards grouped in grids, vertical lists, or carousels must maintain a maximum spacing of 8px between adjacent cards.
-   **Typography**:
    -   Card Titles must use `--droid-sys-typescale-title-medium` (500 1rem/1.5rem `'Google Sans Text'`) or `--droid-sys-typescale-title-large` (1.4rem/1.8rem `'Google Sans'`).
    -   Supporting text must use `--droid-sys-typescale-body-medium` (0.9rem/1.3rem `'Google Sans Text'`) or `--droid-sys-typescale-body-small` (0.8rem/1rem `'Google Sans Text'`).
-   **Motion & Interaction (Android Motion Exclusives)**:
    -   **Interactive Transition**: When a card is clicked or expanded, it must animate using the Emphasized easing curve: `--droid-sys-motion-easing-emphasized` (`cubic-bezier(0.2, 0.0, 0.0, 1.0)`).
    -   **Duration**: Expansion transitions must last `--droid-sys-motion-duration-400` (400ms) or `--droid-sys-motion-duration-500` (500ms). Resting-to-hover state changes must use `--droid-sys-motion-duration-150` (150ms).
    -   **Interactive Ripple**: Clickable cards must feature an ink ripple utilizing `--droid-sys-color-on-surface` with appropriate alpha masking, accelerating with standard easing.

### Critical Android Motion Violations to Flag

-   **Action Stacking (Nested Buttons in Actionable Cards)**: Placing independent interactive buttons or switches inside a card where the entire card surface is already directly actionable, violating interaction patterns.
-   **Unscrimmed Text over Images**: Layering text directly over background images without a translucent scrim or bounding shape beneath the text, failing readability requirements.
-   **Improper Corner Radii or Spacing**: Utilizing sharp (0px) corners or excessive spacing (>8px) between cards within a unified collection.
-   **Non-compliant Motion Curves**: Using linear or standard linear transitions instead of `--droid-sys-motion-easing-emphasized` for expansions.
