# FABs

#### General

1.  [ID: all_fabs_only_one] Does the active screen or viewport hierarchy contain at most ONE Floating Action Button component (standard FAB or Extended FAB)? [Metric type: Hygiene] [Weight: 10]

2.  [ID: all_fabs_location] Is the FAB correctly positioned: placed within the navigation drawer above primary navigation for large viewports, within the navigation overlay/drawer for small viewports, or above the main content of the page (indented to the left, aligning with the icon of the FAB) if no navigation drawer is present? [Metric type: Hygiene] [Weight: 10]

3.  [ID: all_fabs_no_fixed] Is the FAB inside the navigation drawer NOT fixed in position (allowing it to scroll to preserve real estate for users of screen zoom/magnification)? [Metric type: Adherence] [Weight: 5]

4.  [ID: all_fabs_no_external_link] Does the FAB trigger an internal action or page navigation without opening or linking to an external window, tab, or outside website? [Metric type: Adherence] [Weight: 10]

5.  [ID: all_fabs_no_elevation_rest] Is the FAB designed with NO elevation (no drop shadow) at rest, in accordance with the Elements GM3 standard for rail/drawer integrated FABs? [Metric type: Adherence] [Weight: 5]

#### Standard FAB

1.  [ID: standard_fab_specs] If a standard FAB is present, does its container comply with the Elements GM3 specification: size 56x56dp with a corner radius of 16dp (boxier style)? Note: Small, Medium, Large, and Branded FAB sizes are not endorsed. [Metric type: Adherence] [Weight: 10]

2.  [ID: standard_fab_color_style] Does the standard FAB use either the Primary color fill or a White fill (to avoid visually competing with the product logo in the App bar)? [Metric type: Adherence] [Weight: 5]

3.  [ID: standard_fab_filled_icon] Does the standard FAB feature ONLY a centered, filled system icon from the Google Symbols font? [Metric type: Hygiene] [Weight: 3]

#### FAB with Dropdown Menu (Multiple Actions)

1.  [ID: fab_menu_dropdown_specs] If multiple actions are present, does the FAB use a dropdown menu positioned 8dp below the FAB, featuring Elevation 2 and a 4dp corner radius? [Metric type: Adherence] [Weight: 5]

2.  [ID: fab_menu_dropdown_items] Does the dropdown menu show between 2 and 6 related actions, utilizing 24dp Google Symbols icons, without mixing icon and non-icon items or using cascading submenus? [Metric type: Adherence] [Weight: 5]

#### Extended FAB

1.  [ID: extended_fab_specs] If an Extended FAB is present, does its container comply with the Elements GM3 specification: height 56dp (matching standard FAB), with a corner radius of 16dp or a fully rounded pill of 28dp, and 16dp start/end internal padding? [Metric type: Adherence] [Weight: 10]

2.  [ID: extended_fab_text_label] Does the Extended FAB have a clear text label (1-2 short action words) containing a recommended length of 12 characters and a maximum of 20 characters? [Metric type: Hygiene] [Weight: 5]

3.  [ID: extended_fab_icon_placement] If the Extended FAB includes an icon, is a filled-style Google Symbols icon placed directly to the left of the text? [Metric type: Hygiene] [Weight: 3]
