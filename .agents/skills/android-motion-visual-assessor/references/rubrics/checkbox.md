# Checkbox Rubric

1.  [ID: checkbox_usage] Are checkboxes used for multi-select options or toggles requiring a "Submit" or saving step, rather than standalone instantaneous settings? [Metric type: Hygiene] [Weight: 3]

2.  [ID: checkbox_square_corner_radius] Are checkboxes shaped as 18px x 18px squares with a 4px corner radius mapping to `--droid-sys-shape-corner-extra-small` (4px)? [Metric type: Hygiene] [Weight: 3]

3.  [ID: checkbox_checkmark_visibility] Is the checkmark or dash icon only visible when the checkbox is in the selected or indeterminate state? [Metric type: Hygiene] [Weight: 3]

4.  [ID: checkbox_no_drop_shadow] Does the checkbox component stay flat without using custom drop shadows or elevation, maintaining `--droid-sys-elevation-level0`? [Metric type: Hygiene] [Weight: 3]

5.  [ID: checkbox_color_adherence] When selected, does the container fill use `--droid-sys-color-primary` (Light: `#0b57d0`, Dark: `#a8c7fa`) with the checkmark in `--droid-sys-color-on-primary` (Light: `#ffffff`, Dark: `#062e6f`); and when unselected, does the container outline use `--droid-sys-color-outline` (Light: `#747775`, Dark: `#8e918f`)? [Metric type: Adherence] [Weight: 3]

6.  [ID: checkbox_touch_target] Does the checkbox preserve an invisible interactive touch target of at least 48px x 48px centered over the visual container? [Metric type: Hygiene] [Weight: 3]

7.  [ID: checkbox_selection_motion] Does the checkmark draw dynamically and the container fill transition smoothly using `--droid-sys-motion-easing-standard` over `--droid-sys-motion-duration-150` (150ms) or `--droid-sys-motion-duration-200` (200ms) during selection, and collapse over `--droid-sys-motion-duration-100` (100ms) with decelerate or standard-accelerate easing during deselection? [Metric type: Adherence] [Weight: 3]
