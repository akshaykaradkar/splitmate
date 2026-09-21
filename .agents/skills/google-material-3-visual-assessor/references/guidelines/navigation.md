# Primary Navigation - Universal AI Detection Guide & Google Material 3 Specifications

This reference provides universal visual heuristics for detecting primary navigation components—Navigation Bars, Navigation Rails, and Navigation Drawers—across any design system or platform, followed by Google Material 3 (GM3) specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

Primary navigation components are persistent structural containers dedicated to switching between top-level UI views or functional destinations within an app. Agents must detect navigation bars, rails, and drawers based on their placement anchoring, linear arrangement, and active destination highlights, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Placement & Orientation**: Primary navigation anchors to outer screen boundaries based on viewport dimensions:
    -   **Navigation Bar (Bottom Horizontal)**: Anchored at the extreme bottom of compact/mobile screens (directly above the OS system navigation/gesture bar). Spans the full width of the viewport horizontally.
    -   **Navigation Rail (Leading Vertical)**: Anchored vertically along the leading edge of medium/expanded screens. It is a **narrow vertical strip** typically housing only icons, or icons with labels stacked vertically underneath.
    -   **Navigation Drawer (Leading Panel)**: A **wider vertical panel** anchored to the leading screen edge, extending full height. It houses icons with text labels positioned horizontally side-by-side, and often includes section headers. It can be permanently visible (persistent sidebar) or appear as a temporary modal overlay.
-   **Linear Grouping & Destination Items**: Look for clusters of 3 to 7 equal-priority navigation destinations. Each destination typically features an icon paired with a concise text label (1-2 words).
    -   *Horizontal Bar*: 3 to 5 items arranged in a single horizontal row.
    -   *Vertical Rail (Collapsed)*: 3 to 7 items stacked vertically, with each icon placed directly above its text label.
    -   *Vertical Rail (Expanded) / Drawer*: Items stacked vertically, with each icon placed horizontally to the left of its text label.
-   **Active State Signatures**: Look for a clear visual distinction highlighting the currently active page:
    -   *Icon Shift*: Outlined vector (inactive) switching to a solid filled vector (active).
    -   *Active Indicator*: A high-contrast pill-shaped or rounded rectangular background shape positioned behind the active destination's icon or entire label lockup.
-   **Nonstandard Navigation Sidebars**: Some desktop/expanded web layouts feature a permanent vertical navigation sidebar that spans the full height of the left edge. If it is a **wide panel** with icons and text labels side-by-side (and potentially section headers), it MUST be classified as a **`Navigation drawer`** (persistent drawer), even if it is permanent and not collapsible. If it is a **narrow strip** (icons only or stacked labels), map it to **`Navigation rail`**.

---

## Part 2: Google Material 3 Specifications & Compliance Auditing

When conducting a Google Material 3 (GM3) adherence audit, evaluate detected primary navigation components against the following strict standards derived from GM3 and GM3 Expressive guidelines:

### GM3 Navigation Component Specifications

Google Material 3 establishes specific responsive adaptations, sizing, and color tokens across window size classes:

1.  **Navigation Bar Specifications**:
    -   **Usage**: Mandatory primary navigation for compact window sizes (mobile). Houses exactly 3 to 5 top-level destinations of equal importance.
    -   **Dimensions**: Container height is 64dp on Android/Web (56pt on iOS). Active indicator is a horizontal pill shape positioned behind the icon.
    -   **Tokens & Styling**:
        -   **Container Color**: `--md-sys-color-surface` or `--md-sys-color-surface-container` (no drop shadow at rest).
        -   **Active Indicator Shape**: Pill-shaped with corner radius 16px (`--md-sys-shape-corner-large` or `--md-sys-shape-corner-full`). Height 32dp, width 64dp.
        -   **Active Indicator Fill**: `--md-sys-color-secondary-container`.
        -   **Active Icon/Text**: Icon switches from outlined to filled (`--md-sys-color-on-secondary-container`). Active label color is `--md-sys-color-secondary` (under the GM3 Expressive update, which replaced the legacy `on-surface-variant` with `secondary` for better hierarchy and active status prominence).
        -   **Inactive Icon/Text**: Outlined icon in `--md-sys-color-on-surface-variant` and label text in `--md-sys-color-on-surface-variant`.
    -   **GM3 Expressive (Aug 2024 Update)**: Recommends the **flexible navigation bar** over the baseline navigation bar. The flexible nav bar is shorter and supports horizontal items (icon and label side-by-side inside the indicator) in medium windows, while vertical items (label below icon) remain the default for compact mobile views.
    -   **Distinction**: Toolbars contain actions; Navigation Bars contain persistent view destinations. Must not be conflated with the OS system gesture handle.

2.  **Navigation Rail Specifications**:
    -   **Usage**: Primary navigation for medium to expanded window sizes (tablets, desktops). Not for compact mobile screens.
    -   **Variants**:
        -   *Collapsed Navigation Rail*: Replaces the legacy baseline rail. Compact vertical stack of 3 to 7 icons and labels.
        -   *Expanded Navigation Rail*: Replaces the legacy Navigation Drawer in the Expressive update. Wider container allowing side-by-side icon/label pairings, section headers, and secondary destinations. Available in Standard or Modal configurations.
    -   **Styling & Tokens**:
        -   **Container Color**: `--md-sys-color-surface` or transparent (allowing items to sit directly on the parent surface, provided a 3:1 contrast ratio is maintained).
        -   **Active Indicator (Collapsed)**: Height 32dp, width 56dp (or 64dp), corner radius 16px / pill (`--md-sys-shape-corner-full`), filled with `--md-sys-color-secondary-container`.
        -   **Active Indicator (Expanded)**: Visually hugs the label text, but has a target area that spans the full width of the container.
        -   **Active Label Color**: Changed in Expressive update from `--md-sys-color-on-surface-variant` to `--md-sys-color-secondary`.
        -   **FAB in Rail**: Nested FABs use elevation level 0 (`--md-sys-elevation-level0`) and rest at the top of the rail.
        -   **Badges**: Attach to the top-right of icons (collapsed) or adjacent to text labels (expanded).

3.  **Navigation Drawer Specifications**:
    -   **Expressive Deprecation Note**: The legacy Navigation Drawer is no longer recommended under the GM3 Expressive update. Teams are strongly urged to transition to the `Expanded navigation rail` instead, which offers similar side-by-side text/icon layout and adapts better across window size classes.
    -   **Legacy/Standard Tokens**:
        -   **Container Width**: 360dp.
        -   **Asymmetric Corners**: `0, 16px, 16px, 0` corner radii (anchored side is straight, outer side uses `--md-sys-shape-corner-large`).
        -   **Active Indicator**: Large pill shape with height 56dp, width 336dp, and corner radius 28dp (`--md-sys-shape-corner-extra-large` or `--md-sys-shape-corner-full`), filled with `--md-sys-color-secondary-container`.
        -   **Scrim**: Modal navigation drawers must use a scrim (`--md-sys-color-scrim`) to block interaction with underlying content and can be dismissed by tapping the scrim or swiping.

### GM3 Exclusivity & Behavioral Rules

-   **Mutually Exclusive Navigation**: The simultaneous presence of both a horizontal `Navigation bar` and a vertical `Navigation rail` (or drawer) on the same screen layout is a strict violation of Material layout guidelines.
-   **Label Truncation**: Text labels must be kept concise (1-2 words). Scaled text must wrap to a second line rather than truncating with ellipses.

### Critical GM3 Violations to Flag

-   **Simultaneous Nav Bar and Rail**: Presenting both a bottom navigation bar and a side navigation rail on a single screen layout.
-   **Legacy Navigation Drawers**: Using the legacy baseline navigation drawer instead of the updated `Expanded navigation rail` (for expressive layouts).
-   **Conflating Nav Bars with OS Controls**: Misclassifying the bottom OS system gesture bar as a product `Navigation bar`.
-   **Text Truncation**: Truncating navigation destination labels with ellipses instead of wrapping text correctly.
-   **Shadows at Rest**: Giving the navigation bar or rail container a drop shadow at rest (GM3 navigation containers should stay flat and rely on color fill or border/divider to separate from content).
