# Navigation Drawer

1.  [ID: navigation_drawer_modal_scrim] Do modal navigation drawers use a scrim to block interaction with underlying content? [Metric type: Hygiene] [Weight: 5]

2.  [ID: navigation_drawer_standard_anchored_leading] Is the standard navigation drawer anchored to the leading edge of the screen? [Metric type: Hygiene] [Weight: 3]

3.  [ID: navigation_drawer_modal_rounded_corners] Do modal navigation drawers have rounded corners on the outer edge? [Metric type: Hygiene] [Weight: 3]

4.  [ID: navigation_drawer_max_one_visible] Is there only one navigation drawer visible at a time? [Metric type: Hygiene] [Weight: 3]

5.  [ID: navigation_drawer_background_match] Does the navigation drawer container background use surface-container-low (light blue) or surface-container-lowest, and does it match the background colors of both the top app bar header and the main content area? [Metric type: Adherence] [Weight: 10]

6.  [ID: navigation_drawer_item_touch_targets] Do all menu items within the drawer maintain a minimum interactive touch target size of 48x48dp? [Metric type: Hygiene] [Weight: 5]

7.  [ID: navigation_drawer_contrast_active_state] Does the active drawer destination use a filled style icon (with outlined styles for inactive), or semibold icon weight if no filled style is available, and does it automatically apply a large-emphasized text weight? [Metric type: Adherence] [Weight: 10]

8.  [ID: navigation_drawer_keyboard_focus_outline] Is a prominent blue outline focus indicator visible when a navigation drawer item receives keyboard focus? [Metric type: Hygiene] [Weight: 5]

9.  [ID: navigation_drawer_tree_hierarchy_limit] If tree navigation is utilized, is the hierarchy limited to a maximum of 3 levels (Parent > Child > Child), and do all child-level pages consist of text labels only, without system icons? [Metric type: Adherence] [Weight: 5]

10. [ID: navigation_drawer_parent_split_targets] If an expandable parent item also functions as a link, is the row's interaction target split into two areas: an arrow icon for expand/collapse, and the remainder of the row for navigating to the parent link? [Metric type: Adherence] [Weight: 5]

11. [ID: navigation_drawer_fab_specs] If a FAB is present, is it placed as the first element inside the drawer, filled with the primary-container color token, used exclusively for internal product actions (no external links), and is its position NOT fixed to support magnification-scrolling? [Metric type: Adherence] [Weight: 5]

12. [ID: navigation_drawer_forbidden_section_headers] Are section headers omitted when grouping a single item, at the absolute top of the navigation menu, or inside the collapsed rail view? [Metric type: Adherence] [Weight: 3]

13. [ID: navigation_drawer_count_badge_rule] Is the numerical count badge limited to a maximum of 99+ by default, and does the item avoid combining both a numerical count badge and a secondary action? [Metric type: Hygiene] [Weight: 5]

14. [ID: navigation_drawer_collapsed_rail_elements] When the drawer is collapsed to the rail view, are child items, trailing icons, feature badges, and footers hidden, leaving only icons, parent items, count badge dots, expandable row icons, and divider lines visible? [Metric type: Adherence] [Weight: 10]

15. [ID: navigation_drawer_flyout_hover_focus] Does the collapsed rail expand temporarily into the full navigation drawer overlay (flyout behavior) when any contained element (including the FAB) receives hover or keyboard focus, collapsing back when focus/hover leaves? [Metric type: Adherence] [Weight: 10]

16. [ID: navigation_drawer_compact_overlay_dimensions] At viewports smaller than 571dp, does the drawer collapse entirely into a temporary overlay panel with a fixed width of 304dp and a row height of 48dp? [Metric type: Adherence] [Weight: 5]
