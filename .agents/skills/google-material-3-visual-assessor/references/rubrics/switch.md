# Switch

1.  [ID: switch_track_capsule_shape] Is the switch track shaped as a capsule with fully rounded ends (`--md-sys-shape-corner-full`)? [Metric type: Hygiene] [Weight: 3]

2.  [ID: switch_handle_circular] Is the switch handle/thumb circular (`--md-sys-shape-corner-full`)? [Metric type: Hygiene] [Weight: 3]

3.  [ID: switch_no_direct_label] Does the switch track avoid displaying any text labels (like "ON", "OFF", "I", or "O") directly on the component itself? [Metric type: Hygiene] [Weight: 3]

4.  [ID: switch_track_sizing_spec] If standard MD3 is used on Android or Web, does the switch track strictly adhere to the 52dp width and 32dp height dimensions? [Metric type: Adherence] [Weight: 5]

5.  [ID: switch_handle_state_morphing] Does the switch handle visually morph in size depending on state (16dp unselected, 24dp selected, 28dp pressed), complying with state-morphing rules? [Metric type: Adherence] [Weight: 3]

6.  [ID: switch_adjacent_label_color] Does the adjacent text label describing the setting use the `--md-sys-color-on-surface` color role, remaining consistent during interaction? [Metric type: Adherence] [Weight: 3]

7.  [ID: switch_immediate_effect] Do changes to the switch take effect immediately without requiring a separate "Save" or submission action? [Metric type: Hygiene] [Weight: 3]

8.  [ID: switch_correct_semantic_usage] Is the switch reserved strictly for standalone, independent binary settings rather than opposing multi-view options (like Map vs. List view, where a Connected Button Group is required)? [Metric type: Hygiene] [Weight: 3]

9.  [ID: switch_touch_target_size] Does the switch maintain a minimum interactive touch target area of 48x48dp (or 44x44pt on iOS inside list rows), avoiding default layout density that shrinks target sizes? [Metric type: Hygiene] [Weight: 3]
