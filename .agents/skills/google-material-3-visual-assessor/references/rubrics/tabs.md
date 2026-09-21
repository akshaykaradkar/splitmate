# Tabs

1.  [ID: tabs_usage] Are tabs used for separating highly related content
    categories, rather than sequential workflow steps? [Metric type: Hygiene] [Weight: 5]

2.  [ID: tabs_active_indicator_line_position] Is the active indicator line
    positioned directly below the active tab label? [Metric type: Hygiene]
    [Weight: 3]

3.  [ID: tabs_labels_single_line] Are tab labels presented on a single line
    without wrapping? [Metric type: Hygiene]
    [Weight: 3]

4.  [ID: tabs_variants_hierarchy] Are primary tabs used for top-level destination hierarchies (often placed under app bars) and secondary tabs used within content areas to establish secondary hierarchies? [Metric type: Adherence] [Weight: 3]

5.  [ID: tabs_container_height] Do Android/Web tabs have a container height of 47dp (label only) or 63dp (icon + label), or does iOS use a container height of 52pt? [Metric type: Adherence] [Weight: 3]

6.  [ID: tabs_active_indicator_height_shape] Does the primary active indicator feature a 3dp thickness with rounded top corners (shape `3, 3, 0, 0`), and does the secondary active indicator feature a thinner 2dp thickness? [Metric type: Adherence] [Weight: 3]

7.  [ID: tabs_color_tokens] Do active tabs use the primary color role (`--md-sys-color-primary`) for text, icon, and active indicator line, while inactive tabs use the on-surface-variant color role (`--md-sys-color-on-surface-variant`)? [Metric type: Adherence] [Weight: 5]

8.  [ID: tabs_scrollable_offset] For scrollable tabs, is the first visible tab offset by 52dp from the left edge of the device? [Metric type: Adherence] [Weight: 1]

9.  [ID: tabs_badge_character_limit] If a badge is present on a tab, is the text content limited to a maximum of 4 characters? [Metric type: Hygiene] [Weight: 1]

10. [ID: tabs_density_target_size] Do individual tabs maintain a minimum interactive touch target of 48x48dp on Android/Web or 44x44pt on iOS, avoiding default high density that shrinks touch targets? [Metric type: Hygiene] [Weight: 3]

11. [ID: tabs_ios_glass_opaque] If on iOS 26 and utilizing Liquid Glass in the functional layer, does the tab container use glass effects while keeping the active tab content opaque to prevent glass-on-glass layout issues? [Metric type: Adherence] [Weight: 3]
