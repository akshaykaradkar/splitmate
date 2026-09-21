# Bottom Sheet

1.  [ID: bottom_sheet_top_corners_rounded] If a standard or modal bottom sheet
    container is present, do its top-left and top-right corners comply with the
    specification: corner radius of 28dp (sys.shape.corner.extra-large-top /
    md.sys.shape.corner.extra-large.top) with 0dp bottom corners? [Metric type:
    Hygiene] [Weight: 10]

2.  [ID: bottom_sheet_higher_surface_color] If the page background and bottom
    sheet are both visible (i.e. no modal scrim), does the bottom sheet appear
    to use a higher surface color role (darker in light mode, lighter in dark
    mode) than the background? Mark as N/A if the background is a full-bleed
    photo, image, map, or user-generated content (e.g., a photo gallery or
    canvas). [Metric type: Hygiene] [Weight: 3]

3.  [ID: bottom_sheet_drag_handle] Is there a drag handle present? [Metric type:
    Hygiene] [Weight: 3]

4.  [ID: bottom_sheet_drag_handle_top_middle] Is the drag handle placed in the
    top-middle of the sheet? [Metric type: Hygiene] [Weight: 3]

5.  [ID: bottom_sheet_visible_margins] If the UI is mobile landscape or larger
    window size than mobile, are there clearly visible margins around the bottom
    sheet (left, right, and top) ? The bottom sheet should not appear to be
    touching those screen edges. [Metric type: Hygiene] [Weight: 3]
