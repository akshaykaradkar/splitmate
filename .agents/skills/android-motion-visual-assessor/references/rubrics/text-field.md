# Text Field Rubric

1.  [ID: text_field_active_outline_thickness] Do active text fields display a clear, distinct outline or bottom indicator line that is 2px thick and styled with `--droid-sys-color-primary` (Light: `#0b57d0`, Dark: `#a8c7fa`)? [Metric type: Hygiene] [Weight: 3]

2.  [ID: text_field_helper_error_placement] Is helper and error text placed cleanly exactly 4px below the input field container, styled with `--droid-sys-typescale-body-small` (`0.8rem/1rem 'Google Sans Text'`)? [Metric type: Hygiene] [Weight: 1]

3.  [ID: text_field_error_states_color] Do error states utilize a distinct error color role `--droid-sys-color-error` (Light: `#b3261e`, Dark: `#f2b8b5`) for borders, text, and trailing error icons? [Metric type: Hygiene] [Weight: 3]

4.  [ID: text_field_dimensions_shape] Do standard text fields have a visual container height of 56px and, if outlined, use a 4px corner radius matching `--droid-sys-shape-corner-extra-small`? [Metric type: Adherence] [Weight: 3]

5.  [ID: text_field_resting_colors] Do resting filled text fields utilize `--droid-sys-color-tertiary-container` (Light: `#c4eed0`, Dark: `#0f5223`) for their background fill, and resting outlined text fields utilize `--droid-sys-color-outline` (Light: `#747775`, Dark: `#8e918f`) for their 1px border stroke? [Metric type: Adherence] [Weight: 3]

6.  [ID: text_field_transition_motion] Do label floating transitions and focus indicator highlights animate smoothly using standard easing `--droid-sys-motion-easing-standard` (`cubic-bezier(0.2, 0.0, 0.0, 1.0)`) or emphasized easing `--droid-sys-motion-easing-emphasized` (`cubic-bezier(0.2, 0.0, 0.0, 1.0)`) over a duration of `--droid-sys-motion-duration-150` (150ms) or `--droid-sys-motion-duration-200` (200ms)? [Metric type: Adherence] [Weight: 3]
