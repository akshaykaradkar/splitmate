# Split Button

1.  [ID: split_button_edges_spec] Do the leading and trailing buttons have straight inner edges (`--md-sys-shape-corner-none` or flat boundaries) and fully rounded outer edges (`--md-sys-shape-corner-full` / pill-shaped ends)? [Metric type: Hygiene] [Weight: 3]

2.  [ID: split_button_trailing_arrow_icon] Does the trailing button feature ONLY a downward arrow, chevron, or dropdown caret icon? [Metric type: Hygiene] [Weight: 3]

3.  [ID: split_button_divider_width] Is the vertical gap or dividing line between the leading and trailing buttons extremely subtle and thin, conforming to the strictly fixed 2dp specification? [Metric type: Hygiene] [Weight: 1]

4.  [ID: split_button_style_uniformity] Do both the leading and trailing buttons share the exact same visual style variant (Filled, Tonal, Elevated, or Outlined) and color scheme? [Metric type: Adherence] [Weight: 5]

5.  [ID: split_button_sentence_case] If the leading button contains text, does the label comply with sentence case capitalization (only capitalizing the first word and proper nouns)? [Metric type: Hygiene] [Weight: 1]

6.  [ID: split_button_label_brevity] Is the text label on the leading button brief (strictly 1 or 2 words)? [Metric type: Hygiene] [Weight: 1]

7.  [ID: split_button_accessibility_label] Does the trailing icon button include a descriptive accessibility label indicating that it expands additional choices related to the main action button (e.g., "More Watch Options" if the leading button is "Watch Later")? [Metric type: Adherence] [Weight: 3]

8.  [ID: split_button_touch_target] Do both the leading and trailing buttons maintain a minimum interactive touch target of 48x48dp (on Android/Web) or 44x44pt (on iOS), utilizing invisible touch padding where necessary? [Metric type: Adherence] [Weight: 3]
