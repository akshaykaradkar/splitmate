# Time Pickers - Android Motion Design System Guidelines & Specifications

This reference provides universal visual heuristics for detecting Time Pickers
(analog dial or digital input time selection modals) across any application,
followed by Android Motion Design System specifications for compliance auditing.

## Part 1: Universal AI Detection Heuristics

A **Time picker** is an interactive modal component that allows users to select
and set a specific time value (hours and minutes). Agents must detect time
pickers based on circular clock face dials, prominent digital input fields,
AM/PM selectors, and mode toggles, regardless of design system adherence.

### Key Visual & Geometric Heuristics

-   **Modal Containment & Scrim**: Look for an elevated rectangular container
    box featuring rounded corners appearing over a darkened, dimming background
    overlay (`scrim`).
-   **Variant Signatures**: Detect two primary visual configurations:
    -   **Dial Picker (Clock Face)**: A prominent circular clock face displaying
        numbers around the perimeter, featuring a center-origin radial pointer
        line connecting to the active time selection. (12-hour format displays
        numbers 1-12 in a single ring; 24-hour format displays concentric inner
        and outer rings).
    -   **Input Picker (Digital Fields)**: A pair of large, prominent digital
        number input boxes representing hours and minutes (frequently separated
        by a colon), featuring thick borders or highlights identifying the
        active field.
-   **AM/PM & Mode Toggles**:
    -   *AM/PM Selector*: A vertical or segmented grouping of two options (AM
        vs. PM) accompanying 12-hour pickers.
    -   *Mode Toggles**: Look for a small keyboard icon (in Dial view) or clock
        icon (in Input view) positioned at the bottom-left or top-right of the
        container card, allowing users to toggle between input modes.
-   **Action Buttons Lockup**: Pinned "Cancel" and "OK" text buttons aligned to
    the bottom trailing edge of the dialog container.

--------------------------------------------------------------------------------

## Part 2: Android Motion Specifications & Compliance Auditing

When conducting an Android Motion Design System adherence audit, evaluate detected
time pickers against the following strict standards:

### Android Motion Visual, Color & Typographic Rules

-   **Typography Scale**:
    -   Header labels and large digital numbers in input fields must resolve to the bold brand typeface: `Google Sans`. Large digital numbers must resolve to `--droid-sys-typescale-display-medium` (2.8rem, 'Google Sans') or `--droid-sys-typescale-display-small` (2.3rem, 'Google Sans').
    -   Clock face numbers, labels, and AM/PM options must use the plain typeface: `Google Sans Text`. Specifically, clock dial numbers must map to `--droid-sys-typescale-body-large` (1rem, 'Google Sans Text') or `--droid-sys-typescale-title-medium` (1rem, 'Google Sans Text').
    -   Helper labels below fields (e.g. "Hour", "Minute") must use `--droid-sys-typescale-label-medium` (0.8rem, 'Google Sans Text').
-   **Color Mappings**:
    -   *Dialog Container*: Uses `--droid-sys-color-surface` (light-dark(#fdfcfb, #1f1f1f)).
    -   *Scrim Backdrop*: Uses `--droid-sys-color-shadow` or standard scrim color role with a 32% opacity overlay to dim background content.
    -   *Active Input Highlight*: The active digital text box container is filled with `--droid-sys-color-primary-container` (light-dark(#d3e3fd, #0842a0)) with content/text in `--droid-sys-color-on-primary-container` (light-dark(#041e49, #d3e3fd)).
    -   *Inactive Input Box*: Uses `--droid-sys-color-surface-variant` (light-dark(#e1e3e1, #444746)) with borders in `--droid-sys-color-outline` (light-dark(#747775, #8e918f)).
    -   *Dial Selector Track & Hand*: Uses `--droid-sys-color-primary` (light-dark(#0b57d0, #a8c7fa)) for the radial pointer arm and the selected number's circular background, with the selected number text in `--droid-sys-color-on-primary` (light-dark(#ffffff, #062e6f)).
    -   *AM/PM Selector*: Uses a segmented container mapped to `--droid-sys-color-outline` with the active option highlighted in `--droid-sys-color-tertiary-container` (light-dark(#c4eed0, #0f5223)) and `--droid-sys-color-on-tertiary-container` (light-dark(#072711, #c4eed0)).
-   **Container Shape**:
    -   *Modal Container*: Uses a generously rounded rectangular container with corner radius mapping to `--droid-sys-shape-corner-extra-large` (28px).
    -   *Digital Input Boxes*: Must have rounded corners mapping to `--droid-sys-shape-corner-small` (8px).
    -   *Dial Pin/Pointer Tip*: Centered pointer circle must map to `--droid-sys-shape-corner-full` (max(50cqw, 50cqh)).
-   **Touch Targets & Sizing**:
    -   All interactive points on the circular dial (number indicators) and standard text buttons (OK, Cancel, Mode Toggle) must be at least **48x48dp** to ensure accessible hand interaction.

### Android Motion Dynamics & Transition Rules

-   **Appearing Transition**:
    -   Like other high-priority dialogs, the time picker must use the system's **Emphasized Easing set**, expanding and fading in with `--droid-sys-motion-easing-emphasized` (cubic-bezier(0.2, 0.0, 0.0, 1.0)) over a duration of `--droid-sys-motion-duration-500` (500ms) or `--droid-sys-motion-duration-400` (400ms).
-   **Inter-Mode Morphing**:
    -   When toggling between dial and manual input views via the keyboard/clock icon button, the system should smoothly transition the components using the standard fade-through pattern, utilizing `--droid-sys-motion-easing-standard` over a duration of `--droid-sys-motion-duration-300` (300ms).
-   **Radial Drag Motion**:
    -   Interacting with the clock dial pointer should feel fluid and responsive, using standard spring-based mechanics to track drag gestures in real-time, snapping to the nearest hour or five-minute increment when released.

### Critical Android Motion Violations to Flag

-   **Missing Mode Toggle**: Failing to provide a keyboard/clock icon button to switch between circular clock dial selection and manual digital keyboard entry, which violates accessibility standards.
-   **Undersized Dial Touch Targets**: Shrinking clock face numbers or touch targets below the mandatory 48x48dp minimum.
-   **Missing Scrim Overlay**: Rendering a time picker modal dialog without a background dimming scrim overlay mapping to `--droid-sys-color-shadow` (32% opacity).
-   **Incorrect Typography/Fonts**: Displaying display numbers or button labels in a generic sans-serif font instead of the required `Google Sans` (for displays/headers) and `Google Sans Text` (for labels).
-   **Stiff / Linear Animation**: Animating modal popup transitions using a linear curve instead of the organic `--droid-sys-motion-easing-emphasized` easing set.
