# Dialog Rubric

1.  [ID: dialog_container_rounded_corners] Do standard basic dialog containers comply with the specification: corner radius of 28px (`--droid-sys-shape-corner-extra-large`) applied to all four corners? [Metric type: Hygiene] [Weight: 10]

2.  [ID: dialog_scrim_backdrop] Do dialogs utilize a visual scrim backdrop to block interaction with underlying content and focus visual attention? [Metric type: Hygiene] [Weight: 5]

3.  [ID: dialog_max_two_buttons] Does the dialog contain at most two action buttons aligned to the trailing bottom edge? [Metric type: Hygiene] [Weight: 5]

4.  [ID: dialog_drop_shadow_elevation] Do dialogs utilize a prominent drop shadow matching `--droid-sys-elevation-level5` (12px) to elevate over the background scrim? [Metric type: Hygiene] [Weight: 5]

5.  [ID: dialog_container_color_token] Is the dialog container's background filled with the `--droid-sys-color-surface` color token? [Metric type: Adherence] [Weight: 3]

6.  [ID: dialog_typography_system] Does the dialog headline utilize `--droid-sys-typescale-headline-small` (`'Google Sans'`) and body content utilize `--droid-sys-typescale-body-medium` (`'Google Sans Text'`)? [Metric type: Adherence] [Weight: 5]

7.  [ID: dialog_entry_motion] Does the dialog entry transition utilize a duration of `--droid-sys-motion-duration-500` (500ms) with `--droid-sys-motion-easing-emphasized-decelerate` (`cubic-bezier(0.05, 0.7, 0.1, 1.0)`) easing? [Metric type: Adherence] [Weight: 8]

8.  [ID: dialog_exit_motion] Does the dialog exit transition utilize a duration of `--droid-sys-motion-duration-150` (150ms) with `--droid-sys-motion-easing-emphasized-accelerate` (`cubic-bezier(0.3, 0.0, 0.8, 0.15)`) easing? [Metric type: Adherence] [Weight: 5]
