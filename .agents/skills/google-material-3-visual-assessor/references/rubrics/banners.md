# Banners Rubric

1.  [ID: banners_basic_square] If a square basic banner is present, does it have square corners (complying with `--md-sys-shape-corner-none` / 0px) and is it placed at the top of the screen directly below the app bar or persistent search bar? [Metric type: Hygiene] [Weight: 3]

2.  [ID: banners_basic_round] If a round basic banner is present, does its container have a rounded shape (complying with `--md-sys-shape-corner-extra-large` / 28px) with at least `--md-sys-measurement-space200` (16px) margins from the window edges, and is it placed above or inline with body content? [Metric type: Hygiene] [Weight: 3]

3.  [ID: banners_rich_inline] If a rich banner is present, does its container have rounded corners (complying with `--md-sys-shape-corner-extra-large` / 28px) with at least `--md-sys-measurement-space200` (16px) margins, and is it placed inline within scrolling body content? [Metric type: Hygiene] [Weight: 3]

4.  [ID: banners_dismissal_element] Does the banner contain at most one type of dismissal element, featuring either action text buttons or a close icon button, but not both? [Metric type: Hygiene] [Weight: 3]

5.  [ID: banners_action_placement] If action text buttons are present, is the secondary action placed to the left of the primary confirming action (or, on iOS, uses an outlined button for secondary and a filled/tonal for primary)? [Metric type: Hygiene] [Weight: 3]

6.  [ID: banners_rich_image_square] If a leading image is present in a rich banner, does it appear as a square thumbnail (complying with the standard 80x80dp measurement)? [Metric type: Hygiene] [Weight: 3]

7.  [ID: banners_max_one_visible] Does the screen contain at most one visible banner at any time? [Metric type: Hygiene] [Weight: 3]
