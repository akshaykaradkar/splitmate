# Divider

1.  [ID: divider_stroke_thickness] Is the divider implemented as a simple, single line with a fine thickness of exactly 1dp/1px? [Metric type: Hygiene] [Weight: 1]

2.  [ID: divider_flat_no_shadows] Does the divider remain completely flat, without using any bevels, gradients, or drop shadows? [Metric type: Hygiene] [Weight: 3]

3.  [ID: divider_color_outline_variant] Does the divider use the low-contrast dynamic color token `outline-variant` (or standard GM3 equivalent) to avoid creating stark visual barriers? [Metric type: Adherence] [Weight: 3]

4.  [ID: divider_inset_padding_align] If the divider is an inset divider, does it use a 16dp left margin to perfectly align with the text padding of adjacent list or layout elements? [Metric type: Adherence] [Weight: 3]

5.  [ID: divider_uncluttered_list_usage] Is the divider used to separate distinct items inside a list, menu, or group without being placed redundant at the very top or bottom edge of its container? [Metric type: Hygiene] [Weight: 3]
