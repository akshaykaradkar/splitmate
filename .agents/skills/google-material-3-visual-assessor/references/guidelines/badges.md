# Badges - Universal AI Detection Guide & GM3 Specifications

This reference provides universal visual heuristics for detecting Badges (indicating notifications, counts, or status) across any design system or platform, followed by Google Material 3 (GM3) specifications and design tokens for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Badge** is a compact visual indicator attached to navigation items, icons, tabs, or menus to communicate unread notifications, numerical counts, or status updates. Visual models must detect badges based on their distinct geometric and structural qualities, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Placement & Anchoring**: Badges are consistently anchored at the upper trailing edge (top-right quadrant in left-to-right layouts) of a parent control. They frequently overlap the bounding box of underlying navigation components like an `Icon button`, `Tab`, `Navigation bar` item, or `Navigation rail` item.
-   **Visual Boundaries & Containment**: Badges appear as small, high-contrast, solid-colored shapes contrasting sharply with the underlying element or background.
    -   *Dot Badges*: Small, circular indicators without text.
    -   *Pill / Count Badges*: Horizontally expanding rounded rectangles or pill shapes containing numbers or short text.
-   **Core Anatomy**:
    -   *Container*: A high-contrast filled background shape (typically red/error, tertiary, or other alerting brand colors).
    -   *Label (Optional)*: A numerical count (e.g., "1", "99+") or short status text rendered in a miniature font size.
-   **Mandatory Dual Mapping (Parent + Badge)**: When a badge is attached to or overlapping another control, agents MUST explicitly identify BOTH components as separate objects.
    -   **Parent Component**: Identify the underlying interactive target (e.g., `Icon button`, `Tab`, `Navigation bar`, `Navigation rail`).
    -   **Attached Badge (`Badge`) [MANDATORY]**: Exclusively bound the small overlay dot or counter indicator and classify it independently as a `Badge`.

---

## Part 2: GM3 Specifications & Compliance Auditing

When conducting a Google Material 3 (GM3) adherence audit, evaluate the detected badge against the following strict standards derived from Material guidelines:

### GM3 Visual & Behavioral Rules

-   **Badge Variants**:
    -   **Small Badge**: A simple 6dp circular dot indicating an unread notification. It has a HxW of 6dp and a 3dp corner radius (`--md-sys-shape-corner-full` or `3px`). It contains no label text.
    -   **Large Badge**: A 16dp tall rounded container containing label text for counts or status. It has an 8dp corner radius (`--md-sys-shape-corner-full` or `8px`). It expands horizontally as digits increase.
-   **Physical Dimensions & Specs**:
    -   *Large Badge Height*: 16dp.
    -   *Large Badge One Digit Width*: 16dp.
    -   *Large Badge Max Character Count Width*: 34dp (making the maximum size 16x34dp).
    -   *Large Badge Internal Padding*: 4dp horizontal padding between the text and container boundary.
-   **Content & Character Limits**: Large badges must limit their content to a maximum of **four characters** (e.g., "999+").
-   **Color Mappings & Roles**: GM3 specifies Error, Error Container, or Tertiary color roles for badges to provide necessary visual emphasis without overpowering primary product actions:
    -   *Default Container*: `--md-sys-color-error` (`light-dark(#b3261e, #f2b8b5)`) or `--md-sys-color-error-container` (`light-dark(#f9dedc, #8c1d18)`).
    -   *Default Label Text*: `--md-sys-color-on-error` (`light-dark(#ffffff, #601410)`) or `--md-sys-color-on-error-container` (`light-dark(#8c1d18, #f9dedc)`).
-   **Anchor Distances & Alignment**:
    -   *Small Badge Anchor Distance*: 6x6dp from the top trailing corner of the parent icon to the bottom leading corner of the badge container.
    -   *Large Badge Anchor Distance*: 14x12dp from the top trailing corner of the parent icon to the bottom leading corner of the badge container.
-   **Behavior & Accessibility**:
    -   Badges are temporary status indicators; a badge indicating an unread notification must be hidden once the user selects or visits the corresponding navigation destination.
    -   *RTL Layouts*: In right-to-left languages, the badge position mirrors to the upper leading (top-left) edge.
    -   *iOS Platform*: Badge labels on iOS utilize iOS Label Small (`SF 11pt`) to match platform norms.

### Critical GM3 Violations to Flag

-   **Character Overflow**: Displaying more than four characters in a large badge (e.g., "10000" instead of "999+"), violating layout constraints and causing text clipping.
-   **Improper Anchoring**: Positioning the badge completely detached from its parent icon, centered on the parent container, or on the wrong corner (e.g., bottom-left quadrant in an LTR layout).
-   **Color Conflicts**: Custom colors that fail accessibility contrast guidelines or diverge from the standard Error or Tertiary color roles.
-   **Missing Independent Classification**: Failing to classify the `Badge` independently from its parent navigation element during a component audit.
