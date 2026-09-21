# Chip Rubric

1.  [ID: chip_purpose] Are chips used for filtering, suggestions, quick responses, or making selections? [Metric type: Hygiene] [Weight: 3]

2.  [ID: chip_rounded_corners_spec] If standard chips (Assist, Filter, Input, Suggestion) are present, do they comply with the specification: height 32px and corner radius 8px (`--droid-sys-shape-corner-small`), while avatar/people chips use a fully rounded corner radius of 16px (`--droid-sys-shape-corner-large`) or pill shape (`--droid-sys-shape-corner-full`)? [Metric type: Hygiene] [Weight: 10]

3.  [ID: chip_outlined_border_stroke] Do outlined chips display a clear border stroke matching the `--droid-sys-color-outline` token? [Metric type: Hygiene] [Weight: 3]

4.  [ID: chip_multiple_inline_gap] Are multiple chips arranged inline with a clear visual gap of at least 8px separating them? [Metric type: Hygiene] [Weight: 3]

5.  [ID: chip_label_typography] Does the chip label text use the `'Google Sans Text'` font family and match the `--droid-sys-typescale-label-large` token? [Metric type: Adherence] [Weight: 5]

6.  [ID: chip_active_color_tokens] When in an active/selected state, do filter chips utilize the `--droid-sys-color-secondary-container` for background container and `--droid-sys-color-on-secondary-container` for label and icon? [Metric type: Adherence] [Weight: 5]

7.  [ID: chip_touch_target_size] Does the touch target of each interactive chip maintain a minimum area of 48x48px? [Metric type: Accessibility] [Weight: 5]

8.  [ID: chip_selection_motion] Does the selection transition (such as the checkmark appearance or selection fade) use `--droid-sys-motion-duration-150` (150ms) paired with `--droid-sys-motion-easing-standard` (`cubic-bezier(0.2, 0.0, 0.0, 1.0)`) easing? [Metric type: Adherence] [Weight: 5]
