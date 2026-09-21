# Text Field

1.  [ID: text_field_active_outline_thickness] Do active (focused) text fields display a clear, distinct outline or bottom indicator line that thickens and uses the primary color role (`--md-sys-color-primary`)? [Metric type: Adherence] [Weight: 3]

2.  [ID: text_field_helper_error_placement] Is helper and error text placed cleanly 4dp (`--md-sys-measurement-space50`) below the input field container? [Metric type: Hygiene] [Weight: 1]

3.  [ID: text_field_error_states_color] Do error states utilize the distinct error color role (`--md-sys-color-error`) for supporting text, container outlines, labels, and icons? [Metric type: Adherence] [Weight: 3]

4.  [ID: text_field_mandatory_error_icon] In an error state, does the text field present a mandatory trailing error icon in the error color role (`--md-sys-color-error`)? [Metric type: Hygiene] [Weight: 3]

5.  [ID: text_field_container_height] Does the text field container follow standard heights (56dp on Android/Web, or 44dp on iOS) to satisfy touch target and platform accessibility? [Metric type: Adherence] [Weight: 3]

6.  [ID: text_field_outlined_corners] If outlined text fields are used, do they feature the proper corner radius (4dp / `--md-sys-shape-corner-extra-small` on Android/Web, or 10dp on iOS)? [Metric type: Adherence] [Weight: 3]

7.  [ID: text_field_filled_corners] If filled text fields are used, do they feature rounded top corners of 4dp (`--md-sys-shape-corner-extra-small-top`) and square bottom corners? [Metric type: Adherence] [Weight: 3]

8.  [ID: text_field_labels_visible] Is the label text short, clear, fully visible, and free from truncation or multi-line wrapping? [Metric type: Hygiene] [Weight: 3]

9.  [ID: text_field_asterisk_indicator] If a field is required, is an asterisk (`*`) displayed next to the label text, and is this asterisk present in both the visual label and the accessibility label? [Metric type: Hygiene] [Weight: 1]

10. [ID: text_field_variant_consistency] Are filled and outlined variants used consistently, avoiding the intermixing of both variants within the same form section or region? [Metric type: Hygiene] [Weight: 1]

11. [ID: text_field_paddings] Does the text field container adhere to horizontal outer paddings of 16dp (`--md-sys-measurement-space200` without icons) or 12dp (`--md-sys-measurement-space150` with icons) and 16dp spacing between icons and text? [Metric type: Adherence] [Weight: 1]

12. [ID: text_field_chat_vs_search] Are bottom-anchored conversational or chat message input fields mapped to `Text field` and strictly distinguished from top-level `Search bar` components? [Metric type: Hygiene] [Weight: 5]
