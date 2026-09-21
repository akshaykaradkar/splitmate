# Time Picker Rubric - Android Motion Design System

This rubric evaluates Time Pickers for visual and functional compliance with the Android Motion Design System.

## Evaluation Questions

1.  [ID: time_picker_purpose] Is the time picker presented as a clear clock dial or explicit digital text input?
    -   *Heuristic*: Verify that either the analog clock dial view or digital hours/minutes fields are presented inside an elevated modal container.
    -   [Metric type: Hygiene] [Weight: 3]

2.  [ID: time_picker_mode_toggle_button] Does the component support toggling between dial and input modes?
    -   *Heuristic*: Check for a small keyboard icon (in dial view) or clock icon (in input view) allowing users to switch entry methods.
    -   [Metric type: Hygiene] [Weight: 3]

3.  [ID: time_picker_container_corner_radius] Does the time picker container utilize the correct rounded corners token?
    -   *Heuristic*: The modal card must feature generously rounded corners mapping to `--droid-sys-shape-corner-extra-large` (value: `28px`).
    -   [Metric type: Hygiene] [Weight: 3]

4.  [ID: time_picker_typography_hierarchy] Does the time picker utilize standard Google typefaces across its scales?
    -   *Heuristic*: Display digital numbers must use the bold brand typeface `Google Sans` (via display tokens, value: `--droid-sys-typescale-display-medium` or `display-small`). Standard text labels and dial numbers must use the plain typeface `Google Sans Text` (via `--droid-sys-typescale-body-*` or `--droid-sys-typescale-label-*`).
    -   [Metric type: Hygiene] [Weight: 3]

5.  [ID: time_picker_active_input_color] Do active digital field containers use the standout color role?
    -   *Heuristic*: The focused hours or minutes field container must be filled with `--droid-sys-color-primary-container` (value: `light-dark(#d3e3fd, #0842a0)`) with content/text in `--droid-sys-color-on-primary-container` (value: `light-dark(#041e49, #d3e3fd)`).
    -   [Metric type: Compliance] [Weight: 5]

6.  [ID: time_picker_inactive_input_color] Do inactive digital field containers map to the lower-emphasis color roles?
    -   *Heuristic*: Inactive input containers must use a neutral fill mapping to `--droid-sys-color-surface-variant` (value: `light-dark(#e1e3e1, #444746)`) with borders in `--droid-sys-color-outline` (value: `light-dark(#747775, #8e918f)`).
    -   [Metric type: Compliance] [Weight: 3]

7.  [ID: time_picker_dial_pointer_color] Does the circular dial selector track and hand use the correct color mappings?
    -   *Heuristic*: The radial pointer arm and the selected number's background circle must use `--droid-sys-color-primary` (value: `light-dark(#0b57d0, #a8c7fa)`) with selected text in `--droid-sys-color-on-primary` (value: `light-dark(#ffffff, #062e6f)`).
    -   [Metric type: Compliance] [Weight: 5]

8.  [ID: time_picker_scrim_backdrop] Is the dialog presented over a standard background dimming scrim?
    -   *Heuristic*: Verify that a background dimming scrim mapping to `--droid-sys-color-shadow` (at 32% opacity) covers the main content.
    -   [Metric type: Hygiene] [Weight: 3]

9.  [ID: time_picker_entry_motion] Does the dialog container animate on screen with the correct easing curve?
    -   *Heuristic*: Modal entry must animate using the system's **Emphasized Easing set**, specifically `--droid-sys-motion-easing-emphasized` (value: `cubic-bezier(0.2, 0.0, 0.0, 1.0)`) over a transition duration of `--droid-sys-motion-duration-500` (value: `500ms`) or `--droid-sys-motion-duration-400` (value: `400ms`).
    -   [Metric type: Compliance] [Weight: 5]

10. [ID: time_picker_touch_targets] Do all dial number indicators and action text buttons meet the minimum touch target of 48x48dp?
    -   *Heuristic*: Ensure dial numbers and trailing action buttons have standard touch targets of 48x48dp or larger.
    -   [Metric type: Hygiene] [Weight: 3]
