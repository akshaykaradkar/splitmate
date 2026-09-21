# Slider Rubric - Android Motion Design System

This rubric evaluates Sliders for visual and functional compliance with the Android Motion Design System.

## Evaluation Questions

1.  [ID: slider_active_inactive_contrast] Does the active track segment clearly contrast with the inactive segment?
    -   *Heuristic*: The active track fill must be styled with `--droid-sys-color-primary` (value: `light-dark(#0b57d0, #a8c7fa)`) and the inactive segment must use `--droid-sys-color-surface-variant` (value: `light-dark(#e1e3e1, #444746)`) to ensure a prominent contrast boundary.
    -   [Metric type: Hygiene] [Weight: 3]

2.  [ID: slider_range_sliders_horizontal] Are range sliders (having two handles) oriented horizontally rather than vertically?
    -   *Heuristic*: Range sliders must be horizontal to align with standard user interaction expectations and avoid high cognitive load.
    -   [Metric type: Hygiene] [Weight: 3]

3.  [ID: slider_dimensions_by_size] Do track and handle dimensions match the specified size preset and corner tokens?
    -   *Heuristic*: Ensure that heights and shapes align with the correct size tier (XS, S, M, L, XL). For example:
        -   M Slider: Track Height 40dp, Handle Height 52dp, Corner Radius uses `--droid-sys-shape-corner-medium` (value: `12px`).
        -   L Slider: Track Height 56dp, Handle Height 68dp, Corner Radius uses `--droid-sys-shape-corner-large` (value: `16px`).
    -   [Metric type: Compliance] [Weight: 5]

4.  [ID: slider_handle_shape_morph] Does the handle compress and track expand/morph when pressed and dragged?
    -   *Heuristic*: The handle must dynamically change shape/width (e.g. shrinking) and the track must expand slightly to provide interactive feedback.
    -   [Metric type: Compliance] [Weight: 5]

5.  [ID: slider_interaction_spring] Does the handle shape transition utilize the correct effects spring model?
    -   *Heuristic*: Drag interaction states must feel fluid and responsive, utilizing the **Expressive Fast Effects Spring** (or `--droid-sys-motion-easing-standard` over `--droid-sys-motion-duration-150` for web/iOS fallbacks).
    -   [Metric type: Compliance] [Weight: 3]

6.  [ID: slider_snap_motion] For discrete sliders, does the handle snap smoothly utilizing a spring-based physics model?
    -   *Heuristic*: Discrete snaps must utilize spring-based spatial physics (or `--droid-sys-motion-easing-standard` over a duration of `--droid-sys-motion-duration-200` to `--droid-sys-motion-duration-300`).
    -   [Metric type: Compliance] [Weight: 3]

7.  [ID: slider_floating_tooltip_color] Does the floating value indicator use the correct high-contrast color roles?
    -   *Heuristic*: Tooltips must be styled with `--droid-sys-color-inverse-surface` (value: `light-dark(#303030, #e3e3e3)`) for the container background, and `--droid-sys-color-inverse-on-surface` (value: `light-dark(#f2f2f2, #303030)`) for typography.
    -   [Metric type: Compliance] [Weight: 3]

8.  [ID: slider_contrast_anchoring] Is the trailing end of the inactive track anchored visually to ensure at least a 3:1 contrast against the background?
    -   *Heuristic*: The end of the inactive track must be visually anchored (via dot stops or a bounding container) to comply with contrast standards.
    -   [Metric type: Hygiene] [Weight: 3]

9.  [ID: slider_typography] Do tooltip text values utilize standard Google typefaces?
    -   *Heuristic*: Value text inside tooltips must use `--droid-sys-typescale-label-small` or `label-medium` in `Google Sans Text` (or `Google Sans`).
    -   [Metric type: Hygiene] [Weight: 1]
