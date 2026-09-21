# Navigation Rail

1.  [ID: navigation_rail_leading_edge_placement] Is the navigation rail positioned on the leading edge of the screen? [Metric type: Hygiene] [Weight: 3]

2.  [ID: navigation_rail_active_pill_indicator] If a Navigation Rail active destination indicator is present, does it comply with the Elements GM3 specification: height 32dp, width 56dp (or 64dp), corner radius 16dp / pill (sys.shape.corner.full), filled with the secondary-container color token? [Metric type: Hygiene] [Weight: 10]

3.  [ID: navigation_rail_destination_count] Does the navigation rail contain between 3 and 7 destinations? [Metric type: Hygiene] [Weight: 3]

4.  [ID: navigation_rail_no_drop_shadow] Does the navigation rail container stay flat without a drop shadow at rest? [Metric type: Hygiene] [Weight: 3]

5.  [ID: navigation_rail_one_word_labels] Do all navigation rail items utilize a single-word label, and is the text size kept at the standard scale (not shrunk to fit long names) with zero truncation? [Metric type: Adherence] [Weight: 5]

6.  [ID: navigation_rail_content_horizontal_alignment] Does the vertical navigation rail align horizontally with the main content area of the page? [Metric type: Adherence] [Weight: 3]

7.  [ID: navigation_rail_active_state_contrast] Does the active destination item switch to a filled icon (with outlined icons for inactive), or apply a semibold weight if no filled icon variant exists, to satisfy non-text contrast guidelines? [Metric type: Adherence] [Weight: 10]

8.  [ID: navigation_rail_fixed_scrolling] Does the navigation rail remain visible and fixed in position when scrolling vertically and horizontally, using a divider or elevation to distinguish content scrolling underneath it? [Metric type: Hygiene] [Weight: 5]

9.  [ID: navigation_rail_compact_viewport_behavior] At smaller viewports, does the standalone rail collapse into a temporary overlay panel under the menu toggle icon, or transform into a horizontal bottom navigation bar if it has 4 or fewer destinations? [Metric type: Adherence] [Weight: 5]

10. [ID: navigation_rail_count_badge_placement] If a count badge is present on a rail item, is it positioned precisely in the upper-right corner of the icon, and is its count integrated into the icon's accessibility label for screen readers? [Metric type: Hygiene] [Weight: 5]
