# Button Groups - Universal AI Detection Guide & Android Motion Specifications

This reference provides universal visual heuristics for detecting Button Groups
(grouped interactive buttons) across any design system or platform, followed by
Android Motion specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Button group** is a linear cluster of related interactive buttons organized
within a shared container or layout arrangement to manage choices or linked
actions. Agents must detect button groups based on their distinct geometric and
structural qualities, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Placement & Arrangement**: Consistently arranged in a single horizontal
    line. Buttons within a group hug one another closely, share common dividing
    borders, or are separated by consistent narrow padding (e.g., 2dp in
    connected groups). They must move through layouts together without wrapping.
-   **Visual Boundaries & Containment**: Organizes multiple independent button
    touch targets (each with its own label or icon) inside a shared bounding
    area. This bounding area can be:
    -   A visible outer container (like a card or outline).
    -   An implicit layout zone (where related buttons are placed adjacent to
        each other to form a functional block, e.g., Volume Down and Volume Up
        buttons).
    -   A row of pill-shaped choice/category buttons where *every* item has a
        visible background container (e.g., active is filled, inactive are
        tonal) and they are clustered locally (e.g., inline with other header
        elements) rather than spanning the screen as tabs. This MUST be mapped
        to **`Button group`**.
-   **Core Anatomy**:
    -   *Child Buttons*: Individual buttons or icon buttons representing related
        actions (e.g., Back/Pause/Next) or selectable options.
    -   *Dividers / Spacing*: Uniform internal padding or thin vertical divider
        strokes separating adjacent button segments.

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion adherence audit, evaluate detected
button groups against the following strict standards derived from Android Motion
guidelines:

### Android Motion Button Group Specifications

-   **Button Group Variants**:
    -   **Standard Button Group**: Organizes related standalone actions (e.g., Back, Pause, Next). Adds interaction between adjacent buttons; selecting a button triggers dynamic shape and width morphing in both that button and adjacent buttons. Unselected buttons utilize `--droid-sys-shape-corner-full` (rounded pill); selected buttons morph to squarer profiles (e.g., `--droid-sys-shape-corner-medium` which is 12px) using `--droid-sys-motion-duration-200` (200ms) paired with `--droid-sys-motion-easing-standard`.
    -   **Connected Button Group**: Replaces the legacy Segmented Button. Used for single-select or multi-select choices. Buttons are joined or separated by consistent 2dp padding or border dividers using `--droid-sys-color-outline` (light: `#747775`, dark: `#8e918f`). Selection does not affect adjacent button shapes.
-   **Sizing & Layout Rules**:
    -   Works across standard sizes (XS, S, M, L, XL). By default, all buttons in a group must share the exact same height and shape tokens to avoid visual noise.
    -   *Single-Line Constraint*: Button groups must remain on a single line and never wrap under any layout sizing.
-   **Color & Theming**:
    -   Unselected buttons use `--droid-sys-color-surface` or outlined styles.
    -   Selected/Active items are highlighted using `--droid-sys-color-primary` (light: `#0b57d0`, dark: `#a8c7fa`) with `--droid-sys-color-on-primary` text, or `--droid-sys-color-secondary-container` (light: `#c2e7ff`, dark: `#004a77`) with `--droid-sys-color-on-secondary-container` text.
-   **Platform & Target Size Specs**:
    -   *Touch Target Requirement*: Minimum interactive tap target of 48x48dp (or 44x44pt on iOS) is required. Small button groups must specify sufficient outer padding to fulfill touch compliance.

### Critical Android Motion Violations to Flag

-   **Button Group Wrapping**: Allowing a button group to wrap onto a second
    line instead of adjusting button widths or shrinking container resizing.
-   **Legacy Segmented Buttons**: Flagging baseline segmented buttons and
    recommending the updated `Connected button group`.
-   **Shape Discrepancies**: Mixing highly rounded buttons with sharp corner buttons in standard at-rest states (violating uniformity rules).
