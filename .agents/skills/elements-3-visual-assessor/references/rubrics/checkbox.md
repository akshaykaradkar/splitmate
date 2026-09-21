# Checkbox

1.  [ID: checkbox_usage] Are checkboxes used for multi-select lists or settings
    requiring a submit step, while using radio buttons for single-select lists,
    and switches for immediate toggles on mobile settings? [Metric type: Hygiene] [Weight: 3]

2.  [ID: checkbox_size_shape] Does the checkbox container comply with the
    Elements GM3 specification: 18dp x 18dp visual box with a 2dp corner radius
    (Extra Small shape style)? [Metric type: Hygiene] [Weight: 5]

3.  [ID: checkbox_checkmark_visibility] Is the checkmark icon (or horizontal dash)
    only visible when the checkbox is in the selected (or indeterminate) state? [Metric type: Hygiene] [Weight: 3]

4.  [ID: checkbox_indeterminate_hierarchy] If a parent-child checkbox list is present
    with mixed child selections, does the parent checkbox correctly display the
    indeterminate dash state? [Metric type: Hygiene] [Weight: 5]

5.  [ID: checkbox_error_placement] If a single or group validation error occurs, is the
    error text placed directly below the checkbox or selection group? [Metric type: Hygiene] [Weight: 3]

6.  [ID: checkbox_touch_target_size] Does the invisible interactive touch target of
    the checkbox measure at least 48dp x 48dp, without default high density shrinking it? [Metric type: Hygiene] [Weight: 5]

7.  [ID: checkbox_label_style_color] Is the paired text label styled in sentence case
    using the `On Surface` color role, and does it remain visible even when the
    checkbox is disabled? [Metric type: Hygiene] [Weight: 3]
