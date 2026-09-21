# Dialog

1.  [ID: dialog_container_scrim_overlay] Is the dialog container presented on top of a darkened background scrim overlay to block interactions with behind-content? [Metric type: Hygiene] [Weight: 5]

2.  [ID: dialog_container_no_shadow_on_scrim] If positioned over a proper background scrim, does the dialog container sit flat without requiring drop shadows/elevation effects? [Metric type: Adherence] [Weight: 1]

3.  [ID: dialog_container_rounded_corners] Does the dialog container use generously rounded corners matching the increased GM3 extra-large shape tokens? [Metric type: Hygiene] [Weight: 3]

4.  [ID: dialog_dimensions] Does the dialog width fit within the Elements standard range of 280dp to 712dp (default 560dp) and maintain a minimum 72dp margin from the screen edges? [Metric type: Adherence] [Weight: 3]

5.  [ID: dialog_submit_button_always_enabled] If the dialog contains form inputs, is the submit/confirm button kept in an **enabled** state rather than disabled, even if form inputs are currently invalid (displaying validation errors only after clicking) per GAR 1.14? [Metric type: Adherence] [Weight: 10]

6.  [ID: dialog_scroll_vertical_only] If the body content is scrollable, does it scroll vertically only (no horizontal scroll) with the header and footer pinned in place? [Metric type: Hygiene] [Weight: 3]

7.  [ID: dialog_scroll_divider_1px] If the dialog is scrolled, is there a 1px thick divider colored `outline-variant` separating the body from the header or footer? [Metric type: Adherence] [Weight: 3]

8.  [ID: dialog_actions_alignment] Is the primary confirming action button (using clear specific verbs) positioned at the far trailing (bottom-right) edge, with the dismissive action to its left? [Metric type: Hygiene] [Weight: 3]

9.  [ID: dialog_accessibility_esc_dismiss] Does the dialog support dismissal via the **Esc key** (GAR 2.9) and trap keyboard focus inside the container while open? [Metric type: Adherence] [Weight: 5]
