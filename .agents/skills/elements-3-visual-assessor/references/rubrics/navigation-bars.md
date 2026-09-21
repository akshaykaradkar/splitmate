# Navigation Bars

1.  [ID: nav_bars_bottom_location] Is the “Navigation bar” component located at the very bottom of the screen? [Metric type: Adherence] [Weight: 3]

2.  [ID: nav_bars_active_pill] If a Navigation Bar active destination indicator is present, does it comply with the Elements GM3 specification: height 32dp, width 64dp, corner radius 16dp / pill (sys.shape.corner.full), filled with the secondary-container color token? [Metric type: Adherence] [Weight: 10]

3.  [ID: nav_bars_to_items] Are there between 3-5 items in the navigation bar (or 3-4 items if transformed from a standalone rail)? [Metric type: Adherence] [Weight: 3]

4.  [ID: nav_bars_label_present] Is there a label present for each navigation icon? [Metric type: Adherence] [Weight: 3]

5.  [ID: nav_bars_label_below_icon] For compact window sizes (ex. mobile portrait), is there a label, and is it placed directly below the icon? [Metric type: Hygiene] [Weight: 3]

6.  [ID: nav_bars_label_trailing] For medium and larger window sizes (re: not mobile portrait), is the label trailing the icon (ex. to the right of the icon in LTR languages)? [Metric type: Adherence] [Weight: 3]

7.  [ID: nav_bars_height_web_android] Is the horizontal container height of the navigation bar exactly 64dp on Android/Web? [Metric type: Adherence] [Weight: 5]

8.  [ID: nav_bars_active_icon_contrast] Does the active destination item use a filled style icon (with outlined icons for inactive destinations), or apply a semibold icon weight if no filled style exists, to comply with GAR Non-Text Contrast (NTC) guidelines? [Metric type: Adherence] [Weight: 10]

9.  [ID: nav_bars_mutual_exclusivity] Is the screen free from having BOTH a horizontal bottom navigation bar and a vertical navigation rail or drawer simultaneously visible? [Metric type: Adherence] [Weight: 10]
