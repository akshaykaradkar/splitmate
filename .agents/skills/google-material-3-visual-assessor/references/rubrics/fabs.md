# FABs

#### General

1.  [ID: all_fabs_only_one] Does the active screen or viewport hierarchy contain at most ONE Floating Action Button component (FloatingActionButton or ExtendedFloatingActionButton)? [Metric type: Hygiene] [Weight: 10]

2.  [ID: all_fabs_on_top] Does the FAB appear to be on top of all other UI elements, persisting above scrollable content? [Metric type: Hygiene] [Weight: 5]

3.  [ID: all_fabs_prominent] If there are other actions on the screen, does the FAB appear more visually prominent than those actions (e.g., through its size, elevation `--md-sys-elevation-level3`, or use of primary color containers `--md-sys-color-primary-container`)? [Metric type: Hygiene] [Weight: 5]

4.  [ID: all_fabs_location] Is the FAB positioned according to window size class: anchored at bottom-trailing with a 16dp margin from screen edges (`--md-sys-measurement-space200`) in compact/medium views, or docked within the top slot of the Navigation Rail in expanded/desktop views? [Metric type: Hygiene] [Weight: 10]

5.  [ID: all_fabs_drop_shadow] Does the FAB present use a drop shadow to create additional protection against a background (resting elevation `--md-sys-elevation-level3`)? Note: If dark mode or a high-contrast background makes the presence of a shadow ambiguous or impossible to distinguish, mark this question as N/A. [Metric type: Hygiene] [Weight: 3]

#### Standard FAB

1.  [ID: standard_fab_square_rounded] If a standard FAB is present, does its container comply with the specification: size 56x56dp (or 96x96dp for Large FAB) with a rounded-square corner radius of 16dp (`--md-sys-shape-corner-large` / 28dp `--md-sys-shape-corner-extra-large` for Large FAB)? [Metric type: Adherence] [Weight: 10]

2.  [ID: standard_fab_filled_icon] Does the standard FAB feature ONLY a centered filled icon? [Metric type: Hygiene] [Weight: 3]

#### FAB Menu

1.  [ID: fab_menu_circle] When a FAB menu is open, does the close/toggle button conform to the standard 56dp size with a 16dp corner radius (`--md-sys-shape-corner-large`)? [Metric type: Hygiene] [Weight: 3]

2.  [ID: fab_menu_to_actions] Does the FAB menu show 2-6 related actions floating vertically above in separate rounded containers, with text labels and filled icons? [Metric type: Hygiene] [Weight: 3]

3.  [ID: fab_menu_only_x_icon] Does the toggled close button of the active FAB menu feature ONLY a close “x” icon? [Metric type: Hygiene] [Weight: 3]

#### Extended FAB

1.  [ID: extended_fab_rectangle_rounded] If an Extended FAB is present, does its container comply with the specification: height 56dp (or 80dp/96dp for larger expressive variants), with a corner radius of 16dp (`--md-sys-shape-corner-large`) or fully rounded pill 28dp (`--md-sys-shape-corner-full`), and 16dp internal start/end padding? [Metric type: Adherence] [Weight: 10]

2.  [ID: extended_fab_text_label] Does the extended FAB have a clear text label (written in sentence case)? [Metric type: Hygiene] [Weight: 1]

3.  [ID: extended_fab_icon_left] If the extended FAB includes an icon, is the icon placed to the left of the text? [Metric type: Hygiene] [Weight: 1]

4.  [ID: extended_fab_filled_icon] If the extended FAB includes an icon, does it use the filled icon style? [Metric type: Hygiene] [Weight: 3]
