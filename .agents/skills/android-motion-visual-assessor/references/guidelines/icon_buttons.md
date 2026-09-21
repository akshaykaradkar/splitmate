# Icon Buttons - Universal AI Detection Guide & Android Motion Specifications

This reference provides universal visual heuristics for detecting Icon Buttons
(compact interactive controls featuring a standalone icon) across any design
system or platform, followed by Android Motion design system specifications for
compliance auditing.

## Part 1: Universal AI Detection Heuristics

An **Icon button** is a compact interactive target featuring a standalone
central icon without accompanying label text. They allow users to execute common
actions (e.g., bookmarking, filtering, opening menus) with a single tap. Agents
must detect icon buttons based on their central icon isolation, container
styling, and padded touch zones, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Central Icon Isolation**: Look for a single, perfectly centered system
    icon (e.g., magnifying glass, heart, bookmark, three dots) with absolutely
    no accompanying text label inside the control boundary.
-   **Container Styling & Visual Boundaries**: Detect three primary containment
    representations:
    -   *Filled Container*: A solid colored background shape (circle or rounded
        square) enclosing the icon.
    -   *Outlined Container*: A transparent background fill surrounded by a
        distinct border stroke.
    -   *Standard (Isolated) Icon*: Lacking any visible container at rest; the
        container becomes visible only upon hover, focus, or press.
-   **Toggle State Signatures**: Toggle icon buttons represent binary states
    (e.g., favorited vs. unfavorited). Look for three distinct visual state
    changes:
    -   *Style Shift*: Outlined icon vector (unselected) switching to a solid
        filled icon vector (selected).
    -   *Weight Shift*: Regular stroke weight (unselected) switching to
        bold/semibold stroke weight (selected) as a fallback when no filled
        vector exists.
    -   *Shape Morphing*: Container morphing from round (unselected) to square
        (selected).
-   **Interactive Area & DOM Heuristics**: Smaller visual variants (XS/S) have
    visual bounding boxes that are smaller than their minimum accessible touch
    targets. When scanning UI trees or accessibility layers, agents must look
    for invisible padded click zones measuring at least 48x48dp (or 44x44pt on
    iOS).

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion design system adherence audit, evaluate detected
icon buttons against the following strict standards derived from Android Motion guidelines:

### Android Motion Component Variants & Sizing

-   **Icon Button Variants**:
    -   **Default Icon Button**: Triggers a direct action or opens a UI element
        (e.g., menu, search). Uses a filled system icon by default.
    -   **Toggle Icon Button**: Represents a binary choice (e.g., bookmark).
        Uses an outlined icon at rest and a filled icon when selected (or
        semibold/bold weight increase if no filled icon exists, ensuring
        selection is communicated through at least two properties).
-   **Visual Sizes & Dimensions**: Android Motion defines five visual size tiers:
    -   `Extra Small (XS)`: 32dp visual container.
    -   `Small (S)`: 40dp visual container (default).
    -   `Medium (M)`: 56dp visual container.
    -   `Large (L)`: 96dp visual container.
    -   `Extra Large (XL)`: 136dp visual container.
-   **Shape Morphing Specs**: Supports both round and square options.
    -   *Selection Morph*: By default, toggle buttons morph from round
        (unselected, `--droid-sys-shape-corner-full`) to square (selected).
        -   For XS (32dp) and S (40dp): unselected `--droid-sys-shape-corner-full`, selected `--droid-sys-shape-corner-small` (8px).
        -   For M (56dp): unselected `--droid-sys-shape-corner-full`, selected `--droid-sys-shape-corner-medium` (12px).
        -   For L (96dp) and XL (136dp): unselected `--droid-sys-shape-corner-full`, selected `--droid-sys-shape-corner-large` (16px).
    -   *Pressed Morph*: While pressed, buttons morph to become more square.
        Pressed corner radii: XS (`--droid-sys-shape-corner-small` / 8dp), S (`--droid-sys-shape-corner-small` / 8dp), M (`--droid-sys-shape-corner-medium` / 12dp), L (`--droid-sys-shape-corner-large` / 16dp), XL (`--droid-sys-shape-corner-large` / 16dp).
-   **Color Styles & Hierarchy**:
    1.  **Filled**: Highest emphasis, using `--droid-sys-color-primary` (light-dark(`#0b57d0`, `#a8c7fa`)) container fill and `--droid-sys-color-on-primary` (light-dark(`#ffffff`, `#062e6f`)) icon. Used sparingly for high-impact key actions.
    2.  **Tonal**: Medium emphasis, using `--droid-sys-color-secondary-container` (light-dark(`#c2e7ff`, `#004a77`)) fill and `--droid-sys-color-on-secondary-container` (light-dark(`#001d35`, `#c2e7ff`)) icon. Used for secondary actions paired with high-emphasis buttons.
    3.  **Outlined**: Medium emphasis, featuring an `--droid-sys-color-outline` (light-dark(`#747775`, `#8e918f`)) border stroke and transparent container background. Icon uses `--droid-sys-color-primary` (light-dark(`#0b57d0`, `#a8c7fa`)). Ideal for browsing card sets or secondary toolbar actions.
    4.  **Standard**: Lowest emphasis, using `--droid-sys-color-on-surface-variant` (light-dark(`#444746`, `#c4c7c5`)) icon with no container visible at rest.
-   **Density & Accessibility Rules**: Minimum 48x48dp touch target (44pt on
    iOS). Products must not apply high density by default, as doing so reduces
    touch targets below the mandatory 48dp minimum. On web, hovering an icon
    button must display a descriptive tooltip explaining the action (e.g., "Add
    to favorites") rather than the literal icon name.

### Critical Android Motion Violations to Flag

-   **Single-Property Toggle Cue**: Communicating toggle selection solely
    through color change without updating the icon fill, weight, or container
    shape.
-   **Default High Density**: Forcing dense icon button layouts by default that
    shrink interactive touch targets below 48x48dp without providing an
    accessible user override.
-   **Overusing Filled Icon Buttons**: Cluttering a screen with multiple
    prominent `Filled` icon button containers, disrupting the visual hierarchy.
