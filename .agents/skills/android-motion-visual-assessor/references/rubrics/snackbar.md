# Snackbar Rubric - Android Motion Compliance

1.  [ID: snackbar_purpose] Are snackbars used to display brief, low-importance updates at the bottom of the screen? [Metric type: Hygiene] [Weight: 3]

2.  [ID: snackbar_max_one_visible] Is there at most one snackbar visible on the screen at any time? [Metric type: Hygiene] [Weight: 3]

3.  [ID: snackbar_max_single_action] Does the snackbar contain at most a single action text button? [Metric type: Hygiene] [Weight: 3]

4.  [ID: snackbar_color_roles_check] Does the snackbar container use `--droid-sys-color-inverse-surface` (light: `#303030`, dark: `#e3e3e3`) for its background fill and `--droid-sys-color-inverse-on-surface` (light: `#f2f2f2`, dark: `#303030`) for body text? [Requires Code Inspection] [Metric type: Adherence] [Weight: 5]

5.  [ID: snackbar_action_button_color] If an action button is present, does its text color map to `--droid-sys-color-inverse-primary` (light: `#a8c7fa`, dark: `#0b57d0`)? [Requires Code Inspection] [Metric type: Adherence] [Weight: 5]

6.  [ID: snackbar_shape_token_check] Does the snackbar container utilize the `--droid-sys-shape-corner-extra-small` (4px) shape token? [Requires Code Inspection] [Metric type: Adherence] [Weight: 3]

7.  [ID: snackbar_typography_check] Is the snackbar body text styled with `--droid-sys-typescale-body-medium` (`0.9rem/1.3rem 'Google Sans Text'`) and the action button styled with `--droid-sys-typescale-label-large` (`500 0.9rem/1.3rem 'Google Sans Text'`)? [Requires Code Inspection] [Metric type: Adherence] [Weight: 3]

8.  [ID: snackbar_elevation_check] Is the snackbar elevated to `--droid-sys-elevation-level3` (6px) or `--droid-sys-elevation-level2` (3px) at rest? [Requires Code Inspection] [Metric type: Adherence] [Weight: 3]

9.  [ID: snackbar_motion_timing_check] Does the entry motion utilize `--droid-sys-motion-duration-150` (150ms) to `--droid-sys-motion-duration-250` (250ms) paired with `--droid-sys-motion-easing-emphasized-decelerate` (`cubic-bezier(0.05, 0.7, 0.1, 1.0)`)? [Requires Code Inspection] [Metric type: Adherence] [Weight: 5]
