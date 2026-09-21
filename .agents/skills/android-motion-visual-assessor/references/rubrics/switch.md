# Switch Rubric

1.  [ID: switch_track_capsule_shape] Is the switch track shaped as a capsule (strictly 52px width and 32px height) with fully rounded semicircular ends mapping to `--droid-sys-shape-corner-full`? [Metric type: Hygiene] [Weight: 3]

2.  [ID: switch_handle_circular] Is the switch handle/thumb perfectly circular, and does it morph dynamically in size (16px diameter when unselected, expanding to 24px when selected, and expanding to 28px during pressed/dragging states)? [Metric type: Hygiene] [Weight: 3]

3.  [ID: switch_no_direct_label] Does the switch stay flat (maintaining `--droid-sys-elevation-level0`) and avoid putting text labels (like "On" / "Off") directly inside the component track itself? [Metric type: Hygiene] [Weight: 3]

4.  [ID: switch_color_adherence] When selected, does the track fill strictly use `--droid-sys-color-primary` (Light: `#0b57d0`, Dark: `#a8c7fa`) and the handle use `--droid-sys-color-on-primary` (Light: `#ffffff`, Dark: `#062e6f`); and when unselected, does the track outline use `--droid-sys-color-outline` (Light: `#747775`, Dark: `#8e918f`) with the handle in `--droid-sys-color-outline` or `--droid-sys-color-on-surface-variant` (Light: `#444746`, Dark: `#c4c7c5`)? [Metric type: Adherence] [Weight: 3]

5.  [ID: switch_touch_target] Does the switch maintain an invisible interactive touch target of at least 48px x 48px? [Metric type: Hygiene] [Weight: 3]

6.  [ID: switch_slide_motion] Does toggling the switch slide the handle across the track smoothly using `--droid-sys-motion-easing-standard` or the premium `--droid-sys-motion-easing-emphasized` (`cubic-bezier(0.2, 0.0, 0.0, 1.0)`) over a duration of `--droid-sys-motion-duration-250` (250ms) or `--droid-sys-motion-duration-300` (300ms)? [Metric type: Adherence] [Weight: 3]

7.  [ID: switch_elastic_morph_motion] Does pressing and dragging the switch handle elastically expand its diameter to 28px using `--droid-sys-motion-easing-emphasized-decelerate` over `--droid-sys-motion-duration-100` (100ms), and settle/contract to 24px (or 16px) upon release using `--droid-sys-motion-easing-emphasized` over `--droid-sys-motion-duration-200` (200ms)? [Metric type: Adherence] [Weight: 3]
