# List

1.  [ID: list_corners_contained_uncontained] In list layouts, do uncontained/edge-to-edge list item containers have a corner radius of 0dp (sys.shape.corner.none), while contained/card list items use a corner radius of 12dp (sys.shape.corner.medium) or 16dp (sys.shape.corner.large)? [Metric type: Hygiene] [Weight: 10]

2.  [ID: list_supporting_text_lines] Is list item supporting text limited to a maximum of 3 lines? [Metric type: Hygiene] [Weight: 3]

3.  [ID: list_avatar_vs_thumbnail_shape] Are circular avatar shapes used only for people, while square/rectangles are used for products/media? [Metric type: Hygiene] [Weight: 3]

4.  [ID: list_controls_edge_placement] Are checkboxes, switches, or radio buttons positioned at the absolute leading or trailing edge? [Metric type: Hygiene] [Weight: 3]

5.  [ID: list_height_by_lines] Does the height of the list item correspond directly to the tallest element / text lines: 56dp for 1-line (and link elements), 72dp for 2-line, and 88dp for 3-line list items? [Metric type: Adherence] [Weight: 10]

6.  [ID: list_vertical_alignment] Are list elements vertically centered (middle-aligned) for heights of 56dp and 72dp, and top-aligned only for heights of 88dp (3-line text)? [Metric type: Adherence] [Weight: 10]

7.  [ID: list_selected_state_second_indicator] Is the selected or active state of a list item indicated by a second visual cue (such as a checkbox, radio button, or checkmark icon) in addition to color, avoiding reliance on color alone? [Metric type: Adherence] [Weight: 10]

8.  [ID: list_touch_target_size] Do all interactive elements inside leading or trailing list slots have a minimum touch target size of 48x48dp? [Metric type: Hygiene] [Weight: 5]

9.  [ID: list_links_underline_limit] In lists containing more than 20 links, are link underlines removed at rest (re-appearing only on hover or focus) to avoid a visually overwhelming layout? [Metric type: Hygiene] [Weight: 3]

10. [ID: list_complex_spacing_padding] In complex (multi-column) list configurations, are dividers utilized, and is there exactly 8dp spacing above and below each element with 16dp padding within the cells? [Metric type: Adherence] [Weight: 5]

11. [ID: list_mixed_content_dropdowns] In scannability-critical areas such as dropdown menus, does the list avoid mixing different types of content (e.g., mixing system icons and user avatars) that would cause inconsistent visual indentation? [Metric type: Hygiene] [Weight: 3]
