# Date Picker Rubric - Android Motion Design System

This rubric evaluates Date Pickers for visual and functional compliance with the Android Motion Design System.

## Evaluation Questions

1.  [ID: date_picker_purpose] Is the date picker presented as a calendar grid or direct text entry?
    -   *Heuristic*: Verify that calendar grids are used for near-term dates (e.g. scheduling/booking) and manual text fields are used for distant dates (e.g. Date of Birth).
    -   [Metric type: Hygiene] [Weight: 3]

2.  [ID: date_picker_modal_container_shape] Does the modal date picker container use the correct rounded corners token?
    -   *Heuristic*: The elevated dialog container must have generously rounded corners mapping to `--droid-sys-shape-corner-extra-large` (value: `28px`).
    -   [Metric type: Hygiene] [Weight: 3]

3.  [ID: date_picker_typography_family] Does the date picker utilize standard Google typefaces across its scales?
    -   *Heuristic*: Header labels and date highlights must use the bold brand typeface `Google Sans` (via `--droid-sys-typescale-headline-*` or `--droid-sys-typescale-title-*`), and calendar grid numbers/days must use the plain typeface `Google Sans Text` (via `--droid-sys-typescale-body-*` or `--droid-sys-typescale-label-*`).
    -   [Metric type: Hygiene] [Weight: 3]

4.  [ID: date_picker_keyboard_toggle_icon] Does the component provide a clear edit pencil/keyboard icon to switch to manual text input?
    -   *Heuristic*: Ensure a standard toggle icon button is visible to allow users to switch seamlessly between calendar picking and direct text entry modes.
    -   [Metric type: Hygiene] [Weight: 3]

5.  [ID: date_picker_selected_day_shape] Is the selected day indicator represented as a solid filled circle?
    -   *Heuristic*: The active selected day container must be a perfect solid circle mapping to `--droid-sys-shape-corner-full` (value: `max(50cqw, 50cqh)`).
    -   [Metric type: Hygiene] [Weight: 3]

6.  [ID: date_picker_selected_day_colors] Do selected and range-highlighted days map to the correct color roles?
    -   *Heuristic*:
        -   The selected day circle background must use `--droid-sys-color-primary` (value: `light-dark(#0b57d0, #a8c7fa)`) with number text in `--droid-sys-color-on-primary` (value: `light-dark(#ffffff, #062e6f)`).
        -   Intermediate range days must use `--droid-sys-color-primary-container` (value: `light-dark(#d3e3fd, #0842a0)`) for background and `--droid-sys-color-on-primary-container` (value: `light-dark(#041e49, #d3e3fd)`) for text.
    -   [Metric type: Compliance] [Weight: 5]

7.  [ID: date_picker_modal_backdrop_scrim] Is the dialog presented over a standard background dimming scrim?
    -   *Heuristic*: Background surfaces must be dimmed using a temporary overlay mapping to `--droid-sys-color-shadow` (at 32% opacity).
    -   [Metric type: Hygiene] [Weight: 3]

8.  [ID: date_picker_entry_motion] Does the dialog container slide and fade on screen with the correct easing curve?
    -   *Heuristic*: Modal entry must animate using the system's **Emphasized Easing set**, specifically `--droid-sys-motion-easing-emphasized` (value: `cubic-bezier(0.2, 0.0, 0.0, 1.0)`) over a transition duration of `--droid-sys-motion-duration-500` (value: `500ms`) or `--droid-sys-motion-duration-400` (value: `400ms`).
    -   [Metric type: Compliance] [Weight: 5]

9.  [ID: date_picker_mode_transition_motion] Does the switching mode animation use a clean fade-through pattern?
    -   *Heuristic*: Swapping between grid and input views must use the standard fade-through transition, utilizing `--droid-sys-motion-easing-standard` (value: `cubic-bezier(0.2, 0.0, 0.0, 1.0)`) over a duration of `--droid-sys-motion-duration-300` (value: `300ms`).
    -   [Metric type: Compliance] [Weight: 3]

10. [ID: date_picker_touch_target_size] Do all interactive day grid elements, arrow icon buttons, and confirmation action buttons meet touch targets of at least 48x48dp?
    -   *Heuristic*: Ensure all active elements meet the 48x48dp spacing requirements to maintain usability.
    -   [Metric type: Hygiene] [Weight: 3]
