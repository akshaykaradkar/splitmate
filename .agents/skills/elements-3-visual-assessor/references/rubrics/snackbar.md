# Snackbar

1.  [ID: snackbar_purpose] Are snackbars used to display brief, low-importance updates at the bottom of the screen? [Metric type: Hygiene] [Weight: 3]

2.  [ID: snackbar_max_one_visible] Is there at most one snackbar visible on the screen at any time? [Metric type: Hygiene] [Weight: 3]

3.  [ID: snackbar_max_single_action] Does the snackbar contain at most a single action text button (1-word active voice CTA, e.g., "View", "Undo")? [Metric type: Hygiene] [Weight: 3]

4.  [ID: snackbar_no_action_auto_dismiss_5s] Do snackbars without actions auto-dismiss in 5 seconds on mobile/tablet (while avoiding auto-dismissing on web unless coordinating inline feedback is provided), and do snackbars with actions persist without self-dismissing? [Metric type: Adherence] [Weight: 3]

5.  [ID: snackbar_max_70_chars] Is the supporting text concise, keeping within the recommended limit of 70 characters and a maximum of 2 lines? [Metric type: Hygiene] [Weight: 2]

6.  [ID: snackbar_no_text_icons] Is the text description free of embedded icons (e.g., info, check, or warning icons)? [Metric type: Adherence] [Weight: 2]

7.  [ID: snackbar_close_button_with_action] If the snackbar contains an action button, does it feature a close button ("X") to enable manual dismissal for magnification/accessibility users? [Metric type: Adherence] [Weight: 3]

8.  [ID: snackbar_nudge_prevent_overlap] Does the snackbar reposition vertically (nudge up) to avoid overlapping persistent footer elements (e.g., FABs) or focused interactive elements? [Metric type: Hygiene] [Weight: 3]
