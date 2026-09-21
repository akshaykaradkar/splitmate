# Tabs - Universal AI Detection Guide & Android Motion Specifications

This reference provides universal visual heuristics for detecting Tabs
(horizontal navigation containers organizing peer content) across any design
system or platform, followed by Android Motion Design System specifications for
compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Tab** container is an interactive horizontal navigation component used to
organize and switch between different screens, views, or datasets at the exact
same level of hierarchy. Agents must detect tabs based on their horizontal peer
arrangement, active underline indicators, and bottom dividing borders,
regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Horizontal Peer Layout**: Look for a single coplanar row of at least two
    interactive items (housing text labels, icons, or both) arranged
    side-by-side as peers.
    -   *Tabs vs. Button Groups*: Tab rows typically span the full width of the
        content area or are anchored in their own dedicated horizontal row. If
        the row of choice pills is clustered locally (e.g., aligned to one side
        of a header, inline with search inputs or other controls) where all
        items have visible background containers, it represents a **`Button
        group`** (specifically a connected button group) rather than `Tabs`.
-   **Active Indicator (Underline)**: Look for a prominent colored bar or
    underline positioned directly beneath the currently active tab item,
    visually connecting it to the content pane below.
-   **Full-Width Divider**: Look for a continuous horizontal dividing line
    (e.g., 1dp stroke) running along the extreme bottom edge of the tab
    container, separating the tab headers from the underlying scrollable
    content.
-   **Container Structure**: Detect two primary structural patterns:
    -   *Fixed Tabs*: Equal-width divisions spanning the full width of the
        parent container. Best suited for 2 to 4 items.
    -   *Scrollable Tabs*: Variable-width items scaled based on content length,
        supporting horizontal scrolling. Best suited for 5 or more items.
-   **Content Combinations**: Detect four high-fidelity layout lockups within
    tab items:
    -   *Label Only*: Text centered vertically and horizontally.
    -   *Stacked*: Leading icon positioned directly above a text label.
    -   *Inline*: Leading icon positioned horizontally to the left of a text
        label.
    -   *Badged*: Accompanying small or large notification badges attached in
        the top-right quadrant of the tab item.

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion Design System adherence audit, evaluate detected
tabs against the following strict standards derived from Android Motion guidelines:

### Android Motion Tab Variants & Sizing

Android Motion Design System establishes specific layout invariants and sizing tokens:

1.  **Tab Variants & Hierarchy**:
    -   **Primary Tabs**: Placed at the top of the content pane directly beneath
        app bars. They display main content destinations. Features a thicker
        **3dp active indicator** with slightly rounded top corners, colored with `--droid-sys-color-primary`.
    -   **Secondary Tabs**: Used within a content area to further separate
        related content and establish secondary hierarchy (required when a
        screen needs more than one level of tabs). Features a thinner **2dp
        active indicator**, colored with `--droid-sys-color-primary` or `--droid-sys-color-on-surface`.
2.  **Measurements & Sizing Tokens**:
    -   **Container Height**: 47dp container height for label-only tabs; 63dp
        container height for icon + label tabs.
    -   **Indicator Length**: Minimum active indicator length is 24dp.
    -   **Typography**: Tab labels use `--droid-sys-typescale-title-small` or `--droid-sys-typescale-label-large` (specifically utilizing `'Google Sans Text'`).
    -   **Paddings & Alignment**: Icons and labels are vertically centered within the
        container. 8dp padding between inline icons and text; 4dp padding
        between text and badges; 6dp badge overlap on stacked icons.
    -   **Scrollable Offset**: When using scrollable tabs, the first visible tab
        must be offset by 52dp from the left edge of the device.
    -   **Colors**:
        -   Active Tab Item (Text and Icon): Uses `--droid-sys-color-primary`.
        -   Active Underline Indicator: Uses `--droid-sys-color-primary`.
        -   Inactive Tab Item: Uses `--droid-sys-color-on-surface-variant`.
        -   Full-Width Bottom Divider: Uses `--droid-sys-color-outline` (1dp thickness).
3.  **Usage & Badge Rules**:
    -   *Related vs. Sequential*: Tabs must be used exclusively to group related
        peer content, **never** sequential workflow steps.
    -   *Badges*: Limit badge content to 4 characters (including a "+"). Badges
        must update or disappear as soon as the user views the relevant tab
        content.

### Critical Android Motion Violations to Flag

-   **Sequential Workflow Tabs**: Using tabs to navigate linear, sequential
    multi-step workflows (where step buttons or wizards are required) instead of
    peer content categories.
-   **Inconsistent Tab Padding**: Applying inconsistent or uneven padding across
    individual tab items within a single container.
-   **Missing Active Indicator**: Presenting a tab container without a clear
    active underline indicator or color distinction identifying the selected
    view.
-   **Incorrect Font Family**: Using standard system fonts instead of the mandatory `'Google Sans Text'` as specified in the `--droid-sys-typescale-label-large` or `--droid-sys-typescale-title-small` tokens.
