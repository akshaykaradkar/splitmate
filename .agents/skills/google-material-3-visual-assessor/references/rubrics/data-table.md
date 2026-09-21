# Data Table Rubric

#### Structure & Alignment

1.  [ID: data_table_purpose] Is data presented in a structured column and row format for scannability? [Metric type: Hygiene] [Weight: 5]

2.  [ID: data_table_text_left_aligned] Are column headers and data cell text for text-based content left-aligned (for LTR languages)? [Metric type: Hygiene] [Weight: 3]

3.  [ID: data_table_numeric_right_aligned] Are numeric value columns right-aligned? (Use monospaced tabular numbers to keep values optically aligned for scanning.) [Metric type: Hygiene] [Weight: 3]

#### Sizing & Layout Measurements

4.  [ID: data_table_container_outline] Does the data table container utilize a 1dp outline mapped strictly to `sys.color.outline-variant` (`--md-sys-color-outline-variant`)? [Metric type: Adherence] [Weight: 3]

5.  [ID: data_table_header_height] Is the header row height exactly 56dp with centered vertical alignment, 8dp left/right padding for components, and 16dp left/right padding for the headline? [Metric type: Adherence] [Weight: 3]

6.  [ID: data_table_row_height] Are standard data rows exactly 52dp in height with centered vertical alignment, 8dp component padding, and 16dp label padding? [Metric type: Adherence] [Weight: 3]

7.  [ID: data_table_footer_height] If a footer is present, is it exactly 52dp in height with centered vertical alignment, 8dp padding between elements, and 40dp left/right padding for supporting text? [Metric type: Adherence] [Weight: 3]

#### Interactive States & Selection

8.  [ID: data_table_row_divider_thickness] Do horizontal dividers between rows have a subtle, consistent stroke thickness of 1dp? [Metric type: Hygiene] [Weight: 1]

9.  [ID: data_table_row_hover_state] When a cursor hovers over an interactive row, does the entire row display a subtle background state layer overlay (using 8% opacity of the on-surface color)? [Metric type: Adherence] [Weight: 3]

10. [ID: data_table_selected_indicators] Do selected rows display both of the following visual indicators: a selected checkbox on the leading edge and a distinct background color fill across the entire row? [Metric type: Adherence] [Weight: 5]

#### Accessibility & Keyboard Traversal

11. [ID: data_table_horizontal_scroll] When the table exceeds the viewport or parent pane width, is the container cleanly scrollable horizontally without breaking the surrounding layout grid? [Metric type: Hygiene] [Weight: 5]

12. [ID: data_table_initial_keyboard_focus] When navigating to the data table using keyboard tab/arrow traversal, does initial focus land on the first actionable cell or button within the table (rather than focusing the entire data table container)? [Metric type: Adherence] [Weight: 5]

13. [ID: data_table_aria_roles] Does the codebase implementation assign proper semantic table markup or ARIA roles (`role="table"`, `columnheader`, `rowgroup`, `row`, `cell`) to ensure accessibility for screen readers? [Metric type: Adherence] [Weight: 5]
