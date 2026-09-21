# Search Rubric - Android Motion Compliance

1.  [ID: search_container_dimensions_shape] If a Search Bar container is present, does it comply with the Android Motion structural specifications: container height of 56dp and a fully rounded pill shape using `--droid-sys-shape-corner-full`? [Metric type: Hygiene] [Weight: 10]

2.  [ID: search_background_color_tokens] Does the search container's background fill use the standard `--droid-sys-color-surface-variant` (light: `#e1e3e1`, dark: `#444746`) or `--droid-sys-color-surface` (light: `#fdfcfb`, dark: `#1f1f1f`) token? [Requires Code Inspection] [Metric type: Adherence] [Weight: 5]

3.  [ID: search_typography_styles] Is the input and hint text styled using the `--droid-sys-typescale-body-large` token (`1rem/1.5rem 'Google Sans Text'`)? [Requires Code Inspection] [Metric type: Adherence] [Weight: 5]

4.  [ID: search_icon_font_check] Do all system icons in the search container use the `--md-icon-font` ('Google Symbols') font family? [Requires Code Inspection] [Metric type: Adherence] [Weight: 5]

5.  [ID: search_hint_text_contains_search] Does the search container contain clear hint text that includes the word "Search"? [Metric type: Hygiene] [Weight: 3]

6.  [ID: search_max_two_trailing_icons] Does the search container contain at most 2 trailing icon buttons? [Metric type: Hygiene] [Weight: 3]

7.  [ID: search_clear_icon_button] When text is typed, does a clear text "x" icon button display trailing the text? [Metric type: Hygiene] [Weight: 3]

8.  [ID: search_motion_expansion] If inspecting the code/behavior of search expansion, does the search bar transition into the search view using `--droid-sys-motion-duration-300` (300ms) or `--droid-sys-motion-duration-350` (350ms) paired with `--droid-sys-motion-easing-emphasized` (`cubic-bezier(0.2, 0.0, 0.0, 1.0)`)? [Requires Code Inspection] [Metric type: Adherence] [Weight: 5]
