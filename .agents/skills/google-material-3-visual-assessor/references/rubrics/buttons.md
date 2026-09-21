# Buttons Rubric

1.  [ID: buttons_rounded_corners] If standalone buttons with a container (Filled, Elevated, Filled Tonal, or Outlined) are present, do they comply with the specification: container height of 40dp (Android/web) or 44pt (iOS) and a fully rounded pill corner radius of 50% / 20dp (complying with `--md-sys-shape-corner-full`)? [Metric type: Adherence] [Weight: 10]

2.  [ID: buttons_centered_content] For all buttons with a visible container, does the button label or icon (or the combined group if both are present) appear to be centered BOTH horizontally and vertically within the container? [Metric type: Hygiene] [Weight: 3]

3.  [ID: buttons_different_variants] If a pair of labeled actions is present (excluding a clustered "Button group" component), do they use different container style variants to indicate their hierarchy (e.g., one is Filled and the other is Outlined or Tonal) rather than using identical visual weights? [Metric type: Adherence] [Weight: 3]

4.  [ID: buttons_max_one_filled] Within any single viewport, dialog, or distinct card/sheet container, is there at most ONE high-emphasis filled button (container fill mapped to `--md-sys-color-primary`), with all other actions using Outlined, Tonal, or Text button variants to preserve page hierarchy? [Metric type: Hygiene] [Weight: 10]

5.  [ID: buttons_sentence_case] Do text labels on buttons appear to comply with sentence case capitalization (only capitalizing the first word and proper nouns, e.g., "Save changes", "Book with flights") instead of ALL CAPS or title casing? [Metric type: Hygiene] [Weight: 1]

6.  [ID: buttons_icon_leading] If a button contains both an icon and a text label, is the icon placed on the leading side (to the left of the text label in LTR) at the standard size of 20dp? [Metric type: Hygiene] [Weight: 1]

7.  [ID: buttons_disabled_muted] If a disabled button is present, are its container fill and text label visually muted (e.g., displaying the standard 12% container opacity and 38% text opacity of `--md-sys-color-on-surface`) compared to active interactive buttons? [Metric type: Hygiene] [Weight: 3]
