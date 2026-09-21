# Tabs - Universal AI Detection Guide & Elements GM3 Specifications

This reference provides universal visual heuristics for detecting Tabs (horizontal navigation containers organizing peer content) across any design system or platform, followed by Elements GM3 specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Tab** container is an interactive horizontal navigation component used to organize and switch between different screens, views, or datasets at the exact same level of hierarchy. Agents must detect tabs based on their horizontal peer arrangement, active underline indicators, and bottom dividing borders, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Horizontal Peer Layout**: Look for a single coplanar row of at least two interactive items (housing text labels, icons, or both) arranged side-by-side as peers.
    -   *Tabs vs. Button Groups*: Tab rows typically span the full width of the content area or are anchored in their own dedicated horizontal row. If the row of choice pills is clustered locally (e.g., aligned to one side of a header, inline with search inputs or other controls) where all items have visible background containers, it represents a **`Button group`** (specifically a connected button group) rather than `Tabs`.
-   **Active Indicator (Underline)**: Look for a prominent colored bar or underline positioned directly beneath the currently active tab item, visually connecting it to the content pane below.
-   **Full-Width Divider**: Look for a continuous horizontal dividing line (e.g., 1dp stroke) running along the extreme bottom edge of the tab container, separating the tab headers from the underlying content.
-   **Container Structure & Alignment**:
    -   *Alignment*: In desktop environments and across modern layouts, tabs may be left-aligned rather than centered or stretched, optimizing for F-shaped scanning patterns.
-   **Content Combinations**: Detect layout lockups within tab items:
    -   *Label Only*: Text centered vertically.
    -   *Icon + Label*: Icons and labels are vertically centered within the container.
    -   *Badged*: Accompanying small or large notification badges attached in the top-right quadrant of the tab item.

--------------------------------------------------------------------------------

## Part 2: Elements GM3 Specifications & Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate detected tabs against the following strict standards derived from Elements GM3 guidelines:

### Elements GM3 Component Specifications & Sizing

1.  **Left Alignment Standard**:
    -   **Deviating from Material (MD3) Guidelines**: In Elements GM3, both primary and secondary tabs are **strictly left-aligned** across all viewports (including desktop and small screens/mobile). This supports F-pattern reading paths, ensures accessibility for users with magnification, and maintains system-wide consistency. Centralized or fully stretched tabs violate this specification.
2.  **Tab Hierarchy & Indicator Line Sizing**:
    -   **Primary Tabs**: Used to alternate between different main content destinations. Features a thicker **3dp active indicator** directly below the active tab label with slightly rounded top corners.
    -   **Secondary Tabs**: Placed below primary tabs within a content area to further separate related content and establish hierarchical sub-views. Features a thinner **2dp active indicator**.
3.  **Anatomy & Measurements**:
    -   **Icon Positioning**: Icons and labels must be vertically centered within the container (not stacked vertically as in standard MD3).
    -   **Paddings & Spacing**:
        -   Standard label font utilizing standard typography tokens (e.g., `--cee3-typescale-label-large-link` or similar Google Sans Text styling).
        -   Icons use `--md-icon-font` and are 18dp.
4.  **Labels & Character Limits**:
    -   **Single Line Constraint**: Tab labels must appear in a single row and **must not wrap** to multiple lines.
    -   **Character Limit**: Keep tab text labels short and concise, under a **maximum of 30 characters** (including spaces) to prevent truncation. If text truncation occurs, it must utilize a clean ellipsis on a single line.
5.  **Usage & Interaction Rules**:
    -   **Related vs. Sequential**: Tabs must be used to group closely related categories of peer content. They must **never** be used to navigate linear, sequential workflows or step-by-step wizard processes.
    -   **Nesting caution**: Avoid nesting tabs within tabs unless a clear content area separates both tab bars, as it can disorient users.
    -   **Badges**: Limit badge content to 4 characters (including a "+"). Once the user views the relevant tab content, the badge value must update or the badge must disappear.
6.  **Overflow Behavior**:
    -   **Pagination Overflow**: When tabs exceed the container width, horizontal scroll navigation with pagination arrows must be used. Only the right arrow shows initially; both arrows show when scrolling starts. **Dropdown overflow menus are strictly prohibited** as they reduce visible tabs.

### Critical Elements GM3 Violations to Flag

-   **Stretched or Center Alignment**: Designing tabs that stretch to fill the full container width or align to the center instead of being left-aligned.
-   **Sequential Content Navigation**: Using tabs to navigate linear, step-by-step wizards or sequential multi-step forms.
-   **Label Wrapping**: Permitting tab labels to wrap to a second line.
-   **Wrapped or Truncated Long Labels (>30 chars)**: Failing to keep labels short and concise, resulting in messy truncation or clipped content.
-   **Incorrect Active Underline Position**: Placing the indicator line detached or improperly offset from the bottom of the active tab label container.
-   **Use of Dropdown Menus for Overflow**: Using a dropdown menu to house overflowing tabs instead of pagination arrows.
