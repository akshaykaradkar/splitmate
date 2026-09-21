# Primary Navigation - Universal AI Detection Guide \& Elements GM3 Specifications

This reference provides universal visual heuristics for detecting primary navigation components—Navigation Bars, Navigation Rails, and Navigation Drawers—across any design system or platform, followed by Elements GM3 specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

Primary navigation components are persistent structural containers dedicated to switching between top-level UI views or functional destinations within an app. Agents must detect navigation bars, rails, and drawers based on their placement anchoring, linear arrangement, and active destination highlights, regardless of design system adherence.

### Key Visual \& Geometric Heuristics

-   **Placement \& Orientation**: Primary navigation anchors to outer screen boundaries based on viewport dimensions:
    -   **Navigation Bar (Bottom Horizontal)**: Anchored at the extreme bottom of compact/mobile screens (directly above the OS system navigation/gesture bar). Spans the full width of the viewport horizontally.
    -   **Navigation Rail (Leading Vertical)**: Anchored vertically along the leading edge of medium/expanded screens. It is a **narrow vertical strip** typically housing only icons, or icons with labels stacked vertically underneath.
    -   **Navigation Drawer (Leading Panel)**: A **wider vertical panel** anchored to the leading screen edge, extending full height. It houses icons with text labels positioned horizontally side-by-side, and often includes section headers. It can be permanently visible (persistent sidebar) or appear as a temporary modal overlay.
-   **Linear Grouping \& Destination Items**: Look for clusters of 3 to 7 equal-priority navigation destinations. Each destination typically features an icon paired with a concise text label (1-2 words).
    -   *Horizontal Bar*: 3 to 5 items arranged in a single horizontal row.
    -   *Vertical Rail (Collapsed)*: 3 to 7 items stacked vertically, with each icon placed directly above its text label.
    -   *Vertical Rail (Expanded) / Drawer*: Items stacked vertically, with each icon placed horizontally to the left of its text label.
-   **Active State Signatures**: Look for a clear visual distinction highlighting the currently active page:
    -   *Icon Shift*: Outlined vector (inactive) switching to a solid filled vector (active).
    -   *Active Indicator*: A high-contrast pill-shaped or rounded rectangular background shape positioned behind the active destination's icon or entire label lockup.
-   **Nonstandard Navigation Sidebars**: Some desktop/expanded web layouts feature a permanent vertical navigation sidebar that spans the full height of the left edge. If it is a **wide panel** with icons and text labels side-by-side (and potentially section headers), it MUST be classified as a **`Navigation drawer`** (persistent drawer), even if it is permanent and not collapsible. If it is a **narrow strip** (icons only or stacked labels), map it to **`Navigation rail`**.

---

## Part 2: Elements GM3 Specifications \& Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate detected primary navigation components against the following standards derived from Corp Eng guidelines:

### Elements GM3 Primary Navigation Specifications

Unlike baseline MD3, Elements GM3 retains the **Navigation Drawer** as a primary, heavily integrated structural component (highly optimized for tree configurations and long menus) and supports a dedicated **Standalone Navigation Rail** alongside the classic horizontal **Navigation Bar**.

#### 1. Navigation Drawer (Expanded Sidebar / Panel)

-   **Usage**: Primary navigation for medium to expanded desktop/tablet screens. Highly suited for long, hierarchical, or user-defined lists.
-   **Colors \& Integration**:
    -   Default background color is **surface container low** (light blue) or optionally **surface container lowest**.
    -   Drawer background, app bar header, and main content background colors must match.
-   **Touch Targets**: All list items must maintain a **48x48dp** touch target.
-   **Hierarchy Configurations**:
    -   *Flat Navigation*: Linear, predefined list of pages.
    -   *Tree Navigation*: Supports up to **3 levels of hierarchy** (Parent \> Child \> Child). Child items consist of text-only labels (no icons).
-   **Behavior \& Interactivity**:
    -   *Parent-as-Link Split Targets*: When an expandable parent item also functions as a link, the interaction target must be split: an arrow icon for expanding/collapsing, and the rest of the row for navigating to the parent link.
    -   *Dividers \& Section Headers*: Section headers are optional; they are forbidden for single items or at the absolute top of the menu. Dividers can separate related groupings.
    -   *FAB Option*: An optional Floating Action Button (FAB) can be placed as the first element inside the drawer (uses `primary-container` fill). **Fixing the FAB position is not recommended** because it restricts magnification scroll real estate. The FAB must never link to an external tab/window.
    -   *Badge Adornments*: Supports feature status badges (e.g., "New", "Beta" - must be short, single-word) and numerical count badges (default max is 99+, do not combine a numerical count and a secondary action in the same item).
    -   *Secondary Action*: Visible on hover by default, hidden in collapsed rail view.
    -   *Footer*: Fixed to the bottom. Must not duplicate App Bar elements (Settings, Account, Help).
-   **Collapsing Behavior (Rail View)**:
    -   Shown in expanded state by default on first use. Visible toggle button collapses it to a rail. Preference is saved.
    -   Content adapts using an **inset behavior (pushes content)** rather than a temporary overlay.
    -   *Simplified Rail Elements*: Only menu item icons, parent items, badge dots, expandable row icons, and divider lines are visible. Child items, trailing icons, feature badges, and footers are hidden.
    -   *Flyout Overlay*: Temporary overlay expands the full drawer when hover or keyboard focus is placed on any rail element (including the FAB). Collapses back when focus/hover leaves.
-   **Compact Viewport Overlay**: At small viewports (\< 571dp), collapses entirely into an overlay panel of **304dp fixed width** with a row height of **48dp**.

#### 2. Standalone Navigation Rail (Narrow Strip)

-   **Usage**: Narrow vertical bar on the left edge for screens where content space must be maximized. For simple, non-hierarchical menus with exactly **3 to 7 destinations** of equal priority.
-   **Anatomy**: Container, optional FAB (elevation 0 at rest), Icon, Active Indicator, Label Text, optional small/large Badges.
-   **Label Constraints**:
    -   Labels must be **exactly 1 word** and short enough to avoid truncation.
    -   Type scale must not be shrunk to fit long labels. If longer descriptions are needed, the **Navigation Drawer** must be used instead.
-   **Active State**: Selected icon fills and changes color; active indicator pill appears behind the icon. Outlined icons are used for inactive destinations.
-   **Responsive Adaptation**: Spans full height. At smaller viewports, collapses into a navigation overlay or transforms into a horizontal bottom **Navigation Bar** if there are **4 or fewer destinations**.

#### 3. Navigation Bar (Bottom Horizontal)

-   **Usage**: Bottom-anchored bar for compact (mobile) screen layouts. Spans the full width of the viewport.
-   **Height \& Dimensions**: Container height is **64dp** (Android/Web).
-   **Active Destination Indicator**: Comply with specifications: height **32dp**, width **64dp**, corner radius **16dp (pill / sys.shape.corner.full)**, filled with `secondary-container` color token.
-   **Destinations Count**: Exactly **3 to 5 destinations** (or 3 to 4 if transformed from a standalone rail).
-   **Label Placement**:
    -   *Compact (Mobile Portrait)*: Label is required and placed directly **below** the icon.
    -   *Medium and Larger screens*: Label is placed **trailing (to the right of)** the icon in LTR layouts.

### Elements GM3 Contrast \& State Rules

-   **NTC Compliant Active Destination**: The selected item must meet GAR Non-Text Contrast (NTC) standards.
    -   If the icon supports filled/outlined variants, active must use **filled icon** and inactive must use **outlined icon**.
    -   If no filled style exists, apply a **semibold icon weight**.
    -   Active label text automatically receives a **large-emphasized weight** change.
    -   Keyboard focus must show a prominent **blue outline** indicator.
-   **Layout Exclusivity**: Presenting both a bottom horizontal `Navigation bar` and a vertical `Navigation rail` or drawer on the same screen layout is a strict layout violation.

### Critical Elements GM3 Violations to Flag

-   **Simultaneous Multi-Navigation**: Displaying a bottom navigation bar and a left navigation sidebar on the same screen.
-   **External FAB Links**: Using the FAB in a drawer to link to external websites or open new tabs.
-   **Color-Only Selected Nav States**: Changing only the color of active nav icons without switching to filled/semibold weights or showing active indicators.
-   **Inconsistent Color Theming**: Mixing background colors (e.g., using a dark app bar with a light blue navigation drawer).
-   **Truncated Standalone Rail Labels**: Allowing navigation rail labels to truncate (e.g., "Set...") instead of using 1-word concise labels or moving to a drawer.
-   **Fixed FAB in Scrollable Drawers**: Locking the FAB position inside a scrollable drawer, blocking magnification flow.
