# Switches - Universal AI Detection Guide \& Android Motion Specifications

This reference provides universal visual heuristics for detecting Switches (standalone binary selection toggles) across any design system or platform, followed by Android Motion design system specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Switch** is a binary selection control that toggles a specific standalone setting or preference between two opposing states (e.g., On vs. Off, True vs. False) with immediate effect. Agents must detect switches based on their stadium track geometry, sliding circular handles, and binary state indicators, regardless of design system adherence.

### Key Visual \& Geometric Heuristics

-   **Stadium Track Geometry**: Look for a distinct elongated capsule, pill-shaped, or stadium container featuring fully rounded semicircular end corners.
-   **Sliding Circular Handle (Thumb)**: A prominent circular thumb or handle placed directly inside the track container that slides horizontally between the left and right boundaries.
-   **Binary State Signatures**: Look for two distinct visual state representations:
    -   *Off (Unselected / False)*: The circular handle is smaller (e.g., 16px) and positioned flush to the **left** side of the track (in LTR layouts). The track container frequently displays a transparent or neutral surface fill surrounded by a distinct border outline stroke.
    -   *On (Selected / True)*: The circular handle expands in size (e.g., 24px) and slides flush to the **right** side of the track. The track container and handle adopt a solid, high-contrast filled color.
-   **Placement \& Layout Context**: Switches are consistently positioned at the far trailing edge of a list item row (e.g., right-aligned in LTR), directly opposite an inline text label describing the setting.

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications \& Compliance Auditing

When conducting an Android Motion design system adherence audit, evaluate detected switches against the following strict standards:

### Android Motion Component Specifications \& Sizing

-   **Selection Control Semantics**:
    -   *Switches*: Mandatory for standalone, independent binary options (like Wi-Fi, Bluetooth, or dark mode settings) where changes take effect immediately without requiring a separate "Save" or "Apply" button.
    -   *Radio Buttons*: Reserved for mutually exclusive single-select lists.
    -   *Checkboxes*: Reserved for multi-select lists.
    -   *Connected Button Groups*: Required for opposing multi-view options (like Map vs. List view), where switches would be inappropriate.
-   **Dimensions \& Sizing Tokens**:
    -   **Track Dimensions**: Strictly **52px width** and **32px height**. Features fully rounded corners mapping to `--droid-sys-shape-corner-full` (`max(50cqw, 50cqh)`). Unselected tracks feature a 2px border outline stroke.
    -   **Handle (Thumb) Dimensions**:
        -   *Unselected Thumb*: **16px** diameter.
        -   *Selected Thumb* (or thumb with icon): Expands to **24px** diameter.
        -   *Pressed / Dragging Thumb*: Expands to **28px** diameter.
    -   **Touch Target**: Minimum **48px x 48px** invisible square surrounding the component.
-   **Configurations \& Icons**:
    -   Supports three configurations: without icons, icon on the selected handle only, or icons on both selected and unselected handles.
    -   *Icon Sizing*: Optional inner handle icons are 16px. Icons must be clear and unambiguous (e.g., a checkmark for On, an "X" or dash for Off).
-   **Color Roles**:
    -   *Selected State*: Track fill uses `--droid-sys-color-primary` (Light `#0b57d0` / Dark `#a8c7fa`), and the handle uses `--droid-sys-color-on-primary` (Light `#ffffff` / Dark `#062e6f`).
    -   *Unselected State*: Track fill is neutral or transparent with a 2px outline stroke using `--droid-sys-color-outline` (Light `#747775` / Dark `#8e918f`). The unselected handle uses `--droid-sys-color-outline` or `--droid-sys-color-on-surface-variant` (Light `#444746` / Dark `#c4c7c5`).
    -   *Adjacent Inline Labels*: Must use `--droid-sys-color-on-surface` (Light `#1f1f1f` / Dark `#e3e3e3`) or `--droid-sys-color-on-background`, remaining consistent during interaction.

### Android Motion Animation \& State Transitions

As the flagship component for state morphing, switches must embody the high-fidelity motion principles of the Android Motion design system:

-   **Fluid Sliding Animation**: Toggling the switch must slide the thumb smoothly across the track using the standard easing token `--droid-sys-motion-easing-standard` or the premium emphasized easing token `--droid-sys-motion-easing-emphasized` (`cubic-bezier(0.2, 0.0, 0.0, 1.0)`) over a duration of `--droid-sys-motion-duration-250` (250ms) or `--droid-sys-motion-duration-300` (300ms).
-   **Elastic Sizing \& State Morphing**: Pressing, dragging, and releasing the switch must morph the thumb size elastically:
    -   *Press/Hold*: Thumb scales from 16px (or 24px) up to 28px using `--droid-sys-motion-easing-emphasized-decelerate` over `--droid-sys-motion-duration-100` (100ms).
    -   *Slide/Release*: Thumb slides and settles into its new state (shrinking to 24px for selected, or 16px for unselected) using `--droid-sys-motion-easing-emphasized` over `--droid-sys-motion-duration-200` (200ms).
    -   *Hardcoded instant jumps* in position or size violate the core motion requirements.

### Critical Android Motion Violations to Flag

-   **Delayed Setting Application**: Requiring users to click a separate "Save" or submit button for switch toggles to take effect in standalone settings rows.
-   **Misuse for Opposing View Modes**: Using a switch to toggle between two opposing layout views (like Map vs. List view) where a `Connected button group` is required.
-   **Improper Handle Sizing \& Morphing**: Presenting a switch where the unselected and selected handles remain identical in size, failing state morphing rules.
-   **Static Toggling**: Failing to animate the slide or handle morph, which violates the motion-first principles of the Android Motion design system.
