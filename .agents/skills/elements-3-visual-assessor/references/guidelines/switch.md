# Switches - Universal AI Detection Guide & Elements GM3 Specifications

This reference provides universal visual heuristics for detecting Switches (standalone binary selection toggles) across any design system or platform, followed by Elements GM3 specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Switch** is a binary selection control that toggles a specific standalone setting or preference between two opposing states (e.g., On vs. Off, True vs. False) with immediate effect. Agents must detect switches based on their stadium track geometry, sliding circular handles, and binary state indicators, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Stadium Track Geometry**: Look for a distinct elongated capsule, pill-shaped, or stadium container (featuring fully rounded semicircular end corners).
-   **Sliding Circular Handle (Thumb)**: A prominent circular thumb or handle placed directly inside the track container that slides horizontally between the left and right boundaries.
-   **Binary State Signatures**: Look for two distinct visual state representations:
    -   *Off (Unselected / False)*: The circular handle is smaller and positioned flush to the **left** side of the track (in LTR layouts). The track container displays a transparent or neutral surface fill surrounded by a distinct border outline stroke.
    -   *On (Selected / True)*: The circular handle expands in size and slides flush to the **right** side of the track. The track container and handle adopt a solid, high-contrast filled color.
-   **Placement & Layout Context**: Switches are consistently positioned at the far trailing edge of a list item row (e.g., right-aligned in LTR), directly opposite an inline text label describing the setting.

--------------------------------------------------------------------------------

## Part 2: Elements GM3 Specifications & Compliance Auditing

When conducting an Elements GM3 adherence audit, evaluate detected switches against the following strict standards derived from Elements GM3 guidelines:

### Elements GM3 Component Specifications & Sizing

-   **Selection Control Semantics**:
    -   *Switches*: Mandatory for standalone, independent binary options (such as Wi-Fi, Bluetooth, or theme toggles) where changes take effect immediately without requiring a separate "Save" or "Apply" button.
    -   *Radio Buttons*: Reserved for mutually exclusive single-select lists.
    -   *Checkboxes*: Reserved for multi-select lists.
-   **Dimensions & Sizing Tokens**:
    -   **Track Dimensions**: Strictly **52dp width** and **32dp height**. Features fully rounded corners (`--cee3-sys-shape-corner-full` or equivalent). Unselected tracks feature a 2dp border outline stroke.
    -   **Handle (Thumb) Dimensions**: Unselected thumb width is 16dp; Selected thumb width expands to 24dp (with or without an optional icon); Pressed thumb width expands to 28dp.
    -   **Touch Targets**: The minimum accessible touch target is **48x48dp**.
-   **Configurations & Icons**:
    -   Supports optional icons within the switch handle (e.g., a checkmark for the selected state or a dash for unselected) utilizing `--md-icon-font` (`'Google Symbols'`).
-   **Color Roles & Typography**:
    -   Active (On) switches utilize primary color roles like `--cee3-sys-color-extended-blue` for the filled track and handle.
    -   Adjacent inline text labels must use the `On Surface` color role (such as `--cee3-sys-color-extended-on-grey` or standard on-surface neutral text) and remain consistent during interaction.
-   **Labels & Formatting**:
    -   **No Direct Label**: The switch track or handle must **never** contain text labels (like "ON" or "OFF") directly on the component itself. The visual state of the switch alone must be sufficient.
    -   **Switch Text Labels**: Paired text labels should be short, direct, and written in sentence case. Avoid creating a label that includes the text "on" or "off" (e.g., use "Dark theme", not "Dark theme on/off").
-   **Viewport Adaptability**:
    -   *Small Viewports (Mobile/Tablet)*: Switches are preferred to toggle single options on/off in mobile environments.
    -   *Large Viewports (Desktop)*: Use switches with caution in desktop layouts; checkboxes or radio buttons are preferred if space is constrained.

### Critical Elements GM3 Violations to Flag

-   **Direct Text Labels on Switch**: Placing "ON", "OFF", "I", or "O" text labels directly inside the switch track or on the handle.
-   **Delayed Application**: Requiring a separate "Save" or "Submit" button to apply switch toggles instead of activating the setting immediately.
-   **Improper Handle Sizing (No Morphing)**: Keeping the unselected and selected handle widths identical (e.g., both 16dp), failing the Elements GM3 state-dependent morphing rules.
-   **Undersized Touch Targets**: Shrinking the interaction bounds below 48x48dp.
