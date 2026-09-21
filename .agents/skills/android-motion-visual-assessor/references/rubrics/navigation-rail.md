# Navigation Rail

1.  [ID: navigation_rail_leading_edge_placement] Is the navigation rail
    positioned on the leading edge of the screen? [Metric type: Hygiene]
    [Weight: 3]

2.  [ID: navigation_rail_active_pill_indicator] If a Navigation Rail active
    destination indicator is present, does it comply with the specification:
    height 32dp, width 56dp (for collapsed) or 64dp (for expanded), corner radius `--droid-sys-shape-corner-full` (or 16px pill shape), filled with `--droid-sys-color-secondary-container`? [Metric
    type: Hygiene] [Weight: 10]

3.  [ID: navigation_rail_destination_count] Does the navigation rail contain
    between 3 and 7 destinations? [Metric type: Hygiene] [Weight: 3]

4.  [ID: navigation_rail_no_drop_shadow] Does the navigation rail container stay
    flat without a drop shadow at rest (elevation `--droid-sys-elevation-level0`)? [Metric type: Hygiene] [Weight: 3]

5.  [ID: navigation_rail_typography] Do labels in the navigation rail use the font family `'Google Sans Text'` matching `--droid-sys-typescale-label-medium` or `--droid-sys-typescale-label-small`? [Metric type: Adherence] [Weight: 5]

6.  [ID: navigation_rail_active_colors] Do active destination text and icons use `--droid-sys-color-on-secondary-container`? [Metric type: Adherence] [Weight: 5]

7.  [ID: navigation_rail_inactive_colors] Do inactive destination text and icons use `--droid-sys-color-on-surface-variant`? [Metric type: Adherence] [Weight: 3]

8.  [ID: navigation_rail_container_color] Is the navigation rail container background styled with `--droid-sys-color-surface`, `--droid-sys-color-background`, or made transparent (ensuring contrast)? [Metric type: Adherence] [Weight: 3]
