# Loading Indicator Rubric - Android Motion Design System

This rubric evaluates Loading Indicators for visual and functional compliance with the Android Motion Design System.

## Evaluation Questions

1.  [ID: loading_indicator_mandatory_elements] Are mandatory elements present (spinning ring, circular arc, or morphing shape sequence)?
    -   *Heuristic*: Verify that a dynamic loading indicator is active to communicate the short-term wait time (between 200ms and 5s).
    -   [Metric type: Hygiene] [Weight: 3]

2.  [ID: loading_indicator_continuous_animation] Does the loading indicator animate continuously?
    -   *Heuristic*: The loading indicator must maintain a continuous, uninterrupted looping animation during the active wait process.
    -   [Metric type: Hygiene] [Weight: 3]

3.  [ID: loading_indicator_track_ends_rounded] Are track ends and active morphing shapes cleanly rounded?
    -   *Heuristic*: Verify that circular tracks or morphing visual boundaries have rounded ends/edges mapping to `--droid-sys-shape-corner-full` (value: `max(50cqw, 50cqh)`).
    -   [Metric type: Hygiene] [Weight: 3]

4.  [ID: loading_indicator_no_drop_shadows] Do loading indicators remain flat without drop shadows?
    -   *Heuristic*: Indicators must be rendered flat without drop shadows (unless a pull-to-refresh container is elevated, using `--droid-sys-elevation-level1` or `level2`).
    -   [Metric type: Hygiene] [Weight: 3]

5.  [ID: loading_indicator_shape_morph] Does the loading sequence loop through organic, morphing geometric shapes?
    -   *Heuristic*: The animation must cycle through unique curved/geometric shapes from the Android Motion shape library to represent active progression.
    -   [Metric type: Compliance] [Weight: 5]

6.  [ID: loading_indicator_color_containment] Do uncontained and contained loading variants use the correct color mappings?
    -   *Heuristic*:
        -   Uncontained indicators on a surface must use `--droid-sys-color-primary` (value: `light-dark(#0b57d0, #a8c7fa)`).
        -   Contained indicators must use `--droid-sys-color-on-primary-container` (value: `light-dark(#041e49, #d3e3fd)`) on a container fill mapping to `--droid-sys-color-primary-container` (value: `light-dark(#d3e3fd, #0842a0)`).
    -   [Metric type: Compliance] [Weight: 5]

7.  [ID: loading_indicator_non_gesture_refresh] Does the pull-to-refresh interaction provide an accessible non-gesture alternative?
    -   *Heuristic*: Verify that pull-to-refresh has a single-pointer accessible text button or menu action alternative (e.g. "Refresh") for screen readers.
    -   [Metric type: Compliance] [Weight: 5]

8.  [ID: loading_indicator_motion_scheme] Does the indeterminate morphing animation utilize the correct motion physics?
    -   *Heuristic*: Morphing sequences must use the Android Motion **Expressive Motion Scheme** (leveraging elastic spring physics) for a fluid, bouncy, and organic progression feel.
    -   [Metric type: Compliance] [Weight: 3]
