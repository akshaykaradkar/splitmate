# Chip

1.  [ID: chip_purpose] Are chips used for dynamic, contextual choices (filtering, suggestions,
    smart actions, or input tags) in groupings, avoiding misuse as persistent buttons for linear steps? [Metric type: Hygiene] [Weight: 3]

2.  [ID: chip_rounded_corners_spec] If standard chips (Input, Assistive, Suggestive, Filter) are present,
    do they comply with the specification: height 32dp and corner radius of 8dp (Small shape style),
    while People chips feature a fully rounded 16dp / pill-shape corner radius (Full shape style)? [Metric type: Hygiene] [Weight: 10]

3.  [ID: chip_dark_outlined_ntc] Do outlined chips display a dark, high-contrast border stroke (outline)
    that strictly satisfies Elements GM3 Non-Text Contrast (NTC) requirements? [Metric type: Hygiene] [Weight: 5]

4.  [ID: chip_multiple_inline_gap] Are multiple chips in a set separated by a consistent visual gap
    of at least 8dp? [Metric type: Hygiene] [Weight: 3]

5.  [ID: chip_placement_above_field] In multi-select dropdown fields, are the selected chips placed
    directly above the input field to prevent obstruction from active suggestion lists? [Metric type: Hygiene] [Weight: 5]

6.  [ID: chip_split_keyboard_accessibility] If split chips are present, does each interactive zone act
    as a separate focusable element, and do their expanded touch targets avoid overlapping? [Metric type: Hygiene] [Weight: 5]

7.  [ID: chip_deletion_a11y_labels] For removable chips or split chips with deletion, does the component
    provide an explicit `"Remove [visible label text]"` accessibility label for screen readers, while marking supporting thumbnails/avatars as decorative? [Metric type: Hygiene] [Weight: 5]

8.  [ID: chip_truncation_tooltip] If a chip label exceeds 20 characters, is ellipsis truncation applied
    at a fixed width, with the full text string correctly exposed on hover/focus inside a tooltip? [Metric type: Hygiene] [Weight: 5]

9.  [ID: chip_live_region_announcement] Do chip deletion actions or immediate list filtering trigger
    screen reader confirmation announcements (e.g., "Results updated" or "XYZ removed") via ARIA live regions? [Metric type: Hygiene] [Weight: 5]
