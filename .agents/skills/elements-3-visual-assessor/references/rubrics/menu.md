# Menu

1.  [ID: menu_usage] Is the menu used for contextual options, overflow actions, or dropdown selection? [Metric type: Hygiene] [Weight: 3]

2.  [ID: menu_items_single_line] Are menu items presented on a single line without wrapping? [Metric type: Hygiene] [Weight: 3]

3.  [ID: menu_drop_shadow_elevation] Does the menu container utilize a drop shadow to elevate over underlying content? [Metric type: Hygiene] [Weight: 3]

4.  [ID: menu_container_min_width] Is the menu container width at least 112dp? [Metric type: Adherence] [Weight: 5]

5.  [ID: menu_corner_radius_filled_field] Is the corner radius of the menu container 4dp, except when the menu is positioned directly beneath a filled text field, in which case the top corner radius must be 0dp? [Metric type: Adherence] [Weight: 10]

6.  [ID: menu_item_height] Is the vertical height of each menu item exactly 48dp? [Metric type: Adherence] [Weight: 5]

7.  [ID: menu_internal_padding_spacing] Are the internal left/right paddings of the menu item exactly 12dp, and is the spacing between elements inside a single menu item row also exactly 12dp? [Metric type: Adherence] [Weight: 5]

8.  [ID: menu_divider_specs] Do menu dividers have a height of 1dp, a dynamic width, and exactly 8dp top/bottom padding? [Metric type: Adherence] [Weight: 3]

9.  [ID: menu_selection_checkmark_icon] If a menu item has a persistent selection or toggle state, does it use a checkmark icon in addition to color (satisfying GAR Non-Text Contrast) rather than relying on color alone? [Metric type: Adherence] [Weight: 10]

10. [ID: menu_no_mixed_indentation] Are all text labels in a menu group left-aligned with each other, even if some items do not have leading icons (avoiding mixed indentation)? [Metric type: Hygiene] [Weight: 5]

11. [ID: menu_no_forbidden_controls] Does the menu avoid forbidden elements such as standard buttons, search input boxes, fixed headers, fixed footers, and direct secondary interactive controls (switches, independent buttons) within item slots? [Metric type: Adherence] [Weight: 10]

12. [ID: menu_label_truncation_length] Are menu item text labels kept short (recommended under 18 characters) to avoid truncation on smaller viewports? [Metric type: Hygiene] [Weight: 3]

13. [ID: menu_disabled_item_tooltip] Do disabled menu items include a tooltip explaining why they are disabled to provide essential feedback to keyboard and screen reader users? [Metric type: Hygiene] [Weight: 5]

14. [ID: menu_multi_select_behavior] If the menu is a multi-select list, does it stay open when selections are made (only dismissing when clicking outside, or pressing ESC/TAB) and avoid containing other unrelated actions or links? [Metric type: Hygiene] [Weight: 5]
