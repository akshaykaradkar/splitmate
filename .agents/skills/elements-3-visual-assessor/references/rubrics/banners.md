# Banners & Callouts

1.  [ID: banners_app_level_placement] Is an App-level callout/banner placed at the top of the screen directly below the App Bar? [Metric type: Hygiene] [Weight: 3]

2.  [ID: banners_app_page_corner_none] Do App-level and Page-level callouts/banners comply with the straight 0dp corner radius specification (None shape style)? [Metric type: Hygiene] [Weight: 3]

3.  [ID: banners_component_corner_small] Does a Component-level callout/banner comply with the 8dp corner radius specification (Small shape style / sys.shape.corner.small)? [Metric type: Hygiene] [Weight: 3]

4.  [ID: banners_dismissal_element_exclusivity] Does the banner or callout contain at most one type of dismissal element, featuring either secondary action text ("Dismiss") or an "X" close icon button, but not both? [Metric type: Hygiene] [Weight: 3]

5.  [ID: banners_text_no_redundant_prefix] Does the message text avoid repeating the callout type (e.g. starting with "Error" or "Warning") as the first word of the text? [Metric type: Hygiene] [Weight: 3]

6.  [ID: banners_single_sentence_no_period] If the callout/banner message is a single sentence, does it omit a period at the end per Elements GM3 writing guidelines? [Metric type: Hygiene] [Weight: 1]

7.  [ID: banners_stack_spacing_urgency] If multiple callouts are stacked, are they ordered by descending urgency (Error > Caution > Info > Success) and separated by exactly 4dp of spacing? [Metric type: Hygiene] [Weight: 3]
