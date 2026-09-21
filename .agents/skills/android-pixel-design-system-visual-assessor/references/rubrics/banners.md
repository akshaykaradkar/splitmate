# Banners Rubric - Android Pixel Design System

1.  [ID: banners_placement_hierarchical] Is the banner placed below the top app bar or search bar, or pinned to the top inside a bottom sheet? [Metric type: Hygiene] [Weight: 3]

2.  [ID: banners_notifications_color] If it is a Notifications Banner, does it correctly use the APDS color tokens: `scontainer-low` (`--apds-sys-color-scontainer-low` / light-dark(#efefff, #040f38)) for the container background, `on-surface-variant` (`--apds-sys-color-on-surface-variant` / light-dark(#515981, #a0a9d5)) for the body text, and `primary` (`--apds-sys-color-primary` / light-dark(#0058bc, #85adff)) for titles, icons, and action buttons? [Metric type: Adherence] [Weight: 3]

3.  [ID: banners_promotional_color] If it is a Promotional Banner, does it use the correct APDS background color token `surface-container` (`--apds-sys-color-surface-container` / light-dark(#e4e7ff, #091542)) or high-emphasis `surface-bright` (`--apds-sys-color-surface-bright` / light-dark(#f7f5ff, #182760)), with other text/action tokens aligned with the Notifications Banner? [Metric type: Adherence] [Weight: 3]

4.  [ID: banners_standard_padding] Does the banner use the correct left & right padding token `apds.sys.measurement.space.small1` (**16dp**)? [Metric type: Adherence] [Weight: 3]

5.  [ID: banners_emphasis_geometry] For compact/medium sizes, does the icon size, title-to-body spacing, and button padding match the assigned Emphasis level specification (e.g. High: 48dp icon / 16dp spacing; Medium: 24dp icon / 16dp spacing; Low: 24dp icon / 12dp spacing; Lowest: 48dp height)? [Metric type: Adherence] [Weight: 2]

6.  [ID: banners_expanded_geometry] For expanded window size classes, does a one-line banner feature a height of 60dp (`apds.sys.measurement.size.large1`) and full-width placement? [Metric type: Adherence] [Weight: 2]

7.  [ID: banners_bottom_sheet_alignment] If the banner is pinned inside a bottom sheet, is the spacing between the banner and the gesture bar exactly 16dp (`apds.sys.measurement.space.small1`)? [Metric type: Adherence] [Weight: 3]

8.  [ID: banners_dismissal_element] Does the banner contain at most one type of dismissal element, featuring either action text buttons or a close icon button, but not both? [Metric type: Hygiene] [Weight: 3]

9.  [ID: banners_action_placement] If action text buttons are present, is the secondary action placed to the left of the primary confirming action? [Metric type: Hygiene] [Weight: 2]

10. [ID: banners_max_one_visible] Does the screen contain at most one visible banner at any time? [Metric type: Hygiene] [Weight: 3]
