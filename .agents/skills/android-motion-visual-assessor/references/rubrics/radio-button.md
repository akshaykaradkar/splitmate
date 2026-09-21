# Radio Button Rubric

1.  [ID: radio_button_circular_shape] Do the outer container (strictly 20px x 20px) and selected inner dot appear perfectly circular? [Metric type: Hygiene] [Weight: 3]

2.  [ID: radio_button_group_exclusivity] Are selections within the same logical group mutually exclusive, allowing exactly one item to be selected at any given time? [Metric type: Hygiene] [Weight: 3]

3.  [ID: radio_button_no_drop_shadows] Do radio buttons stay flat without drop shadows, adhering to `--droid-sys-elevation-level0`? [Metric type: Hygiene] [Weight: 3]

4.  [ID: radio_button_color_adherence] When selected, do the outer circle outline and inner dot strictly use `--droid-sys-color-primary` (Light: `#0b57d0`, Dark: `#a8c7fa`); and when unselected, does the outer circle use `--droid-sys-color-outline` (Light: `#747775`, Dark: `#8e918f`) with a transparent center? [Metric type: Adherence] [Weight: 3]

5.  [ID: radio_button_touch_target] Do radio buttons have an invisible interactive touch target of at least 48px x 48px centered over the visual circle? [Metric type: Hygiene] [Weight: 3]

6.  [ID: radio_button_selection_motion] Does selecting a radio button trigger an animation where the inner dot scales up from 0px to its full size using `--droid-sys-motion-easing-standard` or `--droid-sys-motion-easing-emphasized` (`cubic-bezier(0.2, 0.0, 0.0, 1.0)`) over `--droid-sys-motion-duration-150` (150ms), and deselecting shrink the inner dot over `--droid-sys-motion-duration-100` (100ms) with a standard-accelerate easing? [Metric type: Adherence] [Weight: 3]
