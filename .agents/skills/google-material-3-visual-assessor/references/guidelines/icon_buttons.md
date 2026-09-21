# Icon Buttons - Google Material 3 Spec \& Compliance Guide

This guide provides the necessary guidelines and specifications to evaluate the implementation of Icon Buttons in Google Material 3 (GM3). It is designed to be used by an LLM visual assessor to detect icon button components, inspect styling in code, and audit compliance with Material Design 3 guidelines.

## Part 1: Universal Visual Detection Heuristics

An **Icon button** is a compact interactive control featuring a standalone central icon without accompanying label text. They allow users to execute common actions (e.g., bookmarking, filtering, opening menus) with a single tap. Visual assessors must detect icon buttons based on their central icon isolation, container styling, and padded touch zones, regardless of custom styling or platform.

### Key Visual \& Geometric Heuristics

-   **Central Icon Isolation**: Look for a single, centered system icon (e.g., magnifying glass, heart, bookmark, three dots) with absolutely no accompanying text label inside the control boundary.
-   **Container Styling \& Visual Boundaries**: Detect four primary containment configurations:
    -   *Filled*: A solid colored background shape (circle or rounded square) enclosing the icon, utilizing highest emphasis colors.
    -   *Tonal*: A medium-emphasis colored background container utilizing recessive color tones.
    -   *Outlined*: A transparent background container surrounded by a distinct border stroke.
    -   *Standard (Uncontained)*: Lacking any visible container at rest; the container and state layer become visible only upon hover, focus, or press.
-   **Toggle State Signatures**: Toggle icon buttons represent binary states (e.g., favorited vs. unfavorited). Look for distinct visual state changes:
    -   *Style Shift*: Outlined icon vector (unselected) switching to a solid filled icon vector (selected).
    -   *Weight Shift*: Regular stroke weight (unselected) switching to bold/semibold stroke weight (selected) as a fallback when no filled vector exists.
    -   *Shape Morphing*: Container morphing from round (unselected) to square (selected) by default.
-   **Interactive Area \& Padded Touch Targets**: Smaller visual variants (XS/S) have visual bounding boxes smaller than their minimum accessible touch targets. When scanning UI trees or accessibility layers, look for invisible padded click zones measuring at least 48x48dp (or 44x44pt on iOS).

---

## Part 2: GM3 Specifications \& Compliance Auditing

When conducting a Google Material 3 adherence audit, evaluate detected icon buttons against the following strict specifications:

### 1. Component Variants \& Color Tokens

GM3 icon buttons are categorized into four styles, mapped to the following token bindings:

|Style|Element|Default State|Toggle Unselected State|Toggle Selected State|
|:---|:---|:---|:---|:---|
|**Filled**|Container<br>Icon|`sys.color.primary-container`<br>`sys.color.on-primary-container`|`sys.color.surface-container-highest`<br>`sys.color.primary`|`sys.color.primary`<br>`sys.color.on-primary`|
|**Tonal**|Container<br>Icon|`sys.color.secondary-container`<br>`sys.color.on-secondary-container`|`sys.color.surface-container-highest`<br>`sys.color.on-surface-variant`|`sys.color.secondary-container`<br>`sys.color.on-secondary-container`|
|**Outlined**|Container<br>Outline<br>Icon|Transparent<br>`sys.color.outline-variant`<br>`sys.color.on-surface-variant`|Transparent<br>`sys.color.outline-variant`<br>`sys.color.on-surface-variant`|`sys.color.inverse-surface`<br>None<br>`sys.color.inverse-on-surface`|
|**Standard**|Container<br>Icon|Transparent<br>`sys.color.on-surface-variant`|Transparent<br>`sys.color.on-surface-variant`|Transparent<br>`sys.color.primary`|

*Note: In CSS, these resolve to `--md-sys-color-primary`, `--md-sys-color-primary-container`, `--md-sys-color-surface-container-highest`, `--md-sys-color-outline-variant`, etc.*

### 2. Sizing \& Geometry Specifications

GM3 Expressive defines five size tiers for visual containers and targets:

-   **Extra Small (XS)**: 32dp container. Interactive touch target **MUST** be padded to at least 48x48dp (44x44pt on iOS) via `--md-sys-measurement-space600` (48px).
-   **Small (S / Default)**: 40dp container. Interactive touch target **MUST** be padded to at least 48x48dp (44x44pt on iOS).
-   **Medium (M)**: 56dp container. Touch target is 56x56dp (matches `--md-sys-measurement-space700`).
-   **Large (L)**: 96dp container. Touch target is 96x96dp.
-   **Extra Large (XL)**: 136dp container. Touch target is 136x136dp.

#### iOS Specific Deviations:

-   The default Small (S) icon button has a container height of 44pt to align with standard iOS cell heights.
-   XS icon button target size must be at least 44x44pt.

### 3. Shape Morphing Rules

GM3 utilizes shape morphing to communicate state changes and selection:

-   **Selection Morph**: By default, toggle buttons morph from a fully round shape (`--md-sys-shape-corner-full`) when unselected, to a square shape with rounded corners when selected. If the unselected container is square, the selected container must morph to round.
    -   *Round Container (Default)*: Mapped to `sys.shape.corner.full` (`--md-sys-shape-corner-full`).
    -   *Square Container*: XS (12dp / `sys.shape.corner.medium`), S (12dp / `sys.shape.corner.medium`), M (16dp / `sys.shape.corner.large`), L (28dp / `sys.shape.corner.extra-large`), XL (28dp / `sys.shape.corner.extra-large`).
-   **Pressed Morph**: While actively pressed, containers morph to become boxier. Pressed corner radii must be:
    -   XS: 8dp (`sys.shape.corner.small` / `--md-sys-shape-corner-small`)
    -   S: 8dp (`sys.shape.corner.small` / `--md-sys-shape-corner-small`)
    -   M: 12dp (`sys.shape.corner.medium` / `--md-sys-shape-corner-medium`)
    -   L: 16dp (`sys.shape.corner.large` / `--md-sys-shape-corner-large`)
    -   XL: 16dp (`sys.shape.corner.large` / `--md-sys-shape-corner-large`)

### 4. Accessibility \& Density Rules

-   **Interactive Touch Target**: Minimum accessible touch target is 48x48dp on Android/Web, and 44x44pt on iOS. Density must **NOT** be applied by default as it shrinks targets below these minimums.
-   **Hover Tooltips**: On desktop/Web, hovering over an icon button must display a plain tooltip (`sys.color.inverse-surface` container and `sys.color.inverse-on-surface` label) describing the action (e.g., "Add to favorites"), rather than the literal symbol name (e.g., "favorite").
-   **Multi-Property Toggle Cues**: Selected states in toggle buttons must be conveyed through at least two visual properties (e.g., color container change + icon change from outlined to filled, or color + icon weight shift to semibold/bold if no filled glyph is available).

### Critical GM3 Violations to Flag

1.  **Single-Property Selection Cue**: Communicating a toggle button's selected state solely through color changes, without modifying the icon (outlined vs. filled), changing the icon weight, or changing the container shape (round vs. square).
2.  **Compact Touch Targets**: Visual layouts where XS or S icon buttons are not padded to the mandatory 48x48dp target (44x44pt on iOS), restricting access for assistive touch or keyboard navigation.
3.  **Mismatched State Layer Color**: Standard or uncontained icon buttons where the hover/focused/pressed state layer color deviates from the icon glyph's color (the state layer must match the color of the active content overlay).
4.  **Literal Tooltip Labels**: Displaying tooltips that label the button with the raw icon metadata (e.g., "bookmark\_border") rather than the functional action (e.g., "Save bookmark").
