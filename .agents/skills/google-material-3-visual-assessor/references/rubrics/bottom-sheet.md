# Bottom Sheet Rubric

1.  [ID: bottom_sheet_top_corners_rounded] If a standard or modal bottom sheet container is present, do its top-left and top-right corners comply with the specification: corner radius of 28px (complying with `--md-sys-shape-corner-extra-large-top` / `28px 28px 0 0`) with 0px bottom corners? [Metric type: Hygiene] [Weight: 10]

2.  [ID: bottom_sheet_higher_surface_color] If the page background and bottom sheet are both visible (i.e., no modal scrim is active), does the bottom sheet container use a higher surface container color role (such as `--md-sys-color-surface-container` or `--md-sys-color-surface-container-high`, which are darker in light mode and lighter in dark mode) than the underlying background to ensure visual separation? Mark as N/A if the background is a full-bleed photo, image, map, or user-generated content (e.g., a photo gallery or canvas). [Metric type: Hygiene] [Weight: 3]

3.  [ID: bottom_sheet_drag_handle] Is there a drag handle present (a centered, horizontal pill or line at the top of the sheet)? [Metric type: Hygiene] [Weight: 3]

4.  [ID: bottom_sheet_drag_handle_top_middle] If a drag handle is present, is it placed horizontally centered in the top-middle of the sheet, with standard `--md-sys-measurement-space200` (16px) top/bottom padding? [Metric type: Hygiene] [Weight: 3]

5.  [ID: bottom_sheet_visible_margins] If the UI is in mobile landscape or a window size larger than mobile, are there clearly visible margins around the bottom sheet (left, right, and top, conforming to the `--md-sys-measurement-space700` / 56px margin specification) so that the bottom sheet does not touch those screen edges? [Metric type: Hygiene] [Weight: 3]
