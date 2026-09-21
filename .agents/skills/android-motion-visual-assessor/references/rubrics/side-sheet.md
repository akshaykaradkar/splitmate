# Side Sheet

1.  [ID: side_sheet_anchored_trailing_edge] Is the side sheet anchored to the
    trailing edge of the viewport by default (for LTR layouts)? [Metric type: Hygiene] [Weight: 3]

2.  [ID: side_sheet_modal_scrim_backdrop] Do modal side sheets utilize a dark scrim backdrop to disable background interaction? [Metric type: Hygiene] [Weight: 5]

3.  [ID: side_sheet_modal_close_button] Do modal side sheets feature a close icon button ("X" or arrow back) at the top, utilizing `'Google Symbols'` (`--md-icon-font`) and a minimum `48x48dp` touch target? [Metric type: Adherence] [Weight: 3]

4.  [ID: side_sheet_container_width] Does the side sheet comply with the standard width specification (default 400dp on wider viewports)? [Metric type: Adherence] [Weight: 3]

5.  [ID: side_sheet_container_color] Is the side sheet container background styled using a standard surface role token, such as `--droid-sys-color-surface` (light-dark(`#fdfcfb`, `#1f1f1f`)) or `--droid-sys-color-surface-variant` (light-dark(`#e1e3e1`, `#444746`))? [Metric type: Adherence] [Weight: 3]

6.  [ID: side_sheet_motion_easing] If auditing the side sheet's enter or exit transition code or behavior, does it utilize the premium duration `--droid-sys-motion-duration-300` (300ms) or `--droid-sys-motion-duration-350` (350ms) for modal, or `--droid-sys-motion-duration-250` (250ms) for standard sheets, paired with `--droid-sys-motion-easing-emphasized` (`cubic-bezier(0.2, 0.0, 0.0, 1.0)`) easing? [Requires Code Inspection] [Metric type: Adherence] [Weight: 5]
