# Navigation Drawer

1.  [ID: navigation_drawer_modal_scrim] Do modal navigation drawers use a scrim
    to block interaction with underlying content? [Metric type: Hygiene]
    [Weight: 5]

2.  [ID: navigation_drawer_standard_anchored_leading] Is the standard navigation
    drawer anchored to the leading edge of the screen? [Metric type: Hygiene]
    [Weight: 3]

3.  [ID: navigation_drawer_modal_rounded_corners] Do modal navigation drawers
    have rounded corners on the outer edge (using `--droid-sys-shape-corner-large` or `--droid-sys-shape-corner-extra-large`)? [Metric type: Hygiene] [Weight: 3]

4.  [ID: navigation_drawer_max_one_visible] Is there only one navigation drawer
    visible at a time? [Metric type: Hygiene] [Weight: 3]

5.  [ID: navigation_drawer_container_width] Does the navigation drawer container width comply with the standard 360dp width? [Metric type: Adherence] [Weight: 3]

6.  [ID: navigation_drawer_active_indicator_size] Does the active drawer item use a pill-shaped indicator with a height of 56dp, width of 336dp, and corner radius of 28dp (or `--droid-sys-shape-corner-full`)? [Metric type: Adherence] [Weight: 5]

7.  [ID: navigation_drawer_typography] Do text labels in the navigation drawer use the correct font family `'Google Sans Text'` and match the `--droid-sys-typescale-label-large` token? [Metric type: Adherence] [Weight: 5]

8.  [ID: navigation_drawer_active_colors] Does the active drawer item background use `--droid-sys-color-secondary-container`, and does its text and icon use `--droid-sys-color-on-secondary-container`? [Metric type: Adherence] [Weight: 5]

9.  [ID: navigation_drawer_inactive_colors] Do inactive drawer item text and icons use `--droid-sys-color-on-surface-variant`? [Metric type: Adherence] [Weight: 3]

10. [ID: navigation_drawer_container_color] Is the drawer container background styled with `--droid-sys-color-surface` or `--droid-sys-color-background`? [Metric type: Adherence] [Weight: 3]
