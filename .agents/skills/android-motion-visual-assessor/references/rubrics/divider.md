# Divider Rubric - Android Motion Compliance

1.  [ID: divider_usage] Are dividers used to separate distinct list items or group sections in menus? [Metric type: Hygiene] [Weight: 3]

2.  [ID: divider_stroke_thickness] Are dividers simple, single lines with a subtle thickness of strictly 1dp or 1px? [Metric type: Hygiene] [Weight: 1]

3.  [ID: divider_no_drop_shadows] Do dividers remain completely flat with no drop shadows, utilizing `--droid-sys-elevation-level0`? [Metric type: Hygiene] [Weight: 3]

4.  [ID: divider_color_token_check] Does the divider use the standard `--droid-sys-color-outline` (light: `#747775`, dark: `#8e918f`) or `--droid-sys-color-surface-variant` (light: `#e1e3e1`, dark: `#444746`) token? [Requires Code Inspection] [Metric type: Adherence] [Weight: 5]

5.  [ID: divider_inset_margins] If configured as an Inset Divider, does it have a 16dp left margin (or 16dp left and right margin for Middle-Inset)? [Requires Code Inspection] [Metric type: Adherence] [Weight: 3]
