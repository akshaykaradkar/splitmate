# Tooltip Rubric - Android Motion Compliance

1.  [ID: tooltip_usage] Is the tooltip used appropriately for simple descriptions (plain) or detailed feature explanations (rich)? [Metric type: Hygiene] [Weight: 3]

2.  [ID: tooltip_plain_no_interactive] Do plain tooltips remain completely non-interactive (no buttons or links)? [Metric type: Hygiene] [Weight: 3]

3.  [ID: tooltip_rich_max_buttons] Do rich tooltips contain a maximum of 2 buttons? [Metric type: Hygiene] [Weight: 3]

4.  [ID: tooltip_obscure_focus_indicators] Do tooltips appear without permanently blocking adjacent focus indicators or critical UI? [Metric type: Hygiene] [Weight: 3]

5.  [ID: tooltip_plain_spec_check] Do plain tooltips adhere to the height specification of strictly 24dp with 8dp of padding on all sides? [Requires Code Inspection] [Metric type: Adherence] [Weight: 3]

6.  [ID: tooltip_plain_color_tokens] Do plain tooltips use the `--droid-sys-color-inverse-surface` (light: `#303030`, dark: `#e3e3e3`) background fill and `--droid-sys-color-inverse-on-surface` (light: `#f2f2f2`, dark: `#303030`) text color tokens? [Requires Code Inspection] [Metric type: Adherence] [Weight: 5]

7.  [ID: tooltip_plain_shape_token] Do plain tooltips apply the `--droid-sys-shape-corner-extra-small` (4px) shape token? [Requires Code Inspection] [Metric type: Adherence] [Weight: 3]

8.  [ID: tooltip_rich_color_tokens] Do rich tooltips use `--droid-sys-color-surface-variant` (light: `#e1e3e1`, dark: `#444746`) or `--droid-sys-color-surface` (light: `#fdfcfb`, dark: `#1f1f1f`) paired with `--droid-sys-color-on-surface` (light: `#1f1f1f`, dark: `#e3e3e3`) text color tokens? [Requires Code Inspection] [Metric type: Adherence] [Weight: 5]

9.  [ID: tooltip_rich_shape_token] Do rich tooltips apply the `--droid-sys-shape-corner-medium` (12px) shape token or larger? [Requires Code Inspection] [Metric type: Adherence] [Weight: 3]

10. [ID: tooltip_typography_check] Is plain tooltip text styled with `--droid-sys-typescale-body-small` (`0.8rem/1rem 'Google Sans Text'`) and are rich tooltip titles/body styled with `--droid-sys-typescale-title-small` (`500 0.9rem/1.3rem 'Google Sans Text'`) and `--droid-sys-typescale-body-medium` (`0.9rem/1.3rem 'Google Sans Text'`) respectively? [Requires Code Inspection] [Metric type: Adherence] [Weight: 3]

11. [ID: tooltip_motion_check] Do tooltips show smooth transitions using `--droid-sys-motion-duration-150` (150ms) for plain or `--droid-sys-motion-duration-200` (200ms) for rich, paired with `--droid-sys-motion-easing-emphasized-decelerate` (`cubic-bezier(0.05, 0.7, 0.1, 1.0)`) on entrance? [Requires Code Inspection] [Metric type: Adherence] [Weight: 5]
