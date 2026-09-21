# Button Group Rubric - Android Motion Compliance

1.  [ID: button_group_usage] Are related controls grouped together to establish clear context? [Metric type: Adherence] [Weight: 5]

2.  [ID: button_group_mandatory_elements] Are there two or more buttons or icon buttons in the group? [Metric type: Hygiene] [Weight: 3]

3.  [ID: button_group_member_size_uniform] Are all member buttons in a button group configured to the same height and shape tokens? [Metric type: Hygiene] [Weight: 3]

4.  [ID: button_group_standard_min_gap] Do standard button groups maintain a clear, consistent spacing gap between adjacent buttons? [Metric type: Hygiene] [Weight: 3]

5.  [ID: button_group_connected_spacing] If using a Connected Button Group, are the buttons joined with consistent 2dp margins or thin outline dividers using `--droid-sys-color-outline` (light: `#747775`, dark: `#8e918f`)? [Requires Code Inspection] [Metric type: Adherence] [Weight: 5]

6.  [ID: button_group_selected_color_tokens] Does the selected button utilize appropriate tokens such as `--droid-sys-color-primary` (light: `#0b57d0`, dark: `#a8c7fa`) or `--droid-sys-color-secondary-container` (light: `#c2e7ff`, dark: `#004a77`) with corresponding on-color text? [Requires Code Inspection] [Metric type: Adherence] [Weight: 5]

7.  [ID: button_group_unselected_shape_token] Do unselected/at-rest buttons in a standard group utilize the `--droid-sys-shape-corner-full` shape token? [Requires Code Inspection] [Metric type: Adherence] [Weight: 3]

8.  [ID: button_group_morphing_animation] If interactive shape morphing occurs in a standard group, does it animate using `--droid-sys-motion-duration-200` (200ms) with `--droid-sys-motion-easing-standard` (`cubic-bezier(0.2, 0.0, 0.0, 1.0)`)? [Requires Code Inspection] [Metric type: Adherence] [Weight: 3]
