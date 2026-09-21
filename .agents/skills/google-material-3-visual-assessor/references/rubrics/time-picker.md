# Time Picker

1.  [ID: time_picker_purpose] Is the time picker presented as a clear clock dial
    or explicit text input? [Metric type: Hygiene] [Weight: 3]

2.  [ID: time_picker_mode_toggle_button] Does the component support toggling
    between dial and input modes using a keyboard (`keyboard`) or clock (`access_time` / `schedule`) icon button? [Metric type: Hygiene] [Weight: 3]

3.  [ID: time_picker_container_corner_radius] Does the time picker container
    utilize generously rounded corners mapping to `--md-sys-shape-corner-extra-large` (28px)? [Metric type: Adherence] [Weight: 3]

4.  [ID: time_picker_dial_touch_targets] In dial mode, do the clock face number targets and the selector handle maintain a minimum interactive size of 48x48dp? [Metric type: Adherence] [Weight: 5]

5.  [ID: time_picker_scrim_overlay] Is the time picker modal container overlaid on a background dimming scrim using `--md-sys-color-scrim`? [Metric type: Hygiene] [Weight: 3]

6.  [ID: time_picker_layout_orientation] Does the time picker layout change orientation seamlessly based on the viewport (portrait stacking vs. landscape side-by-side positioning) to prevent vertical scrolling of the dial? [Metric type: Hygiene] [Weight: 3]

7.  [ID: time_picker_container_paddings] Does the modal dialog container utilize standard 24dp (`--md-sys-measurement-space300`) padding on all sides? [Metric type: Adherence] [Weight: 1]

8.  [ID: time_picker_input_sizing] In input mode, do the hour and minute input containers measure 96dp in width and 72dp in height, with dedicated labels placed cleanly below them? [Metric type: Adherence] [Weight: 3]

9.  [ID: time_picker_no_density_dial] Does the component avoid applying default density to the clock face dial when the viewport is constrained, falling back to input mode instead? [Metric type: Hygiene] [Weight: 3]

10. [ID: time_picker_ios_branding] On iOS platforms, does the time picker utilize a GM3-branded iOS date picker configured for time, incorporating subtle glass effects if placed in the functional layer of iOS 26? [Metric type: Adherence] [Weight: 3]
