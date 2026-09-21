# Menu Rubric

#### Usage & Structural Mapping

1.  [ID: menu_usage] Is the menu used for contextual options, overflow actions, or dropdown selection? [Metric type: Hygiene] [Weight: 3]

2.  [ID: menu_parent_child_mapping] In the visual audit, is the outer temporary container mapped as `Menu` and each distinct choice mapped as a child menu item? [Metric type: Hygiene] [Weight: 5]

#### Geometry & Styling

3.  [ID: menu_items_single_line] Are menu items presented on a single line without wrapping (keeping labels short and concise)? [Metric type: Hygiene] [Weight: 3]

4.  [ID: menu_corner_radii] Does the menu container utilize correct corner shapes according to the variant: 4dp (`sys.shape.corner.extra-small` / `--md-sys-shape-corner-extra-small`) for baseline menus, and 12dp (`sys.shape.corner.medium` / `--md-sys-shape-corner-medium`) or 16dp (`sys.shape.corner.large` / `--md-sys-shape-corner-large`) for expressive vertical menus? [Metric type: Adherence] [Weight: 3]

5.  [ID: menu_drop_shadow_elevation] Does the menu container utilize a standard GM3 elevation shadow corresponding to `sys.elevation.level2` (`--md-sys-elevation-level2` / 3px) or `sys.elevation.level3` (`--md-sys-elevation-level3` / 6px) to elevate over underlying content? [Metric type: Hygiene] [Weight: 3]

#### Sizing & Content Best Practices

6.  [ID: menu_item_touch_targets] Do all menu items maintain a minimum height and touch target of at least 48dp (or 44pt on iOS) to comply with access standards? [Metric type: Hygiene] [Weight: 5]

7.  [ID: menu_no_action_stacking] Are menu item slots completely free of secondary interactive elements (such as nested buttons or switches), ensuring that each row triggers only a single, unified action? [Metric type: Adherence] [Weight: 10]

8.  [ID: menu_gaps_dividers] If gaps are used to visually divide groups, are they limited to 1 or 2 per menu and completely avoided in scrollable menus? Are horizontal dividers (1dp) correctly mapped to `sys.color.outline-variant` (`--md-sys-color-outline-variant`)? [Metric type: Adherence] [Weight: 3]

#### Interactive States & Selection

9.  [ID: menu_selection_cues] For selected menu items, does the UI apply all three of the following required cues: a shape change, a minimum 3:1 color contrast change between selected and unselected elements, and a visual icon cue like a checkmark? [Metric type: Adherence] [Weight: 10]

10. [ID: menu_trigger_pressed_state] While the menu remains expanded on the screen, does the corresponding triggering button or icon button display an active/pressed visual state? [Metric type: Adherence] [Weight: 3]

11. [ID: menu_initial_focus_position] When the menu is triggered and opens, does the keyboard or screen reader focus automatically land on the first menu item? [Metric type: Adherence] [Weight: 5]

#### Color & Token Mapping

12. [ID: menu_standard_color_mappings] For standard menus, does the container background map to `sys.color.surface-container` (`--md-sys-color-surface-container`) or `sys.color.surface-container-high` (`--md-sys-color-surface-container-high`), and do item labels map to `sys.color.on-surface` (`--md-sys-color-on-surface`)? [Metric type: Adherence] [Weight: 5]

13. [ID: menu_vibrant_color_mappings] If a vibrant menu is used for high emphasis, does the container map to `sys.color.tertiary-container` (`--md-sys-color-tertiary-container`) and text/icons map to `sys.color.on-tertiary-container` (`--md-sys-color-on-tertiary-container`)? [Metric type: Adherence] [Weight: 5]
