# Tabs - Universal AI Detection Guide \& MD3 Specifications

This reference provides universal visual heuristics for detecting Tabs
(horizontal navigation containers organizing peer content) across any design
system or platform, followed by Material Design 3 (MD3) specifications for
compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Tab** container is an interactive horizontal navigation component used to
organize and switch between different screens, views, or datasets at the exact
same level of hierarchy. Agents must detect tabs based on their horizontal peer
arrangement, active underline indicators, and bottom dividing borders,
regardless of design system adherence.

### Key Visual \& Geometric Heuristics

-   **Horizontal Peer Layout**: Look for a single coplanar row of at least two
    interactive items (housing text labels, icons, or both) arranged
    side-by-side as peers.
    -   *Tabs vs. Button Groups*: Tab rows typically span the full width of the
        content area or are anchored in their own dedicated horizontal row. If
        the row of choice pills is clustered locally (e.g., aligned to one side
        of a header, inline with search inputs or other controls) where all
        items have visible background containers, it represents a **`Button group`** (specifically a connected button group) rather than `Tabs`.
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

## Part 2: MD3 Specifications \& Compliance Auditing

When conducting a Material Design 3 (MD3) adherence audit, evaluate detected
tabs against the following strict standards derived from Material guidelines:

### MD3 Component Variants \& Sizing

Material Design 3 establishes specific layout invariants and sizing tokens:

1.  **Tab Variants \& Hierarchy**:
    -   **Primary Tabs**: Placed at the top of the content pane directly beneath
        app bars. They display main content destinations. Features a thicker
        **3dp active indicator** with slightly rounded top corners (`3, 3, 0, 0`
        radii).
    -   **Secondary Tabs**: Used within a content area to further separate
        related content and establish secondary hierarchy (required when a
        screen needs more than one level of tabs). Features a thinner **2dp
        active indicator**.
2.  **Measurements \& Sizing Tokens**:
    -   **Container Height (Android/Web)**: 47dp container height for label-only tabs; 63dp
        container height for icon + label tabs.
    -   **Container Height (iOS)**: 52pt container height (to accommodate iOS status or app bar layouts).
    -   **Tab Size (iOS)**: 44pt height to meet iOS's 44x44pt minimum touch target size (versus Android's 40dp tab height which uses padding to meet the 48x48dp touch target).
    -   **Indicator Length**: Minimum active indicator length is 24dp.
    -   **Paddings**: Icons and labels are vertically centered within the
        container. 8dp padding (`--md-sys-measurement-space100`) between inline icons and text; 4dp padding (`--md-sys-measurement-space50`)
        between text and badges; 6dp badge overlap (`--md-sys-measurement-space75`) on stacked icons.
    -   **Scrollable Offset**: When using scrollable tabs, the first visible tab
        must be offset by 52dp from the left edge of the device (on both web and
        mobile).
3.  **Token Bindings \& Styling**:
    -   **Active Text \& Icon Color**: `--md-sys-color-primary`
    -   **Inactive Text \& Icon Color**: `--md-sys-color-on-surface-variant`
    -   **Active Indicator Color**: `--md-sys-color-primary`
    -   **Container Fill**: `--md-sys-color-surface` or `--md-sys-color-surface-container` (often transparent, inheriting background).
    -   **Bottom Divider Color**: `--md-sys-color-outline-variant` (1dp height, `--md-sys-elevation-level0` or aligned with parent).
    -   **Typography**: `--md-sys-typescale-label-large` (active tabs use bold or medium emphasis, e.g., `--md-sys-typescale-emphasized-label-large`).
4.  **Usage \& Badge Rules**:
    -   *Related vs. Sequential*: Tabs must be used exclusively to group related
        peer content, **never** sequential workflow steps.
    -   *Badges*: Limit badge content to 4 characters (including a "+"). Badges
        must update or disappear as soon as the user views the relevant tab
        content.

### iOS 26 Liquid Glass Styling (iOS Only)

-   **Liquid Glass Effects**: GM3 tabs on iOS 26 support semi-transparent glass effects when placed in the functional layer (rather than the content layer).
-   **Opaque Active Selection**: Even in the functional layer, the active tab content must remain opaque to avoid "glass-on-glass" layering issues, which degrade accessibility.
-   **Fade Effect on Overflow**: Overflowing tab groups in Liquid Glass use a horizontal fade effect (20% of container width when width is \<400px; fixed 80px when width is \>=400px) that disappears at the edges of scroll boundaries.

### Critical MD3 Violations to Flag

-   **Sequential Workflow Tabs**: Using tabs to navigate linear, sequential
    multi-step workflows (where step buttons or wizards are required) instead of
    peer content categories.
-   **Inconsistent Tab Padding**: Applying inconsistent or uneven padding across
    individual tab items within a single container.
-   **Missing Active Indicator**: Presenting a tab container without a clear
    active underline indicator or color distinction identifying the selected
    view.
-   **Under-density Touch Targets**: Applying default density that shrinks tab touch targets below the 48x48dp/44x44pt limit without user-facing controls to reverse the setting.
