# Bottom Sheets - Universal AI Detection Guide & Elements GM3 Specifications

This reference provides universal visual heuristics for detecting Bottom Sheets
(anchored bottom containers displaying secondary or supplementary content)
across any design system or platform, followed by Elements GM3
specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Bottom sheet** is a dedicated structural container anchored at the bottom of
a screen or window, used to present supplementary content, menus, or secondary
actions. Agents must detect bottom sheets based on their distinct geometric and
structural qualities, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Placement & Anchoring**: Anchored flush to the bottom of the screen or
    window boundary. In compact window sizes (mobile), it typically spans the
    full width of the screen. In larger window sizes (tablet/desktop), it may be
    centered or inset with visible side margins.
-   **Visual Boundaries & Containment**: Bottom sheets are distinct containers
    that rise over the primary page content. They are visually defined by a
    solid background fill, a top elevation shadow, or a background-dimming
    overlay (scrim) covering the rest of the screen.
-   **Corner Geometry**: Characterized by distinct rounded top corners
    (upper-left and upper-right) while the bottom corners remain straight/flush
    against the bottom edge of the screen.
-   **Core Anatomy & Child Elements**:
    -   *Drag Handle (Optional)*: A small horizontal pill or line centered at
        the extreme top of the sheet container, indicating the sheet can be
        dragged or expanded.
    -   *Header / Title*: An optional top bar or title area containing a
        headline and/or close button.
    -   *Content Area*: Variable layouts containing list items, grid menus,
        text, cards, or media (thumbnails, images, video) that provide
        supplementary information or choices.
-   **Mandatory Container Mapping**: Agents MUST bound and classify the outer
    sheet container as `Bottom sheet`. Agents must not merely map the internal
    child elements (such as lists, buttons, or cards) while omitting the parent
    sheet container.

--------------------------------------------------------------------------------

## Part 2: Elements GM3 Specifications & Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate the detected
bottom sheet against the following strict standards derived from Elements GM3 guidelines:

### Elements GM3 Visual & Behavioral Rules

-   **Bottom Sheet Variants**:
    -   **Modal Bottom Sheet**: Appears in front of primary app content,
        disabling background functionality with a dark overlay (`scrim`).
    -   **Standard Bottom Sheet**: Co-exists with the main UI without a scrim,
        allowing simultaneous viewing and interaction with both regions (e.g., a
        persistent audio playback bar or location details over a map).
-   **Corner Radius & Shape**: Elements GM3 specifies a distinct **28dp top corner
    radius** (Extra large shape style, represented by `sys.shape.corner.extra-large-top` / `md.sys.shape.corner.extra-large.top`) for docked bottom sheet containers, while bottom corners must remain sharp (**0dp**).
-   **Color & Elevation**:
    -   *Modal*: Utilizes an elevation shadow (level 1 / 1dp) over a modal scrim.
    -   *Standard (No Scrim)*: Must use a higher surface color role (darker in
        light mode, lighter in dark mode) than the underlying page background to
        ensure clear visual separation.
-   **Drag Handle Specs**: An optional drag handle must be horizontally centered
    at the top of the sheet, featuring 16dp padding top and bottom.
-   **Responsive Layout Measurements**:
    -   *Compact Windows (Mobile)*: Spans 100% of the screen width up to a
        maximum width of 640dp. Top margin is at least 72dp from the top of the
        screen.
    -   *Medium/Expanded Windows (>640dp)*: Bottom sheets must not exceed 640dp
        in width. They adjust to have clearly visible 56dp side margins and a
        56dp top margin, rather than touching the screen edges.
-   **Behavior & Dismissal**:
    -   Modal sheets are initially capped at 50% screen height but can be pulled
        full-screen if content exceeds that height.
    -   Can be dismissed by tapping an internal action, tapping the scrim,
        swiping down, or selecting a close affordance (mandatory for full-screen
        modal sheets).

### Critical Elements GM3 Violations to Flag

-   **Missing Top Corner Radii**: Presenting a bottom sheet with sharp, 0dp top
    corners instead of the required **28dp** Extra large top rounded corners.
-   **Improper Margins on Large Screens**: Expanding a bottom sheet to full
    width on screens wider than 640dp without applying the mandatory 56dp side margins.
-   **Lacking Visual Separation**: Standard bottom sheets that blend entirely
    into the page background without a distinct surface color shift or elevation shadow.
