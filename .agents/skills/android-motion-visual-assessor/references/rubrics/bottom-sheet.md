# Bottom Sheet

1.  [ID: bottom_sheet_top_corners_rounded] If a standard or modal bottom sheet
    container is present, do its top-left and top-right corners comply with the
    specification: corner radius of 28px (`--droid-sys-shape-corner-extra-large-top` / `28px 28px 0 0`) with 0px bottom corners (`--droid-sys-shape-corner-none`)? [Metric type: Adherence] [Weight: 10]

2.  [ID: bottom_sheet_higher_surface_color] If the page background and bottom
    sheet are both visible (i.e. no modal scrim), does the bottom sheet container use a contrasting surface color role like `--droid-sys-color-surface-variant` (light-dark(`#e1e3e1`, `#444746`)) or `--droid-sys-color-surface` (light-dark(`#fdfcfb`, `#1f1f1f`)) compared to the background `--droid-sys-color-background`? Mark as N/A if the background is a full-bleed photo, image, map, or user-generated content (e.g., a photo gallery or canvas). [Metric type: Adherence] [Weight: 5]

3.  [ID: bottom_sheet_drag_handle] Is there a drag handle present? [Metric type:
    Hygiene] [Weight: 3]

4.  [ID: bottom_sheet_drag_handle_top_middle] Is the drag handle placed in the
    top-middle of the sheet? [Metric type: Hygiene] [Weight: 3]

5.  [ID: bottom_sheet_visible_margins] If the UI is mobile landscape or larger
    window size than mobile, are there clearly visible margins around the bottom sheet (left, right, and top)? The bottom sheet should not
    appear to be touching those screen edges (e.g., maintaining at least a 56dp margin). [Metric type: Hygiene] [Weight: 3]

6.  [ID: bottom_sheet_motion_easing] If auditing the sheet's expanding or dismissing transition code or behavior, does it utilize the premium duration `--droid-sys-motion-duration-300` (300ms) or `--droid-sys-motion-duration-350` (350ms) paired with `--droid-sys-motion-easing-emphasized` (`cubic-bezier(0.2, 0.0, 0.0, 1.0)`) easing? [Requires Code Inspection] [Metric type: Adherence] [Weight: 5]
