# Toolbar

1.  [ID: toolbar_floating_shape_token] If a floating toolbar is present, does it comply with the specification: sitting as a pill-shaped container with fully rounded corners (`--droid-sys-shape-corner-full` / `max(50cqw, 50cqh)`)? [Metric type: Adherence] [Weight: 3]

2.  [ID: toolbar_floating_drop_shadow] If a floating toolbar is present, does the container utilize a prominent drop shadow corresponding to `--droid-sys-elevation-level3` (6px) or `--droid-sys-elevation-level4` (8px) to separate it from background content? [Metric type: Adherence] [Weight: 3]

3.  [ID: toolbar_simultaneous_nav] Are toolbars and bottom navigation bars not displayed on the screen at the exact same time? [Metric type: Hygiene] [Weight: 5]

4.  [ID: toolbar_container_height] Does the toolbar container height comply with the standard 64dp specification? [Metric type: Adherence] [Weight: 5]

5.  [ID: toolbar_container_color] Is the toolbar container's background styled with `--droid-sys-color-surface` (light-dark(`#fdfcfb`, `#1f1f1f`)) or `--droid-sys-color-surface-variant` (light-dark(`#e1e3e1`, `#444746`))? [Metric type: Adherence] [Weight: 3]

6.  [ID: toolbar_motion_animation] If auditing the toolbar's enter/exit or transition code or behavior, does it utilize the premium duration `--droid-sys-motion-duration-250` (250ms) or `--droid-sys-motion-duration-300` (300ms) paired with `--droid-sys-motion-easing-emphasized` (`cubic-bezier(0.2, 0.0, 0.0, 1.0)`) easing? [Requires Code Inspection] [Metric type: Adherence] [Weight: 5]
