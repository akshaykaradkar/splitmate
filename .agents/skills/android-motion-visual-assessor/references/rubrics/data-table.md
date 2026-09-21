# Data Table Rubric

1.  [ID: data_table_purpose] Is data presented in a structured column and row format for scannability and structural clarity? [Metric type: Hygiene] [Weight: 5]

2.  [ID: data_table_text_left_aligned] Are column headers and data cell text for text-based content left-aligned (in LTR layouts)? [Metric type: Hygiene] [Weight: 3]

3.  [ID: data_table_numeric_right_aligned] Are numeric value columns right-aligned and formatted using monospaced tabular figures to ensure clean vertical alignment? [Metric type: Hygiene] [Weight: 3]

4.  [ID: data_table_row_divider_thickness] Do horizontal dividers between rows have a subtle, consistent 1px stroke thickness styled with `--droid-sys-color-outline` (Light: `#747775`, Dark: `#8e918f`)? [Metric type: Hygiene] [Weight: 1]

5.  [ID: data_table_typography_scale] Do column headers utilize `--droid-sys-typescale-label-large` (500 weight, `'Google Sans Text'`) or `--droid-sys-typescale-title-small` (500 weight, `'Google Sans Text'`), and cell data utilize `--droid-sys-typescale-body-medium` or `--droid-sys-typescale-body-small` (`'Google Sans Text'`)? [Metric type: Adherence] [Weight: 3]

6.  [ID: data_table_row_heights] Do column headers have a minimum height of 56px, and standard body rows have a consistent height of 48px or 52px to ensure readability and comfortable touch targets? [Metric type: Adherence] [Weight: 3]

7.  [ID: data_table_interactive_motion] Do interactive rows (such as hovered, focused, or selected rows) transition smoothly between states using `--droid-sys-motion-easing-standard` over `--droid-sys-motion-duration-100` (100ms) or `--droid-sys-motion-duration-150` (150ms) to fade in state layers? [Metric type: Adherence] [Weight: 3]
