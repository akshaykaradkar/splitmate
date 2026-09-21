# Primary Navigation - Universal AI Detection Guide & Android Motion Specifications

This reference provides universal visual heuristics for detecting primary
navigation components—Navigation Bars, Navigation Rails, and Navigation
Drawers—across any design system or platform, followed by Android Motion Design System
specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

Primary navigation components are persistent structural containers dedicated to
switching between top-level UI views or functional destinations within an app.
Agents must detect navigation bars, rails, and drawers based on their placement
anchoring, linear arrangement, and active destination highlights, regardless of
design system adherence.

### Key Visual & Geometric Heuristics

-   **Placement & Orientation**: Primary navigation anchors to outer screen
    boundaries based on viewport dimensions:
    -   **Navigation Bar (Bottom Horizontal)**: Anchored at the extreme bottom
        of compact/mobile screens (directly above the OS system
        navigation/gesture bar). Spans the full width of the viewport
        horizontally.
    -   **Navigation Rail (Leading Vertical)**: Anchored vertically along the
        leading edge of medium/expanded screens. It is a **narrow vertical
        strip** typically housing only icons, or icons with labels stacked
        vertically underneath.
    -   **Navigation Drawer (Leading Panel)**: A **wider vertical panel**
        anchored to the leading screen edge, extending full height. It houses
        icons with text labels positioned horizontally side-by-side, and often
        includes section headers. It can be permanently visible (persistent
        sidebar) or appear as a temporary modal overlay.
-   **Linear Grouping & Destination Items**: Look for clusters of 3 to 7
    equal-priority navigation destinations. Each destination typically features
    an icon paired with a concise text label (1-2 words).
    -   *Horizontal Bar*: 3 to 5 items arranged in a single horizontal row.
    -   *Vertical Rail (Collapsed)*: 3 to 7 items stacked vertically, with each
        icon placed directly above its text label.
    -   *Vertical Rail (Expanded) / Drawer*: Items stacked vertically, with each
        icon placed horizontally to the left of its text label.
-   **Active State Signatures**: Look for a clear visual distinction
    highlighting the currently active page:
    -   *Icon Shift*: Outlined vector (inactive) switching to a solid filled
        vector (active).
    -   *Active Indicator*: A high-contrast pill-shaped or rounded rectangular
        background shape positioned behind the active destination's icon or
        entire label lockup.
-   **Nonstandard Navigation Sidebars**: Some desktop/expanded web layouts
    feature a permanent vertical navigation sidebar that spans the full height
    of the left edge. If it is a **wide panel** with icons and text labels
    side-by-side (and potentially section headers), it MUST be classified as a
    **`Navigation drawer`** (persistent drawer), even if it is permanent and not
    collapsible. If it is a **narrow strip** (icons only or stacked labels), map
    it to **`Navigation rail`**.

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion Design System adherence audit, evaluate detected
primary navigation components against the following strict standards derived
from Android Motion guidelines:

### Android Motion Navigation Component Specifications

Android Motion establishes specific responsive adaptations and sizing tokens
across window size classes:

1.  **Navigation Bar Specifications**:
    -   **Usage**: Mandatory primary navigation for compact window sizes
        (mobile). Houses exactly 3 to 5 top-level destinations.
    -   **Dimensions**: Container height is 64dp on Android/Web.
        Active indicator is pill-shaped (expanded behind the active icon).
    -   **Distinction**: Toolbars contain actions; Navigation Bars contain
        persistent view destinations. Must not be conflated with the OS system
        gesture handle.
    -   **Tokens**: Uses `--droid-sys-color-surface` or `--droid-sys-color-background` for the container background. Active indicator uses `--droid-sys-color-secondary-container` with `--droid-sys-shape-corner-full` (or 16px pill shape). Text and icon on active item use `--droid-sys-color-on-secondary-container`. Inactive items use `--droid-sys-color-on-surface-variant`.
2.  **Navigation Rail Specifications**:
    -   **Usage**: Primary navigation for medium to expanded window sizes
        (tablets, desktops). Not for compact mobile screens.
    -   **Variants**:
        -   *Collapsed Navigation Rail*: Compact vertical stack of icons and labels.
        -   *Expanded Navigation Rail*: Wider container allowing side-by-side
            icon/label pairings and secondary destinations. Available in
            Standard or Modal configurations.
    -   **Styling & Tokens**: FABs placed inside a rail use elevation `--droid-sys-elevation-level0`.
        Rail container fill can be transparent or use `--droid-sys-color-surface` / `--droid-sys-color-background` provided a 3:1 contrast ratio is maintained. Badges attach to the top-right of icons (collapsed) or adjacent to text labels (expanded). Active indicator uses `--droid-sys-color-secondary-container` and shape `--droid-sys-shape-corner-full` (or 16px pill shape).
3.  **Navigation Drawer Specifications**:
    -   **Android Motion Drawer Layout**: Container width is typically 360dp. Asymmetric corners (`--droid-sys-shape-corner-large-end` - 0, 16px, 16px, 0px corner radii—anchored side is straight, outer side is rounded).
        Active indicator is a large pill shape (56dp height, 336dp width, 28dp
        corner radius).
    -   **Tokens**: Container uses `--droid-sys-color-surface` or `--droid-sys-color-background`. Active items use `--droid-sys-color-secondary-container` for the background pill and `--droid-sys-color-on-secondary-container` for the text label and icon. Inactive items use `--droid-sys-color-on-surface-variant`.

### Android Motion Exclusivity & Behavioral Rules

-   **Mutually Exclusive Navigation**: The simultaneous presence of both a
    horizontal `Navigation bar` and a vertical `Navigation rail` (or drawer) on
    the same screen is a strict violation of Android Motion layout guidelines.
-   **Label Truncation**: Text labels must be kept concise (1-2 words) and use `--droid-sys-typescale-label-medium` or `--droid-sys-typescale-label-small`. Scaled
    text must wrap to a second line rather than truncating with ellipses.

### Critical Android Motion Violations to Flag

-   **Simultaneous Nav Bar and Rail**: Presenting both a bottom navigation bar
    and a side navigation rail on a single screen layout.
-   **Legacy Navigation Drawers**: Flagging non-compliant navigation drawers and
    recommending the updated Android Motion specifications.
-   **Conflating Nav Bars with OS Controls**: Misclassifying the bottom OS
    system gesture bar as a product `Navigation bar` during an audit.
-   **Text Truncation**: Truncating navigation destination labels with ellipses
    instead of wrapping text correctly.
