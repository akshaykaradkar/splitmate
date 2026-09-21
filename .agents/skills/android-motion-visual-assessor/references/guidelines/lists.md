# Lists & List Items - Universal AI Detection Guide & Android Motion Specifications

This reference provides universal visual heuristics for detecting Lists (outer
scrollable vertical containers) and List Items (individual horizontal content
rows) across any design system or platform, followed by Android Motion Design System
specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **List** is a continuous vertical group of text or images organized into
distinct horizontal rows called **List items**. They are commonly used for data
feeds, settings menus, and navigation trees. Agents must detect lists and list
items based on their vertical containment flow, row repetition, and slot
structures, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Vertical Flow & Grouping (`List`)**: Look for a primary vertical container
    grouping multiple related horizontal rows. Lists often occupy the full width
    of a screen or layout pane and frequently feature a vertical scrollbar or
    show partial cut-off rows at the bottom.
-   **Row Repetition & Bounding (`List item`)**: Inside the parent list, look
    for repeating horizontal rectangular bounding boxes spanning the full width
    of the list container. Each distinct horizontal row is a `List item`.
-   **Core Anatomy & Slot Structure**: List items follow a highly predictable
    horizontal sequence divided into three primary functional slots:
    -   *Leading Slot (Optional)*: Visual anchors on the far left (e.g.,
        circular user avatars, brand icons, video thumbnails, or selection
        controls like checkboxes/radio buttons).
    -   *Content Slot (Required)*: The central, largest-width area containing
        primary label text and optional supporting body text or badges.
    -   *Trailing Slot (Optional)*: Visual or interactive indicators on the far
        right (e.g., navigation chevrons, trailing timestamps, switches, or icon
        buttons).
-   **Mandatory Dual Mapping (List + List Item)**: When analyzing screens
    presenting a vertical continuous flow of data rows, agents MUST explicitly
    identify BOTH the outer container and each individual row:
    -   **Outer Container (`List`) [MANDATORY]**: Bound the entire scrollable
        vertical container that groups all related items and classify it as
        `List`.
    -   **Individual Rows (`List item`) [MANDATORY]**: Exclusively bound every
        distinct horizontal row within that list and classify it as `List item`.
        Agents must not stop at identifying just the embedded child widgets
        (switches, avatars) inside the row; both the parent `List item` bounding
        box and its embedded controls must be captured.

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion Design System adherence audit, evaluate detected
lists and list items against the following strict standards derived from
Android Motion guidelines:

### Android Motion List Styles & Variants

Android Motion defines two primary structural variations for lists:

1.  **Expressive Lists**: Recommended for new designs. Features flexible
    styling, highlighted selection states (e.g., shape changes using `--droid-sys-shape-corner-medium` or tonal
    background fills using `--droid-sys-color-secondary-container` or `--droid-sys-color-surface-variant` for selected rows), and customizable slots.
2.  **Baseline Lists**: Traditional planar lists. Available as 1-line, 2-line,
    or 3-line configurations, where height adapts based on text density.
3.  **iOS Platform Styles**: iOS supports three specific list styles: *Plain*
    (full-width cells), *Grouped* (tonal surface backgrounds distinguishing
    groups), and *Inset Grouped* (cells fully contained with outer margins on
    every side).

### Android Motion Tokens & Sizing Measurements

-   **Dimensions & Heights**:
    -   *1-Line List Item*: 56dp height (leading/trailing elements vertically
        centered).
    -   *2-Line List Item*: 72dp height (leading/trailing elements vertically
        centered).
    -   *3-Line List Item*: 88dp height (leading/trailing elements top-aligned
        with 8dp/12dp top padding).
-   **Paddings & Alignments**:
    -   *Label Padding*: 16dp left padding from the container edge or leading
        element.
    -   *Trailing Padding*: 16dp left padding from text; 24dp right padding from
        the container edge.
-   **Typography**:
    -   Primary labels use `--droid-sys-typescale-body-large` or `--droid-sys-typescale-body-medium` (utilizing `'Google Sans Text'`).
    -   Supporting text uses `--droid-sys-typescale-body-small`.
-   **Colors**:
    -   Container uses `--droid-sys-color-surface` or `--droid-sys-color-background`.
    -   Primary text uses `--droid-sys-color-on-surface`.
    -   Supporting text and leading/trailing icons use `--droid-sys-color-on-surface-variant`.
-   **Touch Targets & Accessibility**: Minimum accessible touch target is
    **48x48dp** for any interactive element within a slot.
-   **Dividers**: Optional 1dp dividers using `--droid-sys-color-outline` or `--droid-sys-color-surface-variant` can separate list items (inset 16dp on
    the left to align with text labels, 24dp right padding).

### Android Motion Interaction & Behavioral Rules

-   **Selection Modes**: A list can have only one active selection mode at a
    time (e.g., single-select vs. multi-select). Tapping a list item row toggles
    its selection state or activates its rightmost trailing control (e.g.,
    toggling a switch).
-   **Slot Restrictions**: Nested interactive elements inside leading or
    trailing slots must perform only a single action to prevent breaking
    keyboard navigation and screen reader flow.

### Critical Android Motion Violations to Flag

-   **Missing Parent-Child List Mapping**: Failing to classify either the outer
    `List` container or the individual `List item` rows during a structural
    audit.
-   **Multiple Interactive Actions per Slot**: Placing multiple independent
    buttons or controls within a single list item's trailing slot, violating
    accessibility navigation rules.
-   **Undersized Touch Targets**: Designing compact list rows where interactive
    trailing switches or icon buttons fall below the mandatory 48x48dp minimum
    target size.
-   **Incorrect Font Use**: Failing to use the specified Google Sans Text (`'Google Sans Text'`) for labels and body scales.
