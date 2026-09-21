# Menu

1.  [ID: menu_usage] Is the menu used for contextual options, overflow actions,
    or dropdown selection? [Metric type: Hygiene] [Weight: 3]

2.  [ID: menu_items_single_line] Are menu items presented on a single line
    without wrapping? [Metric type: Hygiene] [Weight: 3]

3.  [ID: menu_drop_shadow_elevation] Does the menu container utilize a drop
    shadow (shadow color `--droid-sys-color-shadow`) and elevation level `--droid-sys-elevation-level2` or `--droid-sys-elevation-level3` to elevate over underlying content? [Metric type: Hygiene] [Weight: 3]

4.  [ID: menu_item_touch_target] Do menu items maintain a minimum height and touch target of 48x48dp? [Metric type: Hygiene] [Weight: 5]

5.  [ID: menu_container_shape] Does the menu container have a corner radius conforming to `--droid-sys-shape-corner-extra-small` (4px), `--droid-sys-shape-corner-small` (8px), or `--droid-sys-shape-corner-medium` (12px)? [Metric type: Adherence] [Weight: 3]

6.  [ID: menu_container_color] Is the menu container background styled with `--droid-sys-color-surface`? [Metric type: Adherence] [Weight: 3]

7.  [ID: menu_item_typography] Do menu item labels use the font family `'Google Sans Text'` matching `--droid-sys-typescale-label-large` or `--droid-sys-typescale-body-large`? [Metric type: Adherence] [Weight: 5]

8.  [ID: menu_item_colors] Do menu item texts use `--droid-sys-color-on-surface` and leading/trailing icons use `--droid-sys-color-on-surface-variant`? [Metric type: Adherence] [Weight: 3]

9.  [ID: menu_dividers_styling] Do optional horizontal dividers within the menu have a 1dp thickness and use `--droid-sys-color-outline` or `--droid-sys-color-surface-variant`? [Metric type: Adherence] [Weight: 3]
