# App bars

#### All App Bars

1.  [ID: all_app_bars_top_position] Does the “App bar” appear to be positioned
    at the top of its respective screen or pane, above all other elements in
    that area (ex. top of app, sheet, leading pane in a multi-pane layout)?
    [Metric type: Hygiene] [Weight: 5]

2.  [ID: all_app_bars_one_text_element] Does the App bar (excluding the Search
    app bar) appear to contain only ONE of the following text elements: a
    headline (with an optional subtitle on a new line), search field hint text,
    or a product brand mark? This applies to mobile and tablet; mark as N/A for
    desktop, where app bar-like headers may legitimately combine multiple
    elements like a brand mark and a search field. [Metric type: Hygiene]
    [Weight: 3]

3.  [ID: all_app_bars_subtitle_line] Are all subtitles contained in a single
    line? [Metric type: Hygiene] [Weight: 3]

4.  [ID: all_app_bars_max_trailing] Does the app bar have a maximum of 3
    trailing icon buttons, excluding any icons in the search bar or the account
    particle? [Metric type: Hygiene] [Weight: 3]

5.  [ID: all_app_bars_leading_icon_droid] If a leading navigation icon is present
    in the top app bar, is it implemented using standard Android Motion navigation icons
    (`arrow_back` / `arrow_back_ios` or platform equivalent system back arrow) on
    secondary screens, or `menu` on top-level screens, rendered at 24x24dp with a
    48x48dp interactive bounding touch target and utilizing the `--md-icon-font` ('Google Symbols')? [Metric type: Adherence] [Weight:
    10]

6.  [ID: all_app_bars_overflow_last_right] If there is an overflow icon, is it
    the last icon on the right, for screens in left-to-right languages? [Metric
    type: Hygiene] [Weight: 1]

7.  [ID: all_app_bars_container_corners] Does the app bar container feature completely straight, flush corners with 0px radius (`--droid-sys-shape-corner-none`)? [Metric type: Adherence] [Weight: 5]

8.  [ID: all_app_bars_scroll_color_shift] During scrolling, does the App Bar transition its background color from `--droid-sys-color-surface` (light-dark(`#fdfcfb`, `#1f1f1f`)) to `--droid-sys-color-surface-variant` (light-dark(`#e1e3e1`, `#444746`)), or employ an elevation shadow of at least `--droid-sys-elevation-level1` (1px)? [Metric type: Adherence] [Weight: 5]

#### Medium flexible + Large flexible App Bars

1.  [ID: med_lg_app_bar_headline_lines] For medium flexible and large flexible
    app bars, is the headline text contained in 2 lines or less? [Metric type:
    Hygiene] [Weight: 3]

2.  [ID: med_lg_app_bar_typography] For medium flexible app bars, is the headline styled with `--droid-sys-typescale-headline-medium` (`1.8rem/2.3rem 'Google Sans'`), and for large flexible app bars, is it styled with `--droid-sys-typescale-display-small` (`2.3rem/2.8rem 'Google Sans'`)? [Metric type: Adherence] [Weight: 5]

#### Small App Bars

1.  [ID: small_app_bar_headline_line] For Small app bars, is the headline text
    contained in a single line (e.g., no text wrapping)? [Metric type: Hygiene]
    [Weight: 3]

2.  [ID: small_app_bar_typography] Is the small app bar headline styled using the `--droid-sys-typescale-title-large` token (`1.4rem/1.8rem 'Google Sans'`)? [Metric type: Adherence] [Weight: 5]

#### Search App Bar

1.  [ID: search_app_bar_hint_text_search] Does the search element contain clear
    hint text that includes the word “Search”? [Metric type: Hygiene] [Weight:
    3]

2.  [ID: search_app_bar_surface_container] Does the search element container use the `--droid-sys-color-surface-variant` color token (light-dark(`#e1e3e1`, `#444746`)) or `--droid-sys-color-surface` (light-dark(`#fdfcfb`, `#1f1f1f`)) to distinguish it from the background? [Metric type: Adherence] [Weight: 5]

3.  [ID: search_app_bar_centered] Is the search bar centered within the app bar?
    [Metric type: Hygiene] [Weight: 3]

4.  [ID: search_app_bar_max_trailing] If trailing icons are used in the search
    element, are there at most 2 icons present? [Metric type: Hygiene] [Weight:
    3]

5.  [ID: search_app_bar_avatar_far_right] If a user avatar is present, is it
    placed to the far right of the screen? [Metric type: Hygiene] [Weight: 3]

6.  [ID: search_app_bar_leading_menu_logo] Is the leading icon on the primary
    screen a menu or product logo? [Metric type: Hygiene] [Weight: 3]

7.  [ID: search_app_bar_leading_arrow_logo] Is the leading icon on secondary
    screens an arrow pointing left or a product logo? [Metric type: Hygiene]
    [Weight: 3]

8.  [ID: search_app_bar_typography] Does the search app bar input text utilize the `--droid-sys-typescale-body-large` token (`1rem/1.5rem 'Google Sans Text'`)? [Metric type: Adherence] [Weight: 5]
