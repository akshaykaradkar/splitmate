# Navigation Bars

1.  [ID: nav_bars_bottom_location] Is the “Navigation bar” component located at
    the very bottom of the screen? [Metric type: Adherence] [Weight: 3]

2.  [ID: nav_bars_active_pill] If a Navigation Bar active destination indicator
    is present, does it comply with the specification: height 32dp, width 64dp,
    corner radius `--droid-sys-shape-corner-full` (pill shape), and is it filled with
    `--droid-sys-color-secondary-container`? [Metric type: Adherence] [Weight: 10]

3.  [ID: nav_bars_to_items] Are there between 3-5 items in the navigation bar?
    [Metric type: Adherence] [Weight: 3]

4.  [ID: nav_bars_label_present] Is there a label present for each navigation
    icon? [Metric type: Adherence] [Weight: 3]

5.  [ID: nav_bars_label_below_icon] For compact window sizes (ex. mobile
    portrait), is there a label, and is it placed below the icon? [Metric type:
    Hygiene] [Weight: 3]

6.  [ID: nav_bars_label_trailing] For medium and larger window sizes (re: not
    mobile portrait), is the label trailing the icon (ex. right of the icon in
    LTR languages)? [Metric type: Adherence] [Weight: 3]

7.  [ID: nav_bars_typography] Do the text labels use the correct typeface `'Google Sans Text'` matching the `--droid-sys-typescale-label-medium` or `--droid-sys-typescale-label-small` tokens? [Metric type: Adherence] [Weight: 5]

8.  [ID: nav_bars_active_colors] Do active destination text and icons use the `--droid-sys-color-on-secondary-container` color token? [Metric type: Adherence] [Weight: 5]

9.  [ID: nav_bars_inactive_colors] Do inactive destination text and icons use the `--droid-sys-color-on-surface-variant` color token? [Metric type: Adherence] [Weight: 3]

10. [ID: nav_bars_container_height] Does the navigation bar container height comply with the standard 64dp specification? [Metric type: Adherence] [Weight: 5]

11. [ID: nav_bars_container_color] Is the navigation bar container background styled with `--droid-sys-color-surface` or `--droid-sys-color-background`? [Metric type: Adherence] [Weight: 3]
