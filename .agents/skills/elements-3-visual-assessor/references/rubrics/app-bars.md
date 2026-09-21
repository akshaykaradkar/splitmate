# App Bars

#### All App Bars

1.  [ID: all_app_bars_top_position] Does the “App bar” appear to be positioned
    at the top of its respective screen or pane, above all other elements in
    that area (ex. top of app, sheet, leading pane in a multi-pane layout)?
    [Metric type: Hygiene] [Weight: 5]

2.  [ID: all_app_bars_one_text_element] Does the App bar appear to contain a
    clearly visible, required product name as its main text heading, with page titles
    correctly wrapping rather than truncating with ellipses? [Metric type: Hygiene]
    [Weight: 3]

3.  [ID: all_app_bars_subtitle_line] If an optional subtitle or secondary label
    is present, does it wrap to multiple lines instead of being truncated with ellipses
    on smaller viewports? [Metric type: Hygiene] [Weight: 3]

4.  [ID: all_app_bars_max_actions] Does the app bar have a maximum of 6 actions on the
    right-hand side, including the search bar but excluding the user profile image?
    [Metric type: Hygiene] [Weight: 3]

5.  [ID: all_app_bars_leading_icon_elements] If a leading icon is present in the app bar,
    is it rendered at 24x24dp inside a standard 48x48dp interactive bounding touch target
    (e.g., hamburger menu on top-level screens, or back arrow/close button on secondary/contextual
    screens)? [Metric type: Adherence] [Weight: 10]

6.  [ID: all_app_bars_overflow_last_right] If an overflow menu or icon is present,
    is it positioned to collapse trailing actions from left to right on smaller viewports,
    serving as the last control before the Settings/Profile cluster? [Metric type: Hygiene]
    [Weight: 1]

7.  [ID: all_app_bars_support_menu_consolidation] If there are 2 or more help and feedback actions
    present (Help, Educational content, About team, Send feedback), are they consolidated
    under a single "Support" menu triggered by a Help icon button with the tooltip/ARIA label "Support"?
    [Metric type: Adherence] [Weight: 8]

8.  [ID: all_app_bars_fixed_action_sequence] Is the sequence of actions on the right-hand side fixed
    in the expected right-to-left order: User Profile -> Settings (if present) -> Support Menu -> Custom Actions?
    [Metric type: Adherence] [Weight: 5]

9.  [ID: all_app_bars_profile_aria_dialog] Is the user profile avatar implemented as a non-modal ARIA Dialog
    with focus trapped inside and a close button visually disguised as the avatar button, limited to a maximum of 5 links?
    [Metric type: Adherence] [Weight: 5]

10. [ID: all_app_bars_skip_link_first] Is a 'skip to main content' link configured as the first keyboard tab stop
    inside the app bar, jumping focus directly to the main content landmark? [Metric type: Adherence] [Weight: 5]

#### Responsive & Contextual App Bars

1.  [ID: contextual_app_bar_title_positioning] For contextual app bars, is the page title and secondary label positioned
    within the app bar for large viewports (>= 1024dp) and below the app bar controls for medium to small viewports (<= 840dp)?
    [Metric type: Hygiene] [Weight: 3]

2.  [ID: contextual_app_bar_wrap_no_truncation] Are titles and secondary labels in contextual navigation always allowed to
    wrap to multiple lines instead of being truncated or clipped with ellipses? [Metric type: Hygiene] [Weight: 5]

#### Search App Bar

1.  [ID: search_app_bar_hint_text_search] If a global search bar is present in the app bar, does it contain clear
    hint text that includes the word “Search”? [Metric type: Hygiene] [Weight: 3]

2.  [ID: search_app_bar_container_color] Does the search bar container use a flat visual style with a subtle contrast
    from the background (e.g. Surface Container Lowest against a Surface Container Low background)? [Metric type: Hygiene]
    [Weight: 3]

3.  [ID: search_app_bar_positioning] Is the search bar correctly positioned according to design requirements, either
    left-aligned (for wide, tabular layouts) or center-aligned (for fixed-width layouts)? [Metric type: Hygiene] [Weight: 3]

4.  [ID: search_app_bar_max_trailing] If trailing icon buttons are embedded directly within the search bar element itself,
    are there at most 2 icons present inside the search field? [Metric type: Hygiene] [Weight: 3]

5.  [ID: search_app_bar_avatar_far_right] Is the user profile avatar placed to the far right of the app bar, following the
    fixed sequence guidelines? [Metric type: Hygiene] [Weight: 3]

6.  [ID: search_app_bar_product_badge_specs] If a product badge (Alpha, Beta, Dogfood) is attached to the product name, is it
    non-interactive, hanging bottom-rounded with a `0dp, 0dp, 16dp, 16dp` corner radius, height of 16dp, 16dp left/right padding,
    and are the color and accessibility label appropriately configured? [Metric type: Adherence] [Weight: 8]
