# Data Table

1.  [ID: data_table_purpose] Is data presented in a structured column and row format for scannability? [Metric type: Hygiene] [Weight: 5]

2.  [ID: data_table_borders_and_dividers] Do rows have horizontal divider lines of exactly 1dp thickness with color `outline-variant`? Are table border outlines present by default? [Metric type: Adherence] [Weight: 3]

3.  [ID: data_table_checkbox_color] If selection checkboxes are present, do they use `on-surface-variant` color for unselected states and `primary` color for selected states? [Metric type: Adherence] [Weight: 3]

4.  [ID: data_table_icon_specs] If rows contain action or status icons, do they use 24dp Google Symbols with an interactive touch target of at least 48dp, styled in `on-surface-variant` color? [Metric type: Adherence] [Weight: 3]

5.  [ID: data_table_typography_headers] Do table header titles use small title typography (GM3 Title Small) styled in `on-surface-variant` color? [Metric type: Adherence] [Weight: 3]

6.  [ID: data_table_typography_body] Do table data cells use medium body typography (GM3 Body Medium) in `on-surface` color, and do subdata lines (if present) use small body (GM3 Body Small)? [Metric type: Adherence] [Weight: 3]

7.  [ID: data_table_text_left_aligned] Are strings, text, names, mixed alpha-numeric values, or chips left-aligned inside data cells? [Metric type: Hygiene] [Weight: 3]

8.  [ID: data_table_numeric_right_aligned] Are numbers and structured data (e.g., "hh:mm AM" or decimals with aligned placeholder 0s) right-aligned inside data cells? [Metric type: Hygiene] [Weight: 3]

9.  [ID: data_table_vertical_alignment] Are chips, badges, and buttons centered vertically inside table rows of variable height? Are checkboxes aligned either centered or at the top of tall table cells? [Metric type: Adherence] [Weight: 3]

10. [ID: data_table_padding_outer] Do tables use standard outer edge left/right paddings of 16dp (or 12dp for dense views), and do cells utilize internal horizontal paddings of 16dp? [Metric type: Adherence] [Weight: 3]

11. [ID: data_table_no_default_density] Is the default table density set to 0, ensuring that interactive touch targets inside the table are NOT shrunk below 48x48dp by default? [Metric type: Adherence] [Weight: 5]

12. [ID: data_table_frozen_scrolling] If the table has frozen columns or headers, do they dynamically elevate by +1 (Elevation 1) when scrolled, with elevation removed when scrolled back to the top/beginning? [Metric type: Adherence] [Weight: 3]

13. [ID: data_table_toolbar_spacing] Does the table toolbar leave 8dp of space between separate common buttons? Are primary actions grouped on the left, and secondary on the right, with no more than one filled primary action button per table? [Metric type: Adherence] [Weight: 3]

14. [ID: data_table_row_buttons] Are row-level actions limited to at most one regular text/hairline button per row, with any other actions represented as icon buttons? [Metric type: Adherence] [Weight: 3]

15. [ID: data_table_disabled_buttons] Are actions that require row selections mapped to "soft" disabled buttons rather than "hard" disabled buttons, so they remain discoverable for assistive technology? [Metric type: Adherence] [Weight: 3]

16. [ID: data_table_no_empty_cells] Are there NO empty data cells (using NA, n/s, 0, or "blank" instead to avoid screen reader confusion)? [Metric type: Hygiene] [Weight: 5]

17. [ID: data_table_error_ntc] Are cell-level error or status alerts communicated using an icon or error text rather than relying on color change alone? [Metric type: Hygiene] [Weight: 5]

18. [ID: data_table_invisible_headers] If the table has columns without a visible header (such as select or action columns), is an off-screen accessibility label provided for screen readers? [Metric type: Adherence] [Weight: 5]
