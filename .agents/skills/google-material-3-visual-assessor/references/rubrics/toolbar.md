# Toolbar

1.  [ID: toolbar_floating_shape_token] If a floating toolbar is present, does its container utilize the fully-rounded pill shape (`--md-sys-shape-corner-full` / `max(50cqw, 50cqh)`)? [Metric type: Adherence] [Weight: 5]

2.  [ID: toolbar_docked_straight_corners] If a docked toolbar is present, does its container have straight, flush corners without any rounding (`--md-sys-shape-corner-none`) to avoid implying it can float or expand? [Metric type: Adherence] [Weight: 5]

3.  [ID: toolbar_floating_drop_shadow] If a floating toolbar is present, does the container utilize an elevation drop shadow (`--md-sys-elevation-level2` or higher) to visually separate it from background content, unless the underlying content is already highly visually distinct? [Metric type: Hygiene] [Weight: 3]

4.  [ID: toolbar_simultaneous_nav] Are bottom-aligned toolbars and bottom navigation bars not displayed on the screen at the exact same time? [Metric type: Hygiene] [Weight: 10]

5.  [ID: toolbar_touch_target_size] Do all interactive elements inside the toolbar container maintain a minimum 48x48dp accessible touch target size? [Metric type: Hygiene] [Weight: 5]

6.  [ID: toolbar_floating_button_shape] Are square or sharp-cornered buttons avoided inside a floating toolbar to prevent visual conflict with the fully-rounded container shape? [Metric type: Hygiene] [Weight: 3]

7.  [ID: toolbar_container_height] Does the toolbar container default to a height of 64dp, ensuring comfortable vertical centering of controls? [Metric type: Adherence] [Weight: 3]
