# Progress Indicator Rubric - Android Motion Design System

This rubric evaluates Progress Indicators for visual and functional compliance with the Android Motion Design System.

## Evaluation Questions

1.  [ID: progress_indicator_completion_fill] Does the active indicator fill reflect the actual percentage of completion?
    -   *Heuristic*: The filled active segment must scale proportionally to reflect the real-time completion status of determinate processes.
    -   [Metric type: Hygiene] [Weight: 3]

2.  [ID: progress_indicator_rounded_ends] Do the progress indicator track and active indicators have cleanly rounded ends?
    -   *Heuristic*: Ensure that linear tracks and active indicator fills feature cleanly rounded ends mapping to `--droid-sys-shape-corner-full` (value: `max(50cqw, 50cqh)`).
    -   [Metric type: Hygiene] [Weight: 3]

3.  [ID: progress_indicator_no_drop_shadows] Do progress indicators stay flat without drop shadows?
    -   *Heuristic*: Verify that progress indicators are rendered flat on the surface without dimensional drop shadows.
    -   [Metric type: Hygiene] [Weight: 3]

4.  [ID: progress_indicator_active_color] Do active tracks use the primary color role?
    -   *Heuristic*: The active filled track segment must be styled with `--droid-sys-color-primary` (value: `light-dark(#0b57d0, #a8c7fa)`) against an inactive track background mapping to `--droid-sys-color-surface-variant` (value: `light-dark(#e1e3e1, #444746)`), ensuring at least 3:1 contrast.
    -   [Metric type: Compliance] [Weight: 5]

5.  [ID: progress_indicator_stop_marker] Does the linear determinate track feature a standard circular end stop marker?
    -   *Heuristic*: Check for a mandatory 4dp circular end stop indicator at the trailing tip of active determinate tracks to ensure proper contrast identification.
    -   [Metric type: Compliance] [Weight: 3]

6.  [ID: progress_indicator_min_width] Does the linear progress bar maintain the specified minimum width?
    -   *Heuristic*: Ensure that linear progress bar tracks are at least **40dp** in width.
    -   [Metric type: Hygiene] [Weight: 1]

7.  [ID: progress_indicator_wavy_morph] For wavy configurations, does the track render as a smooth sine wave?
    -   *Heuristic*: The active track in wavy visual states must render as an undulating sine wave with configurable amplitude and wavelength to represent brand playfulness.
    -   [Metric type: Compliance] [Weight: 3]

8.  [ID: progress_indicator_organic_motion] Do indeterminate progress states animate utilizing the correct motion scheme?
    -   *Heuristic*: Indeterminate oscillations and track fills must utilize the bouncy **Expressive Motion Scheme** (leveraging elastic spring physics) rather than legacy cubic-bezier curves.
    -   [Metric type: Compliance] [Weight: 5]

9.  [ID: progress_indicator_rtl_mirroring] Is the fill direction mirrored horizontally for right-to-left languages?
    -   *Heuristic*: In RTL languages, the linear track progress must grow from right to left (except for specific language exceptions like Hebrew).
    -   [Metric type: Hygiene] [Weight: 3]
