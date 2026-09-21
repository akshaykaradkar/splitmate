# Snackbar

1.  [ID: snackbar_purpose] Are snackbars used to display brief, low-importance updates at the bottom of the screen? [Metric type: Hygiene] [Weight: 3]

2.  [ID: snackbar_max_one_visible] Is there at most one snackbar visible on the screen at any time, avoiding simultaneous stacking? [Metric type: Hygiene] [Weight: 3]

3.  [ID: snackbar_max_single_action] Does the snackbar contain at most a single action text button? [Metric type: Hygiene] [Weight: 3]

4.  [ID: snackbar_action_button_style] Is the action button styled as a low-emphasis Text button (using `--md-sys-color-inverse-primary` color role) rather than an icon button, filled button, or outlined button? [Metric type: Adherence] [Weight: 5]

5.  [ID: snackbar_container_color] Does the snackbar container use a high-contrast background fill (`--md-sys-color-inverse-surface`) paired with text in `--md-sys-color-inverse-on-surface`? [Metric type: Adherence] [Weight: 5]

6.  [ID: snackbar_container_shape] Does the snackbar container feature standard 4dp rounded corners (`--md-sys-shape-corner-extra-small`)? [Metric type: Adherence] [Weight: 3]

7.  [ID: snackbar_text_lines_limit] Is the snackbar text limited to at most two lines on mobile, avoiding text truncation with ellipses or excessive text length? [Metric type: Hygiene] [Weight: 3]

8.  [ID: snackbar_no_focus_trap] Does the snackbar avoid grabbing or trapping keyboard/accessibility focus when it appears, allowing users to freely browse underlying page content? [Metric type: Hygiene] [Weight: 3]
