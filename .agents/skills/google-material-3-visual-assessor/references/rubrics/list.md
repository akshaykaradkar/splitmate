# List Rubric

#### Parent-Child Hierarchy & Structural Mapping

1.  [ID: list_parent_child_mapping] Are both the outer scrollable container (`List`) and each distinct horizontal row (`List item`) explicitly identified and mapped in the visual audit? [Metric type: Hygiene] [Weight: 10]

#### Geometry & Shape Specifications

2.  [ID: list_corners_contained_uncontained] In list layouts, do uncontained/edge-to-edge list item containers have a corner radius of 0dp (`sys.shape.corner.none` / `--md-sys-shape-corner-none`), while contained/card list items use a corner radius of 12dp (`sys.shape.corner.medium` / `--md-sys-shape-corner-medium`) or 16dp (`sys.shape.corner.large` / `--md-sys-shape-corner-large`)? [Metric type: Hygiene] [Weight: 10]

3.  [ID: list_avatar_vs_thumbnail_shape] Are circular avatar shapes (`sys.shape.corner.full` / `--md-sys-shape-corner-full`) used strictly for people/entities, while square/rectangular shapes with rounded corners are used for products, media, and video thumbnails? [Metric type: Hygiene] [Weight: 3]

#### Sizing & Alignments

4.  [ID: list_item_heights] Does the list item height adapt correctly to the content lines present: exactly 56dp for 1-line, 72dp for 2-line, and 88dp for 3-line configurations? [Metric type: Adherence] [Weight: 3]

5.  [ID: list_item_vertical_alignment] Are list elements middle-aligned for 1-line and 2-line items (56dp/72dp), and top-aligned with 8dp/12dp top padding for 3-line items (88dp or taller)? [Metric type: Adherence] [Weight: 3]

6.  [ID: list_item_spacing_tokens] Do list paddings and spacings adhere to GM3 spacer specifications: label text is padded 16dp (`--md-sys-measurement-space200`) from the leading visual, and trailing visuals are inset 24dp (`--md-sys-measurement-space300`) from the trailing edge? [Metric type: Adherence] [Weight: 3]

#### Text & Content scan-ability

7.  [ID: list_supporting_text_lines] Is list item supporting text limited to a maximum of 3 lines to ensure legibility and scan-ability? [Metric type: Hygiene] [Weight: 3]

8.  [ID: list_text_truncation_wrapping] Are primary labels kept brief and wrapped or truncated cleanly without breaking page layout? [Metric type: Hygiene] [Weight: 3]

#### Interactive Elements & Accessibility

9.  [ID: list_controls_edge_placement] Are checkboxes, switches, or radio buttons positioned at the absolute leading or trailing edge of the list item? [Metric type: Hygiene] [Weight: 3]

10. [ID: list_secondary_touch_targets] Do interactive secondary controls within list slots (e.g. icon buttons, checkboxes, or switches) have an independent touch target of at least 48x48dp (or 44x44pt on iOS)? [Metric type: Hygiene] [Weight: 5]

11. [ID: list_nested_stacked_actions] Does each slot/list item container support only a single active primary action or destination (excluding secondary actions with distinct targets), avoiding multiple conflicting stacked actions within a single row container? [Metric type: Adherence] [Weight: 10]

#### Color & Divider Token Mapping

12. [ID: list_color_token_mappings] Do list elements strictly bind to standard GM3 color tokens: label text to `sys.color.on-surface` (`--md-sys-color-on-surface`), supporting text to `sys.color.on-surface-variant` (`--md-sys-color-on-surface-variant`), and active highlights to `sys.color.secondary-container` (`--md-sys-color-secondary-container`)? [Metric type: Adherence] [Weight: 5]

13. [ID: list_divider_tokens] If list dividers are present to separate rows, do they utilize a 1dp subtle stroke mapped strictly to `sys.color.outline-variant` (`--md-sys-color-outline-variant`)? [Metric type: Adherence] [Weight: 5]
