# App Bars Rubric

#### All App Bars

1.  [ID: all_app_bars_top_position] Does the “App bar” appear to be positioned at the top of its respective screen or pane, above all other elements in that area (e.g., top of app, sheet, leading pane in a multi-pane layout)? [Metric type: Hygiene] [Weight: 5]

2.  [ID: all_app_bars_one_text_element] Does the App bar (excluding the Search app bar) appear to contain only ONE of the following text elements: a headline (with an optional subtitle on a new line), search field hint text, or a product brand mark? This applies to mobile and tablet; mark as N/A for desktop, where app bar-like headers may legitimately combine multiple elements like a brand mark and a search field. [Metric type: Hygiene] [Weight: 3]

3.  [ID: all_app_bars_subtitle_line] Are all subtitles contained in a single line? [Metric type: Hygiene] [Weight: 3]

4.  [ID: all_app_bars_max_trailing] Does the app bar have a maximum of 3 trailing icon buttons, excluding any icons in the search bar or the account particle? [Metric type: Hygiene] [Weight: 3]

5.  [ID: all_app_bars_leading_icon_gm] If a leading navigation icon is present in the top app bar, is it implemented using standard GM3 navigation icons: `arrow_back` / `arrow_back_ios` (or platform equivalent system back arrow) on secondary screens, or `menu` on top-level screens, rendered at 24x24dp with a 48x48dp interactive bounding touch target? [Metric type: Adherence] [Weight: 10]

6.  [ID: all_app_bars_overflow_last_right] If there is an overflow icon, is it the last icon on the right, for screens in left-to-right languages? [Metric type: Hygiene] [Weight: 1]

#### Medium flexible + Large flexible App Bars

1.  [ID: med_lg_app_bar_headline_lines] For medium flexible and large flexible app bars, is the headline text contained in 2 lines or less? [Metric type: Hygiene] [Weight: 3]

#### Small App Bars

1.  [ID: small_app_bar_headline_line] For Small app bars, is the headline text contained in a single line (e.g., no text wrapping)? [Metric type: Hygiene] [Weight: 3]

#### Search App Bar

1.  [ID: search_app_bar_hint_text_search] Does the search element contain clear hint text that includes the word “Search”? [Metric type: Hygiene] [Weight: 3]

2.  [ID: search_app_bar_surface_container] Does the search element appear to use the “surface container” color role (`--md-sys-color-surface-container`) or a variant (e.g., “surface bright” color role `--md-sys-color-surface-bright`) to distinguish it from the background? [Metric type: Hygiene] [Weight: 3]

3.  [ID: search_app_bar_centered] Is the search bar centered within the app bar? [Metric type: Hygiene] [Weight: 3]

4.  [ID: search_app_bar_max_trailing] If trailing icons are used in the search element, are there at most 2 icons present? [Metric type: Hygiene] [Weight: 3]

5.  [ID: search_app_bar_avatar_far_right] If a user avatar is present, is it placed to the far right of the screen? [Metric type: Hygiene] [Weight: 3]

6.  [ID: search_app_bar_leading_menu_logo] Is the leading icon on the primary screen a menu or product logo? [Metric type: Hygiene] [Weight: 3]

7.  [ID: search_app_bar_leading_arrow_logo] Is the leading icon on secondary screens an arrow pointing left or a product logo? [Metric type: Hygiene] [Weight: 3]
