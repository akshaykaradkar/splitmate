# Bottom Sheets - Universal AI Detection Guide & Android Motion Specifications

This reference provides universal visual heuristics for detecting Bottom Sheets
(anchored bottom containers displaying secondary or supplementary content)
across any design system or platform, followed by Android Motion design system
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

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion design system adherence audit, evaluate the detected
bottom sheet against the following strict standards derived from Android Motion
guidelines:

### Android Motion Visual & Behavioral Rules

-   **Bottom Sheet Variants**:
    -   **Modal Bottom Sheet**: Appears in front of primary app content,
        disabling background functionality with a dark overlay (`scrim`). Used
        as an alternative to inline menus or dialogs on mobile.
    -   **Standard Bottom Sheet**: Co-exists with the main UI without a scrim,
        allowing simultaneous viewing and interaction with both regions (e.g., a
        persistent audio playback bar or location details over a map).
-   **Corner Radius & Shape**: Android Motion specifies a distinct **28px top corner
    radius** for both modal and standard bottom sheet containers, mapped directly to
    `--droid-sys-shape-corner-extra-large-top` (`28px 28px 0 0`), with bottom corners using
    `--droid-sys-shape-corner-none` (`0`).
-   **Color & Elevation**:
    -   *Modal*: Utilizes an elevation shadow of `--droid-sys-elevation-level1` (1px) over a modal scrim.
    -   *Standard (No Scrim)*: Must use a distinct surface color role, such as `--droid-sys-color-surface-variant` (light-dark(`#e1e3e1`, `#444746`)) or `--droid-sys-color-surface` (light-dark(`#fdfcfb`, `#1f1f1f`)), to ensure clear visual separation from the underlying page background.
-   **Drag Handle Specs**: An optional drag handle must be horizontally centered
    at the top of the sheet, featuring 16dp padding top and bottom, and utilizing a rounded pill shape.
-   **Responsive Layout Measurements**:
    -   *Compact Windows (Mobile)*: Spans 100% of the screen width up to a
        maximum width of 640dp. Top margin is at least 72dp from the top of the
        screen.
    -   *Medium/Expanded Windows (>640dp)*: Bottom sheets must not exceed 640dp
        in width. They adjust to have clearly visible 56dp side margins and a
        56dp top margin, rather than touching the screen edges.
-   **Motion & Easing Transitions**: Dragging, expanding, or minimizing bottom sheets must feel premium and organic.
    Transitions should utilize `--droid-sys-motion-duration-300` (300ms) or `--droid-sys-motion-duration-350` (350ms)
    with the signature `--droid-sys-motion-easing-emphasized` (`cubic-bezier(0.2, 0.0, 0.0, 1.0)`).
-   **Behavior & Dismissal**:
    -   Modal sheets are initially capped at 50% screen height but can be pulled
        full-screen if content exceeds that height.
    -   Can be dismissed by tapping an internal action, tapping the scrim,
        swiping down, or selecting a close affordance (mandatory for full-screen
        modal sheets).
    -   *iOS Platform*: Supported with identical specs, but requires a close
        button or tapping outside, as dragging to dismiss behaves differently.

### Critical Android Motion Violations to Flag

-   **Missing Top Corner Radii**: Presenting a bottom sheet with sharp, 0px top
    corners instead of the required `--droid-sys-shape-corner-extra-large-top` shape.
-   **Improper Margins on Large Screens**: Expanding a bottom sheet to full
    width on screens wider than 640dp without applying the mandatory 56dp side
    margins.
-   **Lacking Visual Separation**: Standard bottom sheets that blend entirely
    into the page background without a distinct surface color shift or elevation
    shadow.
-   **Incorrect Easing or Abrupt Snapping**: Falling back to basic linear animations or lacking easing rules, violating `--droid-sys-motion-easing-emphasized` requirements.
