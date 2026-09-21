# Switches - Universal AI Detection Guide & MD3 Specifications

This reference provides universal visual heuristics for detecting Switches (standalone binary selection toggles) across any design system or platform, followed by Material Design 3 (MD3) specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Switch** is a binary selection control that toggles a specific standalone setting or preference between two opposing states (e.g., On vs. Off, True vs. False) with immediate effect. Agents must detect switches based on their stadium track geometry, sliding circular handles, and binary state indicators, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Stadium Track Geometry**: Look for a distinct elongated capsule, pill-shaped, or stadium container (featuring fully rounded semicircular end corners).
-   **Sliding Circular Handle (Thumb)**: A prominent circular thumb or handle placed directly inside the track container that slides horizontally between the left and right boundaries.
-   **Binary State Signatures**: Look for two distinct visual state representations:
    -   *Off (Unselected / False)*: The circular handle is smaller (e.g., 16dp) and positioned flush to the **left** side of the track (in LTR layouts). The track container frequently displays a transparent or neutral surface fill surrounded by a distinct border outline stroke.
    -   *On (Selected / True)*: The circular handle expands in size (e.g., 24dp) and slides flush to the **right** side of the track. The track container and handle adopt a solid, high-contrast filled color.
-   **Placement & Layout Context**: Switches are consistently positioned at the far trailing edge of a list item row (e.g., right-aligned in LTR), directly opposite an inline text label describing the setting.

---

## Part 2: MD3 Specifications & Compliance Auditing

When conducting a Material Design 3 (MD3) adherence audit, evaluate detected switches against the following strict standards derived from Material guidelines and token bindings:

### MD3 Component Specifications & Sizing

-   **Selection Control Semantics**:
    -   *Switches*: Mandatory for standalone, independent binary options (like Wi-Fi, Bluetooth, or dark mode settings) where changes take effect immediately without requiring a separate "Save" or submit button.
    -   *Radio Buttons*: Reserved for mutually exclusive single-select lists.
    -   *Checkboxes*: Reserved for multi-select lists.
    -   *Connected Button Groups*: Required for opposing layout views (like Map vs. List view), where switches would be inappropriate.
-   **Dimensions & Sizing Tokens**:
    -   **Track Dimensions**: Strictly **52dp width** and **32dp height**. Unselected tracks feature a 2dp border outline stroke.
    -   **Track Shape**: Features fully rounded corners (`--md-sys-shape-corner-full`).
    -   **Handle Dimensions**: Unselected thumb width is 16dp; Selected thumb width (or thumb with icon) expands to 24dp; Pressed thumb width expands to 28dp.
    -   **Handle Shape**: Features fully rounded circular shape (`--md-sys-shape-corner-full`).
    -   **Minimum Interactive Touch Target**: Strictly 48x48dp. Avoid applying default density that lowers touch targets below 48x48dp.
-   **Configurations & Icons**:
    -   Supports three configurations: without icons, icon on the selected handle only, or icons on both selected and unselected handles.
    -   *Icon Sizing*: Optional inner handle icons must be 16dp. Icons must be clear and unambiguous (e.g., a checkmark for On, an "X" or dash for Off).
-   **Color Roles**:
    -   Active (Selected) Track: Uses `--md-sys-color-primary` (or primary container role).
    -   Active (Selected) Handle: Uses `--md-sys-color-on-primary` (or standard active handle color).
    -   Unselected Track Border Outline: Uses `--md-sys-color-outline` (2dp width).
    -   Adjacent Inline Text Label: Must use the `--md-sys-color-on-surface` color role. This remains consistent during hover, focus, and press interactions.

### MD3 iOS Platform Considerations

-   **Branded iOS Switch**: Smaller size (28pt height), optimized and used strictly in list rows. Since list rows on iOS are fully tappable, the row interaction fulfills Apple's touch target guidelines. Outside of list rows, a toggle icon button or checkbox should be used instead of a switch. Glass effects do not apply to iOS switches since they live in the content layer.

### Critical MD3 Violations to Flag

-   **Delayed Setting Application**: Requiring users to click a separate "Save" or submit button for switch toggles to take effect in standalone settings rows.
-   **Misuse for Opposing View Modes**: Using a switch to toggle between two opposing layout views (like Map vs. List view) where a `Connected button group` or segmented button is required.
-   **Improper Handle Sizing**: Presenting a switch where the unselected and selected handles remain identical in size, failing MD3 state morphing rules.
-   **Text Labels on Switch Track**: Placing "ON" or "OFF" text labels directly inside or on the track container itself, violating Material's clean geometry guidelines.
