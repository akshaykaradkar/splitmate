# List

1.  [ID: list_corners_contained_uncontained] In list layouts, do
    uncontained/edge-to-edge list item containers have a corner radius of 0px
    (`--droid-sys-shape-corner-none`), while contained/card list items use a corner radius
    of 12px (`--droid-sys-shape-corner-medium`) or 16px (`--droid-sys-shape-corner-large`)? [Metric
    type: Hygiene] [Weight: 10]

2.  [ID: list_supporting_text_lines] Is list item supporting text limited to a
    maximum of 3 lines? [Metric type: Hygiene] [Weight: 3]

3.  [ID: list_avatar_vs_thumbnail_shape] Are circular avatar shapes used only
    for people, while square/rectangles are used for products/media? [Metric
    type: Hygiene] [Weight: 3]

4.  [ID: list_controls_edge_placement] Are checkboxes, switches, or radio
    buttons positioned at the absolute leading or trailing edge? [Metric type:
    Hygiene] [Weight: 3]

5.  [ID: list_item_heights] Do list item row heights conform to standard Android Motion specs: 56dp for 1-line, 72dp for 2-line, and 88dp for 3-line layouts? [Metric type: Adherence] [Weight: 5]

6.  [ID: list_item_touch_targets] Are all interactive elements and slots within the list row sized to meet the minimum accessible 48x48dp touch target? [Metric type: Hygiene] [Weight: 5]

7.  [ID: list_item_typography] Do list item primary labels use the font family `'Google Sans Text'` matching `--droid-sys-typescale-body-large` or `--droid-sys-typescale-body-medium`, and do supporting texts match `--droid-sys-typescale-body-small`? [Metric type: Adherence] [Weight: 5]

8.  [ID: list_item_colors] Is the list item primary text styled with `--droid-sys-color-on-surface`, while supporting text and leading/trailing icons are styled with `--droid-sys-color-on-surface-variant`? [Metric type: Adherence] [Weight: 5]

9.  [ID: list_item_container_color] Is the list item background styled with `--droid-sys-color-surface` or `--droid-sys-color-background`? [Metric type: Adherence] [Weight: 3]

10. [ID: list_item_divider_styling] Do optional horizontal list dividers have a 1dp thickness and use the `--droid-sys-color-outline` or `--droid-sys-color-surface-variant` tokens? [Metric type: Adherence] [Weight: 3]
